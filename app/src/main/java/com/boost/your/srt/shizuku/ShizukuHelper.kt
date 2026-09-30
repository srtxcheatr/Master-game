package com.boost.your.srt.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.boost.your.srt.BuildConfig
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * All privileged commands go through here.
 *
 * Commands run in a Shizuku UserService (public API, no reflection). Every call is
 * blocking, so call [exec] and its helpers from a background dispatcher only.
 * A failed command is always reported through a non-zero [CmdResult.exitCode];
 * callers must never show success unless exitCode == 0.
 */
object ShizukuHelper {

    data class CmdResult(val exitCode: Int, val output: String, val error: String?) {
        val ok: Boolean get() = exitCode == 0
    }

    private const val REQUEST_CODE = 1001
    private const val BIND_TIMEOUT_SEC = 6L

    @Volatile private var service: IShellService? = null
    @Volatile private var bindLatch: CountDownLatch? = null
    private val bindLock = Any()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IShellService.Stub.asInterface(binder)
            bindLatch?.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, ShellUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    fun isGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    fun requestPermission(onResult: (Boolean) -> Unit) {
        if (!isAvailable()) {
            onResult(false)
            return
        }
        if (isGranted()) {
            onResult(true)
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                Shizuku.removeRequestPermissionResultListener(this)
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Throwable) {
            Shizuku.removeRequestPermissionResultListener(listener)
            onResult(false)
        }
    }

    /** Binds (once) and returns the privileged shell service, or null if it cannot be reached. */
    private fun ensureService(): IShellService? {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        synchronized(bindLock) {
            service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
            if (!isAvailable() || !isGranted()) return null
            val latch = CountDownLatch(1)
            bindLatch = latch
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (e: Throwable) {
                return null
            }
            latch.await(BIND_TIMEOUT_SEC, TimeUnit.SECONDS)
            return service
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(userServiceArgs, connection, true)
        } catch (_: Throwable) { }
        service = null
    }

    fun exec(command: String): CmdResult {
        if (!isAvailable()) return CmdResult(-1, "", "Shizuku is not running")
        if (!isGranted()) return CmdResult(-1, "", "Shizuku permission not granted")
        val svc = ensureService() ?: return CmdResult(-1, "", "Could not connect to Shizuku service")
        return try {
            val r = svc.exec(command)
            val code = r.getOrNull(0)?.toIntOrNull() ?: -1
            CmdResult(code, r.getOrNull(1).orEmpty(), r.getOrNull(2)?.ifEmpty { null })
        } catch (e: Throwable) {
            service = null
            CmdResult(-1, "", e.message ?: "Shizuku command failed")
        }
    }

    // Convenience wrappers
    fun stretchScreen(w: Int, h: Int) = exec("wm size ${w}x${h}")
    fun setDensity(dpi: Int) = exec("wm density $dpi")
    fun resetScreen() = exec("wm size reset && wm density reset")
    fun killBackground() = exec("am kill-all")
    fun inputTap(x: Int, y: Int) = exec("input tap $x $y")
    fun setSetting(ns: String, key: String, value: String) = exec("settings put $ns $key $value")
}
