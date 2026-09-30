package com.boost.your.srt.macro

import kotlinx.serialization.Serializable

/**
 * Configuration for one overlay macro button. "E" and "G" each have their own
 * independent instance, persisted separately in DataStore.
 */
@Serializable
data class MacroButtonConfig(
    val id: String,                         // "E" or "G"
    val isEnabled: Boolean = false,
    val posX: Int = 100,                    // screen X pixels
    val posY: Int = 500,                    // screen Y pixels
    val size: Int = 60,                     // dp
    val alpha: Float = 1.0f,                // 0.0 - 1.0 opacity
    val triggerDurationMs: Long = 5L,       // min 5ms, click duration
    val repeatIntervalMs: Long = 5L,        // min 5ms, interval between taps
    val targetX: Int = 0,                   // paired target X on screen
    val targetY: Int = 0,                   // paired target Y on screen
    val isPaired: Boolean = false,
    val label: String = id,                 // "E" or "G"
    val isLocked: Boolean = false           // locked = no dragging
) {
    /** Returns a copy with every value clamped into its legal range. */
    fun normalized(): MacroButtonConfig {
        val range = sizeRangeFor(id)
        return copy(
            size = size.coerceIn(range.first, range.last),
            alpha = alpha.coerceIn(0f, 1f),
            triggerDurationMs = triggerDurationMs.coerceAtLeast(MIN_MS),
            repeatIntervalMs = repeatIntervalMs.coerceAtLeast(MIN_MS),
            posX = posX.coerceAtLeast(0),
            posY = posY.coerceAtLeast(0)
        )
    }

    companion object {
        const val MIN_MS = 5L

        /** E: 40dp..80dp (small-medium). G: 60dp..120dp (medium-large). */
        fun sizeRangeFor(id: String): IntRange = if (id == "G") 60..120 else 40..80

        fun default(id: String): MacroButtonConfig = MacroButtonConfig(
            id = id,
            size = if (id == "G") 90 else 60,
            posX = if (id == "G") 300 else 100,
            posY = 500,
            label = id
        ).normalized()
    }
}
