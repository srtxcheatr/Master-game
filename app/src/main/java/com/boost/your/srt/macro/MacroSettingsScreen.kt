package com.boost.your.srt.macro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentGreen
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BgPrimary
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Settings for the E and G overlay buttons. Used by Gameplay Add-ons -> Magic Button and by the
 * overlay control panel's MACRO tab.
 *
 * Every change is written to DataStore immediately under that button's OWN key; the overlay
 * service observes those keys, so moving/resizing a button updates the live overlay at once.
 * E and G never share state.
 */
@Composable
fun MacroSettingsScreen(
    onPair: (buttonId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MacroButtonSection(id = "E", accent = AccentCyan, onPair = onPair)
        MacroButtonSection(id = "G", accent = AccentAmber, onPair = onPair)
    }
}

@Composable
private fun MacroButtonSection(id: String, accent: Color, onPair: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by AppDataStore.macroConfigFlow(context, id)
        .collectAsStateWithLifecycle(initialValue = MacroButtonConfig.default(id))

    fun update(transform: (MacroButtonConfig) -> MacroButtonConfig) {
        scope.launch { AppDataStore.updateMacroConfig(context, id, transform) }
    }

    var savedFlash by remember { mutableStateOf(false) }
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1500)
            savedFlash = false
        }
    }

    val sizeRange = MacroButtonConfig.sizeRangeFor(id)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + ON/OFF
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) { Text(id, color = BgPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$id BUTTON SETTINGS", color = accent, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.5.sp)
                Text(if (config.isEnabled) "Overlay button is ON" else "Overlay button is OFF", color = TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = config.isEnabled,
                onCheckedChange = { on -> update { it.copy(isEnabled = on) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = BorderColor
                )
            )
        }

        // Size
        SubHeader("SIZE  (${sizeRange.first}–${sizeRange.last} dp)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(config.size.dp.coerceAtMost(84.dp))
                        .clip(CircleShape)
                        .background(accent.copy(alpha = config.alpha)),
                    contentAlignment = Alignment.Center
                ) { Text(id, color = BgPrimary, fontWeight = FontWeight.Black) }
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                LiveSlider(
                    value = config.size.toFloat(),
                    range = sizeRange.first.toFloat()..sizeRange.last.toFloat(),
                    steps = sizeRange.last - sizeRange.first - 1,
                    accent = accent,
                    label = "${config.size} dp"
                ) { v -> update { it.copy(size = v.roundToInt()) } }
            }
        }

        // Opacity
        SubHeader("TRANSPARENCY / OPACITY")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).border(1.dp, BorderColor, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(34.dp).alpha(config.alpha).clip(CircleShape).background(accent))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                LiveSlider(
                    value = config.alpha * 100f,
                    range = 0f..100f,
                    steps = 99,
                    accent = accent,
                    label = "${(config.alpha * 100).roundToInt()}%"
                ) { v -> update { it.copy(alpha = (v / 100f).coerceIn(0f, 1f)) } }
            }
        }

        // Position
        SubHeader("POSITION")
        Text(
            "X: ${config.posX}  Y: ${config.posY}",
            color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )
        // Arrow pad (+/-1)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallKey("▲", accent) { update { it.copy(posY = (it.posY - 1).coerceAtLeast(0)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallKey("◄", accent) { update { it.copy(posX = (it.posX - 1).coerceAtLeast(0)) } }
                Spacer(Modifier.size(44.dp))
                SmallKey("►", accent) { update { it.copy(posX = it.posX + 1) } }
            }
            SmallKey("▼", accent) { update { it.copy(posY = it.posY + 1) } }
        }
        OffsetRow("X", accent) { d -> update { it.copy(posX = (it.posX + d).coerceAtLeast(0)) } }
        OffsetRow("Y", accent) { d -> update { it.copy(posY = (it.posY + d).coerceAtLeast(0)) } }

        // Timing
        SubHeader("TRIGGER DURATION  (min 5 ms)")
        TimingControl(config.triggerDurationMs, accent) { ms -> update { it.copy(triggerDurationMs = ms) } }
        SubHeader("REPEAT INTERVAL  (min 5 ms)")
        TimingControl(config.repeatIntervalMs, accent) { ms -> update { it.copy(repeatIntervalMs = ms) } }

        // Pairing
        SubHeader("PAIRING")
        Text(
            if (config.isPaired) "Paired: (X:${config.targetX}, Y:${config.targetY})" else "Not paired",
            color = if (config.isPaired) AccentGreen else TextSecondary,
            fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Button(
            onClick = { onPair(id) },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.18f), contentColor = accent)
        ) { Text("PAIR WITH TARGET", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp) }
        Text(
            "Drag the crosshair to the on-screen control this button should tap, then press “Set Here”.",
            color = TextMuted, fontSize = 11.sp
        )

        // Save / reset
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        AppDataStore.saveMacroConfig(context, config)
                        savedFlash = true
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = BgPrimary)
            ) { Text(if (savedFlash) "SAVED ✔" else "SAVE SETTINGS", fontWeight = FontWeight.Black, fontSize = 12.sp) }
            OutlinedButton(
                onClick = { scope.launch { AppDataStore.resetMacroConfig(context, id) } },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("RESET TO DEFAULT", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

// ---------------------------------------------------------------- Pieces

@Composable
private fun SubHeader(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

/** Slider that previews locally while dragging and writes to DataStore once the drag ends. */
@Composable
private fun LiveSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    accent: Color,
    label: String,
    onCommit: (Float) -> Unit
) {
    var draft by remember(value) { mutableStateOf(value) }
    Column {
        Slider(
            value = draft.coerceIn(range.start, range.endInclusive),
            onValueChange = { draft = it },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = range,
            steps = steps.coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = BorderColor
            )
        )
        Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
    }
}

@Composable
private fun SmallKey(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BgPrimary)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun OffsetRow(axis: String, accent: Color, onDelta: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(axis, color = accent, fontWeight = FontWeight.Black, modifier = Modifier.width(14.dp))
        listOf(-50, -10, -1, 1, 10, 50).forEach { d ->
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgPrimary)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { onDelta(d) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) { Text(if (d > 0) "+$d" else "$d", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun TimingControl(valueMs: Long, accent: Color, onCommit: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(5L, 10L, 20L, 50L).forEach { preset ->
                val selected = valueMs == preset
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent.copy(alpha = 0.2f) else BgPrimary)
                        .border(1.dp, if (selected) accent else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onCommit(preset) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) { Text("${preset}ms", color = if (selected) accent else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        LiveSlider(
            value = valueMs.toFloat().coerceIn(5f, 500f),
            range = 5f..500f,
            steps = 0,
            accent = accent,
            label = "$valueMs ms"
        ) { v -> onCommit(v.roundToInt().toLong().coerceAtLeast(MacroButtonConfig.MIN_MS)) }
    }
}
