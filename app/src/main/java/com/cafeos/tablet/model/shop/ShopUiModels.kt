package com.cafeos.tablet.model.shop

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.cafeos.tablet.ui.components.Rarity
import com.cafeos.tablet.ui.components.rarityByPrice

/**
 * Plain UI-layer models for the MLBB-style Item Shop checkout screen.
 * These are mapped FROM Room entities in the ViewModel/Repository — never pass
 * Room entities directly into Composables (spec §5.1).
 *
 * All price values are in pesos (Double) to match the existing [Product.price]
 * type and the existing NumberFormat currency rendering used in POSScreen.
 * Combo delta prices are stored as cents (Long) in the DB entity and converted
 * to pesos (Double) here for display consistency.
 */

/** Mode toggle — "All Products" vs "Quick Picks" (Zone A). */
enum class ShopMode { ALL, QUICK_PICKS }

/**
 * A product category for the left rail (Zone B).
 * Maps from [com.cafeos.tablet.data.Category].
 */
data class CategoryUi(
    val id: Int,
    val name: String,
    val icon: String? = null,
    val productCount: Int = 0
)

/**
 * A product card in the grid (Zone C). Rarity is derived via [rarityByPrice]
 * from the product's price — reused from GameComponents.kt, not duplicated.
 * Maps from [com.cafeos.tablet.data.Product].
 */
data class ShopItemUi(
    val id: Int,
    val name: String,
    val description: String? = null,
    val price: Double,
    val rarity: Rarity,
    val imageUrl: String? = null,
    val isTopSeller: Boolean = false,
    /** Stock level: null when unknown (Product has no stock field — bar is omitted, not faked). */
    val stockLevel: Int? = null,
    val minStock: Int = 0
)

/**
 * A node in the build-path / combo tree (Zone F).
 * The root is the selected base item; children are recommended add-ons.
 * Maps (partially) from [com.cafeos.tablet.data.ComboLinkEntity] + Product.
 */
data class BuildPathNodeUi(
    val id: String,
    val name: String,
    val iconRes: Int? = null,
    /** Price delta in pesos (from combo_links.deltaPriceCents / 100). */
    val deltaPrice: Double,
    val included: Boolean = false,
    val children: List<BuildPathNodeUi> = emptyList(),
    /** Maps to Product.id — needed for the purchase flow to create OrderItems. */
    val productId: Int? = null
)

/**
 * A single active modifier shown in the Detail Dock (Zone G).
 * A convenience view of an included [BuildPathNodeUi].
 */
data class ModifierUi(
    val id: String,
    val name: String,
    val deltaPrice: Double,
    val iconRes: Int? = null
)

/**
 * A quick-action icon in the Purchase Bar (Zone H).
 * Maps to existing POSScreen quick actions (hold order, discount, void, etc.).
 */
data class QuickActionUi(
    val id: String,
    val label: String,
    val iconVector: ImageVector,
    val onClick: () -> Unit
)

/** Convenience: format a peso amount for display. */
object ShopCurrency {
    val formatter get() = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "PH"))
    fun format(amount: Double): String = formatter.format(amount)
    fun formatDelta(amount: Double): String = if (amount >= 0) "+${format(amount)}" else format(amount)
}

/** Sample data provider for @Preview and Phase 1 scaffold. */
object SampleDataProvider {
    val categories = listOf(
        CategoryUi(id = 1, name = "Coffee", productCount = 6),
        CategoryUi(id = 2, name = "Pastries", productCount = 8),
        CategoryUi(id = 3, name = "Meals", productCount = 5),
        CategoryUi(id = 4, name = "Cold Drinks", productCount = 4),
        CategoryUi(id = 5, name = "Add-ons", productCount = 7),
        CategoryUi(id = 6, name = "Combos", productCount = 3)
    )

    val allProducts = listOf(
        ShopItemUi(
            id = 101,
            name = "Classic Latte",
            description = "Best seller • Creamy espresso with steamed milk",
            price = 165.0,
            rarity = rarityByPrice(165.0),
            isTopSeller = true
        ),
        ShopItemUi(
            id = 102,
            name = "Cappuccino",
            description = "Rich espresso with velvety foam",
            price = 140.0,
            rarity = rarityByPrice(140.0)
        ),
        ShopItemUi(
            id = 103,
            name = "Caramel Macchiato",
            description = "Vanilla and caramel layered delight",
            price = 185.0,
            rarity = rarityByPrice(185.0),
            isTopSeller = true
        ),
        ShopItemUi(
            id = 104,
            name = "Americano",
            description = "Smooth, bold drip-style coffee",
            price = 95.0,
            rarity = rarityByPrice(95.0)
        ),
        ShopItemUi(
            id = 105,
            name = "Espresso",
            description = "Concentrated coffee shot",
            price = 80.0,
            rarity = rarityByPrice(80.0)
        ),
        ShopItemUi(
            id = 106,
            name = "Mocha",
            description = "Chocolate-infused espresso",
            price = 175.0,
            rarity = rarityByPrice(175.0)
        ),
        ShopItemUi(
            id = 107,
            name = "Affogato",
            description = "Espresso poured over vanilla gelato",
            price = 210.0,
            rarity = rarityByPrice(210.0)
        ),
        ShopItemUi(
            id = 108,
            name = "Green Tea",
            description = "Soothing ceremonial-grade matcha",
            price = 120.0,
            rarity = rarityByPrice(120.0)
        )
    )

    /** Sample build path for "Classic Latte" with addons (Zone F tree). */
    val sampleBuildPath = BuildPathNodeUi(
        id = "root_101",
        name = "Classic Latte",
        deltaPrice = 165.0,
        included = true,
        children = listOf(
            BuildPathNodeUi(
                id = "addon_oat",
                name = "Oat Milk",
                deltaPrice = 30.0,
                included = false,
                productId = 201
            ),
            BuildPathNodeUi(
                id = "addon_shot",
                name = "Extra Shot",
                deltaPrice = 40.0,
                included = true,
                productId = 202
            ),
            BuildPathNodeUi(
                id = "addon_syrup",
                name = "Vanilla Syrup",
                deltaPrice = 25.0,
                included = false,
                productId = 203
            ),
            BuildPathNodeUi(
                id = "addon_foam",
                name = "Extra Foam",
                deltaPrice = 15.0,
                included = false,
                productId = 204
            )
        ),
        productId = 101
    )

    val sampleModifiers: List<ModifierUi> = listOf(
        ModifierUi(id = "addon_shot", name = "Extra Shot", deltaPrice = 40.0),
        ModifierUi(id = "addon_oat", name = "Oat Milk", deltaPrice = 30.0)
    )
}
