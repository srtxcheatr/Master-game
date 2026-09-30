package com.boost.your.srt.overlay

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.display.DisplayManager
import com.boost.your.srt.display.OpStatus
import com.boost.your.srt.display.StretchPreset
import com.boost.your.srt.display.StretchResult
import com.boost.your.srt.display.oriented
import com.boost.your.srt.macro.MacroButtonConfig
import com.boost.your.srt.macro.MacroEngine
import com.boost.your.srt.macro.MacroSettingsScreen
import com.boost.your.srt.shizuku.ShizukuHelper
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentGreen
import com.boost.your.srt.ui.theme.AccentRed
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BgPrimary
import com.boost.your.srt.ui.theme.BoostMasterTheme
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Slide-up control panel (85% width, 75% height) shown when the bubble logo is tapped.
 * Hosts a Compose UI with four tabs: BOOST, MACRO, DISPLAY, TOOLS.
 */
@SuppressLint("ViewConstructor")
class ControlPanelView(
    context: Context,
    @Suppress("UNUSED_PARAMETER") macroEngine: MacroEngine,
    owner: OverlayLifecycleOwner,
    private val fps: StateFlow<Int>,
    private val onPairRequest: (String) -> Unit,
    private val onScreenshot: () -> Unit
) : FrameLayout(context) {

    var onClose: () -> Unit = {}

    init {
        // Compose looks these up from the ROOT view of the window, which is this view.
        setViewTreeLifecycleOwner(owner)
        setViewTreeViewModelStoreOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)

        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                BoostMasterTheme {
                    PanelContent(
                        fps = fps,
                        onClose = { onClose() },
                        onPair = onPairRequest,
                        onScreenshot = onScreenshot
                    )
                }
            }
        }
        addView(compose, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Slides the panel up from the bottom edge. */
    fun slideUp() {
        post {
            translationY = height.toFloat().coerceAtLeast(1f)
            animate().translationY(0f).setDuration(260).start()
        }
    }
}

// ------------------------------------------------------------------ Panel UI

private val TAB_TITLES = listOf("⚡ BOOST", "🎯 MACRO", "📐 DISPLAY", "⚙️ TOOLS")

