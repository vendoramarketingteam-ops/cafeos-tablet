package com.cafeos.tablet.ui.screens

import com.cafeos.tablet.ui.components.GameCard
import com.cafeos.tablet.ui.components.Rarity
import com.cafeos.tablet.ui.components.RarityBadge

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.data.Product
import com.cafeos.tablet.ui.CafeViewModel
import com.cafeos.tablet.ui.CartItem
import com.cafeos.tablet.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.NumberFormat
import java.util.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.cafeos.tablet.data.Order
import com.cafeos.tablet.R
import androidx.compose.animation.core.animateFloatAsState
import com.cafeos.tablet.ui.components.GemCounter
import com.cafeos.tablet.ui.components.StackCounter
import com.cafeos.tablet.ui.gameTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    viewModel: CafeViewModel,
    onNavigateToLoyalty: () -> Unit = {},
    onNavigateToItemShop: () -> Unit = {}
) {
    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val orders by viewModel.allOrders.collectAsState(initial = emptyList())
    val todayOrderCount by viewModel.todayOrderCount.collectAsState()
    val businessSettings by viewModel.businessSettings.collectAsState(initial = null)
    val cartItems = viewModel.cartItems
    val totalAmount by viewModel.totalAmount.collectAsState()
    val showProductOptions by viewModel.showProductOptions.collectAsState()
    val showCheckout by viewModel.showCheckout.collectAsState()
    val placedOrder by viewModel.placedOrder.collectAsState()
    val selectedTableId by viewModel.selectedTableId.collectAsState()

    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var compactPane by rememberSaveable { mutableStateOf(0) }
    var optionGroups by remember { mutableStateOf<List<com.cafeos.tablet.data.OptionGroup>>(emptyList()) }
    var groupOptions by remember { mutableStateOf<Map<Int, List<com.cafeos.tablet.data.Option>>>(emptyMap()) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val startOfToday = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val todayOrders = orders.filter { it.createdAt >= startOfToday && it.status != "CANCELLED" }
    val quotaTarget = businessSettings?.dailyQuotaTarget ?: 0.0
    val quotaMode = businessSettings?.dailyQuotaMode ?: "ORDERS"
    val todayProductUnits by remember(startOfToday) { viewModel.getProductUnitCount(startOfToday, System.currentTimeMillis()) }.collectAsState(initial = 0)
    val quotaCurrent = if (quotaMode == "REVENUE") todayOrders.sumOf { it.totalAmount } else todayProductUnits.toDouble()

    LaunchedEffect(showProductOptions) {
        showProductOptions?.let { product ->
            val groups = viewModel.getOptionGroupsForProduct(product.id)
            optionGroups = groups
            val opts = mutableMapOf<Int, List<com.cafeos.tablet.data.Option>>()
            for (g in optionGroups) {
                opts[g.id] = viewModel.getOptionsForGroup(g.id)
            }
            groupOptions = opts
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.cartItems }.collect {
            // Cart updated
        }
    }

    val isPortraitPhone = LocalConfiguration.current.screenWidthDp < 600

    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = if (selectedCategoryId == null) {
        products.filter { it.available && it.name.contains(searchQuery, ignoreCase = true) }
    } else {
        products.filter { it.available && it.categoryId == selectedCategoryId && it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (isPortraitPhone) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PosCoffee)
        ) {
            Surface(
                color = PosCoffeeLight,
                shadowElevation = Dimens.elevationLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    FilledTonalButton(
                        onClick = { compactPane = 0 },
                        modifier = Modifier.weight(1f).heightIn(min = Dimens.touchMin),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (compactPane == 0) PosAccentSoft else PosSurface,
                            contentColor = if (compactPane == 0) PosAccent else PosInkSoft
                        )
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null)
                        Spacer(Modifier.width(Dimens.space8))
                        Text("Menu")
                    }
                    FilledTonalButton(
                        onClick = { compactPane = 1 },
                        modifier = Modifier.weight(1f).heightIn(min = Dimens.touchMin),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (compactPane == 1) PosAccentSoft else PosSurface,
                            contentColor = if (compactPane == 1) PosAccent else PosInkSoft
                        )
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null)
                        Spacer(Modifier.width(Dimens.space8))
                        Text("Order (${cartItems.sumOf { it.quantity }})", maxLines = 1)
                    }
                }
            }
            if (compactPane == 0) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(Dimens.space12)
                ) {
                    PosMenuContent(
                        viewModel = viewModel,
                        products = products,
                        categories = categories,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { selectedCategoryId = it },
                        filteredProducts = filteredProducts,
                        currencyFormatter = currencyFormatter,
                        todayOrderCount = todayOrderCount,
                        businessSettings = businessSettings,
                        onNavigateToLoyalty = onNavigateToLoyalty,
                        onNavigateToItemShop = onNavigateToItemShop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(PosSurface)
                        .padding(Dimens.space12)
                ) {
                    PosCartContent(
                        viewModel = viewModel,
                        tables = tables,
                        cartItems = cartItems,
                        currencyFormatter = currencyFormatter,
                        totalAmount = totalAmount,
                        selectedTableId = selectedTableId,
                        quotaTarget = quotaTarget,
                        quotaMode = quotaMode,
                        quotaCurrent = quotaCurrent
                    )
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Menu (left, scrollable) ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(PosCoffee)
                    .padding(Dimens.space16)
            ) {
                PosMenuContent(
                    viewModel = viewModel,
                    products = products,
                    categories = categories,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it },
                    filteredProducts = filteredProducts,
                    currencyFormatter = currencyFormatter,
                    todayOrderCount = todayOrderCount,
                    businessSettings = businessSettings,
                    onNavigateToLoyalty = onNavigateToLoyalty,
                    onNavigateToItemShop = onNavigateToItemShop
                )
            }
            // ── Cart (right, fixed width) ──
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 380.dp)
                    .fillMaxHeight()
                    .background(PosSurface)
                    .padding(Dimens.space16)
            ) {
                PosCartContent(
                    viewModel = viewModel,
                    tables = tables,
                    cartItems = cartItems,
                    currencyFormatter = currencyFormatter,
                    totalAmount = totalAmount,
                    selectedTableId = selectedTableId,
                    quotaTarget = quotaTarget,
                    quotaMode = quotaMode,
                    quotaCurrent = quotaCurrent
                )
            }
        }
    }

    showProductOptions?.let { product ->
        ProductOptionsBottomSheet(
            product = product,
            optionGroups = optionGroups,
            groupOptions = groupOptions,
            onDismiss = { viewModel.closeProductOptions() },
            onConfirm = { selectedOptions, priceDelta ->
                viewModel.addToCart(product, selectedOptions, priceDelta)
                viewModel.closeProductOptions()
                if (isPortraitPhone) compactPane = 1
            }
        )
    }

    if (showCheckout) {
        CheckoutDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeCheckout() }
        )
    }

    placedOrder?.let { order ->
        OrderConfirmationDialog(
            viewModel = viewModel,
            order = order,
            onDismiss = { viewModel.dismissPlacedOrder() }
        )
    }
}


