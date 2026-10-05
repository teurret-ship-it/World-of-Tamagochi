package com.worldoftamagochi.sim.shop

/** Where an item sits on the pet. One item per slot. */
enum class Slot { HEAD, FACE, NECK }

/** A cosmetic: looks only, never an advantage (CLAUDE.md section 2). */
data class Cosmetic(
    val id: String,
    val slot: Slot,
    val price: Int,
)

/**
 * Everything the shop sells, cheapest first. Prices are set against the
 * economy test: a first item on day one, something new to save for every few
 * days, and the whole wardrobe in about five weeks of caring and racing.
 */
object Catalog {
    val ITEMS: List<Cosmetic> =
        listOf(
            Cosmetic("ribbon", Slot.HEAD, price = 40),
            Cosmetic("blossom", Slot.HEAD, price = 60),
            Cosmetic("bell", Slot.NECK, price = 70),
            Cosmetic("glasses", Slot.FACE, price = 80),
            Cosmetic("cap", Slot.HEAD, price = 90),
            Cosmetic("scarf", Slot.NECK, price = 110),
            Cosmetic("butterfly", Slot.HEAD, price = 120),
            Cosmetic("sunglasses", Slot.FACE, price = 150),
            Cosmetic("grad_cap", Slot.HEAD, price = 180),
            Cosmetic("goggles", Slot.FACE, price = 200),
            Cosmetic("headphones", Slot.HEAD, price = 220),
            Cosmetic("gem", Slot.NECK, price = 240),
            Cosmetic("top_hat", Slot.HEAD, price = 260),
            Cosmetic("crown", Slot.HEAD, price = 400),
        ).sortedBy { it.price }

    fun byId(id: String): Cosmetic? = ITEMS.firstOrNull { it.id == id }
}

/** What the player owns and wears. */
data class Wardrobe(
    val owned: Set<String> = emptySet(),
    val equipped: Map<Slot, String> = emptyMap(),
)

sealed interface Purchase {
    data class Bought(
        val coinsLeft: Long,
        val wardrobe: Wardrobe,
    ) : Purchase

    data class NotEnoughCoins(
        val missing: Long,
    ) : Purchase

    data object AlreadyOwned : Purchase
}

object Shop {
    /** Buys [item] and puts it on straight away: the reward should be visible at once. */
    fun buy(
        coins: Long,
        wardrobe: Wardrobe,
        item: Cosmetic,
    ): Purchase =
        when {
            item.id in wardrobe.owned -> Purchase.AlreadyOwned
            coins < item.price -> Purchase.NotEnoughCoins(item.price - coins)
            else -> Purchase.Bought(coins - item.price, Wardrobe(wardrobe.owned + item.id, wardrobe.equipped + (item.slot to item.id)))
        }

    /** Puts on an owned item, or takes it off when it is already worn. */
    fun toggle(
        wardrobe: Wardrobe,
        item: Cosmetic,
    ): Wardrobe =
        when {
            item.id !in wardrobe.owned -> wardrobe
            wardrobe.equipped[item.slot] == item.id -> wardrobe.copy(equipped = wardrobe.equipped - item.slot)
            else -> wardrobe.copy(equipped = wardrobe.equipped + (item.slot to item.id))
        }
}
