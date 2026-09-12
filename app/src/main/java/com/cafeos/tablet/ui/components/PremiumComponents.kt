package com.cafeos.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.ui.theme.*

@Composable
fun PremiumScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val compact = LocalConfiguration.current.screenWidthDp < Dimens.tabletBreakpoint.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosCoffee)
            .padding(
                horizontal = if (compact) Dimens.space12 else Dimens.space20,
                vertical = if (compact) Dimens.space8 else Dimens.space12
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
        content = content
    )
}

@Composable
fun PremiumHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val classic = isClassic()
    val compact = LocalConfiguration.current.screenWidthDp < Dimens.tabletBreakpoint.value
    Column(modifier = Modifier.fillMaxWidth()) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                if (!classic) GemIcon(modifier = Modifier.size(22.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (classic) PosInk else PosPaper,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            subtitle?.let {
                Text(
                    it,
                    color = PosInkSoft,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Dimens.space4)
                )
            }
            action?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.space8),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    it()
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    if (!classic) GemIcon(modifier = Modifier.size(22.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (classic) PosInk else PosPaper,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                action?.invoke()
            }
            subtitle?.let {
                Text(it, color = PosInkSoft, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (!classic) {
            Spacer(Modifier.height(Dimens.space8))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (isGamified()) glowColor() else PosBorder.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun PremiumPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val classic = isClassic()
    val gamified = isGamified()
    // MOBA quest-log frame: neon-cyan border + lift in GAMIFIED, flat gray frame in
    // FAST, plain card in CLASSIC. The gamified visual layer is always-on except
    // Classic (spec FR-004 "plain revert").
    val frameColor = if (gamified) glowColor() else PosBorder.copy(alpha = 0.5f)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 1200.dp)
            .then(
                if (classic) Modifier else Modifier.border(
                    width = if (gamified) 1.5.dp else 1.dp,
                    color = frameColor,
                    shape = RoundedCornerShape(Dimens.radiusLarge)
                )
            ),
        shape = RoundedCornerShape(Dimens.radiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (classic) PosSurface else PosSurface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (gamified) Dimens.space4 else 2.dp
        )
    ) {
        Column(Modifier.padding(Dimens.space16)) {
            title?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    if (!classic) GemIcon(modifier = Modifier.size(Dimens.space16))
                    Text(
                        it,
                        color = PosPaper,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(Dimens.space12))
            }
            content()
        }
    }
}

@Composable
fun StatusDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
        Icon(Icons.Default.Circle, contentDescription = null, tint = color, modifier = Modifier.size(Dimens.space8))
        Text(label, color = PosMuted, style = MaterialTheme.typography.labelMedium)
    }
}

fun statusColor(status: String): Color = when (status.uppercase()) {
    "COMPLETED", "AVAILABLE", "ACTIVE", "APPROVED" -> Color(0xFF3E7D4A)
    "PENDING" -> Color(0xFFB58A4A)
    "PREPARING", "RESERVED" -> PosInfo
    "CANCELLED", "CLOSED", "REJECTED", "LOW" -> Color(0xFFB3261E)
    else -> PosInkSoft
}