/**
 * Menu section shared by both portrait (Column) and landscape (Row) layouts.
 * Contains the shop header, search bar, category tabs, and the scrollable
 * product grid.
 */
@Composable
private fun PosMenuContent(
    viewModel: CafeViewModel,
    products: List<Product>,
    categories: List<com.cafeos.tablet.data.Category>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    filteredProducts: List<Product>,
    currencyFormatter: NumberFormat,
    todayOrderCount: Int,
    businessSettings: com.cafeos.tablet.data.BusinessSettings?,
    onNavigateToLoyalty: () -> Unit,
    onNavigateToItemShop: () -> Unit,
) {
    val compact = LocalConfiguration.current.screenWidthDp < Dimens.tabletBreakpoint.value
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
    // Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = businessSettings?.shopName ?: "Menu",
            style = GameHeader,
            color = PosInk
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            if (!compact) {
                Text("$todayOrderCount served today", style = MaterialTheme.typography.labelMedium, color = PosInkSoft)
            }
            GemCounter(
                count = todayOrderCount,
                onClick = onNavigateToLoyalty
            )
            if (!isClassic()) {
                Box(
                    modifier = Modifier
                        .size(Dimens.space32)
                        .background(PosGold.copy(alpha = 0.1f), shape = RoundedCornerShape(Dimens.radiusSmall))
                        .border(1.dp, PosGold.copy(alpha = 0.4f), shape = RoundedCornerShape(Dimens.radiusSmall))
                        .gameTap(onNavigateToItemShop)
                        .padding(Dimens.space4),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Open Item Shop",
                        tint = PosGold
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(Dimens.space12))

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        label = { Text("Search products") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PosInkSoft) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PosAccent,
            unfocusedBorderColor = PosBorder,
            focusedTextColor = PosInk,
            unfocusedTextColor = PosInk,
            cursorColor = PosAccent,
            disabledTextColor = PosInkSoft
        ),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(Dimens.space12))

    ScrollableTabRow(
        selectedTabIndex = categories.indexOfFirst { it.id == selectedCategoryId }.let { if (it == -1) 0 else it + 1 },
        edgePadding = 0.dp,
        divider = {},
        containerColor = PosCoffeeLight,
        contentColor = PosPaper
    ) {
        Tab(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            text = { Text("All", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selectedContentColor = PosGold,
            unselectedContentColor = PosMuted
        )
        categories.forEach { category ->
            Tab(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                text = { Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selectedContentColor = PosGold,
                unselectedContentColor = PosMuted
            )
        }
    }
    Spacer(modifier = Modifier.height(Dimens.space16))

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (compact) 136.dp else 160.dp),
        contentPadding = PaddingValues(Dimens.space4),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
        modifier = Modifier.weight(1f)
    ) {
        items(filteredProducts) { product ->
            ProductCard(
                product = product,
                formatter = currencyFormatter,
                onClick = { viewModel.openProductOptions(product) }
            )
        }
    }
    }
}

