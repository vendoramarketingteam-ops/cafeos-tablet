package com.cafeos.tablet.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.data.Product
import com.cafeos.tablet.model.shop.BuildPathNodeUi
import com.cafeos.tablet.model.shop.CategoryUi
import com.cafeos.tablet.model.shop.ModifierUi
import com.cafeos.tablet.model.shop.QuickActionUi
import com.cafeos.tablet.model.shop.SampleDataProvider
import com.cafeos.tablet.model.shop.ShopCurrency
import com.cafeos.tablet.model.shop.ShopItemUi
import com.cafeos.tablet.model.shop.ShopMode
import com.cafeos.tablet.ui.GameFeedback
import com.cafeos.tablet.ui.ItemShopViewModel
import com.cafeos.tablet.ui.components.rarityByPrice
import com.cafeos.tablet.ui.components.shop.sampleQuickActions
import com.cafeos.tablet.ui.components.PremiumHeader
import com.cafeos.tablet.ui.components.PremiumPanel
import com.cafeos.tablet.ui.gameTap
import com.cafeos.tablet.ui.components.shop.BuildPathHeader
import com.cafeos.tablet.ui.components.shop.BuildPathTree
import com.cafeos.tablet.ui.components.shop.CategoryRail
import com.cafeos.tablet.ui.components.shop.ItemDetailDock
import com.cafeos.tablet.ui.components.shop.PurchaseBar
import com.cafeos.tablet.ui.components.shop.ShopItemCard
import com.cafeos.tablet.ui.components.shop.ShopModeTabRow
import com.cafeos.tablet.ui.theme.PosCoffee
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.Dimens
import com.cafeos.tablet.ui.theme.PebotTheme
import com.cafeos.tablet.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * MLBB Item Shop checkout screen (spec §4).
 *
 * This is a NEW screen — does NOT replace POSScreen. Reachable via a "Shop View"
 * toggle from POSScreen. All state flows from [ShopScreenState] (hoisted), so the
 * ViewModel layer (Phase 2) simply produces a [ShopScreenState] and this
 * composable remains pure.
 *
 * Composable tree (spec §4):
 *   PremiumHeader (Zone I)
 *   Row {
 *       CategoryRail (Zone B)
 *       Column {
 *           ShopModeTabRow (Zone A)
 *           ShopItemGrid (Zone C, D)
 *       }
 *       PremiumPanel {
 *           BuildPathHeader (Zone E)
 *           BuildPathTree (Zone F)
 *           ItemDetailDock (Zone G)
 *       }
 *   }
 *   PurchaseBar (Zone H, pinned bottom)
 */
