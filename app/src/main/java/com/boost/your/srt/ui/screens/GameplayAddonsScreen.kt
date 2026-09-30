package com.boost.your.srt.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.data.Setting
import com.boost.your.srt.shizuku.ShizukuHelper
import com.boost.your.srt.ui.components.NavigationCard
import com.boost.your.srt.ui.components.SettingToggleCard
import com.boost.your.srt.ui.components.SliderSettingCard
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentGreen
import com.boost.your.srt.ui.theme.AccentRed
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import com.boost.your.srt.util.collectAsStateValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AddonPage { MAIN, VOICE, TILT, TOUCH }

@Composable
fun GameplayAddonsScreen(onOpenMacro: () -> Unit) {
    var page by remember { mutableStateOf(AddonPage.MAIN) }
    when (page) {
        AddonPage.MAIN -> AddonsMain(onOpenMacro = onOpenMacro, onOpen = { page = it })
        AddonPage.VOICE -> SubPage("Magic Voice Changer", { page = AddonPage.MAIN }) { VoicePage() }
        AddonPage.TILT -> SubPage("Tilt Controls", { page = AddonPage.MAIN }) { TiltPage() }
        AddonPage.TOUCH -> SubPage("Touch Optimization", { page = AddonPage.MAIN }) { TouchOptimizationScreen() }
    }
}

@Composable
private fun AddonsMain(onOpenMacro: () -> Unit, onOpen: (AddonPage) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val eyeCare by AppDataStore.addonEyeCare.collectAsStateValue(false)
    val imageStab by AppDataStore.addonImageStab.collectAsStateValue(false)
    val bypass by AppDataStore.addonBypass.collectAsStateValue(false)
    val offscreen by AppDataStore.addonOffscreen.collectAsStateValue(false)
    val immersive by AppDataStore.addonImmersive.collectAsStateValue(false)
    val graphic by AppDataStore.graphicEnhance.collectAsStateValue("classic")
    var eyeError by remember { mutableStateOf<String?>(null) }
    var showBypassInfo by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NavigationCard("Single-Press Combo", "Configure a repeat-tap combo on the E / G buttons", onClick = onOpenMacro)

        SettingToggleCard(
            title = "Eye Care for Gaming",
            subtitle = "Warm night-light tint (needs Shizuku)",
            checked = eyeCare,
            onCheckedChange = { on ->
                scope.launch {
                    eyeError = null
                    val r = withContext(Dispatchers.IO) {
                        ShizukuHelper.setSetting("secure", "night_display_activated", if (on) "1" else "0")
                    }
                    if (r.exitCode == 0) AppDataStore.addonEyeCare.set(ctx, on)
                    else eyeError = "Eye care failed: ${r.error ?: r.output.ifBlank { "exit ${r.exitCode}" }}"
                }
            }
        )
        eyeError?.let { Text(it, color = AccentRed, fontSize = 12.sp) }

        // Orange/red when ON, like the reference screenshot.
        SettingToggleCard(
            title = "Image Stabilization",
            subtitle = "Preference for supported games",
            checked = imageStab,
            activeColor = AccentRed,
            onCheckedChange = { on -> scope.launch { AppDataStore.addonImageStab.set(ctx, on) } }
        )

        NavigationCard("Touch Optimization", "Tap, swipe and accuracy profile", onClick = { onOpen(AddonPage.TOUCH) })

        SettingToggleCard(
            title = "Bypass Charging",
            subtitle = "Preference; needs hardware support",
            checked = bypass,
            onCheckedChange = { on -> scope.launch { AppDataStore.addonBypass.set(ctx, on) } },
            trailingExtra = {
                Icon(
                    Icons.Default.Info, contentDescription = "About bypass charging", tint = AccentCyan,
                    modifier = Modifier.clickable { showBypassInfo = true }
                )
            }
        )
        SettingToggleCard(
            title = "Off-screen Gaming",
            subtitle = "Preference for keeping a game running with the screen off",
            checked = offscreen,
            onCheckedChange = { on -> scope.launch { AppDataStore.addonOffscreen.set(ctx, on) } }
        )

        NavigationCard("Magic Voice Changer", "Choose a voice preset", onClick = { onOpen(AddonPage.VOICE) })

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCard)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Graphic Enhancement", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("classic" to "Classic", "colorful" to "Colorful", "soft" to "Soft", "realistic" to "Realistic").forEach { (key, label) ->
                    val sel = key == graphic
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) AccentCyan.copy(alpha = 0.16f) else com.boost.your.srt.ui.theme.BgPrimary)
                            .border(1.dp, if (sel) AccentCyan else BorderColor, RoundedCornerShape(10.dp))
                            .clickable { scope.launch { AppDataStore.graphicEnhance.set(ctx, key) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (sel) AccentCyan else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        SettingToggleCard(
            title = "Immersive Sound",
            subtitle = "Wider, more spatial sound where the game supports it",
            checked = immersive,
            onCheckedChange = { on -> scope.launch { AppDataStore.addonImmersive.set(ctx, on) } }
        )

        NavigationCard("Tilt Controls", "Use device tilt as an input", onClick = { onOpen(AddonPage.TILT) })
        NavigationCard("Magic Button", "Set up the E and G overlay buttons", onClick = onOpenMacro)
    }

    if (showBypassInfo) {
        AlertDialog(
            onDismissRequest = { showBypassInfo = false },
            confirmButton = { TextButton(onClick = { showBypassInfo = false }) { Text("OK", color = AccentCyan) } },
            title = { Text("Bypass Charging") },
            text = { Text("Bypass charging powers the phone directly from the charger instead of the battery. It needs support from the device's charging hardware and firmware; this switch only records your preference.") },
            containerColor = BgCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun VoicePage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val preset by AppDataStore.voicePreset.collectAsStateValue("off")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("off" to "Off", "deep" to "Deep", "bright" to "Bright", "robot" to "Robot").forEach { (key, label) ->
            val sel = key == preset
            Text(
                label,
                color = if (sel) AccentCyan else TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.5.dp, if (sel) AccentCyan else BorderColor, RoundedCornerShape(14.dp))
                    .clickable { scope.launch { AppDataStore.voicePreset.set(ctx, key) } }
                    .padding(16.dp)
            )
        }
        Text("Saved as your preset. Live voice processing is not applied to other apps' microphone audio.", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun TiltPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by AppDataStore.tiltEnabled.collectAsStateValue(false)
    val sens by AppDataStore.tiltSensitivity.collectAsStateValue(2)

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    // Live readout so the user can see the sensor working and calibrate sensitivity.
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            hasSensor = false
            onDispose { }
        } else {
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    tiltX = e.values[0]
                    tiltY = e.values[1]
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(l) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingToggleCard(
            title = "Enable Tilt Controls",
            subtitle = "Preference for supported games",
            checked = enabled,
            onCheckedChange = { on -> scope.launch { AppDataStore.tiltEnabled.set(ctx, on) } }
        )
        SliderSettingCard(
            title = "Tilt Sensitivity",
            labels = listOf("Low", "", "Medium", "", "High"),
            selectedIndex = sens.coerceIn(0, 4),
            onSelect = { i -> scope.launch { AppDataStore.tiltSensitivity.set(ctx, i) } }
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCard)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp)).padding(16.dp)
        ) {
            Text("LIVE TILT", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            if (hasSensor) {
                Text("X: %+.2f   Y: %+.2f  m/s²".format(tiltX, tiltY), color = AccentGreen, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("This device has no accelerometer", color = AccentAmber, fontSize = 13.sp)
            }
        }
    }
}
