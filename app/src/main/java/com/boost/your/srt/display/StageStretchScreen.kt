package com.boost.your.srt.display

import android.app.Application
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.boost.your.srt.shizuku.ShizukuHelper
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentGreen
import com.boost.your.srt.ui.theme.AccentRed
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BgPrimary
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- ViewModel

enum class StretchPreset(val label: String, val width: Int, val height: Int, val density: Int) {
    RATIO_4_3("4:3", 1440, 1080, 320),
    RATIO_16_9("16:9", 1920, 1080, 420),
    P720("1280×720", 1280, 720, 280),
    P540("960×540", 960, 540, 240),
    CUSTOM("Custom", 0, 0, 0)
}

sealed class OpStatus {
    object Idle : OpStatus()
    object Working : OpStatus()
    data class Success(val message: String) : OpStatus()
    data class Error(val message: String) : OpStatus()
}

/**
 * Preset size oriented to the device's natural orientation, which is what `wm size WxH` expects:
 * portrait phones get (short x long), natural-landscape tablets get (long x short).
 */
fun StretchPreset.oriented(naturalPortrait: Boolean): Triple<Int, Int, Int> {
    val big = maxOf(width, height)
    val small = minOf(width, height)
    return if (naturalPortrait) Triple(small, big, density) else Triple(big, small, density)
}

data class StageUiState(
    val current: DisplayConfig = DisplayConfig(0, 0, 0),
    val stretchActive: Boolean = false,
    val backup: DisplayConfig? = null,
    val shizukuReady: Boolean = false,
    val naturalPortrait: Boolean = true,
    val preset: StretchPreset = StretchPreset.RATIO_4_3,
    val customW: String = "",
    val customH: String = "",
    val customD: String = "",
    val applyStatus: OpStatus = OpStatus.Idle,
    val restoreStatus: OpStatus = OpStatus.Idle
)

class StageStretchViewModel(app: Application) : AndroidViewModel(app) {

    private val manager = DisplayManager(app)
    private val _state = MutableStateFlow(StageUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        val cur = manager.readCurrentConfig()
        _state.update {
            it.copy(
                current = cur,
                stretchActive = manager.isStretchActive(),
                backup = manager.backup.getOriginal(),
                shizukuReady = ShizukuHelper.isAvailable() && ShizukuHelper.isGranted(),
                naturalPortrait = manager.isNaturalPortrait(),
                customW = it.customW.ifEmpty { cur.width.toString() },
                customH = it.customH.ifEmpty { cur.height.toString() },
                customD = it.customD.ifEmpty { cur.density.toString() }
            )
        }
    }

    fun selectPreset(p: StretchPreset) = _state.update { it.copy(preset = p, applyStatus = OpStatus.Idle) }
    fun setCustomW(v: String) = _state.update { it.copy(customW = v.filter(Char::isDigit).take(5)) }
    fun setCustomH(v: String) = _state.update { it.copy(customH = v.filter(Char::isDigit).take(5)) }
    fun setCustomD(v: String) = _state.update { it.copy(customD = v.filter(Char::isDigit).take(4)) }

    /** Preset dimensions oriented to the device's natural orientation (what `wm size` expects). */
    fun effectiveTarget(s: StageUiState = _state.value): Triple<Int, Int, Int>? {
        if (s.preset == StretchPreset.CUSTOM) {
            val w = s.customW.toIntOrNull() ?: return null
            val h = s.customH.toIntOrNull() ?: return null
            val d = s.customD.toIntOrNull() ?: return null
            return Triple(w, h, d)
        }
        return s.preset.oriented(s.naturalPortrait)
    }

