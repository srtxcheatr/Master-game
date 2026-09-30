package com.boost.your.srt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary

/** Card with a title, optional description, and a switch: [activeColor] when ON, gray when OFF. */
@Composable
fun SettingToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    activeColor: Color = AccentCyan,
    trailingExtra: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, if (checked) activeColor.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        trailingExtra?.invoke()
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BorderColor,
                uncheckedBorderColor = TextMuted
            )
        )
    }
}

/** Tappable row that navigates to a sub-screen. */
@Composable
fun NavigationCard(title: String, subtitle: String? = null, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", color = AccentCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
