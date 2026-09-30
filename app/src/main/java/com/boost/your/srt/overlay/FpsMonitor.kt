package com.boost.your.srt.overlay

import android.content.Context
import android.view.Choreographer

/**
 * Counts frames delivered by the Choreographer on the thread it was started from (main thread
 * in OverlayService) and reports the rate once per second.
 *
 * This measures the display / UI-thread frame cadence seen by this process. Android does not
 * let a normal app read another app's (the game's) render FPS.
 */
class FpsMonitor {
    private var frameCount = 0
    private var lastNs = System.nanoTime()
    private var running = false

    var currentFps: Int = 0
        private set
    var onUpdate: (Int) -> Unit = {}

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            frameCount++
            val elapsed = frameTimeNanos - lastNs
            if (elapsed >= 1_000_000_000L) {
                currentFps = (frameCount * 1_000_000_000L / elapsed).toInt()
                frameCount = 0
                lastNs = frameTimeNanos
                onUpdate(currentFps)
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        frameCount = 0
        lastNs = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(callback)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(callback)
    }
}