    fun apply() {
        val target = effectiveTarget() ?: run {
            _state.update { it.copy(applyStatus = OpStatus.Error("Enter valid width, height and density")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(applyStatus = OpStatus.Working, restoreStatus = OpStatus.Idle) }
            val result = manager.applyStretch(target.first, target.second, target.third)
            delay(400) // let the system settle before re-reading metrics
            val status = when (result) {
                is StretchResult.Success -> OpStatus.Success("Stretch Applied")
                is StretchResult.Restored -> OpStatus.Success("Stretch Applied")
                is StretchResult.ShizukuUnavailable -> OpStatus.Error("Shizuku is not running. Start it and try again.")
                is StretchResult.Failed -> OpStatus.Error(result.reason)
            }
            _state.update { it.copy(applyStatus = status) }
            refresh()
        }
    }

    fun restore() {
        viewModelScope.launch {
            _state.update { it.copy(restoreStatus = OpStatus.Working, applyStatus = OpStatus.Idle) }
            val result = manager.restoreOriginal()
            delay(400)
            _state.update { it.copy(restoreStatus = restoreStatus(result)) }
            refresh()
        }
    }

    fun resetToSystemDefault() {
        viewModelScope.launch {
            _state.update { it.copy(restoreStatus = OpStatus.Working, applyStatus = OpStatus.Idle) }
            val result = manager.resetToDefault()
            delay(400)
            _state.update { it.copy(restoreStatus = restoreStatus(result)) }
            refresh()
        }
    }

    private fun restoreStatus(result: StretchResult): OpStatus = when (result) {
        is StretchResult.Restored, is StretchResult.Success -> OpStatus.Success("Display Restored")
        is StretchResult.ShizukuUnavailable -> OpStatus.Error("Shizuku is not running. Start it and try again.")
        is StretchResult.Failed -> OpStatus.Error(result.reason)
    }
}

// ---------------------------------------------------------------- Screen

@Composable
fun StageStretchScreen(vm: StageStretchViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    // Keep live values fresh (rotation, external wm changes, Shizuku starting/stopping)
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            vm.refresh()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SECTION A — current display state
        SectionCard("CURRENT DISPLAY STATE") {
            InfoRow("Current Resolution", "${s.current.width} × ${s.current.height}")
            InfoRow("Current Density", "${s.current.density} dpi")
            InfoRow("Stretch Active", if (s.stretchActive) "YES" else "NO", if (s.stretchActive) AccentAmber else TextSecondary)
            InfoRow("Original Backup", if (s.backup != null) "AVAILABLE" else "NONE", if (s.backup != null) AccentGreen else TextSecondary)
            InfoRow("Shizuku", if (s.shizukuReady) "READY" else "NOT READY", if (s.shizukuReady) AccentGreen else AccentRed)
        }

        // SECTION B — configuration
        SectionCard("STRETCH CONFIGURATION") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StretchPreset.values().forEach { p ->
                    PresetChip(p.label, p == s.preset, Modifier.weight(1f)) { vm.selectPreset(p) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (s.preset == StretchPreset.CUSTOM) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Width", s.customW, vm::setCustomW, Modifier.weight(1f))
                    NumberField("Height", s.customH, vm::setCustomH, Modifier.weight(1f))
                    NumberField("Density", s.customD, vm::setCustomD, Modifier.weight(1f))
                }
            } else {
                val t = vm.effectiveTarget(s)
                if (t != null) {
                    Text(
                        "wm size ${t.first}x${t.second}  •  wm density ${t.third}",
                        color = AccentCyan, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // SECTION C — actions
        SectionCard("ACTIONS") {
            Button(
                onClick = vm::apply,
                enabled = s.applyStatus !is OpStatus.Working && s.restoreStatus !is OpStatus.Working,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgPrimary)
            ) {
                Text(
                    if (s.applyStatus is OpStatus.Working) "Applying..." else "APPLY STRETCH",
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp
                )
            }
            StatusLine(s.applyStatus)
            Spacer(Modifier.height(6.dp))
            Text(
                "⚠️ Changing resolution may affect display. You can restore below.",
                color = AccentAmber, fontSize = 11.sp
            )
        }

        // SECTION D — restore
        SectionCard("RESTORE DISPLAY") {
            val b = s.backup
            Text(
                if (b != null) "Original backup saved at: ${formatTime(b.timestamp)}  (${b.width}×${b.height} @ ${b.density}dpi)"
                else "No backup available",
                color = TextSecondary, fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = vm::restore,
                enabled = b != null && s.applyStatus !is OpStatus.Working && s.restoreStatus !is OpStatus.Working,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber, contentColor = BgPrimary,
                    disabledContainerColor = BorderColor, disabledContentColor = TextMuted
                )
            ) {
                Text(
                    if (s.restoreStatus is OpStatus.Working) "Restoring..." else "RESTORE ORIGINAL",
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp
                )
            }
            StatusLine(s.restoreStatus)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = vm::resetToSystemDefault,
                enabled = s.applyStatus !is OpStatus.Working && s.restoreStatus !is OpStatus.Working,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Reset to System Default", color = TextSecondary) }
        }
    }
}

// ---------------------------------------------------------------- Pieces

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, color = AccentCyan, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimary) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentCyan.copy(alpha = 0.18f) else BgPrimary)
            .border(1.dp, if (selected) AccentCyan else BorderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) AccentCyan else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = BorderColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = AccentCyan,
            unfocusedLabelColor = TextSecondary,
            cursorColor = AccentCyan
        ),
        modifier = modifier
    )
}

/** Success is only ever rendered for OpStatus.Success, which is only produced when every command exited 0. */
@Composable
private fun StatusLine(status: OpStatus) {
    when (status) {
        is OpStatus.Success -> Text(
            "✔ ${status.message}",
            color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(AccentGreen.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
        is OpStatus.Error -> Text(
            status.message,
            color = AccentRed, fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        else -> Unit
    }
}

private fun formatTime(ms: Long): String =
    if (ms <= 0) "unknown" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
