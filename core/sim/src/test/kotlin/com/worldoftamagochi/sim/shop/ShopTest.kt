package com.worldoftamagochi.sim.shop

import com.worldoftamagochi.sim.CareAction
import com.worldoftamagochi.sim.CareResult
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.PlayerProgress
import com.worldoftamagochi.sim.ProgressRules
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.race.Medal
import com.worldoftamagochi.sim.race.RaceRewards
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ShopTest {
    private val cap = requireNotNull(Catalog.byId("cap"))
    private val ribbon = requireNotNull(Catalog.byId("ribbon"))

    @Test
    fun `buying spends coins and puts the item on`() {
        val bought = Shop.buy(100, Wardrobe(), cap).shouldBeInstanceOf<Purchase.Bought>()
        bought.coinsLeft shouldBe 10
        bought.wardrobe.owned shouldBe setOf("cap")
        bought.wardrobe.equipped shouldBe mapOf(Slot.HEAD to "cap")
    }

    @Test
    fun `no coins, no item - and nothing is bought twice`() {
        Shop.buy(30, Wardrobe(), cap) shouldBe Purchase.NotEnoughCoins(60)
        Shop.buy(1_000, Wardrobe(owned = setOf("cap")), cap) shouldBe Purchase.AlreadyOwned
    }

    @Test
    fun `one item per slot, worn items come off with a second tap`() {
        val both = Wardrobe(owned = setOf("cap", "ribbon"), equipped = mapOf(Slot.HEAD to "cap"))
        Shop.toggle(both, ribbon).equipped shouldBe mapOf(Slot.HEAD to "ribbon")
        Shop.toggle(both, cap).equipped shouldBe emptyMap()
        Shop.toggle(Wardrobe(), cap) shouldBe Wardrobe()
    }

    @Test
    fun `the catalog is sorted, unique and every slot has something`() {
        Catalog.ITEMS
            .map { it.id }
            .toSet()
            .size shouldBe Catalog.ITEMS.size
        Catalog.ITEMS.zipWithNext().all { (a, b) -> a.price <= b.price } shouldBe true
        Slot.entries.all { slot -> Catalog.ITEMS.any { it.slot == slot } } shouldBe true
        Catalog.byId("nope") shouldBe null
    }

    @Test
    fun `economy - something new on day one, regular goals, the whole wardrobe in about five weeks`() {
        // A typical day: care for needs 3 times (about 8 answered needs) and 5 races,
        // with the medals a player earns over the first days. The player buys the
        // cheapest item they do not own whenever they can afford it.
        val progression = ProgressRules.DEFAULT
        val races = RaceRewards.DEFAULT
        val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), 0)
        var progress = PlayerProgress()
        var wardrobe = Wardrobe()
        val boughtOnDay = mutableListOf<Int>()
        val medalsByDay = mapOf(1L to Medal.BRONZE, 2L to Medal.SILVER, 4L to Medal.GOLD)
        for (day in 1L..60L) {
            repeat(8) { progress = progression.reward(progress, CareResult.Done(pet, emptyMap(), true), CareAction.FEED, day).progress }
            repeat(5) { i ->
                val medal = if (i == 0) medalsByDay[day] else null
                val before = medalsByDay.filterKeys { it < day }.values.maxOrNull()
                progress = races.reward(progress, true, medal, before, day).progress
            }
            var bought = buyCheapest(progress.coins, wardrobe)
            while (bought != null) {
                progress = progress.copy(coins = bought.coinsLeft)
                wardrobe = bought.wardrobe
                boughtOnDay += day.toInt()
                bought = buyCheapest(progress.coins, wardrobe)
            }
        }
        boughtOnDay.first() shouldBe 1
        boughtOnDay.count { it <= 7 } shouldBeInRange 4..7
        // Never more than a week without something new to wear.
        (listOf(0) + boughtOnDay).zipWithNext().maxOf { (a, b) -> b - a } shouldBeInRange 1..7
        boughtOnDay.size shouldBe Catalog.ITEMS.size
        boughtOnDay.last() shouldBeInRange 28..50
    }

    private fun buyCheapest(
        coins: Long,
        wardrobe: Wardrobe,
    ): Purchase.Bought? = Catalog.ITEMS.firstOrNull { it.id !in wardrobe.owned }?.let { Shop.buy(coins, wardrobe, it) as? Purchase.Bought }
}
