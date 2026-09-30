package com.boost.your.srt.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.data.Setting
import com.boost.your.srt.ui.components.SliderSettingCard
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextSecondary
import com.boost.your.srt.util.collectAsStateValue
import kotlinx.coroutines.launch

private val TOUCH_MODES = listOf("common" to "Common", "fps" to "FPS", "moba" to "MOBA", "custom" to "Custom")
private val TICK_LABELS = listOf("-2", "-1", "0", "1", "2")

/** Preset slider values per mode (tap, swipe, accuracy). "custom" leaves the sliders untouched. */
private val MODE_PRESETS = mapOf(
    "common" to Triple(0f, 0f, 0f),
    "fps" to Triple(1f, 2f, 1f),
    "moba" to Triple(1f, 0f, 2f)
)

@Composable
fun TouchOptimizationScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by AppDataStore.touchMode.collectAsStateValue("common")
    val tap by AppDataStore.touchSensitivity.collectAsStateValue(0f)
    val swipe by AppDataStore.touchSwipe.collectAsStateValue(0f)
    val accuracy by AppDataStore.touchAccuracy.collectAsStateValue(0f)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CONTROL ADJUSTMENT", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TOUCH_MODES.forEach { (key, label) ->
                val sel = key == mode
                Box(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.5.dp, if (sel) AccentCyan else BorderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            scope.launch {
                                AppDataStore.touchMode.set(ctx, key)
                                MODE_PRESETS[key]?.let { (t, s, a) ->
                                    AppDataStore.touchSensitivity.set(ctx, t)
                                    AppDataStore.touchSwipe.set(ctx, s)
                                    AppDataStore.touchAccuracy.set(ctx, a)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text(label, color = if (sel) AccentCyan else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        TouchSlider("Tap Sensitivity", tap, AppDataStore.touchSensitivity) { scope.launch { AppDataStore.touchMode.set(ctx, "custom") } }
        TouchSlider("Swipe Responsiveness", swipe, AppDataStore.touchSwipe) { scope.launch { AppDataStore.touchMode.set(ctx, "custom") } }
        TouchSlider("Micro Control Accuracy", accuracy, AppDataStore.touchAccuracy) { scope.launch { AppDataStore.touchMode.set(ctx, "custom") } }

        Text(
            "Saved as your touch profile. Android does not let apps change another app's touch handling, so these values apply only where a game reads the profile.",
            color = TextMuted, fontSize = 11.sp
        )
    }
}

@Composable
private fun TouchSlider(title: String, value: Float, setting: Setting<Float>, onEdited: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    SliderSettingCard(
        title = title,
        labels = TICK_LABELS,
        selectedIndex = (value.toInt() + 2).coerceIn(0, 4),
        onSelect = { i ->
            scope.launch { setting.set(ctx, (i - 2).toFloat()) }
            onEdited()
        }
    )
}
