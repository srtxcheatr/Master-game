package com.boost.your.srt.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import com.boost.your.srt.util.PermissionHelper

data class GameInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    /** Duration of the most recent foreground session in seconds; null if usage access is missing or no session found. */
    val lastSessionSeconds: Long?
)

/** Finds installed games and reads their real last-session time from UsageStats. */
class GameProfileManager(context: Context) {

    private val context = context.applicationContext
    private val pm = this.context.packageManager

    /** Blocking; call from a background dispatcher. */
    @Suppress("DEPRECATION")
    fun getInstalledGames(): List<GameInfo> {
        val sessions = if (PermissionHelper.hasUsageStatsAccess(context)) readLastSessions() else emptyMap()
        return pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { isGame(it) }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                GameInfo(
                    packageName = app.packageName,
                    name = app.loadLabel(pm).toString(),
                    icon = app.loadIcon(pm),
                    lastSessionSeconds = sessions[app.packageName]
                )
            }
            .sortedWith(compareByDescending<GameInfo> { it.lastSessionSeconds != null }.thenBy { it.name.lowercase() })
            .toList()
    }

    @Suppress("DEPRECATION")
    private fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME || (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    fun launch(packageName: String): Boolean {
        val intent = pm.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** Pairs foreground/background events from the last 7 days into sessions; keeps the newest per package. */
    @Suppress("DEPRECATION")
    private fun readLastSessions(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - 7L * 24 * 3600 * 1000, end)
        val resumedAt = HashMap<String, Long>()
        val result = HashMap<String, Long>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> resumedAt[e.packageName] = e.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = resumedAt.remove(e.packageName) ?: continue
                    val secs = (e.timeStamp - start) / 1000
                    if (secs > 0) result[e.packageName] = secs
                }
            }
        }
        return result
    }
}
