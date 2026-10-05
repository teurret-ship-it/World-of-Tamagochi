package com.worldoftamagochi.feature.shop

import com.worldoftamagochi.data.GameSave
import com.worldoftamagochi.data.ProgressSave
import com.worldoftamagochi.data.WardrobeSave
import com.worldoftamagochi.data.toSave
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.shop.Catalog
import com.worldoftamagochi.sim.shop.Slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val repository = FakeGameRepository()
    private val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), 0).toSave(seed = 7, name = "Mochi")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun shop(
        coins: Long,
        wardrobe: WardrobeSave = WardrobeSave(),
    ): Pair<ShopViewModel, MutableList<ShopEffect>> {
        runBlocking { repository.saveGame(GameSave(pet = pet, progress = ProgressSave(coins = coins), wardrobe = wardrobe)) }
        val vm = ShopViewModel(repository)
        val effects = mutableListOf<ShopEffect>()
        scope.launch { vm.effects.toList(effects) }
        dispatcher.scheduler.runCurrent()
        return vm to effects
    }

    private val ShopViewModel.ui: ShopUiState get() = requireNotNull(state.value)

    @Test
    fun `every catalog item is listed with a name, nothing selected at first`() {
        val (vm, _) = shop(coins = 100)
        assertEquals(Catalog.ITEMS.map { it.id }, vm.ui.items.map { it.id })
        Catalog.ITEMS.forEach { itemName(it.id) }
        assertNull(vm.ui.selected)
        assertEquals(
            listOf("ribbon", "blossom", "bell", "glasses", "cap"),
            vm.ui.items
                .filter { it.affordable }
                .map { it.id },
        )
    }

    @Test
    fun `trying on is free and shows the item over what the pet wears`() {
        val (vm, effects) = shop(coins = 0, wardrobe = WardrobeSave(owned = listOf("bell", "cap"), worn = listOf("bell", "cap")))
        vm.onSelect("crown")
        dispatcher.scheduler.runCurrent()
        assertEquals(setOf("bell", "crown"), vm.ui.preview.toSet())
        assertEquals(0L, vm.ui.coins)
        assertEquals(listOf<ShopEffect>(ShopEffect.TriedOn), effects)
        assertEquals(mapOf(Slot.HEAD to "cap", Slot.NECK to "bell"), vm.ui.worn)
    }

    @Test
    fun `buying spends coins, saves the item and puts it on`() {
        val (vm, effects) = shop(coins = 100)
        vm.onSelect("cap")
        vm.onBuy()
        dispatcher.scheduler.runCurrent()
        val saved = requireNotNull(repository.saved)
        assertEquals(10L, saved.progress.coins)
        assertEquals(WardrobeSave(owned = listOf("cap"), worn = listOf("cap")), saved.wardrobe)
        assertTrue(vm.ui.selected!!.worn)
        assertEquals(ShopEffect.Bought("cap"), effects.last())
    }

    @Test
    fun `without enough coins nothing changes and the player hears a gentle no`() {
        val (vm, effects) = shop(coins = 50)
        vm.onSelect("crown")
        vm.onBuy()
        dispatcher.scheduler.runCurrent()
        assertEquals(50L, repository.saved!!.progress.coins)
        assertTrue(
            repository.saved!!
                .wardrobe.owned
                .isEmpty(),
        )
        assertEquals(ShopEffect.NotEnoughCoins, effects.last())
    }

    @Test
    fun `owned items go on and come off`() {
        val (vm, effects) = shop(coins = 0, wardrobe = WardrobeSave(owned = listOf("glasses")))
        vm.onSelect("glasses")
        vm.onToggleWear()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("glasses"), repository.saved!!.wardrobe.worn)
        assertEquals(ShopEffect.Changed(wearing = true), effects.last())
        vm.onToggleWear()
        dispatcher.scheduler.runCurrent()
        assertTrue(
            repository.saved!!
                .wardrobe.worn
                .isEmpty(),
        )
        assertFalse(vm.ui.selected!!.worn)
        assertEquals(ShopEffect.Changed(wearing = false), effects.last())
    }
}
