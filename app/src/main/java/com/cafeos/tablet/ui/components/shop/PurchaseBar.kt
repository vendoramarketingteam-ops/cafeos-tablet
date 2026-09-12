package com.cafeos.tablet.ui.components.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.cafeos.tablet.ui.components.CurrencyCountUp
import com.cafeos.tablet.ui.components.rarityPulse
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Pause
import com.cafeos.tablet.model.shop.QuickActionUi
import com.cafeos.tablet.ui.theme.PosAccent
import com.cafeos.tablet.ui.theme.PosDanger
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosInk
import com.cafeos.tablet.ui.theme.PosInkSoft
import com.cafeos.tablet.ui.theme.PosPaper
import com.cafeos.tablet.ui.theme.PosSurface
import com.cafeos.tablet.ui.theme.glowColor
import com.cafeos.tablet.ui.theme.isGamified
import com.cafeos.tablet.ui.gameTap

/**
 * Full-width checkout bar pinned to the bottom of the screen (Zone H).
 *
 * Shows the combined total (base item + included addons) and a large primary
 * "Purchase" / "Pay ₱{total}" button, plus a quick-action icon row mapping to
 * existing POSScreen quick actions (Hold Order, Split Bill, Apply Discount, Void Item).
 *
 * Total is passed in cents (Long) for integer-precision money math; the display
 * converts to pesos via ShopCurrency.
 */
@Composable
fun PurchaseBar(
    totalCents: Long,
    currencyLabel: String = "₱",
    onPurchase: () -> Unit,
    quickActions: List<QuickActionUi> = emptyList(),
    modifier: Modifier = Modifier
) {
    val gamified = isGamified()
    val totalPeso = totalCents / 100.0
    val totalText = "$currencyLabel${String.format("%.2f", totalPeso)}"

    val barBg = if (gamified) PosSurface.copy(alpha = 0.95f) else PosSurface
    val borderColor = if (gamified) glowColor().copy(alpha = 0.4f) else Color(0xFFE0D6C6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .borderIf(gamified, 1.dp, borderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // —— Quick-action icon row ——
        if (quickActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { action ->
                    IconButton(
                        onClick = action.onClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = action.iconVector,
                            contentDescription = action.label,
                            tint = PosInkSoft,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // —— Total + Purchase button ——
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Total",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = PosInkSoft
                )
                CurrencyCountUp(
                    amountCents = totalCents,
                    currencyLabel = currencyLabel
                )
            }

            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .height(56.dp)
                    .then(
                        if (gamified) Modifier
                            .border(
                                width = 1.dp,
                                color = glowColor(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 4.dp)
                        else Modifier.padding(horizontal = 24.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gamified) PosAccent else PosAccent
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Pay $totalText",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = PosPaper,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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

/** Sample quick-actions for previews. */
val sampleQuickActions = listOf(
    QuickActionUi("hold_order", "Hold Order", Icons.Default.Pause) { /* noop */ },
    QuickActionUi("split_bill", "Split Bill", Icons.Default.Paid) { /* noop */ },
    QuickActionUi("discount", "Apply Discount", Icons.Default.LocalOffer) { /* noop */ },
    QuickActionUi("void", "Void Item", Icons.Default.Delete) { /* noop */ }
)
