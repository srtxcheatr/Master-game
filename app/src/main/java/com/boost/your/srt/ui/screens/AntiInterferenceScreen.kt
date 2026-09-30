package com.boost.your.srt.ui.screens

import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.data.Setting
import com.boost.your.srt.ui.components.NavigationCard
import com.boost.your.srt.ui.components.SettingToggleCard
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextSecondary
import com.boost.your.srt.util.PermissionHelper
import com.boost.your.srt.util.collectAsStateValue
import kotlinx.coroutines.launch

private enum class AntiPage { MAIN, NOTIF_STYLE, HIDE_NOTIF }

@Composable
fun AntiInterferenceScreen() {
    var page by remember { mutableStateOf(AntiPage.MAIN) }
    when (page) {
        AntiPage.MAIN -> AntiMain(onOpen = { page = it })
        AntiPage.NOTIF_STYLE -> SubPage("Message Notification Style", onBack = { page = AntiPage.MAIN }) { NotifStylePage() }
        AntiPage.HIDE_NOTIF -> SubPage("Hide Notifications", onBack = { page = AntiPage.MAIN }) { HideNotifPage() }
    }
}

@Composable
private fun AntiMain(onOpen: (AntiPage) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PolicyToggle("Call Rejection", "Silence incoming calls while gaming", AppDataStore.antiCallReject)
        PolicyToggle("Call on Hold", "Keep calls quiet until you finish", AppDataStore.antiCallHold)
        NavigationCard("Message Notification Style", "How message alerts appear in-game", onClick = { onOpen(AntiPage.NOTIF_STYLE) })
        NavigationCard("Hide Notifications", "Turn on Do Not Disturb while gaming", onClick = { onOpen(AntiPage.HIDE_NOTIF) })
        PlainToggle("Mistouch Prevention for Navigation Gestures", null, AppDataStore.antiMistouch)
        PlainToggle("Block Drag-down Notification Bar", null, AppDataStore.antiDragNotif)
        PlainToggle("Block Three-finger Gestures", null, AppDataStore.antiThreeFinger)
        Text(
            "Call and notification options use Do Not Disturb, which needs notification-policy access. " +
                "Gesture blocking options are saved as preferences; Android does not let apps intercept system gestures.",
            color = TextMuted, fontSize = 11.sp
        )
    }
}

/** Toggle that needs notification-policy access; sends the user to system settings when it is missing. */
@Composable
private fun PolicyToggle(title: String, subtitle: String, setting: Setting<Boolean>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val checked by setting.collectAsStateValue(false)
    SettingToggleCard(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onCheckedChange = { on ->
            if (on && !PermissionHelper.hasNotificationPolicyAccess(ctx)) {
                ctx.startActivity(PermissionHelper.notificationPolicySettingsIntent())
            } else {
                scope.launch { setting.set(ctx, on) }
            }
        }
    )
}

@Composable
private fun PlainToggle(title: String, subtitle: String?, setting: Setting<Boolean>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val checked by setting.collectAsStateValue(false)
    SettingToggleCard(title, checked, { on -> scope.launch { setting.set(ctx, on) } }, subtitle = subtitle)
}

@Composable
internal fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = AccentCyan, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("  $title", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun NotifStylePage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val style by AppDataStore.notifStyle.collectAsStateValue("banner")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("banner", "Banner", "Standard heads-up banner"),
            Triple("small", "Compact", "Small, short-lived alert"),
            Triple("none", "None", "No pop-ups; check later")
        ).forEach { (key, label, desc) ->
            val sel = key == style
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.5.dp, if (sel) AccentCyan else BorderColor, RoundedCornerShape(14.dp))
                    .clickable { scope.launch { AppDataStore.notifStyle.set(ctx, key) } }
                    .padding(16.dp)
            ) {
                Text(label, color = if (sel) AccentCyan else com.boost.your.srt.ui.theme.TextPrimary, fontWeight = FontWeight.Bold)
                Text(desc, color = TextSecondary, fontSize = 11.sp)
            }
        }
        Text("Saved as a preference; per-app notification styling is controlled by each app.", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun HideNotifPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val hide by AppDataStore.hideNotifications.collectAsStateValue(false)
    var msg by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingToggleCard(
            title = "Hide Notifications (Do Not Disturb)",
            subtitle = "Turns Do Not Disturb on for priority-only interruptions",
            checked = hide,
            onCheckedChange = { on ->
                msg = null
                if (!PermissionHelper.hasNotificationPolicyAccess(ctx)) {
                    ctx.startActivity(PermissionHelper.notificationPolicySettingsIntent())
                } else {
                    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.setInterruptionFilter(
                        if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
                    )
                    scope.launch { AppDataStore.hideNotifications.set(ctx, on) }
                    msg = if (on) "Do Not Disturb is on" else "Do Not Disturb is off"
                }
            }
        )
        msg?.let { Text(it, color = AccentAmber, fontSize = 12.sp) }
    }
}
