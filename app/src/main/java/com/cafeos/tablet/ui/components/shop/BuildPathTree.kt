package com.cafeos.tablet.ui.components.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafeos.tablet.model.shop.BuildPathNodeUi
import com.cafeos.tablet.model.shop.ShopCurrency
import com.cafeos.tablet.ui.components.GemIcon
import com.cafeos.tablet.ui.gameTap
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PosInk
import com.cafeos.tablet.ui.theme.PosNeon
import com.cafeos.tablet.ui.theme.glowColor
import com.cafeos.tablet.ui.theme.isGamified

/**
 * Branching combo/upsell tree (Zone F).
 *
 * Renders a tree: the selected base product at the top-center, connector lines
 * fanning down to child addon nodes, mirroring the screenshot's 1→2→4 branching
 * shape. Each node is tappable to toggle inclusion; checked nodes show a check
 * mark + gold tint, unchecked nodes show an empty circle.
 *
 * Geometry (per spec §3 Zone F):
 * - Root node centered at the top.
 * - Children arranged below, fanning with connector lines.
 * - Node size: 64dp square, rarity-framed via GameCard.
 * - Nested children (if any) branch from their parent in the same fashion.
 * - Depth limited to 2 levels (1 base → N addons) unless nested combos exist.
 */
@Composable
fun BuildPathTree(
    root: BuildPathNodeUi,
    onNodeToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val gamified = isGamified()
    val connColor = connectorColor(gamified)  // pre-compute OUTSIDE any Canvas

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        BuildPathNode(
            node = root,
            isRoot = true,
            depth = 0,
            gamified = gamified,
            connColor = connColor,
            onNodeToggled = onNodeToggled
        )
    }
}

@Composable
private fun BuildPathNode(
    node: BuildPathNodeUi,
    isRoot: Boolean,
    depth: Int,
    gamified: Boolean,
    connColor: Color,
    onNodeToggled: (String) -> Unit
) {
    val hasChildren = node.children.isNotEmpty()
    val showChildren = depth < 2 && hasChildren

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuildPathNodeCard(
            node = node,
            isRoot = isRoot,
            gamified = gamified,
            onNodeToggled = onNodeToggled
        )

        if (showChildren) {
            val childCount = node.children.size

            if (childCount == 1) {
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.TopCenter)
                    ) {
                        val cx = size.width / 2f
                        drawLine(
                            color = connColor,
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            strokeWidth = if (gamified) 2.dp.toPx() else 1.dp.toPx()
                        )
                    }
                }
                BuildPathNode(
                    node = node.children[0],
                    isRoot = false,
                    depth = depth + 1,
                    gamified = gamified,
                    connColor = connColor,
                    onNodeToggled = onNodeToggled
                )
            } else {
                val childWidth = 64.dp
                val gap = 16.dp
                val connectorWidth = childCount * childWidth.value + (childCount - 1) * gap.value

                Box(
                    modifier = Modifier
                        .width(connectorWidth.dp)
                        .height(72.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val startX = size.width / 2f
                        val startY = 0f
                        val childY = size.height - 32f
                        val stepX = if (childCount > 1) size.width / (childCount - 1) else size.width / 2f
                        for (i in 0 until childCount) {
                            val childX = stepX * i
                            drawLine(
                                color = connColor,
                                start = Offset(startX, startY),
                                end = Offset(childX, childY),
                                strokeWidth = if (gamified) 2.dp.toPx() else 1.dp.toPx()
                            )
                            drawCircle(
                                color = connColor,
                                radius = if (gamified) 4.dp.toPx() else 2.dp.toPx(),
                                center = Offset(childX, childY)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        node.children.forEach { child ->
                            BuildPathNode(
                                node = child,
                                isRoot = false,
                                depth = depth + 1,
                                gamified = gamified,
                                connColor = connColor,
                                onNodeToggled = onNodeToggled
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildPathNodeCard(
    node: BuildPathNodeUi,
    isRoot: Boolean,
    gamified: Boolean,
    onNodeToggled: (String) -> Unit
) {
    val included = node.included
    val glow = if (gamified) glowColor() else Color.Transparent
    val borderColor = if (gamified) {
        if (included) PosGold.copy(alpha = 0.7f) else glow.copy(alpha = 0.3f)
    } else Color(0xFFE0D6C6)

    val borderWidth = if (included && gamified) 2.dp else 1.dp

    val bgBrush = if (gamified && included) {
        Brush.radialGradient(
            colors = listOf(PosGold.copy(alpha = 0.15f), Color.Transparent)
        )
    } else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(bgBrush, shape = RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = { onNodeToggled(node.id) })
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Check / uncheck icon
            Icon(
                imageVector = if (included) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (included) "Included" else "Not included",
                tint = if (included) PosGold else PosInk.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Node icon (gem canvas)
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GemIcon(modifier = Modifier.size(16.dp))
                if (gamified && included) {
                    val nodeGlow = glow  // pre-computed, safe inside Canvas
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = nodeGlow,
                            style = Stroke(width = 1.dp.toPx()),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = 10.dp.toPx()
                        )
                    }
                }
            }

            // Delta price or "Base" label
            if (node.deltaPrice > 0) {
                Text(
                    text = "+${ShopCurrency.format(node.deltaPrice)}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = if (included) PosGold else PosInk.copy(alpha = 0.6f),
                    fontWeight = if (included) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 9.sp
                )
            } else if (isRoot) {
                Text(
                    text = "Base",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = PosInk.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            }
        }

        // Root crown star
        if (isRoot) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Base item",
                tint = PosGold,
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun connectorColor(gamified: Boolean): Color =
    if (gamified) glowColor().copy(alpha = 0.5f) else Color(0xFFD1C8BA)
