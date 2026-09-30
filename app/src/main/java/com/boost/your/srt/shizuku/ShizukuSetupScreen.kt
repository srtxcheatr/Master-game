package com.boost.your.srt.shizuku

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
private const val ADB_COMMAND = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"

private fun isShizukuInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}

@Composable
fun ShizukuSetupScreen(onContinue: () -> Unit, onSkip: (() -> Unit)? = null) {
    val context = LocalContext.current

    var installed by remember { mutableStateOf(isShizukuInstalled(context)) }
    var running by remember { mutableStateOf(ShizukuHelper.isAvailable()) }
    var granted by remember { mutableStateOf(ShizukuHelper.isGranted()) }
    var copied by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Poll status so checkmarks animate in as soon as the user completes each step.
    LaunchedEffect(Unit) {
        while (true) {
            installed = isShizukuInstalled(context)
            running = ShizukuHelper.isAvailable()
            granted = running && ShizukuHelper.isGranted()
            delay(1000)
        }
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SHIZUKU SETUP", color = AccentCyan, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
        Text(
            "BOOST MASTER uses Shizuku for display stretch, memory cleanup and macro taps. Complete the three steps below.",
            color = TextSecondary, fontSize = 13.sp
        )

        StepCard(
            number = 1,
            title = "Install Shizuku",
            body = "Get the Shizuku app from the Play Store.",
            done = installed
        ) {
            Button(
                onClick = {
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PKG"))
                    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PKG"))
                    runCatching { context.startActivity(market) }.onFailure { runCatching { context.startActivity(web) } }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgPrimary)
            ) { Text("Install Shizuku", fontWeight = FontWeight.Bold) }
        }

        StepCard(
            number = 2,
            title = "Start via ADB",
            body = "Connect your phone to a computer with USB debugging on and run:",
            done = running
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF03060D))
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(ADB_COMMAND, color = AccentGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("adb", ADB_COMMAND))
                copied = true
            }) { Text(if (copied) "Copied" else "Copy", color = AccentCyan) }
            Spacer(Modifier.height(4.dp))
            Text("Android 11+: you can instead use Wireless debugging from inside the Shizuku app.", color = TextMuted, fontSize = 11.sp)
        }

        StepCard(
            number = 3,
            title = "Open Shizuku and tap Start",
            body = "Open the Shizuku app and make sure it says \"Shizuku is running\".",
            done = running
        ) {
            OutlinedButton(onClick = {
                val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
                if (launch != null) runCatching { context.startActivity(launch) }
            }, enabled = installed) { Text("Open Shizuku", color = AccentCyan) }
        }

        if (running && !granted) {
            Text(
                if (permissionDenied) "Permission was denied. Tap Continue to ask again, or grant it in the Shizuku app."
                else "Shizuku is running. Tap Continue to grant BOOST MASTER permission.",
                color = AccentAmber, fontSize = 12.sp
            )
        }

        Button(
            onClick = {
                // Continue is only enabled when pingBinder() == true (see `running`).
                if (ShizukuHelper.isGranted()) {
                    onContinue()
                } else {
                    ShizukuHelper.requestPermission { ok ->
                        permissionDenied = !ok
                        granted = ok
                        if (ok) onContinue()
                    }
                }
            },
            enabled = running,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = BgPrimary,
                disabledContainerColor = BorderColor,
                disabledContentColor = TextMuted
            )
        ) { Text("CONTINUE", fontWeight = FontWeight.Black, letterSpacing = 3.sp) }

        if (onSkip != null) {
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now (Shizuku features disabled)", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, if (done) AccentGreen.copy(alpha = 0.6f) else BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (done) AccentGreen else AccentCyan.copy(alpha = 0.18f))
                .border(1.5.dp, if (done) AccentGreen else AccentCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = done,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = BgPrimary, modifier = Modifier.size(20.dp))
            }
            if (!done) Text("$number", color = AccentCyan, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
