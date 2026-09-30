package com.boost.your.srt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.shizuku.ShizukuHelper
import com.boost.your.srt.ui.components.SettingToggleCard
import com.boost.your.srt.ui.theme.AccentRed
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.util.collectAsStateValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NetworkEnhancementScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dual by AppDataStore.networkDual.collectAsStateValue(false)
    val accel by AppDataStore.networkAccel.collectAsStateValue(false)
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingToggleCard(
            title = "Dual Channel Acceleration",
            subtitle = "Prefer Wi-Fi and mobile data together where the device supports it",
            checked = dual,
            onCheckedChange = { on -> scope.launch { AppDataStore.networkDual.set(ctx, on) } }
        )
        SettingToggleCard(
            title = "Game Network Acceleration",
            subtitle = "Wi-Fi low-latency mode (needs Shizuku)",
            checked = accel,
            onCheckedChange = { on ->
                scope.launch {
                    error = null
                    val r = withContext(Dispatchers.IO) {
                        ShizukuHelper.exec("cmd wifi force-low-latency-mode ${if (on) "enabled" else "disabled"}")
                    }
                    // Only persist when the system actually accepted the change.
                    if (r.exitCode == 0) AppDataStore.networkAccel.set(ctx, on)
                    else error = "Could not change low-latency mode: ${r.error ?: r.output.ifBlank { "exit ${r.exitCode}" }}"
                }
            }
        )
        error?.let { Text(it, color = AccentRed, fontSize = 12.sp) }
        Text(
            "Dual Channel is saved as a preference only; Android does not let apps bond Wi-Fi and mobile data for another app.",
            color = TextMuted, fontSize = 11.sp
        )
    }
}
