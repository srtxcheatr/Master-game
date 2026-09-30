package com.boost.your.srt.macro

import com.boost.your.srt.shizuku.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs the looping tap for the E and G buttons. Each button has its own Job, so starting or
 * stopping one never touches the other. Taps are injected with Shizuku `input tap x y`.
 *
 * Note: every tap spawns a shell command, so the real tap rate is bounded by how fast the
 * device can run `input tap` (typically well above the 5ms floor of the delays below).
 */
class MacroEngine(private val scope: CoroutineScope) {

    private var eJob: Job? = null
    private var gJob: Job? = null

    /** Loops taps at the paired target until cancelled. */
    fun startLoop(config: MacroButtonConfig): Job {
        return scope.launch(Dispatchers.IO) {
            while (isActive && config.isPaired) {
                // Tap at the paired target coordinates
                ShizukuHelper.inputTap(config.targetX, config.targetY)
                // Wait triggerDurationMs (minimum 5ms)
                delay(maxOf(MacroButtonConfig.MIN_MS, config.triggerDurationMs))
                // Wait repeatIntervalMs before the next tap (minimum 5ms)
                delay(maxOf(MacroButtonConfig.MIN_MS, config.repeatIntervalMs))
            }
        }
    }

    fun startE(config: MacroButtonConfig) { eJob?.cancel(); eJob = startLoop(config) }
    fun startG(config: MacroButtonConfig) { gJob?.cancel(); gJob = startLoop(config) }
    fun stopE() { eJob?.cancel(); eJob = null }
    fun stopG() { gJob?.cancel(); gJob = null }

    fun start(config: MacroButtonConfig) = if (config.id == "G") startG(config) else startE(config)
    fun stop(id: String) = if (id == "G") stopG() else stopE()

    fun isRunning(id: String): Boolean = (if (id == "G") gJob else eJob)?.isActive == true

    fun stopAll() { stopE(); stopG() }
}
