package com.boost.your.srt.display

import android.content.Context

data class DisplayConfig(
    val width: Int,
    val height: Int,
    val density: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Persists the user's ORIGINAL display configuration (taken before the first stretch).
 * The backup is written once and is never overwritten by modified values.
 */
class DisplayBackup(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("display_backup", Context.MODE_PRIVATE)

    /** Saves [config] only if no backup exists yet. Returns true if it was written. */
    fun saveOriginalIfAbsent(config: DisplayConfig): Boolean {
        if (hasBackup()) return false
        // commit() so the backup is on disk before any wm command changes the display
        return prefs.edit()
            .putInt("orig_width", config.width)
            .putInt("orig_height", config.height)
            .putInt("orig_density", config.density)
            .putLong("orig_timestamp", config.timestamp)
            .commit()
    }

    fun hasBackup(): Boolean = prefs.contains("orig_width")

    fun getOriginal(): DisplayConfig? {
        if (!hasBackup()) return null
        return DisplayConfig(
            width = prefs.getInt("orig_width", 0),
            height = prefs.getInt("orig_height", 0),
            density = prefs.getInt("orig_density", 0),
            timestamp = prefs.getLong("orig_timestamp", 0)
        )
    }

    fun clearBackup() {
        prefs.edit().clear().apply()
    }
}
