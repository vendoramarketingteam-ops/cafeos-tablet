package com.cafeos.tablet.ui.components.shop

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.model.shop.ShopItemUi
import com.cafeos.tablet.ui.components.GameCard
import com.cafeos.tablet.model.shop.ShopCurrency
import com.cafeos.tablet.ui.components.GemIcon
import com.cafeos.tablet.ui.components.Rarity
import com.cafeos.tablet.ui.components.StackCounter
import com.cafeos.tablet.ui.components.rarityColor
import com.cafeos.tablet.ui.components.rarityPulse
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosInk
import com.cafeos.tablet.ui.theme.PosInkSoft
import com.cafeos.tablet.ui.theme.PosPaper
import androidx.compose.runtime.setValue
import com.cafeos.tablet.ui.gameTap

/**
 * A product card in the MLBB item-grid (Zone C/D).
 *
 * Wraps the existing GameCard, adding:
 * - icon/photo at the top
 * - name + one-line description
 * - price bottom-left
 * - StackCounter quantity badge (selection state)
 * - optional thin stock progress bar (via GameCard's `progress` param, added in §5.2)
 * - RarityPulse glow ring when selected (delegated to GameCard's `selected` param)
 *
 * Internally uses GameCard(rarity = item.rarity, ...) — does NOT fork GameCard's
 * border/glow logic; extends its parameters instead (spec §5.2).
 */
@Composable
fun ShopItemCard(
    item: ShopItemUi,
    selected: Boolean,
    quantity: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(item.imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(item.imageUrl) {
        bitmap = null
        val path = when {
            item.imageUrl.isNullOrBlank() -> null
            item.imageUrl.startsWith("file://") -> android.net.Uri.parse(item.imageUrl).path
            item.imageUrl.startsWith("content://") -> item.imageUrl
            else -> item.imageUrl
        }
        bitmap = when {
            path == null -> null
            path.startsWith("content://") -> {
                try {
                    context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) { null }
            }
            else -> {
                val f = java.io.File(path)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            }
        }
    }

    // Stock progress: green→amber→red as stock depletes; null = omit (no stock field)
    val stockProgress = item.stockLevel?.let { level ->
        if (item.minStock <= 0) null
        else (level.toFloat() / item.minStock.toFloat()).coerceIn(0f, 1f)
    }

    val rarity = item.rarity

    GameCard(
        rarity = rarity,
        selected = selected,
        progress = stockProgress,
        modifier = modifier
            .fillMaxWidth()
            .rarityPulse(rarity = rarity, selected = selected)
            .gameTap(onTap)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // —— Icon / Photo ——
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val tint = rarityColor(rarity)
                    Canvas(modifier = Modifier.size(48.dp)) {
                        drawCircle(color = tint.copy(alpha = 0.3f))
                        // Draw a simple gem diamond
                        val s = size
                        drawLine(
                            color = tint,
                            start = androidx.compose.ui.geometry.Offset(s.width / 2f, 0f),
                            end = androidx.compose.ui.geometry.Offset(s.width / 2f, s.height),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = tint,
                            start = androidx.compose.ui.geometry.Offset(0f, s.height / 2f),
                            end = androidx.compose.ui.geometry.Offset(s.width, s.height / 2f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Rarity corner badge (text, never color alone)
                val badgeText = when (rarity) {
                    Rarity.COMMON -> null
                    Rarity.UNCOMMON -> "UNCOMMON"
                    Rarity.RARE -> "RARE"
                    Rarity.EPIC -> "EPIC"
                }
                badgeText?.let { label ->
                    val badgeTint = rarityColor(rarity)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                color = badgeTint.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(
                                    topStart = 0.dp, topEnd = 8.dp,
                                    bottomStart = 8.dp, bottomEnd = 8.dp
                                )
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = PosPaper,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // —— Name ——
            Text(
                text = item.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                color = PosInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            // —— One-line description ——
            item.description?.let { desc ->
                Text(
                    text = desc,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = PosInkSoft,
                    maxLines = 1
                )
            }

            // —— Price bottom-left ——
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = ShopCurrency.format(item.price),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = PosGold,
                fontWeight = FontWeight.Bold
            )

            // —— Quantity badge (StackCounter) when selected ——
            if (selected && quantity > 0) {
                StackCounter(
                    quantity = quantity,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = (-8).dp)
                )
            }
        }
    }
}
