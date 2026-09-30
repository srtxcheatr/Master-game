package com.boost.your.srt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BgSideNav
import com.boost.your.srt.ui.theme.TextSecondary

data class NavItem(val label: String, val icon: ImageVector)

/** Vertical rail. The selected item gets a cyan left border and a lighter background. */
@Composable
fun SideNavRail(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxHeight()
            .background(BgSideNav)
            .verticalScroll(rememberScrollState())
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (selected) BgCard else BgSideNav)
                    .clickable { onSelect(i) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .size(width = 3.dp, height = 64.dp)
                        .background(if (selected) AccentCyan else BgSideNav)
                )
                Column(
                    Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (selected) AccentCyan else TextSecondary, modifier = Modifier.size(24.dp))
                    Text(
                        item.label,
                        color = if (selected) AccentCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
