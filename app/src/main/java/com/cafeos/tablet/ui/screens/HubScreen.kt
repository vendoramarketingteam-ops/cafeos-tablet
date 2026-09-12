package com.cafeos.tablet.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cafeos.tablet.data.DashboardTileOrder
import com.cafeos.tablet.data.Ingredient
import com.cafeos.tablet.data.Order
import com.cafeos.tablet.data.Staff
import com.cafeos.tablet.ui.AccessControl
import com.cafeos.tablet.ui.CafeViewModel
import com.cafeos.tablet.ui.GameFeedback
import com.cafeos.tablet.ui.theme.Dimens
import com.cafeos.tablet.ui.theme.HubCream
import com.cafeos.tablet.ui.theme.HubEspresso
import com.cafeos.tablet.ui.theme.HubEspressoDeep
import com.cafeos.tablet.ui.theme.HubEspressoDarker
import com.cafeos.tablet.ui.theme.HubGold
import com.cafeos.tablet.ui.theme.HubGoldDark
import com.cafeos.tablet.ui.theme.HubInk
import com.cafeos.tablet.ui.theme.HubSage
import com.cafeos.tablet.ui.theme.HubSageDark
import com.cafeos.tablet.ui.theme.HubSienna
import com.cafeos.tablet.ui.theme.HubSiennaDark
import com.cafeos.tablet.ui.theme.PosCoffee
import com.cafeos.tablet.ui.theme.PosGold
import com.cafeos.tablet.ui.theme.PebotTheme
import com.cafeos.tablet.ui.theme.ThemeMode
import com.cafeos.tablet.ui.theme.isGamified
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Tile model ─────────────────────────────────────────────────────────────

/**
 * One app tile on the Home grid (spec 003 hub-and-spoke).
 * @property label      Short name shown below the icon.
 * @property icon       Material icon rendered inside the colored tile.
 * @property route      NavHost route this tile navigates to.
 * @property tint       Background color for the tile (sienna, gold, sage, etc.).
 * @property badgeCount Optional notification badge (e.g. pending orders, low stock).
 * @property showBadge  Whether to display the badge.
 * @property isLargeTile Whether the tile spans 2 grid columns instead of 1
 *                       for high-frequency actions like New Order and Live Orders.
 */
data class HomeTile(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color,
    val badgeCount: Int = 0,
    val showBadge: Boolean = false,
    val isLargeTile: Boolean = false,
)

// ─── Master tile list — 10 modules ─────────────────────────────────────────

val HomeTiles: List<HomeTile> = listOf(
    HomeTile("Live Orders", Icons.Default.FormatListBulleted, "live_orders",
        tint = HubEspresso, isLargeTile = true),
    HomeTile("New Order", Icons.Default.ShoppingCart, "pos",
        tint = HubSienna, isLargeTile = true),
    HomeTile("Kitchen", Icons.Default.Restaurant, "kitchen",
        tint = HubGold),
    HomeTile("Inventory", Icons.Default.Inventory, "inventory",
        tint = HubSage),
    HomeTile("Tables", Icons.Default.TableBar, "tables",
        tint = HubEspressoDarker),
    HomeTile("HR", Icons.Default.Group, "staff",
        tint = HubSiennaDark),
    HomeTile("Reports", Icons.Default.BarChart, "analytics",
        tint = HubSageDark),
    HomeTile("Settings", Icons.Default.Settings, "settings",
        tint = HubEspressoDeep),
    HomeTile("Products", Icons.Default.Inventory, "products",
        tint = HubSage),
    HomeTile("Admin", Icons.Default.VerifiedUser, "audit_logs",
        tint = HubGoldDark),
)

// ─── Greeting helpers ───────────────────────────────────────────────────────

fun timeOfDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "GOOD NIGHT"
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }
}

fun todayDateLabel(): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())

// ─── HubScreen (stateless) ──────────────────────────────────────────────────