/**
 * Cart / order-summary section shared by both portrait and landscape layouts.
 * Contains the order header, quota bar, table picker, scrollable cart items,
 * total, and Checkout button.
 */
@Composable
private fun PosCartContent(
    viewModel: CafeViewModel,
    tables: List<com.cafeos.tablet.data.CafeTable>,
    cartItems: List<CartItem>,
    currencyFormatter: NumberFormat,
    totalAmount: Double,
    selectedTableId: Int?,
    quotaTarget: Double,
    quotaMode: String,
    quotaCurrent: Double,
) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Current Order", style = MaterialTheme.typography.titleLarge, color = PosInk)
        if (cartItems.isNotEmpty()) {
            TextButton(onClick = { viewModel.clearCart() }) {
                Text("Clear", color = PosDanger, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Spacer(modifier = Modifier.height(Dimens.space8))

    if (quotaTarget > 0.0) {
        val quotaProgress = (quotaCurrent / quotaTarget).coerceIn(0.0, 1.0).toFloat()
        Surface(color = if (quotaProgress >= 1f) PosAccentSoft else PosCoffee, shape = RoundedCornerShape(Dimens.radiusMedium)) {
            Column(Modifier.fillMaxWidth().padding(Dimens.space12), verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily quota", color = PosInkSoft, style = MaterialTheme.typography.labelMedium)
                    Text(if (quotaMode == "REVENUE") "${currencyFormatter.format(quotaCurrent)} / ${currencyFormatter.format(quotaTarget)}" else "${quotaCurrent.toInt()} / ${quotaTarget.toInt()} products", color = PosAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = quotaProgress, modifier = Modifier.fillMaxWidth().height(Dimens.progressHeight), color = PosAccent, trackColor = PosBorder)
            }
        }
        Spacer(modifier = Modifier.height(Dimens.space8))
    }

    Text("Table", style = MaterialTheme.typography.labelMedium, color = PosInkSoft)
    Spacer(modifier = Modifier.height(Dimens.progressHeight))
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 92.dp),
        modifier = Modifier.height(112.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.space8)
    ) {
        items(tables, key = { it.id }) { table ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setSelectedTableId(table.id) },
                color = if (selectedTableId == table.id) PosAccentSoft else PosCoffee,
                shape = RoundedCornerShape(Dimens.space8),
                border = androidx.compose.foundation.BorderStroke(Dimens.borderWidth, if (selectedTableId == table.id) PosAccent else PosBorder)
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(table.name, color = PosInk, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${table.currentOccupants}/${table.capacity}", color = PosInkSoft, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(Dimens.space8))

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Dimens.space8)
    ) {
        items(cartItems) { item ->
            CartItemRow(item, currencyFormatter, viewModel)
        }
    }
    Spacer(modifier = Modifier.height(Dimens.space12))
    Divider(color = PosBorder)
    Spacer(modifier = Modifier.height(Dimens.space12))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Total", style = MaterialTheme.typography.titleLarge, color = PosInkSoft)
        Text(
            currencyFormatter.format(totalAmount),
            style = MaterialTheme.typography.headlineMedium,
            color = PosGold,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(Dimens.space16))

    Button(
        onClick = { viewModel.openCheckout() },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        enabled = cartItems.isNotEmpty(),
        shape = RoundedCornerShape(Dimens.space12),
        colors = ButtonDefaults.buttonColors(containerColor = PosAccent, disabledContainerColor = PosMuted)
    ) {
        Text("Checkout", fontWeight = FontWeight.SemiBold)
    }
    }
}

/**
 * Cosmetic price-tier rarity for the shop grid. Premium-priced items surface as
 * RARE/EPIC corner badges; the rest stay COMMON. This is a presentational label
 * over REAL price data — no business values are fabricated. True best-seller /
 * low-stock rarity requires upstream VM data (out of scope for a re-skin; see
 * specs/001-gamified-pos-ui/tasks.md T002).
 */
