package com.boost.your.srt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Discrete slider with a label under every stop. Cyan track, white thumb.
 * The selection is committed via [onSelect] when the drag finishes (and on every tap).
 */
@Composable
fun SliderSettingCard(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = AccentCyan
) {
    require(labels.size >= 2) { "Need at least two stops" }
    val last = labels.lastIndex
    var draft by remember(selectedIndex) { mutableFloatStateOf(selectedIndex.toFloat()) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(labels[draft.roundToInt().coerceIn(0, last)], color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = {
                val idx = draft.roundToInt().coerceIn(0, last)
                draft = idx.toFloat()
                onSelect(idx)
            },
            valueRange = 0f..last.toFloat(),
            steps = (labels.size - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = BorderColor,
                activeTickColor = BgCard,
                inactiveTickColor = TextSecondary
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, l ->
                Text(
                    l,
                    color = if (i == draft.roundToInt()) accent else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (i == draft.roundToInt()) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
