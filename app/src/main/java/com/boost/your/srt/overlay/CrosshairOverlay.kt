package com.boost.your.srt.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Full-screen transparent overlay used to pick the tap target for a macro button.
 * Touch or drag anywhere to move the crosshair, then press "SET HERE".
 * Coordinates reported are real screen pixels (what `input tap x y` expects).
 */
@SuppressLint("ViewConstructor")
class CrosshairOverlay(
    context: Context,
    private val buttonId: String,
    initialX: Int,
    initialY: Int,
    private val onConfirm: (x: Int, y: Int) -> Unit,
    private val onCancel: () -> Unit
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private val accent = if (buttonId == "G") Color.parseColor("#FFB020") else Color.parseColor("#00C8FF")
    private var cx = initialX.toFloat()
    private var cy = initialY.toFloat()
    private val coordText: TextView

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        strokeWidth = 3f * density
        style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    /** Window params for a full-screen, touchable overlay. Screen coordinates == view coordinates. */
    fun layoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

    private fun dp(v: Int) = (v * density).toInt()

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.parseColor("#33000000"))

        val hint = TextView(context).apply {
            text = "PAIR BUTTON $buttonId\nDrag the crosshair onto the control this button should tap"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#E60D1220"))
                setStroke(dp(1), accent)
            }
        }
        addView(hint, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(48)
        })

        coordText = TextView(context).apply {
            setTextColor(accent)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#CC060A14"))
            }
        }
        addView(coordText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val cancel = Button(context).apply {
            text = "CANCEL"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#E6222B3F"))
            }
            setOnClickListener { onCancel() }
        }
        val set = Button(context).apply {
            text = "SET HERE"
            setTextColor(Color.parseColor("#060A14"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(accent)
            }
            setOnClickListener {
                val loc = IntArray(2)
                getLocationOnScreen(loc)
                onConfirm((cx + loc[0]).toInt(), (cy + loc[1]).toInt())
            }
        }
        buttons.addView(cancel, LinearLayout.LayoutParams(dp(130), dp(50)).apply { marginEnd = dp(12) })
        buttons.addView(set, LinearLayout.LayoutParams(dp(150), dp(50)))
        addView(buttons, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(40)
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cx <= 0f && cy <= 0f) {
            cx = w / 2f
            cy = h / 2f
        }
        cx = cx.coerceIn(0f, w.toFloat())
        cy = cy.coerceIn(0f, h.toFloat())
        updateLabel()
    }

    private fun updateLabel() {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        coordText.text = "X:${(cx + loc[0]).toInt()}  Y:${(cy + loc[1]).toInt()}"
        val lp = coordText.layoutParams as LayoutParams
        lp.gravity = Gravity.TOP or Gravity.START
        val labelW = coordText.width.takeIf { it > 0 } ?: dp(120)
        lp.leftMargin = (cx - labelW / 2f).toInt().coerceIn(0, (width - labelW).coerceAtLeast(0))
        lp.topMargin = (cy - dp(64)).toInt().coerceAtLeast(dp(4))
        coordText.layoutParams = lp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val arm = 28f * density
        canvas.drawLine(cx - arm, cy, cx + arm, cy, linePaint)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, linePaint)
        canvas.drawCircle(cx, cy, 16f * density, ringPaint)
        canvas.drawCircle(cx, cy, 3f * density, dotPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                cx = e.x.coerceIn(0f, width.toFloat())
                cy = e.y.coerceIn(0f, height.toFloat())
                updateLabel()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}