/**
 * Hub-and-spoke Home screen (spec 003).
 *
 * A single-level hub: a grid of app tiles. Each tile opens its module
 * full-screen. The only way to navigate between modules is via the Home
 * button (top-left, fixed position on every app screen).
 *
 * Supports long-press drag-to-reorder (spec 003, US4). Tiles can be dragged
 * to a new position; [animateItemPlacement] provides smooth displacement.
 * High-frequency tiles ("New Order", "Live Orders") span 2 columns.
 * Reordered layout is persisted via [DashboardTileOrder] Room entity.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    navController: NavHostController,
    staff: Staff,
    visibleTiles: List<HomeTile>,
    pendingCount: Int = 0,
    lowStockCount: Int = 0,
    tileOrders: List<DashboardTileOrder> = emptyList(),
    onTileReorder: (List<Pair<String, String>>) -> Unit = {},
    onTileTap: (HomeTile) -> Unit = { tile ->
        navController.navigate(tile.route) { launchSingleTop = true }
    },
) {
    val context = LocalContext.current
    val gamified = isGamified()
    val greeting = timeOfDayGreeting()
    val dateStr = todayDateLabel()
    val subLine = (staff.position ?: staff.role) + " · " + dateStr
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val density = LocalDensity.current

    val bgBrush = if (gamified) {
        Brush.verticalGradient(colors = listOf(HubEspresso, HubEspressoDeep))
    } else {
        Brush.horizontalGradient(colors = listOf(PosCoffee, PosCoffee))
    }

    // Apply saved order from Room
    val orderedTiles = remember(visibleTiles, tileOrders) {
        if (tileOrders.isNotEmpty()) {
            val orderMap = tileOrders.associate { it.tileRoute to it.sortOrder }
            visibleTiles.sortedBy { orderMap[it.route] ?: Int.MAX_VALUE }
        } else {
            visibleTiles
        }
    }

    // Reorderable state (mutable list that animates via animateItemPlacement)
    val tileList = remember(orderedTiles) {
        mutableStateListOf<HomeTile>().apply { addAll(orderedTiles) }
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var cumulativeDragX by remember { mutableStateOf(0f) }
    var cumulativeDragY by remember { mutableStateOf(0f) }

    val cols = 4
    val cellSizePx = with(density) {
        val gridWidthDp = 399f - 32f - (cols - 1) * 12  // screenWidth - padding - spacing
        (gridWidthDp / cols).dp.toPx()
    }
    val dragThreshold = cellSizePx / 2.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // ── Greeting ──
        Column(
            modifier = Modifier.padding(
                top = 24.dp, start = 20.dp, end = 20.dp, bottom = 8.dp
            )
        ) {
            Text(
                text = greeting,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = PosGold,
                letterSpacing = 0.02.sp
            )
            Text(
                text = staff.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HubCream,
                fontFamily = FontFamily.Serif,
                lineHeight = 26.sp
            )
            Text(
                text = subLine,
                fontSize = 12.5.sp,
                color = HubCream.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── Tile grid ──
        if (tileList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No modules available for your role.",
                    color = HubCream.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(Dimens.space16, Dimens.space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                items(
                    items = tileList,
                    key = { it.route + it.label },
                    span = { tile -> if (tile.isLargeTile) GridItemSpan(2) else GridItemSpan(1) }
                ) { tile ->
                    val currentIndex = tileList.indexOf(tile)
                    val isDragging = draggedIndex == currentIndex

                    Box(
                        modifier = Modifier
                            .animateItemPlacement()
                            .pointerInput(tile.route) {
                                // Long-press then drag to reorder.
                                // detectDragGesturesAfterLongPress is experimental in
                                // compose 1.5.4 and parameter names may be unavailable,
                                // so we use detectDragGestures with a time-based long-press gate.
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        dragStartTime = System.currentTimeMillis()
                                    },
                                    onDrag = { change, dragAmount ->
                                        val elapsed = System.currentTimeMillis() - dragStartTime
                                        if (elapsed > 500 && draggedIndex == null) {
                                            draggedIndex = currentIndex
                                            if (gamified) GameFeedback.tap(context)
                                            vibrator.vibrate(
                                                VibrationEffect.createOneShot(
                                                    50,
                                                    VibrationEffect.DEFAULT_AMPLITUDE
                                                )
                                            )
                                        }
                                        if (draggedIndex != null && currentIndex < tileList.size) {
                                            change.consume()
                                            cumulativeDragX += dragAmount.x
                                            cumulativeDragY += dragAmount.y

                                            var newIndex = currentIndex

                                            when {
                                                cumulativeDragY > dragThreshold -> {
                                                    newIndex = (currentIndex + cols).coerceAtMost(tileList.size - 1)
                                                    cumulativeDragY = 0f
                                                    cumulativeDragX = 0f
                                                }
                                                cumulativeDragY < -dragThreshold -> {
                                                    newIndex = (currentIndex - cols).coerceAtLeast(0)
                                                    cumulativeDragY = 0f
                                                    cumulativeDragX = 0f
                                                }
                                                cumulativeDragX > dragThreshold -> {
                                                    newIndex = (currentIndex + 1).coerceAtMost(tileList.size - 1)
                                                    cumulativeDragX = 0f
                                                    cumulativeDragY = 0f
                                                }
                                                cumulativeDragX < -dragThreshold -> {
                                                    newIndex = (currentIndex - 1).coerceAtLeast(0)
                                                    cumulativeDragX = 0f
                                                    cumulativeDragY = 0f
                                                }
                                            }

                                            if (newIndex != currentIndex && newIndex in 0 until tileList.size) {
                                                val item = tileList.removeAt(currentIndex)
                                                tileList.add(newIndex, item)
                                                draggedIndex = newIndex
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggedIndex != null) {
                                            vibrator.vibrate(
                                                VibrationEffect.createOneShot(
                                                    30,
                                                    VibrationEffect.DEFAULT_AMPLITUDE
                                                )
                                            )
                                            draggedIndex = null
                                            onTileReorder(tileList.map { it.label to it.route })
                                        }
                                        dragStartTime = 0L
                                    },
                                    onDragCancel = {}
                                )
                            }
                    ) {
                        HomeTileButton(
                            tile = tile,
                            gamified = gamified,
                            isDragging = isDragging,
                            onClick = {
                                if (draggedIndex == null) {
                                    if (gamified) GameFeedback.tap(context)
                                    onTileTap(tile)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── HubScreen (ViewModel-backed) ───────────────────────────────────────────

/**
 * ViewModel-backed overload — collects tile orders + badge data from [CafeViewModel]
 * and filters tiles via [AccessControl], then delegates to the stateless [HubScreen].
 */
