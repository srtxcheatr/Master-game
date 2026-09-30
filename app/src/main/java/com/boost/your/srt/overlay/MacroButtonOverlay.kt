package com.boost.your.srt.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.boost.your.srt.macro.MacroButtonConfig
import com.boost.your.srt.macro.MacroEngine
import kotlin.math.hypot

/**
 * One draggable on-screen macro button (E or G). Each instance owns its OWN
 * WindowManager.LayoutParams, so updating one button can never touch the other.
 *
 * Modes
 *  - Unlocked (edit): drag to reposition, dashed border shown, macro does NOT fire.
 *  - Locked (play):   no border, cannot be dragged; press-and-hold runs the tap loop at the paired
 *                     target, release stops it. Only the button's own small window captures touches,
 *                     everything else on screen keeps reaching the game.
 * The small lock icon in the top-right corner toggles between the two modes.
 * Taps are injected with Shizuku (`input tap x y`) by [MacroEngine].
 */
@SuppressLint("ViewConstructor")
class MacroButtonOverlay(
    context: Context,
    initial: MacroButtonConfig,
    private val engine: MacroEngine,
    private val onPositionCommitted: (id: String, x: Int, y: Int) -> Unit,
    private val onLockToggled: (id: String, locked: Boolean) -> Unit
) : View(context) {

    var config: MacroButtonConfig = initial.normalized()
        private set

    /** This button's private window params. */
    val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        0, 0,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private val density = resources.displayMetrics.density
    private val accent = if (config.id == "G") Color.parseColor("#FFB020") else Color.parseColor("#00C8FF")

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f * density
        pathEffect = DashPathEffect(floatArrayOf(10f * density, 6f * density), 0f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#060A14")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val lockBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CC060A14") }
    private val lockFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val lockFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private var pressed = false
    private var dragging = false
    private var lockTouch = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0

    init {
        applyConfigToParams()
    }

    private fun sizePx() = (config.size * density).toInt()

    private fun applyConfigToParams() {
        val px = sizePx()
        params.width = px
        params.height = px
        val dm = resources.displayMetrics
        params.x = config.posX.coerceIn(0, (dm.widthPixels - px).coerceAtLeast(0))
        params.y = config.posY.coerceIn(0, (dm.heightPixels - px).coerceAtLeast(0))
    }

    /** Applies a new config (size, alpha, position, lock...) to this button only and updates the live window. */
    fun applyConfig(wm: WindowManager, newConfig: MacroButtonConfig) {
        config = newConfig.normalized()
        // Do not fight the finger while it is dragging this very button.
        if (!dragging) applyConfigToParams()
        if (isAttachedToWindow) runCatching { wm.updateViewLayout(this, params) }
        if (!config.isLocked) {
            engine.stop(config.id)
            pressed = false
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f - 4f * density
        val a = (config.alpha * 255).toInt().coerceIn(0, 255)

        if (pressed) {
            glow.color = accent
            glow.alpha = (a * 0.6f).toInt()
            glow.strokeWidth = 5f * density
            canvas.drawCircle(cx, cy, r + 1.5f * density, glow)
        }
        fill.color = accent
        fill.alpha = if (pressed) a else (a * 0.9f).toInt()
        canvas.drawCircle(cx, cy, r, fill)

        labelPaint.textSize = r * 0.95f
        labelPaint.alpha = a
        val baseline = cy - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(config.label, cx, baseline, labelPaint)

        if (!config.isLocked) {
            border.alpha = 255
            canvas.drawCircle(cx, cy, r, border)
        }
        drawLockIcon(canvas)
    }

    private fun lockCenterX() = width * 0.80f
    private fun lockCenterY() = height * 0.20f
    private fun lockRadius() = maxOf(width * 0.17f, 9f * density)

    private fun drawLockIcon(canvas: Canvas) {
        val cx = lockCenterX()
        val cy = lockCenterY()
        val r = lockRadius()
        canvas.drawCircle(cx, cy, r, lockBg)
        val bw = r * 0.85f
        val bh = r * 0.6f
        val top = cy - bh * 0.1f
        canvas.drawRoundRect(cx - bw / 2f, top, cx + bw / 2f, top + bh, 2f * density, 2f * density, lockFill)
        val shackleR = bw * 0.32f
        if (config.isLocked) {
            canvas.drawArc(cx - shackleR, top - shackleR * 1.5f, cx + shackleR, top + shackleR * 0.5f, 180f, 180f, false, lockFg)
        } else {
            // open shackle, shifted to the right
            canvas.drawArc(cx - shackleR + shackleR * 0.9f, top - shackleR * 1.5f, cx + shackleR + shackleR * 0.9f, top + shackleR * 0.5f, 180f, 180f, false, lockFg)
        }
    }

    private fun inLockIcon(x: Float, y: Float) = hypot(x - lockCenterX(), y - lockCenterY()) <= lockRadius() * 1.3f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (inLockIcon(e.x, e.y)) {
                    lockTouch = true
                    return true
                }
                lockTouch = false
                if (config.isLocked) {
                    // PointerDown -> start the macro loop for THIS button
                    if (!config.isPaired) {
                        Toast.makeText(context, "Button ${config.id}: pair a target first", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    pressed = true
                    engine.start(config)
                    invalidate()
                } else {
                    dragging = true
                    downRawX = e.rawX
                    downRawY = e.rawY
                    startX = params.x
                    startY = params.y
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!config.isLocked && dragging) {
                    val dm = resources.displayMetrics
                    params.x = (startX + (e.rawX - downRawX)).toInt().coerceIn(0, (dm.widthPixels - width).coerceAtLeast(0))
                    params.y = (startY + (e.rawY - downRawY)).toInt().coerceIn(0, (dm.heightPixels - height).coerceAtLeast(0))
                    runCatching { wm.updateViewLayout(this, params) }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (lockTouch) {
                    lockTouch = false
                    if (e.actionMasked == MotionEvent.ACTION_UP && inLockIcon(e.x, e.y)) {
                        engine.stop(config.id)
                        pressed = false
                        onLockToggled(config.id, !config.isLocked)
                    }
                    return true
                }
                if (pressed) {
                    // PointerUp -> stop the macro loop
                    engine.stop(config.id)
                    pressed = false
                    invalidate()
                }
                if (dragging) {
                    dragging = false
                    onPositionCommitted(config.id, params.x, params.y)
                }
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}
