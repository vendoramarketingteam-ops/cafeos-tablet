package com.cafeos.tablet.model.shop

import com.cafeos.tablet.data.CafeDao
import com.cafeos.tablet.data.ComboLinkEntity
import com.cafeos.tablet.data.Product
import com.cafeos.tablet.ui.components.rarityByPrice
import kotlinx.coroutines.flow.Flow

/**
 * Repository for combo / upsell-tree data (spec 002, MLBB Item Shop).
 *
 * Wraps [CafeDao] combo-link queries and builds [BuildPathNodeUi] trees
 * by joining [ComboLinkEntity] rows with [Product] entities — never passes
 * Room entities directly to Composables (spec §5.1).
 */
class ComboRepository(private val dao: CafeDao) {

    suspend fun getComboLinks(baseProductId: Int): List<ComboLinkEntity> =
        dao.getComboLinksForBase(baseProductId)

    fun getAllComboLinks(): Flow<List<ComboLinkEntity>> = dao.getAllComboLinks()

    suspend fun insertComboLink(link: ComboLinkEntity) = dao.insertComboLink(link)

    suspend fun insertComboLinks(links: List<ComboLinkEntity>) = dao.insertComboLinks(links)

    suspend fun deleteComboLinksForBase(baseProductId: Int) =
        dao.deleteComboLinksForBase(baseProductId)

    /**
     * Build a [BuildPathNodeUi] tree from a base product + its combo links.
     *
     * The root node represents the base product (deltaPrice = 0, included = true).
     * Each child node is an addon product from a [ComboLinkEntity] link.
     * Required addons start `included = true`; optional ones start unchecked.
     *
     * @param baseProductId The product id of the selected base item.
     * @param allProducts   All available products (for name/lookup).
     * @return The build-path tree root, or `null` if the base product has no combo links.
     */
    suspend fun buildComboTree(
        baseProductId: Int,
        allProducts: List<Product>
    ): BuildPathNodeUi? {
        val base = allProducts.find { it.id == baseProductId } ?: return null
        val links = dao.getComboLinksForBase(baseProductId)

        // If no combo links exist for this product, there's no build path to show.
        if (links.isEmpty()) return null

        val productMap = allProducts.associateBy { it.id }
        val children = links.mapNotNull { link ->
            val addon = productMap[link.addonProductId]
            if (addon != null && addon.available) {
                BuildPathNodeUi(
                    id = "addon_${link.id}",
                    name = addon.name,
                    iconRes = null,
                    deltaPrice = link.deltaPriceCents / 100.0,
                    included = link.required,
                    children = emptyList(),
                    productId = addon.id
                )
            } else null
        }

        return BuildPathNodeUi(
            id = "base_${base.id}",
            name = base.name,
            iconRes = null,
            deltaPrice = 0.0,
            included = true,
            children = children,
            productId = base.id
        )
    }

    companion object {
        /** Convenience: convert a [Product] to a [ShopItemUi] for the grid. */
        fun Product.toShopItemUi(
            isTopSeller: Boolean = false
        ): ShopItemUi = ShopItemUi(
            id = this.id,
            name = this.name,
            description = this.description,
            price = this.price,
            rarity = rarityByPrice(this.price),
            imageUrl = this.imageUrl,
            isTopSeller = isTopSeller
        )
    }
}