private fun rarityByPrice(price: Double): Rarity = when {
    price >= 150.0 -> Rarity.EPIC
    price >= 80.0 -> Rarity.RARE
    price >= 40.0 -> Rarity.UNCOMMON
    else -> Rarity.COMMON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(product: Product, formatter: NumberFormat, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(product.imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(product.imageUrl) {
        bitmap = null
        val normalizedPath = when {
            product.imageUrl.isNullOrBlank() -> null
            product.imageUrl.startsWith("file://") -> android.net.Uri.parse(product.imageUrl).path
            product.imageUrl.startsWith("content://") -> product.imageUrl
            else -> product.imageUrl
        }

        val file = normalizedPath?.let {
            if (it.startsWith("content://")) {
                null
            } else {
                java.io.File(it)
            }
        }

        bitmap = when {
            file != null && file.exists() -> android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            normalizedPath != null && normalizedPath.startsWith("content://") -> {
                try {
                    val input = context.contentResolver.openInputStream(android.net.Uri.parse(normalizedPath))
                    val decoded = input?.let { android.graphics.BitmapFactory.decodeStream(it) }
                    input?.close()
                    decoded
                } catch (_: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    val rarity = rarityByPrice(product.price)
    GameCard(
        rarity = rarity,
        modifier = Modifier
            .fillMaxWidth()
            .gameTap(onClick)
    ) {
        Column(modifier = Modifier.padding(Dimens.space12)) {
            if (rarity != Rarity.COMMON) {
                RarityBadge(
                    rarity = rarity,
                    label = rarity.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                )
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(Dimens.space8))
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = PosInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(Dimens.progressHeight))
            Text(
                text = formatter.format(product.price),
                style = MaterialTheme.typography.bodyLarge,
                color = PosAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, formatter: NumberFormat, viewModel: CafeViewModel) {
    GameCard(
        rarity = rarityByPrice(item.product.price),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.bodyLarge, color = PosInk, fontWeight = FontWeight.Medium)
                if (item.selectedOptions.isNotEmpty()) {
                    Text(
                        text = item.selectedOptions.joinToString(", ") { it.second },
                        style = MaterialTheme.typography.bodySmall,
                        color = PosInkSoft
                    )
                }
                Text(
                    text = formatter.format(item.product.price * item.quantity + item.priceDelta * item.quantity),
                    style = MaterialTheme.typography.labelMedium,
                    color = PosAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.updateCartItemQuantity(item, item.quantity - 1) },
                    modifier = Modifier.size(Dimens.touchMin)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PosInkSoft, modifier = Modifier.size(Dimens.space16))
                }
                StackCounter(quantity = item.quantity, modifier = Modifier.padding(horizontal = Dimens.space4))
                IconButton(
                    onClick = { viewModel.updateCartItemQuantity(item, item.quantity + 1) },
                    modifier = Modifier.size(Dimens.touchMin)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = PosInk, modifier = Modifier.size(Dimens.space16))
                }
                IconButton(
                    onClick = { viewModel.removeFromCart(item) },
                    modifier = Modifier.size(Dimens.touchMin)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = PosDanger, modifier = Modifier.size(Dimens.space16))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductOptionsBottomSheet(
    product: Product,
    optionGroups: List<com.cafeos.tablet.data.OptionGroup>,
    groupOptions: Map<Int, List<com.cafeos.tablet.data.Option>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<String, String>>, Double) -> Unit
) {
    val selectedValues = remember { mutableStateMapOf<Int, String>() }
    val selectedMultiValues = remember { mutableStateMapOf<Int, MutableList<String>>() }

    LaunchedEffect(optionGroups) {
        selectedValues.clear()
        selectedMultiValues.clear()
        for (group in optionGroups) {
            val opts = groupOptions[group.id] ?: emptyList()
            if (group.selectionType.equals("single", ignoreCase = true) && opts.isNotEmpty()) {
                selectedValues[group.id] = opts.first().name
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.space8
    ) {
        // Scrollable so every group + the "Add to Order" button stay reachable
        // on any screen height (clipped content hid the confirm button before).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.space20)
        ) {
            Text(product.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(Dimens.space16))

            if (optionGroups.isEmpty()) {
                Text("No options configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Dimens.space24))
                Button(onClick = {
                    onConfirm(emptyList(), 0.0)
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.space12), colors = ButtonDefaults.buttonColors(containerColor = PosAccent)) {
                    Text("Add to Order", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(Dimens.space16))
                return@ModalBottomSheet
            }

            for (group in optionGroups) {
                val options = groupOptions[group.id] ?: emptyList()
                Text(group.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Dimens.space8))
                if (group.selectionType.equals("single", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                        for (option in options) {
                            val isSelected = selectedValues[group.id] == option.name
                            val leadingIcon: @Composable (() -> Unit)? = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.space16)) }
                            } else null
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedValues[group.id] = option.name },
                                label = { Text("${option.name}${if (option.priceDelta > 0) " +₱${option.priceDelta}" else ""}") },
                                leadingIcon = leadingIcon,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosAccentSoft,
                                    selectedLabelColor = PosAccent
                                )
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                        for (option in options) {
                            val current = selectedMultiValues.getOrPut(group.id) { mutableStateListOf() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (current.contains(option.name)) current.remove(option.name) else current.add(option.name)
                                    }
                                    .padding(vertical = Dimens.space8),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${option.name}${if (option.priceDelta > 0) " +₱${option.priceDelta}" else ""}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Checkbox(
                                    checked = current.contains(option.name),
                                    onCheckedChange = { checked ->
                                        if (checked) current.add(option.name) else current.remove(option.name)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = PosAccent)
                                )
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.space16))
            }

            val totalDelta = optionGroups.sumOf { group ->
                val options = groupOptions[group.id] ?: emptyList()
                if (group.selectionType.equals("single", ignoreCase = true)) {
                    val selected = selectedValues[group.id]
                    options.find { it.name == selected }?.priceDelta ?: 0.0
                } else {
                    val selected = selectedMultiValues[group.id] ?: emptyList()
                    options.filter { selected.contains(it.name) }.sumOf { it.priceDelta }
                }
            }

            val finalOptions = optionGroups.flatMap { group ->
                val options = groupOptions[group.id] ?: emptyList()
                if (group.selectionType.equals("single", ignoreCase = true)) {
                    val selected = selectedValues[group.id]
                    listOf(group.name to (selected ?: ""))
                } else {
                    val selected = selectedMultiValues[group.id] ?: emptyList()
                    selected.map { group.name to it }
                }
            }

            Button(
                onClick = {
                    onConfirm(finalOptions, totalDelta)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.space12),
                colors = ButtonDefaults.buttonColors(containerColor = PosAccent)
            ) {
                Text("Add to Order", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(Dimens.space16))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(viewModel: CafeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val totalAmount by viewModel.totalAmount.collectAsState()
    val subtotalAmount by viewModel.subtotalAmount.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val amountTendered by viewModel.amountTendered.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val discountType by viewModel.discountType.collectAsState()
    val discountRate by viewModel.discountRate.collectAsState()
    val orderType by viewModel.orderType.collectAsState()
    val deliveryAddress by viewModel.deliveryAddress.collectAsState()
    val selectedTableId by viewModel.selectedTableId.collectAsState()
    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val businessSettings by viewModel.businessSettings.collectAsState(initial = null)
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val change = (amountTendered.toDoubleOrNull() ?: 0.0) - totalAmount
    val deliveryMissing = orderType == "DELIVERY" && deliveryAddress.isBlank()
    val canComplete = (paymentMethod != "CASH" || (amountTendered.toDoubleOrNull() ?: 0.0) >= totalAmount) && !deliveryMissing

    var tableExpanded by remember { mutableStateOf(false) }
    var showInvoiceReview by remember { mutableStateOf(false) }
    val selectedTable = tables.find { it.id == selectedTableId }
    val selectedTableLabel = selectedTable?.name ?: "No table selected"

    // Skippable count-up reveal of the total (constitution IV: 0-duration in Fast Mode).
    var revealTotal by remember { mutableStateOf(0f) }
    LaunchedEffect(totalAmount) { revealTotal = totalAmount.toFloat() }
    val animatedTotal by animateFloatAsState(
        targetValue = revealTotal,
        animationSpec = currencyCountUpSpec()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checkout", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currencyFormatter.format(animatedTotal.toDouble()), style = MaterialTheme.typography.headlineMedium, color = PosGold, fontWeight = FontWeight.Bold)
                }

                if (showInvoiceReview) {
                    InvoiceReview(
                        viewModel = viewModel,
                        orderTotal = totalAmount,
                        customerName = customerName,
                        tableLabel = selectedTableLabel,
                        paymentMethod = paymentMethod,
                        discountType = discountType,
                        amountTendered = amountTendered,
                        change = change,
                        currencyFormatter = currencyFormatter
                    )
                } else {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.setCustomerName(it) },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PosAccent, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
                        Column(Modifier.weight(1f)) {
                            Text("Table", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(Dimens.progressHeight))
                            ExposedDropdownMenuBox(expanded = tableExpanded, onExpandedChange = { tableExpanded = it }) {
                                OutlinedTextField(
                                    value = selectedTableLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tableExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PosAccent, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                )
                                ExposedDropdownMenu(expanded = tableExpanded, onDismissRequest = { tableExpanded = false }) {
                                    tables.forEach { table ->
                                        DropdownMenuItem(text = { Text("${table.name} (${table.currentOccupants}/${table.capacity})") }, onClick = { viewModel.setSelectedTableId(table.id); tableExpanded = false })
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Voucher", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(Dimens.progressHeight))
                            VoucherApplicationSection(viewModel = viewModel, totalAmount = subtotalAmount)
                        }
                    }

                    Column(Modifier.fillMaxWidth()) {
                        Text("Order Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Dimens.progressHeight))
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                            listOf(
                                Pair("DINE_IN", "Dine-in"),
                                Pair("TAKEOUT", "Takeout"),
                                Pair("DELIVERY", "Delivery")
                            ).forEach { (value, label) ->
                                FilterChip(
                                    selected = orderType == value,
                                    onClick = {
                                        viewModel.setOrderType(value)
                                        if (value != "DELIVERY") viewModel.setDeliveryAddress("")
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PosAccentSoft, selectedLabelColor = PosAccent)
                                )
                            }
                        }
                        if (orderType == "DELIVERY") {
                            Spacer(Modifier.height(Dimens.space8))
                            OutlinedTextField(
                                value = deliveryAddress,
                                onValueChange = { viewModel.setDeliveryAddress(it) },
                                label = { Text("Delivery Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = deliveryMissing,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PosAccent, unfocusedBorderColor = MaterialTheme.colorScheme.outline, errorBorderColor = PosDanger)
                            )
                            if (deliveryMissing) {
                                Spacer(Modifier.height(Dimens.space4))
                                Text("A delivery address is required for delivery orders", style = MaterialTheme.typography.bodySmall, color = PosDanger)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
                        Column(Modifier.weight(1f)) {
                            Text("Discount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(Dimens.progressHeight))
                            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                                listOf("NONE", "PWD", "STUDENT").forEach { type ->
                                    FilterChip(selected = discountType == type, onClick = { viewModel.setDiscountType(type) }, label = { Text(if (type == "NONE") "None" else type) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PosAccentSoft, selectedLabelColor = PosAccent))
                                }
                            }
                            if (discountType != "NONE" && discountType != "VOUCHER") Text("${(discountRate * 100).roundToInt()}% discount applied", style = MaterialTheme.typography.bodySmall, color = PosAccent, fontWeight = FontWeight.Medium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Payment Method", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(Dimens.progressHeight))
                            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                                listOf("CASH", "GCASH", "PAYMAYA").forEach { method ->
                                    FilterChip(selected = paymentMethod == method, onClick = { viewModel.setPaymentMethod(method) }, label = { Text(method) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PosAccentSoft, selectedLabelColor = PosAccent))
                                }
                            }
                        }
                    }

                if (paymentMethod == "CASH") {
                    OutlinedTextField(
                        value = amountTendered,
                        onValueChange = { viewModel.setAmountTendered(it) },
                        label = { Text("Amount Tendered") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PosAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = PosInk,
                            unfocusedTextColor = PosInk,
                            cursorColor = PosAccent
                        )
                    )
                    Text(
                        text = "Change: ${currencyFormatter.format(change.coerceAtLeast(0.0))}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (change >= 0) PosAccent else PosDanger,
                        fontWeight = FontWeight.Bold
                    )
                }

                val screenshotPath by viewModel.screenshotPath.collectAsState()
                val screenshotLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
                    if (uri != null) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val file = java.io.File(context.filesDir, "payment_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                        }
                        viewModel.setScreenshotPath(file.absolutePath)
                    }
                }

                if (paymentMethod == "GCASH" || paymentMethod == "PAYMAYA") {
                    val accountName = if (paymentMethod == "GCASH") businessSettings?.gcashAccountName else businessSettings?.paymayaAccountName
                    val qrPath = if (paymentMethod == "GCASH") businessSettings?.gcashQrPath else businessSettings?.paymayaQrPath
                    Text("${paymentMethod} payment details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    accountName?.takeIf { it.isNotBlank() }?.let {
                        Text("Account: $it", color = PosInk, fontWeight = FontWeight.SemiBold)
                    }
                    qrPath?.let { path ->
                        val qrBitmap = remember(path) { BitmapFactory.decodeFile(path) }
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "$paymentMethod QR code",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Text("Payment Proof", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Dimens.space8))
                    if (screenshotPath != null) {
                        val file = java.io.File(screenshotPath!!)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Payment proof",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clickable { screenshotLauncher.launch("image/*") },
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { screenshotLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(Dimens.borderWidth, PosBorder)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PosGold, modifier = Modifier.size(Dimens.space24))
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text("Upload Payment Screenshot", color = PosGold)
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (showInvoiceReview) viewModel.placeOrder() else showInvoiceReview = true },
                enabled = canComplete,
                shape = RoundedCornerShape(Dimens.radiusMedium),
                colors = ButtonDefaults.buttonColors(containerColor = PosAccent)
            ) {
                Text(if (showInvoiceReview) "Confirm & Complete" else "Review Invoice", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (showInvoiceReview) showInvoiceReview = false else onDismiss() }) {
                Text(if (showInvoiceReview) "Back" else "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusXLarge)
    )
}

@Composable
private fun InvoiceReview(
    viewModel: CafeViewModel,
    orderTotal: Double,
    customerName: String,
    tableLabel: String,
    paymentMethod: String,
    discountType: String,
    amountTendered: String,
    change: Double,
    currencyFormatter: NumberFormat
) {
    val items = viewModel.cartItems
    Surface(color = PosCoffee, shape = RoundedCornerShape(Dimens.space12), border = androidx.compose.foundation.BorderStroke(Dimens.borderWidth, PosBorder)) {
        Column(Modifier.padding(Dimens.space16), verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Invoice review", style = MaterialTheme.typography.titleLarge, color = PosInk, fontWeight = FontWeight.Bold)
                Text("${items.sumOf { it.quantity }} items", style = MaterialTheme.typography.labelMedium, color = PosInkSoft)
            }
            Text(customerName.ifBlank { "Walk-in Customer" }, color = PosInk, fontWeight = FontWeight.SemiBold)
            Text("Seated at $tableLabel", color = PosAccent, style = MaterialTheme.typography.bodySmall)
            Divider(color = PosBorder)
            items.forEach { item ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.quantity} x ${item.product.name}", color = PosInk)
                        Text(currencyFormatter.format(item.product.price * item.quantity + item.priceDelta * item.quantity), color = PosInk, fontWeight = FontWeight.SemiBold)
                    }
                    if (item.selectedOptions.isNotEmpty()) Text(item.selectedOptions.joinToString(", ") { it.second }, color = PosInkSoft, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (discountType != "NONE") Text("Discount: $discountType", color = PosAccent, style = MaterialTheme.typography.bodySmall)
            Divider(color = PosBorder)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Payment", color = PosInkSoft); Text(paymentMethod, color = PosInk, fontWeight = FontWeight.SemiBold) }
            if (paymentMethod == "CASH") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tendered", color = PosInkSoft); Text(currencyFormatter.format(amountTendered.toDoubleOrNull() ?: 0.0), color = PosInk) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Change", color = PosInkSoft); Text(currencyFormatter.format(change.coerceAtLeast(0.0)), color = PosAccent, fontWeight = FontWeight.Bold) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total", style = MaterialTheme.typography.titleMedium, color = PosInk, fontWeight = FontWeight.Bold); Text(currencyFormatter.format(orderTotal), style = MaterialTheme.typography.titleMedium, color = PosGold, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherApplicationSection(viewModel: CafeViewModel, totalAmount: Double) {
    val vouchers by viewModel.activeVouchers.collectAsState(initial = emptyList())
    val discountType by viewModel.discountType.collectAsState()
    val selectedVoucher by viewModel.selectedVoucher.collectAsState()

    var voucherExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = voucherExpanded,
        onExpandedChange = { voucherExpanded = it }
    ) {
        OutlinedTextField(
            value = selectedVoucher?.code ?: "Select voucher",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voucherExpanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PosAccent,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = PosPaper,
                unfocusedTextColor = PosPaper,
                disabledTextColor = PosPaper,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = PosMuted
            ),
            enabled = true
        )

        ExposedDropdownMenu(expanded = voucherExpanded, onDismissRequest = { voucherExpanded = false }) {
            vouchers.forEach { voucher ->
                val isApplicable = totalAmount >= voucher.minOrderAmount && voucher.active
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(voucher.code)
                            Text(
                                if (voucher.discountType == "PERCENTAGE") "${voucher.discountValue}%" else "₱${voucher.discountValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosMuted
                            )
                            if (!isApplicable && voucher.minOrderAmount > 0) {
                                Text(
                                    "Requires minimum of ₱${voucher.minOrderAmount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PosMuted
                                )
                            }
                        }
                    },
                    onClick = {
                        if (isApplicable) {
                            viewModel.setSelectedVoucher(voucher) // also clears any PWD/Student discount
                        }
                        voucherExpanded = false
                    },
                    enabled = isApplicable
                )
            }
            DropdownMenuItem(
                text = { Text("Clear") },
                onClick = { viewModel.setSelectedVoucher(null); voucherExpanded = false }
            )
        }
    }

    selectedVoucher?.let { voucher ->
        Spacer(modifier = Modifier.height(Dimens.space8))
        val discountAmount = if (voucher.discountType == "PERCENTAGE") {
            totalAmount * voucher.discountValue / 100.0
        } else {
            voucher.discountValue
        }.coerceAtMost(if (voucher.maxDiscount > 0) voucher.maxDiscount else Double.MAX_VALUE)
            .coerceAtMost(totalAmount)

        Text(
            text = "Voucher: ${voucher.code} (-${NumberFormat.getCurrencyInstance(Locale("en", "PH")).format(discountAmount)})",
            style = MaterialTheme.typography.bodySmall,
            color = PosAccent,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationDialog(viewModel: CafeViewModel, order: Order, onDismiss: () -> Unit) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val context = LocalContext.current
    var paymentProofPath by remember(order.id) { mutableStateOf<String?>(null) }

    val servedToday by viewModel.todayOrderCount.collectAsState()
    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    val businessSettings by viewModel.businessSettings.collectAsState(initial = null)

    var receiptModel by remember(order.id) { mutableStateOf<ReceiptModel?>(null) }
    var expandReceipt by remember { mutableStateOf(false) }

    LaunchedEffect(order.id) {
        val items = viewModel.getOrderItemsSync(order.id)
        val optionsByItem = items.associate { item -> item.id to viewModel.getOrderOptionsForItemSync(item.id) }
        val payment = viewModel.getPaymentsForOrder(order.id).firstOrNull()
        paymentProofPath = payment?.screenshotPath
        receiptModel = buildReceiptModel(order, items, products, businessSettings, optionsByItem, payment)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
                Icon(
                    painter = painterResource(R.drawable.ic_quest_check),
                    contentDescription = null,
                    tint = PosGold,
                    modifier = Modifier.size(Dimens.space32)
                )
                Column {
                    Text("Order Confirmed!", style = MaterialTheme.typography.headlineSmall, color = PosInk, fontWeight = FontWeight.Bold)
                    Text("+1 gem earned  ·  ${servedToday} served today", style = MaterialTheme.typography.labelMedium, color = PosInkSoft)
                }
            }
        },
        text = {
            Column {
                Text("Order Number", style = MaterialTheme.typography.labelMedium, color = PosMuted)
                Text(order.orderNumber, style = MaterialTheme.typography.titleLarge, color = PosPaper, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(Dimens.space12))

                Text("Total", style = MaterialTheme.typography.labelMedium, color = PosMuted)
                Text(currencyFormatter.format(order.totalAmount), style = MaterialTheme.typography.titleMedium, color = PosGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(Dimens.space16))

                paymentProofPath?.let { path ->
                    val proofBitmap = remember(path) { BitmapFactory.decodeFile(path) }
                    proofBitmap?.let {
                        Text("Payment proof", style = MaterialTheme.typography.labelMedium, color = PosInkSoft, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Dimens.progressHeight))
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Payment proof image",
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(Dimens.space12))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFF5F0E8), shape = RoundedCornerShape(Dimens.radiusMedium)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val barWidth = 4f
                        val barHeight = 100f
                        val spacing = 2f
                        val chars = order.barcodeValue ?: order.orderNumber
                        for (i in chars.indices) {
                            val barHeightActual = if (i % 2 == 0) barHeight else barHeight * 0.6f
                            drawRect(
                                color = PosCoffee,
                                topLeft = Offset(i * (barWidth + spacing), 0f),
                                size = Size(barWidth, barHeightActual)
                            )
                        }
                    }
                }
                // Full BIR receipt, expandable (spec FR-002 / T016). Rendered from
                // the SAME buildReceiptModel used by ReceiptParityTest so the
                // on-screen receipt never drifts from the printed PDF. Default
                // collapsed so it never adds a required tap (guardrail #6).
                Spacer(Modifier.height(Dimens.space12))
                TextButton(
                    onClick = { expandReceipt = !expandReceipt },
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Text(
                        text = if (expandReceipt) "Hide receipt" else "View full receipt",
                        color = if (isGamified()) PosGold else PosInk
                    )
                }
                if (expandReceipt && receiptModel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ReceiptView(model = receiptModel!!)
                    }
                }
            }
        },
        confirmButton = {
            val scope = rememberCoroutineScope()

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        val items = viewModel.getOrderItemsSync(order.id)
                        val optionsByItem = items.associate { item ->
                            item.id to viewModel.getOrderOptionsForItemSync(item.id)
                        }
                        val payment = viewModel.getPaymentsForOrder(order.id).firstOrNull()
                        viewModel.receiptPrinter.printReceipt(order, items, products, businessSettings, optionsByItem, payment)
                    }
                }, shape = RoundedCornerShape(Dimens.radiusMedium), border = androidx.compose.foundation.BorderStroke(Dimens.borderWidth, PosGold)) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = PosGold)
                    Spacer(modifier = Modifier.width(Dimens.space8))
                    Text("Print Receipt", color = PosGold)
                }
                Button(onClick = onDismiss, shape = RoundedCornerShape(Dimens.radiusMedium), colors = ButtonDefaults.buttonColors(containerColor = PosAccent)) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusXLarge)
    )
}