@Composable
fun HubScreen(
    navController: NavHostController,
    viewModel: CafeViewModel,
    staff: Staff,
) {
    val allOrders by viewModel.allOrders.collectAsState(initial = emptyList<Order>())
    val tileOrders by viewModel.tileOrders.collectAsState(initial = emptyList<DashboardTileOrder>())
    val lowStockIngredients by viewModel.lowStockIngredients.collectAsState(initial = emptyList<Ingredient>())

    val pendingCount = allOrders.count {
        it.status == "PENDING" || it.status == "PREPARING" || it.status == "COMPLETED"
    }

    val lowStockCount = lowStockIngredients.size
    val visibleTiles = remember(staff, pendingCount, lowStockCount, tileOrders) {
        HomeTiles
            .filter { tile ->
                when (tile.label) {
                    "Admin" -> staff.role.uppercase() == "ADMIN"
                    else -> AccessControl.canAccessRoute(staff, tile.route)
                }
            }
            .map { tile ->
                when (tile.route) {
                    "live_orders" -> tile.copy(badgeCount = pendingCount, showBadge = pendingCount > 0)
                    "inventory" -> tile.copy(
                        badgeCount = lowStockIngredients.size,
                        showBadge = lowStockIngredients.isNotEmpty()
                    )
                    else -> tile
                }
            }
            .let { tiles ->
                val orderMap = tileOrders.associate { it.tileRoute to it.sortOrder }
                if (orderMap.isNotEmpty()) {
                    tiles.sortedBy { orderMap[it.route] ?: Int.MAX_VALUE }
                } else tiles
            }
    }

    HubScreen(
        navController = navController,
        staff = staff,
        visibleTiles = visibleTiles,
        pendingCount = pendingCount,
        lowStockCount = lowStockIngredients.size,
        tileOrders = tileOrders,
        onTileReorder = { reordered ->
            viewModel.saveTileOrders(reordered)
        }
    )
}

