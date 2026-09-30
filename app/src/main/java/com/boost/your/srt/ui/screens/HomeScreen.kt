package com.boost.your.srt.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.data.GameInfo
import com.boost.your.srt.data.GameProfileManager
import com.boost.your.srt.data.SystemMetrics
import com.boost.your.srt.data.SystemMetricsReader
import com.boost.your.srt.service.OverlayService
import com.boost.your.srt.shizuku.ShizukuHelper
import com.boost.your.srt.ui.components.NavItem
import com.boost.your.srt.ui.components.SideNavRail
import com.boost.your.srt.ui.components.SystemMetricsBar
import com.boost.your.srt.display.StageStretchScreen
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
import com.boost.your.srt.util.PermissionHelper
import com.boost.your.srt.util.collectAsStateValue
import com.boost.your.srt.util.toDurationText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NAV_ITEMS = listOf(
    NavItem("Basic Features", Icons.Default.GridView),
    NavItem("Quick Access Tools", Icons.Default.Tune),
    NavItem("Anti-Interference", Icons.Default.Block),
    NavItem("Gameplay Add-ons", Icons.Default.AutoAwesome)
)

private val QUICK_TOOLS = listOf(
    "Game Mode", "GPU Settings", "Touch Optimization", "Network Enhancement", "Restore Default", "Stage / Stretch Screen"
)

