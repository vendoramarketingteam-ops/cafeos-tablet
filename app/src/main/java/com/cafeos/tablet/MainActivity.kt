package com.cafeos.tablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cafeos.tablet.data.Staff
import com.cafeos.tablet.ui.AccessControl
import com.cafeos.tablet.ui.CafeViewModel
import com.cafeos.tablet.ui.ItemShopViewModel
import com.cafeos.tablet.ui.SettingsStore
import com.cafeos.tablet.ui.components.AppTopBar
import com.cafeos.tablet.ui.screens.HubScreen
import com.cafeos.tablet.ui.screens.*
import com.cafeos.tablet.ui.screens.shop.ItemShopPOSScreen
import com.cafeos.tablet.ui.theme.PebotTheme
import com.cafeos.tablet.ui.theme.PosInk

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsStore.init(this)
        enableEdgeToEdge()
        setContent {
            val uiMode by SettingsStore.uiMode.collectAsState()
            PebotTheme(themeMode = uiMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// ─── Hub-and-Spoke Navigation (spec 003) ──────────────────────────────────────

/** Route → AppTopBar title mapping. */
val RouteTitles: Map<String, String> = mapOf(
    "live_orders" to "Live Orders",
    "pos" to "New Order",
    "item_shop" to "Item Shop",
    "kitchen" to "Kitchen",
    "products" to "Products",
    "inventory" to "Inventory",
    "stock_history" to "Stock History",
    "expenses" to "Expenses",
    "loyalty" to "Loyalty",
    "order_history" to "Order History",
    "reviews" to "Reviews",
    "tables" to "Tables",
    "stations" to "Stations",
    "staff" to "HR",
    "analytics" to "Reports",
    "settings" to "Settings",
    "audit_logs" to "Audit Logs",
    "connection" to "Connection"
)

/** 220ms slide-up + fade-in easing (matches HTML mockup `ease`). */
private val HubTransitionEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val HubEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(220, easing = HubTransitionEasing)) +
            slideInVertically(animationSpec = tween(220, easing = HubTransitionEasing)) { 10 }
}
private val HubExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(220, easing = HubTransitionEasing)) +
            slideOutVertically(animationSpec = tween(220, easing = HubTransitionEasing)) { 10 }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: CafeViewModel = viewModel()
    var currentStaff by remember { mutableStateOf<Staff?>(null) }

    if (currentStaff == null) {
        LoginScreen(viewModel = viewModel, onLoginSuccess = { currentStaff = it })
        return
    }

    fun canAccess(route: String) =
        currentStaff != null && AccessControl.canAccessRoute(currentStaff!!, route)

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isOnHome = currentRoute == "home"

    // ── Back handler: from any app screen → HubScreen (never app exit) ─────
    BackHandler(enabled = !isOnHome) {
        navController.popBackStack("home", inclusive = false)
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── Hub (Home) ────────────────────────────────────────────────────
        composable(
            "home",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            HubScreen(
                navController = navController,
                viewModel = viewModel,
                staff = currentStaff!!
            )
        }

        // ── Spokes (app screens — each wrapped with AppTopBar) ────────────
        composable(
            "live_orders",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("live_orders"))
                AppScreenFrame("Live Orders", navController) {
                    LiveOrdersScreen(viewModel)
                }
            else AccessDeniedScreen()
        }
        composable(
            "pos",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("pos"))
                AppScreenFrame("New Order", navController) {
                    POSScreen(
                        viewModel = viewModel,
                        onNavigateToLoyalty = { navController.navigate("loyalty") { launchSingleTop = true } },
                        onNavigateToItemShop = { navController.navigate("item_shop") { launchSingleTop = true } }
                    )
                }
            else AccessDeniedScreen()
        }
        composable(
            "item_shop",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            val itemShopViewModel: ItemShopViewModel = viewModel()
            val posViewModel: CafeViewModel = viewModel()
            if (canAccess("pos"))
                AppScreenFrame("Item Shop", navController) {
                    val lastOrder by itemShopViewModel.lastPlacedOrder.collectAsState()
                    ItemShopPOSScreen(
                        viewModel = itemShopViewModel,
                        onNavigateBack = { navController.popBackStack("home", inclusive = false) },
                        onNavigateToCheckoutConfirm = {}
                    )
                    lastOrder?.let { order ->
                        OrderConfirmationDialog(
                            viewModel = posViewModel,
                            order = order,
                            onDismiss = { navController.popBackStack("home", inclusive = false) }
                        )
                    }
                }
            else AccessDeniedScreen()
        }
        composable(
            "kitchen",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("kitchen"))
                AppScreenFrame("Kitchen", navController) { KitchenScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "analytics",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("analytics"))
                AppScreenFrame("Reports", navController) { AnalyticsScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "products",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("products"))
                AppScreenFrame("Products", navController) { ProductScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "inventory",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("inventory"))
                AppScreenFrame("Inventory", navController) { InventoryScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "stock_history",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("stock_history"))
                AppScreenFrame("Stock History", navController) { StockHistoryScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "expenses",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("expenses"))
                AppScreenFrame("Expenses", navController) { ExpensesScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "loyalty",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("loyalty"))
                AppScreenFrame("Loyalty", navController) { LoyaltyScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "order_history",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("order_history"))
                AppScreenFrame("Order History", navController) { OrderHistoryScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "reviews",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("reviews"))
                AppScreenFrame("Reviews", navController) { ReviewsScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "tables",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("tables"))
                AppScreenFrame("Tables", navController) { TablesManagementScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "stations",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("stations"))
                AppScreenFrame("Stations", navController) { StationsScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "staff",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("staff"))
                AppScreenFrame("HR", navController) { StaffManagementScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "settings",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("settings"))
                AppScreenFrame("Settings", navController) { SettingsScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "audit_logs",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("audit_logs"))
                AppScreenFrame("Audit Logs", navController) { AuditLogScreen(viewModel) }
            else AccessDeniedScreen()
        }
        composable(
            "connection",
            enterTransition = HubEnterTransition,
            exitTransition = HubExitTransition,
            popEnterTransition = HubEnterTransition,
            popExitTransition = HubExitTransition,
        ) {
            if (canAccess("connection"))
                AppScreenFrame("Connection", navController) { ConnectionScreen(viewModel) }
            else AccessDeniedScreen()
        }
    }
}

/**
 * Wraps an app screen (spoke) with the persistent Home top bar.
 * The top bar is at fixed position; the screen content fills the remaining
 * height and scrolls independently.
 */
@Composable
private fun AppScreenFrame(
    title: String,
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AppTopBar(title = title, navController = navController)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            content()
        }
    }
}

@Composable
private fun AccessDeniedScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Access restricted",
            color = PosInk,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