@Composable
fun ItemShopPOSScreen(
    state: ShopScreenState,
    onNavigateBack: () -> Unit,
    onNavigateToCheckoutConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier
        .fillMaxSize()
        .background(PosCoffee)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val compact = LocalConfiguration.current.screenWidthDp < Dimens.tabletBreakpoint.value
            // —— Zone I: Top HUD ——
            PremiumHeader(
                title = state.shopName,
                subtitle = "${state.orderCount} served today",
                action = {
                    ShopHeaderBackAction(onNavigateBack)
                }
            )

            Spacer(modifier = Modifier.height(Dimens.space12))

            if (compact) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.space12)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                        contentPadding = PaddingValues(vertical = Dimens.space4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems(state.categories, key = { it.id }) { category ->
                            val selected = category.id == state.selectedCategoryId
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) PosGold.copy(alpha = 0.18f) else Color.White,
                                        RoundedCornerShape(Dimens.radiusXLarge)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) PosGold else Color(0xFFD8CDBE),
                                        RoundedCornerShape(Dimens.radiusXLarge)
                                    )
                                    .clickable { state.onCategorySelected(category.id) }
                                    .padding(horizontal = Dimens.space16, vertical = Dimens.space12)
                            ) {
                                Text(
                                    category.name,
                                    color = if (selected) PosGold else Color(0xFF675C52),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    ShopModeTabRow(
                        mode = state.shopMode,
                        onModeChange = state.onModeToggled,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Dimens.space12))

                    ShopItemGrid(
                        items = state.gridItems,
                        selectedItemId = state.selectedItemId,
                        selectedQuantities = state.selectedQuantities,
                        onItemTapped = state.onItemTapped,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(visible = state.selectedItem != null) {
                        PremiumPanel(title = null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                BuildPathHeader(
                                    enabledNodeCount = state.buildPathNodes.count { it.isIncluded() },
                                    allIncluded = state.allAddonsIncluded,
                                    onAddAllTapped = state.onAddAllTapped
                                )
                                state.buildPathRoot?.let { root ->
                                    Spacer(modifier = Modifier.height(Dimens.space8))
                                    BuildPathTree(
                                        root = root,
                                        onNodeToggled = state.onBuildNodeToggled,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                ItemDetailDock(
                                    baseItem = state.selectedItem,
                                    activeModifiers = state.activeModifiers,
                                    perkText = state.perkText,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.weight(1f).animateContentSize()) {
                    CategoryRail(
                        categories = state.categories,
                        selectedId = state.selectedCategoryId,
                        onSelect = state.onCategorySelected,
                        modifier = Modifier.width(200.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(Dimens.space16)
                    ) {
                        ShopModeTabRow(
                            mode = state.shopMode,
                            onModeChange = state.onModeToggled,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Dimens.space12))
                        ShopItemGrid(
                            items = state.gridItems,
                            selectedItemId = state.selectedItemId,
                            selectedQuantities = state.selectedQuantities,
                            onItemTapped = state.onItemTapped,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(visible = state.selectedItem != null) {
                        PremiumPanel(
                            modifier = Modifier
                                .widthIn(min = 280.dp, max = 340.dp)
                                .fillMaxHeight(),
                            title = null
                        ) {
                            BuildPathHeader(
                                enabledNodeCount = state.buildPathNodes.count { it.isIncluded() },
                                allIncluded = state.allAddonsIncluded,
                                onAddAllTapped = state.onAddAllTapped
                            )

                            Spacer(modifier = Modifier.height(Dimens.space12))

                            val rootNode = state.buildPathRoot
                            if (rootNode != null) {
                                BuildPathTree(
                                    root = rootNode,
                                    onNodeToggled = state.onBuildNodeToggled,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(Dimens.space12))

                                ItemDetailDock(
                                    baseItem = state.selectedItem,
                                    activeModifiers = state.activeModifiers,
                                    perkText = state.perkText,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // —— Zone H: Purchase bar (pinned bottom) ——
            PurchaseBar(
                totalCents = state.totalCents,
                onPurchase = {
                    state.onPurchaseTapped()
                    onNavigateToCheckoutConfirm()
                },
                quickActions = state.quickActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 104.dp else 120.dp)
            )
        }
    }
}

/** Check if a BuildPathNodeUi (or any descendant) is included. */
private fun BuildPathNodeUi.isIncluded(): Boolean {
    return included || children.any { it.isIncluded() }
}

/**
 * ViewModel-backed entry point for the Item Shop screen.
 *
 * Collects state from [ItemShopViewModel] and maps it into a pure [ShopScreenState],
 * then delegates to the stateless [ItemShopPOSScreen] overload. This keeps the
 * composable pure (all state flows in) while the ViewModel owns the data.
 *
 * All tap interactions are wired to [GameFeedback] (haptics/SFX) per spec §5.3.
 */
@Composable
fun ItemShopPOSScreen(
    viewModel: ItemShopViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCheckoutConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // —— Collect ViewModel state ——
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val visibleProducts by viewModel.visibleProducts.collectAsState()
    val selectedProductId by viewModel.selectedProductId.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val comboTree by viewModel.comboTree.collectAsState()
    val totalCents by viewModel.totalCents.collectAsState()

    // Map the selected Product (Room entity) → ShopItemUi for the detail dock.
    val selectedItem = allProducts.find { it.id == selectedProductId }
        ?.let { p ->
            ShopItemUi(
                id = p.id,
                name = p.name,
                description = p.description,
                price = p.price,
                rarity = rarityByPrice(p.price),
                imageUrl = p.imageUrl
            )
        }

    val state = ShopScreenState(
        shopName = "Pebot",
        orderCount = 0,
        categories = categories,
        selectedCategoryId = selectedCategoryId ?: 0,
        gridItems = visibleProducts,
        selectedItemId = selectedProductId,
        selectedItem = selectedItem,
        buildPathRoot = comboTree,
        totalCents = totalCents,
        quickActions = sampleQuickActions,
        onCategorySelected = { viewModel.selectCategory(it) },
        onModeToggled = { /* mode is a UI-only toggle; could persist to SettingsStore */ },
        onItemTapped = {
            GameFeedback.tap(context)
            viewModel.selectProduct(it.id)
        },
        onBuildNodeToggled = { nodeId ->
            GameFeedback.tap(context)
            viewModel.toggleAddon(nodeId)
        },
        onAddAllTapped = {
            GameFeedback.confirm(context)
            viewModel.addAllAddons()
        },
        onPurchaseTapped = {
            GameFeedback.confirm(context)
            scope.launch {
                val orderId = viewModel.purchaseOrder()
                if (orderId > 0L) {
                    onNavigateToCheckoutConfirm()
                }
            }
        }
    )

    ItemShopPOSScreen(
        state = state,
        onNavigateBack = {
            GameFeedback.tap(context)
            onNavigateBack()
        },
        onNavigateToCheckoutConfirm = {},
        modifier = modifier
    )
}

/** Simple item grid (Zone C) — wraps ShopItemCard in a LazyVerticalGrid. */
@Composable
private fun ShopItemGrid(
    items: List<ShopItemUi>,
    selectedItemId: Int?,
    selectedQuantities: Map<Int, Int>,
    onItemTapped: (ShopItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No products in this category",
                color = Color(0xFF918B84),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 144.dp),
        contentPadding = PaddingValues(Dimens.space8),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
        modifier = modifier
    ) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id == selectedItemId
            val qty = selectedQuantities[item.id] ?: 0
            ShopItemCard(
                item = item,
                selected = isSelected,
                quantity = qty,
                onTap = { onItemTapped(item) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShopHeaderBackAction(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(PosGold.copy(alpha = 0.15f), shape = RoundedCornerShape(Dimens.radiusXLarge))
            .border(1.dp, PosGold.copy(alpha = 0.5f), shape = RoundedCornerShape(Dimens.radiusXLarge))
            .gameTap(onClick)
            .padding(horizontal = Dimens.space12, vertical = Dimens.space4),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "← List View",
            color = PosGold,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── State ────────────────────────────────────────────────────────────

/**
 * Pure UI state for [ItemShopPOSScreen].
 * The ViewModel (Phase 2) produces this; for Phase 1 the @Preview uses sample
 * data. Every field is a plain value — no StateFlow/Room entities (spec §5.1:
 * "never pass Room entities directly into Composables").
 */
data class ShopScreenState(
    val shopName: String = "Pebot",
    val orderCount: Int = 0,
    val categories: List<CategoryUi> = emptyList(),
    val selectedCategoryId: Int = 0,
    val shopMode: ShopMode = ShopMode.ALL,
    val gridItems: List<ShopItemUi> = emptyList(),
    val selectedItemId: Int? = null,
    val selectedQuantities: Map<Int, Int> = emptyMap(),
    val selectedItem: ShopItemUi? = null,
    val buildPathRoot: BuildPathNodeUi? = null,
    val allAddonsIncluded: Boolean = false,
    val activeModifiers: List<ModifierUi> = emptyList(),
    val perkText: String? = null,
    val totalCents: Long = 0L,
    val quickActions: List<QuickActionUi> = emptyList(),
    val onCategorySelected: (Int) -> Unit = {},
    val onModeToggled: (ShopMode) -> Unit = {},
    val onItemTapped: (ShopItemUi) -> Unit = {},
    val onBuildNodeToggled: (String) -> Unit = {},
    val onAddAllTapped: () -> Unit = {},
    val onPurchaseTapped: () -> Unit = {}
) {
    val buildPathNodes: List<BuildPathNodeUi>
        get() = buildPathRoot?.children ?: emptyList()
}

// ─── Sample data helpers ───────────────────────────────────────────────

private fun BuildPathNodeUi.toActiveModifiers(): List<ModifierUi> =
    (if (included) listOf(ModifierUi(id, name, deltaPrice, iconRes)) else emptyList()) +
        children.flatMap { it.toActiveModifiers() }

private fun BuildPathNodeUi.allIncluded(): Boolean =
    included && children.all { it.allIncluded() }

// ─── Previews ──────────────────────────────────────────────────────────

@Preview(name = "Item Shop - Gamified")
@Composable
private fun ItemShopScreenPreviewGamified() {
    val cats = SampleDataProvider.categories
    val items = SampleDataProvider.allProducts
    val root = SampleDataProvider.sampleBuildPath
    PebotTheme(themeMode = ThemeMode.GAMIFIED) {
        ItemShopPOSScreen(
            state = ShopScreenState(
                shopName = "Pebot",
                orderCount = 23,
                categories = cats,
                selectedCategoryId = cats[1].id,
                shopMode = ShopMode.ALL,
                gridItems = items,
                selectedItemId = 101,
                selectedQuantities = mapOf(101 to 2),
                selectedItem = items[0],
                buildPathRoot = root,
                allAddonsIncluded = root.allIncluded(),
                activeModifiers = root.toActiveModifiers(),
                perkText = "Buy 5 get 1 free • 2 more to go",
                totalCents = 23500L,
                quickActions = sampleQuickActions,
            ),
            onNavigateBack = {},
            onNavigateToCheckoutConfirm = {}
        )
    }
}

@Preview(name = "Item Shop - Classic")
@Composable
private fun ItemShopScreenPreviewClassic() {
    val cats = SampleDataProvider.categories
    val items = SampleDataProvider.allProducts
    val root = SampleDataProvider.sampleBuildPath
    PebotTheme(themeMode = ThemeMode.CLASSIC) {
        ItemShopPOSScreen(
            state = ShopScreenState(
                shopName = "Pebot",
                orderCount = 23,
                categories = cats,
                selectedCategoryId = cats[1].id,
                shopMode = ShopMode.ALL,
                gridItems = items,
                selectedItemId = 101,
                selectedQuantities = mapOf(101 to 2),
                selectedItem = items[0],
                buildPathRoot = root,
                allAddonsIncluded = root.allIncluded(),
                activeModifiers = root.toActiveModifiers(),
                perkText = "Buy 5 get 1 free • 2 more to go",
                totalCents = 23500L,
                quickActions = sampleQuickActions,
            ),
            onNavigateBack = {},
            onNavigateToCheckoutConfirm = {}
        )
    }
}
