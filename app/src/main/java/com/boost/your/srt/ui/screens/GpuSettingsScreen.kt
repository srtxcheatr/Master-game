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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.ui.components.SliderSettingCard
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.util.collectAsStateValue
import kotlinx.coroutines.launch

private val AF_VALUES = listOf(0, 2, 4, 8, 16)
private val AF_LABELS = listOf("Off", "2", "4", "8", "16")
private val TEXTURE_LABELS = listOf("Speed First", "Equalization", "Quality First")
private val TEXTURE_VALUES = listOf(0f, 0.5f, 1f)

@Composable
fun GpuSettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val af by AppDataStore.gpuAfLevel.collectAsStateValue(0)
    val tex by AppDataStore.gpuTexture.collectAsStateValue(0.5f)

    val afIndex = AF_VALUES.indexOf(af).coerceAtLeast(0)
    val texIndex = TEXTURE_VALUES.indexOfFirst { it == tex }.let { if (it >= 0) it else if (tex < 0.25f) 0 else if (tex < 0.75f) 1 else 2 }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SliderSettingCard(
            title = "Anisotropic Filtering",
            labels = AF_LABELS,
            selectedIndex = afIndex,
            onSelect = { i -> scope.launch { AppDataStore.gpuAfLevel.set(ctx, AF_VALUES[i]) } }
        )
        SliderSettingCard(
            title = "Texture Filter",
            labels = TEXTURE_LABELS,
            selectedIndex = texIndex,
            onSelect = { i -> scope.launch { AppDataStore.gpuTexture.set(ctx, TEXTURE_VALUES[i]) } }
        )
        Text(
            "Saved as your GPU profile. Android does not let apps change another app's GPU filtering, so these values apply only where a game reads the profile.",
            color = TextMuted, fontSize = 11.sp
        )
    }
}
