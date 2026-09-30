package com.boost.your.srt.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * Real system metrics only. Anything the platform will not expose to a normal app is
 * returned as null and shown as "N/A" in the UI - values are never fabricated.
 */
data class SystemMetrics(
    val cpuPercent: Int?,          // null = N/A (/proc/stat is blocked for apps on Android 8+ on most devices)
    val ramUsedGb: Float,
    val ramTotalGb: Float,
    val tempCelsius: Float?,       // null = N/A
    val batteryPercent: Int,
    val batteryMinutes: Int?,      // null = N/A
    val batteryCharging: Boolean,
    val wifiStrength: String,      // "Strong" / "Medium" / "Weak" / "N/A"
    val romUsedGb: Float,
    val romTotalGb: Float,
    val gpuPercent: Int?           // null = N/A (no public API)
)

class SystemMetricsReader(private val context: Context) {

    private val appContext = context.applicationContext
    private var lastTotal = 0L
    private var lastIdle = 0L

    fun read(): SystemMetrics {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val gb = 1024f * 1024f * 1024f
        val ramTotal = memInfo.totalMem / gb
        val ramUsed = (memInfo.totalMem - memInfo.availMem) / gb

        val battery: Intent? = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battPct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val battTempTenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE

        val temp = readTemperature(battTempTenths)

        val stat = StatFs(Environment.getDataDirectory().path)
        val romTotal = stat.totalBytes / gb
        val romUsed = romTotal - stat.availableBytes / gb

        return SystemMetrics(
            cpuPercent = readCpuPercent(),
            ramUsedGb = ramUsed,
            ramTotalGb = ramTotal,
            tempCelsius = temp,
            batteryPercent = battPct,
            batteryMinutes = estimateBatteryMinutes(charging),
            batteryCharging = charging,
            wifiStrength = readWifiStrength(),
            romUsedGb = romUsed,
            romTotalGb = romTotal,
            gpuPercent = null
        )
    }

    /** CPU load from two /proc/stat samples. Returns null when the file cannot be read. */
    private fun readCpuPercent(): Int? {
        return try {
            val line = File("/proc/stat").bufferedReader().use { it.readLine() } ?: return null
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 8 || parts[0] != "cpu") return null
            val values = parts.drop(1).take(8).map { it.toLong() }
            val idle = values[3] + values[4]
            val total = values.sum()
            val dTotal = total - lastTotal
            val dIdle = idle - lastIdle
            val first = lastTotal == 0L
            lastTotal = total
            lastIdle = idle
            if (first || dTotal <= 0L) null else (((dTotal - dIdle) * 100) / dTotal).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            null
        }
    }

    /** Prefers a readable thermal zone, falls back to the battery sensor (always available). */
    private fun readTemperature(batteryTenths: Int): Float? {
        try {
            val zones = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
            zones?.sortedBy { it.name }?.forEach { zone ->
                val type = runCatching { File(zone, "type").readText().trim().lowercase() }.getOrNull() ?: return@forEach
                if (type.contains("cpu") || type.contains("soc") || type.contains("tsens") || type.contains("cpuss")) {
                    val raw = runCatching { File(zone, "temp").readText().trim().toFloat() }.getOrNull()
                    if (raw != null && raw > 0f) {
                        val c = if (raw > 1000f) raw / 1000f else raw
                        if (c in 5f..120f) return c
                    }
                }
            }
        } catch (_: Exception) { }
        return if (batteryTenths != Int.MIN_VALUE) batteryTenths / 10f else null
    }

    /** Minutes until full (charging) or empty (discharging); null when the device cannot tell. */
    private fun estimateBatteryMinutes(charging: Boolean): Int? {
        val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (charging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val ms = bm.computeChargeTimeRemaining()
            if (ms > 0) return (ms / 60_000L).toInt()
        }
        val counterUah = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val currentUa = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (!charging && counterUah > 0 && currentUa != 0L && currentUa != Long.MIN_VALUE) {
            val hours = counterUah.toDouble() / kotlin.math.abs(currentUa).toDouble()
            val minutes = (hours * 60).toInt()
            if (minutes in 1..(60 * 99)) return minutes
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun readWifiStrength(): String {
        return try {
            val wm = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            val rssi = info?.rssi ?: return "N/A"
            if (info.networkId == -1 || rssi <= -127) return "N/A"
            when {
                rssi > -50 -> "Strong"
                rssi > -70 -> "Medium"
                else -> "Weak"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }
}
