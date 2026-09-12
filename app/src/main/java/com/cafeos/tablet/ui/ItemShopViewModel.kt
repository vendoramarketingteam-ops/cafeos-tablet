package com.cafeos.tablet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.cafeos.tablet.data.CafeDao
import com.cafeos.tablet.data.CafeDatabase
import com.cafeos.tablet.data.BusinessSettings
import com.cafeos.tablet.data.Order
import com.cafeos.tablet.data.OrderItem
import com.cafeos.tablet.data.Product
import com.cafeos.tablet.model.shop.BuildPathNodeUi
import com.cafeos.tablet.model.shop.CategoryUi
import com.cafeos.tablet.model.shop.ComboRepository
import com.cafeos.tablet.model.shop.ShopItemUi
import com.cafeos.tablet.ui.components.rarityByPrice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

/**
 * ViewModel for the MLBB-style Item Shop checkout screen (spec 002).
 *
 * Holds:
 * - Categories for the left rail (Zone B).
 * - All available products for the grid (Zone C).
 * - Top-selling product IDs (for the "Top Seller" badge).
 * - The currently selected base product and its combo build-path tree (Zone F).
 * - Included addon product IDs (for total calculation in Zone H).
 *
 * Room entities are never exposed to the UI layer — only UI models
 * ([CategoryUi], [ShopItemUi], [BuildPathNodeUi]) cross the boundary.
 */
class ItemShopViewModel(application: Application) : AndroidViewModel(application) {

    private val db: CafeDatabase = CafeDatabase.getDatabase(application)
    private val dao: CafeDao = db.cafeDao()
    private val repository = ComboRepository(dao)

    // ── Mutable state (private, declared first for init-order safety) ────────

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    private val _selectedProductId = MutableStateFlow<Int?>(null)
    private val _includedNodeIds = MutableStateFlow<Set<String>>(emptySet())

    // ── Categories (Zone B rail) ──────────────────────────────────────────────