// ─── Tile button ─────────────────────────────────────────────────────────────

/**
 * Single tile: 56×56px colored icon + 11px label.
 * Tap scale (transform: scale(0.92)) matches the HTML mockup.
 * When [isDragging] is true, the tile lifts with a slight scale + alpha.
 * Badge overlay for notification counts (e.g. pending orders, low stock).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTileButton(
    tile: HomeTile,
    gamified: Boolean,
    isDragging: Boolean = false,
    onClick: () -> Unit,
) {
    val scale = if (isDragging) 1.08f else 1f
    val alpha = if (isDragging) 0.85f else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
    ) {
        Box(
            modifier = Modifier
                .size(if (tile.isLargeTile) 104.dp else 56.dp)
                .background(tile.tint, shape = RoundedCornerShape(Dimens.radiusMedium))
                .padding(if (tile.isLargeTile) Dimens.space8 else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (tile.showBadge && tile.badgeCount > 0) {
                        Badge(
                            containerColor = Color(0xFFB3261E),
                            contentColor = Color.White,
                            modifier = Modifier
                                .padding(start = 20.dp, top = 8.dp)
                        ) {
                            Text(
                                if (tile.badgeCount > 99) "99+" else tile.badgeCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.label,
                    tint = HubCream,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = tile.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (gamified) HubCream.copy(alpha = 0.85f) else HubInk,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@Preview(name = "Hub - Gamified")
@Preview(device = "spec:parent=mobile")
@Composable
private fun HubGamifiedPreview() {
    val staff = Staff(
        id = 1, name = "Maria Santos", role = "ADMIN",
        email = "maria@cafeos.com", position = "Front counter"
    )
    val mockTiles = listOf(
        HomeTile("Live Orders", Icons.Default.FormatListBulleted, "live_orders", HubEspresso, badgeCount = 3, showBadge = true, isLargeTile = true),
        HomeTile("New Order", Icons.Default.ShoppingCart, "pos", HubSienna, isLargeTile = true),
        HomeTile("Kitchen", Icons.Default.Restaurant, "kitchen", HubGold),
        HomeTile("Products", Icons.Default.Inventory, "products", HubSage),
        HomeTile("Inventory", Icons.Default.Inventory, "inventory", HubSage),
        HomeTile("Tables", Icons.Default.TableBar, "tables", HubEspressoDarker),
        HomeTile("HR", Icons.Default.Group, "staff", HubSiennaDark),
        HomeTile("Reports", Icons.Default.BarChart, "analytics", HubSageDark),
        HomeTile("Settings", Icons.Default.Settings, "settings", HubEspressoDeep),
        HomeTile("Admin", Icons.Default.VerifiedUser, "audit_logs", HubGoldDark),
    )
    PebotTheme(themeMode = ThemeMode.GAMIFIED) {
        Surface(modifier = Modifier.fillMaxSize()) {
            HubScreen(
                navController = rememberNavController(),
                staff = staff,
                visibleTiles = mockTiles,
                pendingCount = 3
            )
        }
    }
}

@Preview(name = "Hub - Classic")
@Preview(device = "spec:parent=mobile")
@Composable
private fun HubClassicPreview() {
    val staff = Staff(
        id = 1, name = "Maria Santos", role = "ADMIN",
        email = "maria@cafeos.com", position = "Front counter"
    )
    val mockTiles = listOf(
        HomeTile("Live Orders", Icons.Default.FormatListBulleted, "live_orders", HubEspresso, badgeCount = 1, showBadge = true, isLargeTile = true),
        HomeTile("New Order", Icons.Default.ShoppingCart, "pos", HubSienna, isLargeTile = true),
        HomeTile("Kitchen", Icons.Default.Restaurant, "kitchen", HubGold),
        HomeTile("Products", Icons.Default.Inventory, "products", HubSage),
        HomeTile("Inventory", Icons.Default.Inventory, "inventory", HubSage),
    )
    PebotTheme(themeMode = ThemeMode.CLASSIC) {
        Surface(modifier = Modifier.fillMaxSize()) {
            HubScreen(
                navController = rememberNavController(),
                staff = staff,
                visibleTiles = mockTiles,
                pendingCount = 1
            )
        }
    }
}
