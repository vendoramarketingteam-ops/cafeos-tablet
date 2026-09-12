package com.cafeos.tablet.ui.components.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.model.shop.ModifierUi
import com.cafeos.tablet.model.shop.ShopCurrency
import com.cafeos.tablet.model.shop.ShopItemUi
import com.cafeos.tablet.ui.components.GemIcon
import com.cafeos.tablet.ui.theme.PosAccent
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosInk
import com.cafeos.tablet.ui.theme.PosInkSoft
import com.cafeos.tablet.ui.theme.PosPaper
import com.cafeos.tablet.ui.theme.PosSurface
import com.cafeos.tablet.ui.theme.glowColor
import com.cafeos.tablet.ui.theme.isGamified

/**
 * Detail dock for the selected base item (Zone G).
 *
 * Shows directly below the build-path tree. Renders:
 * - Base item name (large)
 * - Each currently checked modifier as a "+ Modifier Name (+₱X)" line
 * - A "Perk" line if applicable (loyalty progress, "Buy 5 get 1 free" nudge, etc.)
 *
 * Pulls perk text from existing Loyalty logic already present in OrderHistory/Loyalty
 * screens rather than inventing new loyalty rules.
 */
@Composable
fun ItemDetailDock(
    baseItem: ShopItemUi?,
    activeModifiers: List<ModifierUi>,
    perkText: String? = null,
    modifier: Modifier = Modifier
) {
    val gamified = isGamified()
    val bg = if (gamified) PosSurface.copy(alpha = 0.88f) else PosSurface
    val borderColor = if (gamified) glowColor().copy(alpha = 0.4f) else Color(0xFFE0D6C6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, shape = RoundedCornerShape(12.dp))
            .borderIf(gamified, borderWidth = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (baseItem != null) {
            Text(
                text = baseItem.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = PosGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            baseItem.description?.let { desc ->
                Text(
                    text = desc,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = PosInkSoft,
                    maxLines = 2
                )
            }
        } else {
            Text(
                text = "Select an item",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = PosInkSoft
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeModifiers.isNotEmpty()) {
            Text(
                text = "Modifiers",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = PosInkSoft
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                activeModifiers.forEach { mod ->
                    ModifierLine(modifier = Modifier.fillMaxWidth(), name = mod.name, deltaPrice = mod.deltaPrice)
                }
            }
        }

        perkText?.let { perk ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GemIcon(modifier = Modifier.size(14.dp))
                Text(
                    text = perk,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = PosAccent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModifierLine(modifier: Modifier, name: String, deltaPrice: Double) {
    val gamified = isGamified()
    Row(
        modifier = modifier
            .background(
                color = if (gamified) PosGold.copy(alpha = 0.08f) else Color(0xFFF5F3EE),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+ $name",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = PosInk
        )
        Text(
            text = "+${ShopCurrency.format(deltaPrice)}",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = if (gamified) PosGold else PosInkSoft,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Conditional border — only drawn in gamified mode. */
private fun Modifier.borderIf(
    enabled: Boolean,
    borderWidth: androidx.compose.ui.unit.Dp,
    color: Color,
    shape: RoundedCornerShape
): Modifier = if (enabled) {
    this.border(width = borderWidth, color = color, shape = shape)
} else this
