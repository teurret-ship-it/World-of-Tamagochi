package com.mymagicalpet.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.data.GameSave
import com.mymagicalpet.data.record
import com.mymagicalpet.data.toSave
import com.mymagicalpet.data.toWardrobe
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.journal.Deed
import com.mymagicalpet.sim.localEpochDay
import com.mymagicalpet.sim.shop.Catalog
import com.mymagicalpet.sim.shop.Purchase
import com.mymagicalpet.sim.shop.Shop
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

/** The shop: try on for free, buy with coins, put on and take off. Rules live in [Shop]. */
class ShopViewModel(
    private val repository: GameRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val selectedId = MutableStateFlow<String?>(null)

    /** Null until the save is loaded. */
    val state: StateFlow<ShopUiState?> =
        combine(repository.game, selectedId) { game, selected -> game?.let { stateOf(it, selected) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _effects = Channel<ShopEffect>(Channel.BUFFERED)
    val effects: Flow<ShopEffect> = _effects.receiveAsFlow()

    fun onSelect(id: String) {
        if (Catalog.byId(id) == null) return
        selectedId.value = id
        _effects.trySend(ShopEffect.TriedOn)
    }

    fun onBuy() {
        val item = selectedId.value?.let(Catalog::byId) ?: return
        viewModelScope.launch {
            var outcome: Purchase? = null
            repository.updateGame { game ->
                val purchase = Shop.buy(game.progress.coins, game.wardrobe.toWardrobe(), item)
                outcome = purchase
                if (purchase is Purchase.Bought) {
                    game
                        .copy(progress = game.progress.copy(coins = purchase.coinsLeft), wardrobe = purchase.wardrobe.toSave())
                        .record(listOf(Deed.PURCHASE), localEpochDay(clock(), zone))
                        .first
                } else {
                    game
                }
            }
            when (outcome) {
                is Purchase.Bought -> _effects.send(ShopEffect.Bought(item.id))
                is Purchase.NotEnoughCoins -> _effects.send(ShopEffect.NotEnoughCoins)
                Purchase.AlreadyOwned, null -> Unit
            }
        }
    }

    /** Puts the selected item on, or takes it off when it is already worn. */
    fun onToggleWear() {
        val item = selectedId.value?.let(Catalog::byId) ?: return
        viewModelScope.launch {
            var wearing = false
            repository.updateGame { game ->
                val wardrobe = Shop.toggle(game.wardrobe.toWardrobe(), item)
                wearing = wardrobe.equipped[item.slot] == item.id
                game.copy(wardrobe = wardrobe.toSave())
            }
            _effects.send(ShopEffect.Changed(wearing))
        }
    }

    private fun stateOf(
        game: GameSave,
        selected: String?,
    ): ShopUiState {
        val wardrobe = game.wardrobe.toWardrobe()
        val coins = game.progress.coins
        return ShopUiState(
            name = game.pet.name,
            genome = Genome.fromSeed(game.pet.seed),
            coins = coins,
            items =
                Catalog.ITEMS.map {
                    ShopItem(
                        cosmetic = it,
                        owned = it.id in wardrobe.owned,
                        worn = wardrobe.equipped[it.slot] == it.id,
                        affordable = coins >= it.price,
                    )
                },
            worn = wardrobe.equipped,
            selectedId = selected,
        )
    }
}
