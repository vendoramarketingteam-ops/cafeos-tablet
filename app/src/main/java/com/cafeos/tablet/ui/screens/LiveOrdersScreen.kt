package com.cafeos.tablet.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.data.*
import com.cafeos.tablet.ui.CafeViewModel
import com.cafeos.tablet.ui.components.GameCard
import com.cafeos.tablet.ui.components.PremiumHeader
import com.cafeos.tablet.ui.components.PremiumScreen
import com.cafeos.tablet.ui.components.Rarity
import com.cafeos.tablet.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun LiveOrdersScreen(viewModel: CafeViewModel) {
    val orders by viewModel.allOrders.collectAsState(initial = emptyList())
    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val stations by viewModel.stations.collectAsState(initial = emptyList())
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val dateFormatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Kanban", "Tables", "Stations")

    PremiumScreen {
        PremiumHeader("Live Orders", "Monitor preparation across the floor")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = PosCoffeeLight,
            contentColor = PosGold,
            edgePadding = Dimens.space16,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        when (selectedTab) {
            0 -> KanbanBoard(orders, currencyFormatter, dateFormatter, viewModel)
            1 -> TablesTab(tables, viewModel)
            2 -> StationsTab(stations, viewModel)
        }
    }
}

private val kanbanLanes = listOf("PENDING", "PREPARING", "COMPLETED", "CANCELLED")

private val kanbanLaneColors = mapOf(
    "PENDING" to PosGold,
    "PREPARING" to PosInfo,
    "COMPLETED" to PosAccent,
    "CANCELLED" to PosDanger
)

