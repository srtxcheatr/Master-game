package com.boost.your.srt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import com.boost.your.srt.util.collectAsStateValue
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class ModeProfile(
    val key: String,
    val title: String,
    val description: String,
    /** Relative emphasis 0..1: Performance, Control, Battery Life, Video Quality, Temperature Rising */
    val axes: List<Float>
)

private val AXIS_LABELS = listOf("Performance", "Control", "Battery Life", "Video Quality", "Temp Rising")

private val MODES = listOf(
    ModeProfile("power_saving", "Power Saving Mode", "Favors battery life and cooler temperatures. Frame rate and visual quality are kept modest.", listOf(0.35f, 0.5f, 0.95f, 0.4f, 0.25f)),
    ModeProfile("balanced", "Equilibrium Mode", "A balance of smoothness, quality and battery life for everyday gaming sessions.", listOf(0.65f, 0.7f, 0.65f, 0.65f, 0.55f)),
    ModeProfile("performance", "Performance Mode", "Prioritizes frame rate, responsiveness and visual quality. Expect faster battery drain and more heat.", listOf(0.95f, 0.9f, 0.3f, 0.9f, 0.9f))
)

@Composable
fun GameModeScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedKey by AppDataStore.performanceMode.collectAsStateValue("performance")
    val selected = MODES.first { it.key == selectedKey }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MODES.forEach { m ->
                val sel = m.key == selectedKey
                Box(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) AccentAmber.copy(alpha = 0.15f) else BgCard)
                        .border(1.5.dp, if (sel) AccentAmber else BorderColor, RoundedCornerShape(14.dp))
                        .clickable { scope.launch { AppDataStore.performanceMode.set(ctx, m.key) } }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.title, color = if (sel) AccentAmber else TextSecondary, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadarChart(selected.axes, Modifier.size(190.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(selected.title, color = AccentAmber, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(selected.description, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** Pentagon radar: five axes, grid rings, and the selected mode filled in amber. */
@Composable
private fun RadarChart(values: List<Float>, modifier: Modifier) {
    Canvas(modifier) {
        val n = AXIS_LABELS.size
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 30.dp.toPx()

        fun point(i: Int, scale: Float): Offset {
            val a = (-PI / 2 + 2 * PI * i / n).toFloat()
            return Offset(center.x + cos(a) * radius * scale, center.y + sin(a) * radius * scale)
        }

        // grid rings
        for (ring in 1..4) {
            val s = ring / 4f
            val p = Path()
            for (i in 0 until n) {
                val o = point(i, s)
                if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y)
            }
            p.close()
            drawPath(p, BorderColor, style = Stroke(1.dp.toPx()))
        }
        // spokes
        for (i in 0 until n) drawLine(BorderColor, center, point(i, 1f), 1.dp.toPx())

        // data polygon
        val poly = Path()
        values.forEachIndexed { i, v ->
            val o = point(i, v.coerceIn(0f, 1f))
            if (i == 0) poly.moveTo(o.x, o.y) else poly.lineTo(o.x, o.y)
        }
        poly.close()
        drawPath(poly, AccentAmber.copy(alpha = 0.30f))
        drawPath(poly, AccentAmber, style = Stroke(2.dp.toPx()))
        values.forEachIndexed { i, v -> drawCircle(AccentAmber, 3.dp.toPx(), point(i, v.coerceIn(0f, 1f))) }

        drawLabels(::point)
    }
}

private fun DrawScope.drawLabels(point: (Int, Float) -> Offset) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8899BB.toInt()
        textSize = 9.sp.toPx()
        textAlign = android.graphics.Paint.Align.CENTER
    }
    AXIS_LABELS.forEachIndexed { i, label ->
        val o = point(i, 1.22f)
        drawContext.canvas.nativeCanvas.drawText(label, o.x, o.y + paint.textSize / 3f, paint)
    }
}