@Composable
private fun PanelContent(
    fps: StateFlow<Int>,
    onClose: () -> Unit,
    onPair: (String) -> Unit,
    onScreenshot: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

    Column(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(Color(0xF20D1220))
            .border(1.dp, BorderColor, shape)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TAB_TITLES.forEachIndexed { i, title ->
                val selected = i == tab
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AccentCyan.copy(alpha = 0.18f) else Color.Transparent)
                        .border(1.dp, if (selected) AccentCyan else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { tab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = if (selected) AccentCyan else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Box(
                Modifier
                    .padding(start = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text("✕", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> BoostTab(fps)
                1 -> MacroSettingsScreen(onPair = onPair)
                2 -> DisplayTab()
                else -> ToolsTab(onScreenshot)
            }
        }
    }
}

// ---- BOOST ------------------------------------------------------------

@Composable
private fun BoostTab(fps: StateFlow<Int>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val fpsNow by fps.collectAsState()
    val mode by AppDataStore.performanceMode.flow(ctx).collectAsState(initial = "performance")
    val gameBoost by AppDataStore.gameBoost.flow(ctx).collectAsState(initial = false)

    var cleaning by remember { mutableStateOf(false) }
    var cleanResult by remember { mutableStateOf<String?>(null) }
    var cleanFailed by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(isDndOn(ctx)) }
    var boostError by remember { mutableStateOf<String?>(null) }

    val fpsColor = when {
        fpsNow > 60 -> AccentGreen
        fpsNow >= 30 -> AccentAmber
        else -> AccentRed
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FRAME RATE", color = TextSecondary, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(
                if (fpsNow > 0) "$fpsNow" else "--",
                color = fpsColor, fontSize = 56.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
            )
            Text("display frame cadence (FPS)", color = TextMuted, fontSize = 10.sp)
        }

        PanelLabel("PERFORMANCE MODE")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("power_saving" to "Power Saving", "balanced" to "Balanced", "performance" to "Performance").forEach { (key, label) ->
                val sel = mode == key
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) AccentCyan.copy(alpha = 0.18f) else BgPrimary)
                        .border(1.dp, if (sel) AccentCyan else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { scope.launch { AppDataStore.performanceMode.set(ctx, key) } }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, color = if (sel) AccentCyan else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
        }

        PanelLabel("MEMORY")
        ActionButton(if (cleaning) "Cleaning..." else "MEMORY CLEAN", enabled = !cleaning) {
            scope.launch {
                cleaning = true
                cleanResult = null
                val (msg, failed) = cleanMemory(ctx)
                cleanResult = msg
                cleanFailed = failed
                cleaning = false
            }
        }
        cleanResult?.let { Text(it, color = if (cleanFailed) AccentRed else AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

        PanelLabel("SYSTEM")
        ToggleRow("DND Mode", "Silence interruptions while gaming", dnd) { on ->
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (!nm.isNotificationPolicyAccessGranted) {
                ctx.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                nm.setInterruptionFilter(
                    if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
                )
                dnd = isDndOn(ctx)
            }
        }
        ToggleRow("Game Boost", "Sets system animation scales to 0 (via Shizuku)", gameBoost) { on ->
            scope.launch {
                boostError = null
                val v = if (on) "0" else "1"
                val results = withContext(Dispatchers.IO) {
                    listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
                        .map { ShizukuHelper.setSetting("global", it, v) }
                }
                val failed = results.firstOrNull { it.exitCode != 0 }
                if (failed == null) AppDataStore.gameBoost.set(ctx, on)
                else boostError = "Game Boost failed: ${failed.error ?: "exit ${failed.exitCode}"}"
            }
        }
        boostError?.let { Text(it, color = AccentRed, fontSize = 12.sp) }
    }
}

// ---- DISPLAY ----------------------------------------------------------

@Composable
private fun DisplayTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dm = remember { DisplayManager(ctx) }
    var current by remember { mutableStateOf(dm.readCurrentConfig()) }
    var hasBackup by remember { mutableStateOf(dm.backup.hasBackup()) }
    var status by remember { mutableStateOf<OpStatus>(OpStatus.Idle) }

    fun refresh() {
        current = dm.readCurrentConfig()
        hasBackup = dm.backup.hasBackup()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            refresh()
        }
    }

    fun perform(success: String, block: suspend () -> StretchResult) {
        scope.launch {
            status = OpStatus.Working
            val result = block()
            delay(400)
            status = when (result) {
                is StretchResult.Success, is StretchResult.Restored -> OpStatus.Success(success)
                is StretchResult.ShizukuUnavailable -> OpStatus.Error("Shizuku is not running")
                is StretchResult.Failed -> OpStatus.Error(result.reason)
            }
            refresh()
        }
    }

    val presets = listOf(
        "4:3" to StretchPreset.RATIO_4_3,
        "16:9" to StretchPreset.RATIO_16_9,
        "720p" to StretchPreset.P720,
        "540p" to StretchPreset.P540
    )
    val busy = status is OpStatus.Working

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PanelLabel("CURRENT RESOLUTION")
        Text(
            "${current.width} × ${current.height}  @ ${current.density} dpi",
            color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )

        PanelLabel("STRETCH PRESETS")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { (label, preset) ->
                MiniButton(label, !busy, Modifier.weight(1f)) {
                    val (w, h, d) = preset.oriented(dm.isNaturalPortrait())
                    perform("Stretch Applied") { dm.applyStretch(w, h, d) }
                }
            }
            MiniButton("Reset", !busy, Modifier.weight(1f)) {
                perform("Display Restored") { dm.resetToDefault() }
            }
        }

        ActionButton("RESTORE ORIGINAL", enabled = hasBackup && !busy, color = AccentAmber) {
            perform("Display Restored") { dm.restoreOriginal() }
        }
        Text(if (hasBackup) "Original backup available" else "No backup yet (created on first stretch)", color = TextMuted, fontSize = 11.sp)

        when (val s = status) {
            is OpStatus.Working -> Text("Working...", color = AccentCyan, fontSize = 12.sp)
            is OpStatus.Success -> Text("✔ ${s.message}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            is OpStatus.Error -> Text(s.message, color = AccentRed, fontSize = 12.sp)
            else -> Unit
        }
    }
}

