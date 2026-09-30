package com.boost.your.srt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary

/**
 * Animated arc gauge. [fraction] is 0..1, or null when the value is unavailable, in which case the
 * gauge shows an empty track and "N/A" rather than an invented number.
 */
@Composable
fun CircularGauge(
    fraction: Float?,
    centerText: String?,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = AccentCyan,
    diameter: Dp = 52.dp
) {
    val animated by animateFloatAsState(
        targetValue = (fraction ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "gauge"
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(diameter)) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = BorderColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                if (fraction != null) {
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = 270f * animated,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                centerText ?: "N/A",
                color = if (fraction != null) TextPrimary else TextMuted,
                fontSize = if (centerText == null) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}