@Composable
fun HomeScreen(
    initialSection: Int = 0,
    onOpenMacro: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val ctx = LocalContext.current
    var section by rememberSaveable { mutableIntStateOf(initialSection) }

    // Live metrics every 2 seconds
    val reader = remember { SystemMetricsReader(ctx) }
    var metrics by remember { mutableStateOf<SystemMetrics?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            metrics = withContext(Dispatchers.IO) { runCatching { reader.read() }.getOrNull() }
            delay(2000)
        }
    }

    Column(Modifier.fillMaxSize().background(BgPrimary).statusBarsPadding()) {
        SystemMetricsBar(
            metrics = metrics,
            onProfile = { Toast.makeText(ctx, "BOOST MASTER 1.0.0", Toast.LENGTH_SHORT).show() },
            onMic = { Toast.makeText(ctx, "Voice presets are under Gameplay Add-ons", Toast.LENGTH_SHORT).show() },
            onSettings = { section = 1 },
            onMenu = onOpenShizuku
        )
        Row(Modifier.fillMaxSize()) {
            SideNavRail(NAV_ITEMS, section, { section = it }, Modifier.weight(0.25f))
            Box(Modifier.weight(0.75f).fillMaxHeight()) {
                when (section) {
                    0 -> BasicFeatures()
                    1 -> QuickAccess()
                    2 -> AntiInterferenceScreen()
                    else -> GameplayAddonsScreen(onOpenMacro = onOpenMacro)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Basic Features

@Composable
private fun BasicFeatures() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { GameProfileManager(ctx) }
    var games by remember { mutableStateOf<List<GameInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedPkg by rememberSaveable { mutableStateOf<String?>(null) }
    var hasUsage by remember { mutableStateOf(PermissionHelper.hasUsageStatsAccess(ctx)) }
    val mode by AppDataStore.performanceMode.collectAsStateValue("performance")
    val gameBoost by AppDataStore.gameBoost.collectAsStateValue(false)

    LaunchedEffect(hasUsage) {
        loading = true
        games = withContext(Dispatchers.IO) { runCatching { manager.getInstalledGames() }.getOrDefault(emptyList()) }
        loading = false
    }
    // Re-check usage access when the user comes back from system settings
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            hasUsage = PermissionHelper.hasUsageStatsAccess(ctx)
        }
    }

    val selected = games.firstOrNull { it.packageName == selectedPkg } ?: games.firstOrNull()

    var recording by remember { mutableStateOf(false) }
    var wifiOn by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            loading -> Text("Looking for installed games…", color = TextSecondary, fontSize = 12.sp)
            games.isEmpty() -> Text("No games found on this device.", color = TextSecondary, fontSize = 12.sp)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(games, key = { it.packageName }) { g ->
                    GameCard(
                        game = g,
                        selected = g.packageName == selected?.packageName,
                        hasUsage = hasUsage,
                        tags = tagsFor(mode, gameBoost),
                        onSelect = { selectedPkg = g.packageName },
                        onStart = {
                            if (!PermissionHelper.canDrawOverlays(ctx)) {
                                ctx.startActivity(PermissionHelper.overlaySettingsIntent(ctx))
                            } else {
                                OverlayService.start(ctx)
                                if (!manager.launch(g.packageName)) {
                                    Toast.makeText(ctx, "Could not launch ${g.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
        if (!hasUsage) {
            Text(
                "Grant usage access to show your last play time.",
                color = AccentAmber, fontSize = 11.sp,
                modifier = Modifier.clickable { ctx.startActivity(PermissionHelper.usageAccessSettingsIntent()) }
            )
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Screenshot", Icons.Default.PhotoCamera, false, Modifier.weight(1f)) {
                scope.launch {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val r = withContext(Dispatchers.IO) { ShizukuHelper.exec("screencap -p /sdcard/Pictures/BoostMaster_$stamp.png") }
                    toast(ctx, if (r.exitCode == 0) "Saved to Pictures/BoostMaster_$stamp.png" else "Screenshot failed: ${r.error ?: "exit ${r.exitCode}"}")
                }
            }
            QuickAction(if (recording) "Stop Rec" else "Screen Record", Icons.Default.Videocam, recording, Modifier.weight(1f)) {
                scope.launch {
                    if (!recording) {
                        recording = true
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        // Blocks until stopped or the 3-minute system limit is reached.
                        val r = withContext(Dispatchers.IO) { ShizukuHelper.exec("screenrecord --time-limit 180 /sdcard/Movies/BoostMaster_$stamp.mp4") }
                        recording = false
                        toast(ctx, if (r.exitCode == 0) "Saved to Movies/BoostMaster_$stamp.mp4" else "Recording failed: ${r.error ?: "exit ${r.exitCode}"}")
                    } else {
                        withContext(Dispatchers.IO) { ShizukuHelper.exec("pkill -SIGINT screenrecord") }
                    }
                }
            }
            QuickAction(if (wifiOn) "Wi-Fi On" else "Wi-Fi Off", Icons.Default.Wifi, wifiOn, Modifier.weight(1f)) {
                scope.launch {
                    val target = !wifiOn
                    val r = withContext(Dispatchers.IO) { ShizukuHelper.exec("svc wifi ${if (target) "enable" else "disable"}") }
                    if (r.exitCode == 0) wifiOn = target else toast(ctx, "Wi-Fi change failed: ${r.error ?: "exit ${r.exitCode}"}")
                }
            }
            QuickAction("Memory Cleanup", Icons.Default.CleaningServices, false, Modifier.weight(1f), enabled = !busy) {
                scope.launch {
                    busy = true
                    val before = availMb(ctx)
                    val r = withContext(Dispatchers.IO) { ShizukuHelper.killBackground() }
                    if (r.exitCode == 0) {
                        delay(1500)
                        toast(ctx, "Freed ${(availMb(ctx) - before).coerceAtLeast(0)} MB")
                    } else {
                        toast(ctx, "Cleanup failed: ${r.error ?: r.output.ifBlank { "exit ${r.exitCode}" }}")
                    }
                    busy = false
                }
            }
        }

        // Performance mode
        Text("PERFORMANCE MODE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("power_saving" to "Power Saving", "balanced" to "Balanced", "performance" to "Performance").forEach { (key, label) ->
                val sel = key == mode
                val perf = key == "performance"
                val c = if (perf) AccentAmber else AccentCyan
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) c.copy(alpha = 0.16f) else BgCard)
                        .border(1.5.dp, if (sel) c else BorderColor, RoundedCornerShape(12.dp))
                        .clickable { scope.launch { AppDataStore.performanceMode.set(ctx, key) } }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, color = if (sel) c else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun tagsFor(mode: String, gameBoost: Boolean): List<String> = buildList {
    add(when (mode) { "power_saving" -> "Power Saving"; "balanced" -> "Balanced"; else -> "Esports Pro" })
    if (mode == "performance") add("Smart Frame Stability")
    if (gameBoost) add("Game Boost")
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

private fun availMb(ctx: Context): Long {
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    return info.availMem / (1024L * 1024L)
}

@Composable
private fun GameCard(
    game: GameInfo,
    selected: Boolean,
    hasUsage: Boolean,
    tags: List<String>,
    onSelect: () -> Unit,
    onStart: () -> Unit
) {
    val bitmap = remember(game.packageName) { game.icon.safeBitmap()?.asImageBitmap() }
    Column(
        Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(if (selected) 2.dp else 1.dp, if (selected) AccentCyan else BorderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = game.name, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)))
        } else {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(BorderColor))
        }
        Text(game.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(
            when {
                !hasUsage -> "TIME --"
                game.lastSessionSeconds != null -> "TIME ${game.lastSessionSeconds.toDurationText()}"
                else -> "TIME --"
            },
            color = TextSecondary, fontSize = 11.sp
        )
        Row(
            Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { t ->
                Text(
                    t, color = AccentCyan, fontSize = 9.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(AccentCyan.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp),
                    maxLines = 1
                )
            }
        }
        if (selected) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgPrimary)
            ) { Text("Start", fontWeight = FontWeight.Black, letterSpacing = 2.sp) }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, if (active) AccentGreen else BorderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (active) AccentGreen else AccentCyan, modifier = Modifier.size(24.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun Drawable.safeBitmap(): Bitmap? = runCatching {
    val w = intrinsicWidth.coerceAtLeast(1).coerceAtMost(192)
    val h = intrinsicHeight.coerceAtLeast(1).coerceAtMost(192)
    toBitmap(w, h)
}.getOrNull()

// ------------------------------------------------------------------ Quick Access Tools

@Composable
private fun QuickAccess() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tool by rememberSaveable { mutableIntStateOf(0) }
    var restoreMsg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUICK_TOOLS.forEachIndexed { i, name ->
                val sel = i == tool
                Text(
                    name,
                    color = if (sel) AccentCyan else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) AccentCyan.copy(alpha = 0.14f) else BgCard)
                        .border(1.dp, if (sel) AccentCyan else BorderColor, RoundedCornerShape(50))
                        .clickable { tool = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tool) {
                0 -> GameModeScreen()
                1 -> GpuSettingsScreen()
                2 -> TouchOptimizationScreen()
                3 -> NetworkEnhancementScreen()
                4 -> Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Restore Default resets performance mode, GPU, touch, network, anti-interference and add-on settings. " +
                            "Your E / G macro buttons and bubble position are kept.",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                AppDataStore.restoreDefaults(ctx)
                                restoreMsg = "Default settings restored"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgPrimary)
                    ) { Text("RESTORE DEFAULT SETTINGS", fontWeight = FontWeight.Black) }
                    restoreMsg?.let { Text("✔ $it", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Spacer(Modifier.height(1.dp))
                }
                else -> StageStretchScreen()
            }
        }
    }
}
