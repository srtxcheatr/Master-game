package com.boost.your.srt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.data.SystemMetrics
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentGreen
import com.boost.your.srt.ui.theme.AccentPurple
import com.boost.your.srt.ui.theme.AccentRed
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import kotlin.math.roundToInt

/** Top bar: [profile, mic] [CPU, RAM, Temp, Battery, WiFi] [ROM, GPU, settings, menu]. Refreshed by the caller every 2s. */
@Composable
fun SystemMetricsBar(
    metrics: SystemMetrics?,
    onProfile: () -> Unit,
    onMic: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(BgCard)
            .border(1.dp, BorderColor)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BarIcon(Icons.Default.Person, "Profile", onProfile)
        BarIcon(Icons.Default.Mic, "Microphone", onMic)

        val m = metrics
        CircularGauge(m?.cpuPercent?.let { it / 100f }, m?.cpuPercent?.let { "$it%" }, "CPU", color = AccentCyan)
        CircularGauge(
            m?.let { if (it.ramTotalGb > 0) it.ramUsedGb / it.ramTotalGb else null },
            m?.let { "${(it.ramUsedGb / it.ramTotalGb * 100).roundToInt()}%" },
            "RAM", color = AccentPurple
        )
        TextMetric("TEMP", m?.tempCelsius?.let { "%.0f°C".format(it) }, m?.tempCelsius?.let { if (it >= 45) AccentRed else if (it >= 40) AccentAmber else AccentGreen })
        TextMetric(
            "BATTERY",
            m?.let { "${it.batteryPercent}%" + if (it.batteryCharging) " ⚡" else "" },
            m?.let { if (it.batteryPercent <= 15 && !it.batteryCharging) AccentRed else AccentGreen }
        )
        TextMetric("WIFI", m?.wifiStrength, when (m?.wifiStrength) { "Strong" -> AccentGreen; "Medium" -> AccentAmber; "Weak" -> AccentRed; else -> null })
        CircularGauge(
            m?.let { if (it.romTotalGb > 0) it.romUsedGb / it.romTotalGb else null },
            m?.let { "${(it.romUsedGb / it.romTotalGb * 100).roundToInt()}%" },
            "ROM", color = AccentAmber
        )
        CircularGauge(m?.gpuPercent?.let { it / 100f }, m?.gpuPercent?.let { "$it%" }, "GPU", color = AccentGreen)

        BarIcon(Icons.Default.Settings, "Settings", onSettings)
        BarIcon(Icons.Default.Menu, "Menu", onMenu)
    }
}

@Composable
private fun BarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = description, tint = AccentCyan,
        modifier = Modifier.size(28.dp).clickable(onClick = onClick)
    )
}

@Composable
private fun TextMetric(label: String, value: String?, color: Color?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: "N/A", color = color ?: TextPrimary.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}
