package com.boost.your.srt.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.boost.your.srt.R
import kotlin.math.hypot

/**
 * Floating [ logo | FPS ] pill. Drag anywhere; tap the logo to toggle the control panel;
 * long-press for the "Stop Service" option.
 */
@SuppressLint("ViewConstructor")
class MiniBubbleView(context: Context) : LinearLayout(context) {

    var onLogoClick: () -> Unit = {}
    var onLongPress: () -> Unit = {}
    /** Called once after a drag ends with the final window position. */
    var onMoved: (x: Int, y: Int) -> Unit = { _, _ -> }

    private val density = resources.displayMetrics.density
    private val logo: FrameLayout
    private val fpsText: TextView
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var longPressed = false

    private val longPressRunnable = Runnable {
        if (!dragging) {
            longPressed = true
            onLongPress()
        }
    }

    private fun dp(v: Int) = (v * density).toInt()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(3), dp(3), dp(10), dp(3))
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(Color.parseColor("#E60D1220"))
            setStroke(dp(1), Color.parseColor("#1E2A42"))
        }

        // Logo circle with purple glow ring (38dp)
        logo = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A0F1A"))
                setStroke(dp(2), Color.parseColor("#9B59B6"))
            }
            elevation = dp(4).toFloat()
        }
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        logo.addView(icon, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        addView(logo, LayoutParams(dp(38), dp(38)))

        fpsText = TextView(context).apply {
            text = "--"
            setTextColor(Color.parseColor("#00E87A"))
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            minWidth = dp(44)
        }
        addView(fpsText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
    }

    fun updateFps(fps: Int) {
        fpsText.text = fps.toString()
        fpsText.setTextColor(
            when {
                fps > 60 -> Color.parseColor("#00E87A")   // green
                fps >= 30 -> Color.parseColor("#FFB020")  // amber
                else -> Color.parseColor("#FF3B5C")       // red
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val lp = layoutParams as? WindowManager.LayoutParams ?: return false
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = e.rawX
                downRawY = e.rawY
                startX = lp.x
                startY = lp.y
                dragging = false
                longPressed = false
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downRawX
                val dy = e.rawY - downRawY
                if (!dragging && hypot(dx, dy) > slop) {
                    dragging = true
                    removeCallbacks(longPressRunnable)
                }
                if (dragging) {
                    val dm = resources.displayMetrics
                    lp.x = (startX + dx).toInt().coerceIn(0, (dm.widthPixels - width).coerceAtLeast(0))
                    lp.y = (startY + dy).toInt().coerceIn(0, (dm.heightPixels - height).coerceAtLeast(0))
                    runCatching { wm.updateViewLayout(this, lp) }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)
                if (dragging) {
                    onMoved(lp.x, lp.y)
                } else if (!longPressed && e.x <= logo.right) {
                    onLogoClick()
                }
                dragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}
