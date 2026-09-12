package com.cafeos.tablet.ui.components.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.model.shop.ShopMode
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosInk
import com.cafeos.tablet.ui.theme.PosPaper
import com.cafeos.tablet.ui.theme.PosSurface
import com.cafeos.tablet.ui.theme.glowColor
import com.cafeos.tablet.ui.theme.isClassic
import com.cafeos.tablet.ui.theme.isGamified

/**
 * Two-segment pill toggle between "All Products" and "Quick Picks" (Zone A).
 * Visually matches the screenshot's compact "All Equipment / Simple Mode" toggle —
 * NOT a full TabRow, just a pill switch.
 */
@Composable
fun ShopModeTabRow(
    mode: ShopMode,
    onModeChange: (ShopMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val classic = isClassic()
    val gamified = isGamified()
    val bgColor = if (classic) PosSurface else PosPaper.copy(alpha = 0.10f)
    val borderColor = if (classic) Color(0xFFE0D6C6) else PosGold.copy(alpha = 0.4f)
    val selectedBg = if (classic) PosPaper else PosGold.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .height(44.dp)
            .background(bgColor, shape = RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, shape = RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
        ) {
            val isAll = mode == ShopMode.ALL

            // Selected segment background
            val selectedOffset = if (isAll) 0.dp else 200.dp
            val selectedWidth = 200.dp
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(selectedWidth)
                    .offset(x = selectedOffset)
                    .clip(RoundedCornerShape(24.dp))
                    .background(selectedBg)
            )

            Segment(
                text = "All Products",
                selected = isAll,
                gamified = gamified,
                onClick = { if (!isAll) onModeChange(ShopMode.ALL) },
                modifier = Modifier.weight(1f)
            )
            Segment(
                text = "Quick Picks",
                selected = !isAll,
                gamified = gamified,
                onClick = { if (isAll) onModeChange(ShopMode.QUICK_PICKS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    selected: Boolean,
    gamified: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (selected) PosGold else PosInk
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
    val bgTint = if (selected && gamified) glowColor().copy(alpha = 0.2f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(bgTint)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = fontWeight,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
        )
    }
}
