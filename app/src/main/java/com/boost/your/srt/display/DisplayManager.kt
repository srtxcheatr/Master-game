package com.boost.your.srt.display

import android.content.Context
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import com.boost.your.srt.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class StretchResult {
    object Success : StretchResult()
    object Restored : StretchResult()
    object ShizukuUnavailable : StretchResult()
    data class Failed(val reason: String) : StretchResult()
}

class DisplayManager(context: Context) {

    private val context = context.applicationContext
    val backup = DisplayBackup(this.context)

    /**
     * Current display config in the device's NATURAL orientation, which is what
     * `wm size WxH` expects (rotation would otherwise swap width and height).
     */
    @Suppress("DEPRECATION")
    fun readCurrentConfig(): DisplayConfig {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val rotated = when (wm.defaultDisplay.rotation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> true
            else -> false
        }
        return DisplayConfig(
            width = if (rotated) metrics.heightPixels else metrics.widthPixels,
            height = if (rotated) metrics.widthPixels else metrics.heightPixels,
            density = metrics.densityDpi
        )
    }

    /** True when the device's natural orientation is portrait (phones), false for natural-landscape tablets. */
    fun isNaturalPortrait(): Boolean {
        val c = readCurrentConfig()
        return c.height >= c.width
    }

    /** True when a backup exists and the live display differs from it. */
    fun isStretchActive(): Boolean {
        val original = backup.getOriginal() ?: return false
        val cur = readCurrentConfig()
        return cur.width != original.width || cur.height != original.height || cur.density != original.density
    }

    /**
     * Applies size + density. The ORIGINAL config is saved first (only if absent),
     * so the backup can never contain already-modified values.
     * Success is returned only when both wm commands exit with code 0.
     */
    suspend fun applyStretch(width: Int, height: Int, density: Int): StretchResult = withContext(Dispatchers.IO) {
        if (width < 200 || height < 200 || density < 72) {
            return@withContext StretchResult.Failed("Invalid values: ${width}x$height @ ${density}dpi")
        }
        if (!ShizukuHelper.isAvailable()) return@withContext StretchResult.ShizukuUnavailable
        if (!ShizukuHelper.isGranted()) return@withContext StretchResult.Failed("Shizuku permission not granted")

        // Step 1: back up the ORIGINAL before touching anything
        val before = readCurrentConfig()
        backup.saveOriginalIfAbsent(before)
        if (!backup.hasBackup()) {
            return@withContext StretchResult.Failed("Could not save a backup of the original display; aborting")
        }

        // Step 2: apply
        val sizeResult = ShizukuHelper.exec("wm size ${width}x$height")
        if (sizeResult.exitCode != 0) {
            return@withContext StretchResult.Failed("wm size failed: ${sizeResult.error ?: sizeResult.output.ifBlank { "exit ${sizeResult.exitCode}" }}")
        }
        val densResult = ShizukuHelper.exec("wm density $density")
        if (densResult.exitCode != 0) {
            // Size changed but density did not: put the size back so we don't leave a half-applied state.
            val rollback = ShizukuHelper.exec("wm size ${before.width}x${before.height}")
            val note = if (rollback.exitCode == 0) "size was rolled back" else "rollback also failed - use Restore Original"
            return@withContext StretchResult.Failed(
                "wm density failed: ${densResult.error ?: densResult.output.ifBlank { "exit ${densResult.exitCode}" }} ($note)"
            )
        }
        StretchResult.Success
    }

    /** Restores the backed-up original. The backup is kept if any command fails so the user can retry. */
    suspend fun restoreOriginal(): StretchResult = withContext(Dispatchers.IO) {
        val original = backup.getOriginal()
            ?: return@withContext StretchResult.Failed("No backup found — cannot restore")
        if (!ShizukuHelper.isAvailable()) return@withContext StretchResult.ShizukuUnavailable
        if (!ShizukuHelper.isGranted()) return@withContext StretchResult.Failed("Shizuku permission not granted")

        val sizeResult = ShizukuHelper.exec("wm size ${original.width}x${original.height}")
        val densResult = ShizukuHelper.exec("wm density ${original.density}")

        if (sizeResult.exitCode == 0 && densResult.exitCode == 0) {
            backup.clearBackup()
            StretchResult.Restored
        } else {
            val why = listOf(sizeResult, densResult)
                .firstOrNull { it.exitCode != 0 }
                ?.let { it.error ?: it.output.ifBlank { "exit ${it.exitCode}" } }
            StretchResult.Failed("Restore failed: $why")
        }
    }

    /** `wm size reset` + `wm density reset` to the hardware defaults. */
    suspend fun resetToDefault(): StretchResult = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isAvailable()) return@withContext StretchResult.ShizukuUnavailable
        if (!ShizukuHelper.isGranted()) return@withContext StretchResult.Failed("Shizuku permission not granted")

        val size = ShizukuHelper.exec("wm size reset")
        val dens = ShizukuHelper.exec("wm density reset")
        if (size.exitCode == 0 && dens.exitCode == 0) {
            backup.clearBackup()
            StretchResult.Restored
        } else {
            val why = listOf(size, dens).firstOrNull { it.exitCode != 0 }
                ?.let { it.error ?: it.output.ifBlank { "exit ${it.exitCode}" } }
            StretchResult.Failed("Reset failed: $why")
        }
    }
}