    /** Categories with product counts, sorted by sortOrder (same as DAO). */
    val categories: StateFlow<List<CategoryUi>> = dao.getAllCategories()
        .map { list ->
            list.map { c ->
                CategoryUi(
                    id = c.id,
                    name = c.name,
                    icon = c.icon,
                    productCount = 0
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Products (Zone C grid) ─────────────────────────────────────────────

    /** All available products, kept as a StateFlow for combo-tree lookups. */
    val allProducts: StateFlow<List<Product>> = dao.getAllProducts()
        .map { list -> list.filter { it.available } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Top-selling product IDs — used for the "Top Seller" badge on ShopItemCard. */
    val topSellingIds: StateFlow<Set<Int>> = dao.getTopSellingProducts()
        .map { list -> list.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /**
     * Products mapped to [ShopItemUi], filtered by the currently selected
     * category. If no category is selected, all products are shown.
     */
    val visibleProducts: StateFlow<List<ShopItemUi>> = combine(
        allProducts,
        topSellingIds,
        _selectedCategoryId
    ) { products, topSellers, categoryId ->
        val filtered = if (categoryId != null && categoryId != 0) {
            products.filter { it.categoryId == categoryId }
        } else {
            products
        }
        filtered.map { p ->
            ShopItemUi(
                id = p.id,
                name = p.name,
                description = p.description,
                price = p.price,
                rarity = rarityByPrice(p.price),
                imageUrl = p.imageUrl,
                isTopSeller = topSellers.contains(p.id)
            )
        }.sortedBy { it.name }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Category selection (Zone B) ──────────────────────────────────────────

    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(id: Int?) {
        _selectedCategoryId.update { id }
    }

    // ── Combo build path (Zones E–F) ─────────────────────────────────────────

    val selectedProductId: StateFlow<Int?> = _selectedProductId.asStateFlow()

    /**
     * The combo tree for the currently selected product.
     * Recomputed when [selectedProductId] or [allProducts] changes.
     */
    val comboTree: StateFlow<BuildPathNodeUi?> = combine(
        _selectedProductId,
        allProducts
    ) { productId, products ->
        if (productId == null) return@combine null
        repository.buildComboTree(productId, products)
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // ── Included addons (Zone F–G) ──────────────────────────────────────────

    val includedNodeIds: StateFlow<Set<String>> = _includedNodeIds.asStateFlow()

    /** Toggle whether a specific addon node is included in the combo. */
    fun toggleAddon(nodeId: String) {
        val current = _includedNodeIds.value
        _includedNodeIds.update {
            if (current.contains(nodeId)) current - nodeId else current + nodeId
        }
    }

    /** Include all addons in the build path. */
    fun addAllAddons() {
        val tree = comboTree.value
        if (tree != null) {
            val ids = tree.children
                .filterNot { it.included }
                .map { it.id }
            _includedNodeIds.update { it + ids }
        }
    }

    /** Remove all addons from the build path. */
    fun removeAllAddons() {
        _includedNodeIds.update { emptySet() }
    }

    // ── Product selection (Zones C → E) ───────────────────────────────────────

    fun selectProduct(productId: Int) {
        _selectedProductId.update { productId }
    }

    // ── Total calculation (Zone H) ───────────────────────────────────────────

    /**
     * Total price in cents: base product price (in pesos → ×100) + sum of included
     * addon delta prices (in pesos → ×100).
     */
    val totalCents: StateFlow<Long> = combine(
        selectedProductId,
        includedNodeIds,
        allProducts,
        comboTree
    ) { productId, includedIds, products, tree ->
        if (productId == null) return@combine 0L
        val base = products.find { it.id == productId } ?: return@combine 0L
        var totalCents = (base.price * 100).toLong()
        if (tree != null) {
            val deltaCents = tree.children
                .filter { includedIds.contains(it.id) }
                .sumOf { (it.deltaPrice * 100).toLong() }
            totalCents += deltaCents
        }
        totalCents
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    // ── Purchase (Zone H → Confirmation) ─────────────────────────────────────

    /** The most recently placed Order, exposed for the confirmation dialog. */
    private val _lastPlacedOrder = MutableStateFlow<Order?>(null)
    val lastPlacedOrder: StateFlow<Order?> = _lastPlacedOrder.asStateFlow()

    /**
     * Write an Order + OrderItems for the selected base product + included addons.
     *
     * The entire order write (order row + line items) is a single Room transaction,
     * so a partial failure can never leave a half-recorded sale (Constitution §II).
     *
     * @return The new orderId, or 0 if there's no selected product.
     */
    suspend fun purchaseOrder(): Long {
        val productId = _selectedProductId.value ?: return 0L
        val products = allProducts.value
        val base = products.find { it.id == productId } ?: return 0L
        val tree = comboTree.value
        val totalPeso = totalCents.value / 100.0

        val orderIdLong = db.withTransaction {
            // Reuse the same receipt-number logic as CafeViewModel.placeOrder()
            val settings = dao.getBusinessSettingsSync() ?: BusinessSettings()
            val prefix = settings.receiptPrefix?.takeIf { it.isNotBlank() } ?: "INV"
            val fromCounter = dao.getLastReceiptNumber()
            val fromOrders = dao.getMaxReceiptSuffix("$prefix-%", prefix.length + 2)
            val next = maxOf(fromCounter, fromOrders) + 1
            dao.updateLastReceiptNumber(next)
            val nextReceipt = "${prefix}-${next.toString().padStart(6, '0')}"

            val order = Order(
                orderNumber = nextReceipt,
                customerName = "Walk-in Customer",
                status = "PENDING",
                totalAmount = totalPeso,
                barcodeValue = nextReceipt
            )
            val orderId = dao.insertOrder(order).toInt()

            val items = ArrayList<OrderItem>()
            items += OrderItem(
                orderId = orderId,
                productId = base.id,
                quantity = 1,
                unitPrice = base.price,
                subtotal = base.price
            )
            tree?.children
                ?.filter { _includedNodeIds.value.contains(it.id) && it.productId != null }
                ?.forEach { node ->
                    val addon = products.find { p -> p.id == node.productId }
                    if (addon != null) {
                        items += OrderItem(
                            orderId = orderId,
                            productId = addon.id,
                            quantity = 1,
                            unitPrice = addon.price,
                            subtotal = addon.price
                        )
                    }
                }
            dao.insertOrderItems(items)
            orderId.toLong()
        }
        // Fetch the full Order for the confirmation dialog.
        _lastPlacedOrder.value = dao.getOrderById(orderIdLong.toInt())
        return orderIdLong
    }
}