// ---- TOOLS ------------------------------------------------------------

@Composable
private fun ToolsTab(onScreenshot: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val eCfg by AppDataStore.macroConfigFlow(ctx, "E").collectAsState(initial = MacroButtonConfig.default("E"))
    val gCfg by AppDataStore.macroConfigFlow(ctx, "G").collectAsState(initial = MacroButtonConfig.default("G"))
    val netBoost by AppDataStore.networkAccel.flow(ctx).collectAsState(initial = false)

    var killing by remember { mutableStateOf(false) }
    var killResult by remember { mutableStateOf<String?>(null) }
    var killFailed by remember { mutableStateOf(false) }
    var netError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton("SCREENSHOT", enabled = true) { onScreenshot() }

        ActionButton(if (killing) "Killing..." else "KILL BACKGROUND APPS", enabled = !killing, color = AccentAmber) {
            scope.launch {
                killing = true
                killResult = null
                val (msg, failed) = cleanMemory(ctx)
                killResult = msg
                killFailed = failed
                killing = false
            }
        }
        killResult?.let { Text(it, color = if (killFailed) AccentRed else AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

        ToggleRow("Network boost", "Wi-Fi low-latency mode (via Shizuku)", netBoost) { on ->
            scope.launch {
                netError = null
                val r = withContext(Dispatchers.IO) {
                    ShizukuHelper.exec("cmd wifi force-low-latency-mode ${if (on) "enabled" else "disabled"}")
                }
                if (r.exitCode == 0) AppDataStore.networkAccel.set(ctx, on)
                else netError = "Network boost failed: ${r.error ?: r.output.ifBlank { "exit ${r.exitCode}" }}"
            }
        }
        netError?.let { Text(it, color = AccentRed, fontSize = 12.sp) }

        PanelLabel("OVERLAY BUTTONS")
        ToggleRow("Show E button", "Cyan macro button", eCfg.isEnabled) { on ->
            scope.launch { AppDataStore.updateMacroConfig(ctx, "E") { it.copy(isEnabled = on) } }
        }
        ToggleRow("Show G button", "Amber macro button", gCfg.isEnabled) { on ->
            scope.launch { AppDataStore.updateMacroConfig(ctx, "G") { it.copy(isEnabled = on) } }
        }
    }
}

// ---- helpers ----------------------------------------------------------

private fun isDndOn(ctx: Context): Boolean {
    val nm = ctx.getSystemService(NotificationManager::class.java)
    val f = nm.currentInterruptionFilter
    return f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
}

private fun availMb(ctx: Context): Long {
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    return info.availMem / (1024L * 1024L)
}

/** Kills cached background processes through Shizuku and reports the measured change in free RAM. */
private suspend fun cleanMemory(ctx: Context): Pair<String, Boolean> {
    val before = availMb(ctx)
    val r = withContext(Dispatchers.IO) { ShizukuHelper.killBackground() }
    if (r.exitCode != 0) return "Failed: ${r.error ?: r.output.ifBlank { "exit ${r.exitCode}" }}" to true
    delay(1500)
    val freed = (availMb(ctx) - before).coerceAtLeast(0)
    return "Freed $freed MB" to false
}

@Composable
private fun PanelLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, color: Color = AccentCyan, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color, contentColor = BgPrimary,
            disabledContainerColor = BorderColor, disabledContentColor = TextMuted
        )
    ) { Text(text, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 13.sp) }
}

@Composable
private fun MiniButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgPrimary)
            .border(1.dp, if (enabled) AccentCyan.copy(alpha = 0.6f) else BorderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (enabled) AccentCyan else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary, uncheckedTrackColor = BorderColor
            )
        )
    }
}
