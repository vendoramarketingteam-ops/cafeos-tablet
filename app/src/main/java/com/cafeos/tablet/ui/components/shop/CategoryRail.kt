package com.cafeos.tablet.ui.components.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.model.shop.CategoryUi
import com.cafeos.tablet.ui.components.GemIcon
import com.cafeos.tablet.ui.theme.PosCoffeeLight
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosInk
import com.cafeos.tablet.ui.theme.PosInkSoft

/**
 * Left vertical category rail (Zone B).
 * Selected row gets a left accent bar + background tint, matching the ML screenshot's
 * blue-highlighted "Attack" row.
 *
 * Row height ≥ 64dp — fat-finger safe touch targets for tablet POS.
 */
@Composable
fun CategoryRail(
    categories: List<CategoryUi>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(200.dp)
            .background(PosCoffeeLight)
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            categories.forEach { category ->
                val selected = category.id == selectedId
                CategoryRailItem(
                    category = category,
                    selected = selected,
                    onSelect = { onSelect(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRailItem(
    category: CategoryUi,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val accentColor = if (selected) PosGold else Color.Transparent
    val bgColor = if (selected) PosGold.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (selected) PosGold else PosInk
    val subtitleColor = if (selected) PosGold.copy(alpha = 0.8f) else PosInkSoft

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left accent bar (selection indicator)
            Box(
                modifier = Modifier
                    .size(4.dp, 40.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.size(12.dp))

            GemIcon(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    color = textColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                )
                if (category.productCount > 0) {
                    Text(
                        text = "${category.productCount} items",
                        color = subtitleColor,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
