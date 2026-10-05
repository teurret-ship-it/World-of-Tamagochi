package com.worldoftamagochi.feature.shop

import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.shop.Cosmetic
import com.worldoftamagochi.sim.shop.Slot

/** One tile in the shop. */
data class ShopItem(
    val cosmetic: Cosmetic,
    val owned: Boolean,
    val worn: Boolean,
    val affordable: Boolean,
) {
    val id: String get() = cosmetic.id
}

/** Everything the shop shows. Trying on is free: [selectedId] is shown on the pet before buying. */
data class ShopUiState(
    val name: String,
    val genome: Genome,
    val coins: Long,
    val items: List<ShopItem>,
    val worn: Map<Slot, String>,
    val selectedId: String? = null,
) {
    val selected: ShopItem? get() = items.firstOrNull { it.id == selectedId }

    /** What the pet wears in the fitting room: the selected item tried on over the worn ones. */
    val preview: List<String>
        get() = (selected?.let { worn + (it.cosmetic.slot to it.id) } ?: worn).values.toList()
}

/** One-off moments for sound, haptics and sparkles. */
sealed interface ShopEffect {
    data object TriedOn : ShopEffect

    data class Bought(
        val id: String,
    ) : ShopEffect

    data object NotEnoughCoins : ShopEffect

    data class Changed(
        val wearing: Boolean,
    ) : ShopEffect
}
