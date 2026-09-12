package com.cafeos.tablet.ui.components.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.cafeos.tablet.ui.components.GemIcon
import com.cafeos.tablet.ui.gameTap
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosPaper
import com.cafeos.tablet.ui.theme.glowColor
import com.cafeos.tablet.ui.theme.isGamified

/**
 * Header for the "Suggested Combo" right panel (Zone E).
 * Contains the panel title + an "Add All" pill button that toggles every
 * build-path node to included in one tap.
 *
 * Mirrors the screenshot's "Builds" panel header with "Prioritize" pill.
 */
@Composable
fun BuildPathHeader(
    enabledNodeCount: Int = 0,
    allIncluded: Boolean = false,
    onAddAllTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gamified = isGamified()
    val glow = if (gamified) glowColor() else androidx.compose.ui.graphics.Color.Transparent
    val pillBorder = if (gamified) glow else PosGold.copy(alpha = 0.3f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            if (gamified) GemIcon(modifier = Modifier.size(20.dp))
            Text(
                text = "Suggested Combo",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = PosGold,
                fontWeight = FontWeight.Bold
            )
            if (gamified) {
                Box(
                    modifier = Modifier
                        .background(glow.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$enabledNodeCount items",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = PosGold
                    )
                }
            }
        }

        // "Add All" pill button
        Box(
            modifier = Modifier
                .background(
                    color = if (gamified) glow.copy(alpha = 0.2f) else PosPaper.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(1.dp, pillBorder, shape = RoundedCornerShape(20.dp))
                .gameTap(onAddAllTapped)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PosGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add All",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = PosGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