private val kanbanLaneTitles = mapOf(
    "PENDING" to "Pending",
    "PREPARING" to "Preparing",
    "COMPLETED" to "Completed",
    "CANCELLED" to "Cancelled"
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KanbanBoard(
    orders: List<Order>,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    viewModel: CafeViewModel
) {
    val pagerState = rememberPagerState { kanbanLanes.size }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var pendingCancellation by remember { mutableStateOf<Order?>(null) }

    fun requestStatusChange(order: Order, newStatus: String) {
        if (newStatus == "CANCELLED") pendingCancellation = order
        else {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            viewModel.updateOrderStatus(order.id, newStatus)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // - Pill row: lane names + live counts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimens.space8, vertical = Dimens.space8),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            kanbanLanes.forEachIndexed { index, status ->
                val count = orders.count { it.status == status }
                FilterChip(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    label = {
                        Text(
                            text = "${kanbanLaneTitles[status] ?: status} ($count)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = (kanbanLaneColors[status] ?: PosMuted).copy(alpha = 0.2f),
                        selectedLabelColor = kanbanLaneColors[status] ?: PosMuted,
                        containerColor = PosCoffeeLight,
                        labelColor = PosMuted
                    ),
                    modifier = Modifier.height(Dimens.touchComfortable)
                )
            }
        }

        // - Pager: one lane per page
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { kanbanLanes[it] }
        ) { pageIndex ->
            val status = kanbanLanes[pageIndex]
            val laneOrders = orders.filter { it.status == status }

            if (laneOrders.isEmpty()) {
                KanbanEmptyState(
                    status = status,
                    laneColor = kanbanLaneColors[status] ?: PosMuted
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Dimens.space8),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.space8)
                ) {
                    items(laneOrders, key = { it.id }) { order ->
                        KanbanOrderCard(
                            order = order,
                            currencyFormatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            onStatusChange = { newStatus ->
                                requestStatusChange(order, newStatus)
                            },
                            onClick = { selectedOrder = order }
                        )
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        OrderDetailDialog(
            order = order,
            viewModel = viewModel,
            onDismiss = { selectedOrder = null },
            onStatusChange = { newStatus ->
                requestStatusChange(order, newStatus)
                if (newStatus == "CANCELLED" || newStatus == "COMPLETED") {
                    selectedOrder = null
                }
            }
        )
    }

    pendingCancellation?.let { order ->
        AdminPasscodeDialog(
            viewModel = viewModel,
            onDismiss = { pendingCancellation = null },
            onAuthorized = {
                viewModel.updateOrderStatus(order.id, "CANCELLED", adminAuthorized = true)
                pendingCancellation = null
                if (selectedOrder?.id == order.id) selectedOrder = null
            }
        )
    }
}

@Composable
fun KanbanEmptyState(status: String, laneColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PosCoffeeLight.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val icon = when (status) {
                "COMPLETED" -> Icons.Default.CheckCircle
                "CANCELLED" -> Icons.Default.Cancel
                else -> Icons.Default.Inbox
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = laneColor.copy(alpha = 0.5f),
                modifier = Modifier.size(Dimens.iconXL)
            )
            Spacer(modifier = Modifier.height(Dimens.space12))
            Text(
                text = when (status) {
                    "PENDING" -> "No pending orders"
                    "PREPARING" -> "No orders being prepared"
                    "COMPLETED" -> "All caught up"
                    "CANCELLED" -> "No cancelled orders"
                    else -> "Empty"
                },
                color = PosMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanOrderCard(
    order: Order,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onStatusChange: (String) -> Unit,
    onClick: () -> Unit
) {
    val elapsedMin = ((System.currentTimeMillis() - order.createdAt) / 60000).toInt()
    val urgencyColor = when {
        elapsedMin <= 5 -> PosAccent      // green: 0-5 min
        elapsedMin <= 15 -> PosGold       // amber: 6-15 min
        else -> PosDanger                 // red: 15+ min
    }

    // Available status transitions: next lane + Cancel
    val currentIdx = kanbanLanes.indexOf(order.status)
    val availableTransitions = buildList {
        if (currentIdx >= 0 && currentIdx < kanbanLanes.lastIndex) {
            add(kanbanLanes[currentIdx + 1])
        }
        if (currentIdx in 0..1) add("CANCELLED")
    }.distinct()

    var menuExpanded by remember { mutableStateOf(false) }

    GameCard(
        rarity = Rarity.COMMON,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Dimens.space12)) {
            // - Header: order info + urgency dot + status badge + overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space4)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.space12)
                            .background(urgencyColor, shape = RoundedCornerShape(Dimens.radiusPill))
                    )
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = PosMuted,
                        modifier = Modifier.size(Dimens.space16)
                    )
                    Column {
                        Text(
                            order.orderNumber,
                            style = MaterialTheme.typography.labelMedium,
                            color = PosPaper,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            order.tableLocation ?: order.tableId?.let { "Table $it" } ?: "Takeaway",
                            style = MaterialTheme.typography.labelSmall,
                            color = PosAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space4)
                ) {
                    Surface(
                        color = kanbanLaneColors[order.status] ?: PosMuted,
                        shape = RoundedCornerShape(Dimens.radiusMedium)
                    ) {
                        Text(
                            text = order.status,
                            modifier = Modifier.padding(horizontal = Dimens.space8, vertical = Dimens.space4),
                            style = MaterialTheme.typography.labelSmall,
                            color = PosPaper,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Overflow menu for non-drag status changes
                    if (availableTransitions.isNotEmpty()) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(Dimens.touchMin)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = PosMuted
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            availableTransitions.forEach { newStatus ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            kanbanLaneTitles[newStatus] ?: newStatus,
                                            color = kanbanLaneColors[newStatus] ?: PosMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onStatusChange(newStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.progressHeight))

            Text(
                if (order.customerName.isBlank()) "Walk-in Customer" else order.customerName,
                style = MaterialTheme.typography.bodySmall,
                color = PosPaper,
                fontWeight = FontWeight.Medium
            )
            Text(
                when (order.orderType) {
                    "DELIVERY" -> "🛵 Delivery"
                    "TAKEOUT" -> "🥡 Takeout"
                    else -> "🍽️ Dine-in"
                },
                style = MaterialTheme.typography.labelSmall,
                color = PosGold,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Dimens.space8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currencyFormatter.format(order.totalAmount),
                    style = MaterialTheme.typography.labelMedium,
                    color = PosGold,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = urgencyColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(Dimens.radiusPill)
                ) {
                    Text(
                        text = "${elapsedMin}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = urgencyColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = Dimens.space8, vertical = Dimens.space4)
                    )
                }
            }

            if (order.status == "PENDING") {
                Spacer(modifier = Modifier.height(Dimens.space12))
                Button(
                    onClick = { onStatusChange("PREPARING") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusMedium),
                    colors = ButtonDefaults.buttonColors(containerColor = PosInfo)
                ) {
                    Text("Start Preparing", fontWeight = FontWeight.SemiBold)
                }
            }
            if (order.status == "PREPARING") {
                Spacer(modifier = Modifier.height(Dimens.space12))
                Button(
                    onClick = { onStatusChange("COMPLETED") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusMedium),
                    colors = ButtonDefaults.buttonColors(containerColor = PosAccent)
                ) {
                    Text("Mark Ready", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
@Composable
fun OrderDetailDialog(order: Order, viewModel: CafeViewModel, onDismiss: () -> Unit, onStatusChange: (String) -> Unit) {
    var items by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var optionMap by remember { mutableStateOf<Map<Int, List<OrderOption>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    val productMap = products.associateBy { it.id }
    val businessSettings by viewModel.businessSettings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(order.id) {
        items = viewModel.getOrderItemsSync(order.id)
        optionMap = items.associate { item ->
            item.id to viewModel.getOrderOptionsForItemSync(item.id)
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Order ${order.orderNumber}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Surface(
                    color = when (order.status) {
                        "PENDING" -> PosGold
                        "PREPARING" -> PosInfo
                        "COMPLETED" -> PosAccent
                        "CANCELLED" -> PosDanger
                        else -> PosMuted
                    },
                    shape = RoundedCornerShape(Dimens.radiusXLarge)
                ) {
                    Text(
                        text = order.status,
                        modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space4),
                        style = MaterialTheme.typography.labelMedium,
                        color = PosPaper,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.space12)) {
                    item {
                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Customer:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.customerName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(Dimens.space4))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Seated at:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.tableLocation ?: order.tableId?.let { "Table $it" } ?: "Takeaway", color = PosAccent, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(Dimens.space4))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₱${"%.2f".format(order.totalAmount)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(Dimens.space4))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Payment:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.paymentMethod, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (order.discountAmount > 0) {
                                Spacer(modifier = Modifier.height(Dimens.space4))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Discount:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-₱${"%.2f".format(order.discountAmount)}", color = PosAccent)
                                }
                            }
                        }
                    }
                    item {
                        Divider()
                        Text("Items", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }

                    if (loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(Dimens.space24), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PosAccent)
                            }
                        }
                    } else {
                        items(items) { item ->
                            val product = productMap[item.productId]
                            val selectedOptions = optionMap[item.id].orEmpty()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product?.name ?: "Unknown Product", color = MaterialTheme.colorScheme.onSurface)
                                    if (selectedOptions.isNotEmpty()) {
                                        Text(
                                            selectedOptions.joinToString(", ") { "${it.name}: ${it.value}" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PosAccent
                                        )
                                    }
                                    if (!item.notes.isNullOrBlank()) {
                                        Text(item.notes, style = MaterialTheme.typography.bodySmall, color = PosInkSoft)
                                    }
                                }
                                Text("x${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(Dimens.space16))
                                Text("₱${"%.2f".format(item.subtotal)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val payment = viewModel.getPaymentsForOrder(order.id).firstOrNull()
                            viewModel.receiptPrinter.printReceipt(order, items, products, businessSettings, optionMap, payment)
                        }
                    },
                    enabled = !loading,
                    shape = RoundedCornerShape(Dimens.space12),
                    border = androidx.compose.foundation.BorderStroke(Dimens.borderWidth, PosBorder)
                ) { Text("Print Receipt", color = PosGold) }
                if (order.status == "PENDING") {
                    Button(
                        onClick = { onStatusChange("PREPARING") },
                        colors = ButtonDefaults.buttonColors(containerColor = PosInfo),
                        shape = RoundedCornerShape(Dimens.space12)
                    ) { Text("Start Preparing", fontWeight = FontWeight.SemiBold) }
                }
                if (order.status == "PREPARING") {
                    Button(
                        onClick = { onStatusChange("COMPLETED") },
                        colors = ButtonDefaults.buttonColors(containerColor = PosAccent),
                        shape = RoundedCornerShape(Dimens.space12)
                    ) { Text("Mark Ready", fontWeight = FontWeight.SemiBold) }
                }
                if (order.status != "CANCELLED") {
                    TextButton(
                        onClick = { onStatusChange("CANCELLED") },
                    ) {
                        Text("Cancel", color = PosDanger)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusXLarge)
    )
}

@Composable
fun TablesTab(tables: List<CafeTable>, viewModel: CafeViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.space12)) {
        items(tables) { table ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.space16),
                colors = CardDefaults.cardColors(containerColor = PosCoffeeLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.space16),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(table.name, style = MaterialTheme.typography.titleMedium, color = PosPaper, fontWeight = FontWeight.SemiBold)
                        Text("Capacity: ${table.capacity}", style = MaterialTheme.typography.bodySmall, color = PosMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = table.status,
                            style = MaterialTheme.typography.labelLarge,
                            color = when (table.status) {
                                "AVAILABLE" -> PosAccent
                                "OCCUPIED" -> PosGold
                                "RESERVED" -> PosInfo
                                else -> PosMuted
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text("${table.currentOccupants}/${table.capacity}", style = MaterialTheme.typography.bodySmall, color = PosMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun StationsTab(stations: List<Station>, viewModel: CafeViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.space12)) {
        items(stations) { station ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.space16),
                colors = CardDefaults.cardColors(containerColor = PosCoffeeLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.space16),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(station.name, style = MaterialTheme.typography.titleMedium, color = PosPaper, fontWeight = FontWeight.SemiBold)
                        Text(station.stationType.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = PosMuted)
                    }
                    Text(
                        text = if (station.active) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (station.active) PosAccent else PosDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
