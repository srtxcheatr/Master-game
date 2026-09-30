package com.boost.your.srt.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.boost.your.srt.macro.MacroButtonConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.boostDataStore: DataStore<Preferences> by preferencesDataStore(name = "boost_master_settings")

/** One persisted setting: exposes a Flow, a suspend getter and a suspend setter. */
class Setting<T>(private val key: Preferences.Key<T>, private val default: T) {
    fun flow(context: Context): Flow<T> =
        context.applicationContext.boostDataStore.data.map { it[key] ?: default }

    suspend fun get(context: Context): T = flow(context).first()

    suspend fun set(context: Context, value: T) {
        context.applicationContext.boostDataStore.edit { it[key] = value }
    }
}

/**
 * All persistent settings live here (Jetpack DataStore / Preferences).
 * Usage: `AppDataStore.performanceMode.get(ctx)` / `.set(ctx, "balanced")` / `.flow(ctx)`.
 */
object AppDataStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Performance ---
    val performanceMode = Setting(stringPreferencesKey("PERFORMANCE_MODE"), "performance") // power_saving|balanced|performance

    // --- GPU ---
    val gpuAfLevel = Setting(intPreferencesKey("GPU_AF_LEVEL"), 0)          // 0,2,4,8,16
    val gpuTexture = Setting(floatPreferencesKey("GPU_TEXTURE"), 0.5f)      // 0.0-1.0

    // --- Touch ---
    val touchMode = Setting(stringPreferencesKey("TOUCH_MODE"), "common")   // common|fps|moba|custom
    val touchSensitivity = Setting(floatPreferencesKey("TOUCH_SENSITIVITY"), 0f)
    val touchSwipe = Setting(floatPreferencesKey("TOUCH_SWIPE"), 0f)
    val touchAccuracy = Setting(floatPreferencesKey("TOUCH_ACCURACY"), 0f)

    // --- Network ---
    val networkDual = Setting(booleanPreferencesKey("NETWORK_DUAL"), false)
    val networkAccel = Setting(booleanPreferencesKey("NETWORK_ACCEL"), false)

    // --- Anti-interference ---
    val antiCallReject = Setting(booleanPreferencesKey("ANTI_CALL_REJECT"), false)
    val antiCallHold = Setting(booleanPreferencesKey("ANTI_CALL_HOLD"), false)
    val antiMistouch = Setting(booleanPreferencesKey("ANTI_MISTOUCH"), false)
    val antiDragNotif = Setting(booleanPreferencesKey("ANTI_DRAG_NOTIF"), false)
    val antiThreeFinger = Setting(booleanPreferencesKey("ANTI_THREE_FINGER"), false)

    // --- Gameplay add-ons ---
    val addonEyeCare = Setting(booleanPreferencesKey("ADDON_EYE_CARE"), false)
    val addonImageStab = Setting(booleanPreferencesKey("ADDON_IMAGE_STAB"), false)
    val addonOffscreen = Setting(booleanPreferencesKey("ADDON_OFFSCREEN"), false)
    val addonBypass = Setting(booleanPreferencesKey("ADDON_BYPASS"), false)
    val addonImmersive = Setting(booleanPreferencesKey("ADDON_IMMERSIVE"), false)
    val graphicEnhance = Setting(stringPreferencesKey("GRAPHIC_ENHANCE"), "classic") // classic|colorful|soft|realistic

    // --- Bubble position ---
    val bubbleX = Setting(intPreferencesKey("BUBBLE_X"), 0)
    val bubbleY = Setting(intPreferencesKey("BUBBLE_Y"), 200)

    // --- Sub-screen settings ---
    val notifStyle = Setting(stringPreferencesKey("NOTIF_STYLE"), "banner")          // banner|small|none
    val hideNotifications = Setting(booleanPreferencesKey("HIDE_NOTIFICATIONS"), false)
    val tiltEnabled = Setting(booleanPreferencesKey("TILT_ENABLED"), false)
    val tiltSensitivity = Setting(intPreferencesKey("TILT_SENSITIVITY"), 2)          // 0..4
    val voicePreset = Setting(stringPreferencesKey("VOICE_PRESET"), "off")           // off|deep|bright|robot

    // --- Extra runtime toggles used by the overlay panel ---
    val gameBoost = Setting(booleanPreferencesKey("GAME_BOOST"), false)
    val dndMode = Setting(booleanPreferencesKey("DND_MODE"), false)
    val lastSessionSeconds = Setting(stringPreferencesKey("LAST_SESSIONS_JSON"), "{}") // pkg -> seconds JSON

    // --- Macro buttons (E and G are stored under separate keys, fully independent) ---
    private val macroEKey = stringPreferencesKey("MACRO_E_CONFIG")
    private val macroGKey = stringPreferencesKey("MACRO_G_CONFIG")

    private fun macroKey(id: String) = if (id == "G") macroGKey else macroEKey

    fun macroConfigFlow(context: Context, id: String): Flow<MacroButtonConfig> =
        context.applicationContext.boostDataStore.data.map { prefs -> parseMacro(id, prefs[macroKey(id)]) }

    suspend fun getMacroConfig(context: Context, id: String): MacroButtonConfig =
        macroConfigFlow(context, id).first()

    suspend fun saveMacroConfig(context: Context, config: MacroButtonConfig) {
        val safe = config.normalized()
        context.applicationContext.boostDataStore.edit { it[macroKey(safe.id)] = json.encodeToString(safe) }
    }

    /**
     * Atomic read-modify-write of ONE button's config. Rapid taps on +/- buttons can never lose
     * an update, and E and G are always written under their own key.
     */
    suspend fun updateMacroConfig(context: Context, id: String, transform: (MacroButtonConfig) -> MacroButtonConfig) {
        context.applicationContext.boostDataStore.edit { prefs ->
            val current = parseMacro(id, prefs[macroKey(id)])
            val next = transform(current).copy(id = id, label = id).normalized()
            prefs[macroKey(id)] = json.encodeToString(next)
        }
    }

    suspend fun resetMacroConfig(context: Context, id: String): MacroButtonConfig {
        val def = MacroButtonConfig.default(id)
        saveMacroConfig(context, def)
        return def
    }

    private fun parseMacro(id: String, raw: String?): MacroButtonConfig {
        if (raw.isNullOrBlank()) return MacroButtonConfig.default(id)
        return try {
            // The stored id is authoritative for its slot; never let E data leak into G.
            json.decodeFromString<MacroButtonConfig>(raw).copy(id = id, label = id).normalized()
        } catch (e: Exception) {
            MacroButtonConfig.default(id)
        }
    }

    /** Wipes every stored setting except the macro configs (used by "Restore Default"). */
    suspend fun restoreDefaults(context: Context) {
        context.applicationContext.boostDataStore.edit { prefs ->
            val keep = setOf(macroEKey.name, macroGKey.name, bubbleXName, bubbleYName)
            @Suppress("UNCHECKED_CAST")
            prefs.asMap().keys.filter { it.name !in keep }.forEach { prefs.remove(it as Preferences.Key<Any>) }
        }
    }

    private const val bubbleXName = "BUBBLE_X"
    private const val bubbleYName = "BUBBLE_Y"
}
