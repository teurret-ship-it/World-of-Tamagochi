package com.mymagicalpet.feature.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.sim.Expression
import com.mymagicalpet.ui.fx.ParticleLayer
import com.mymagicalpet.ui.fx.rememberParticleField
import com.mymagicalpet.ui.pet.Pet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.mymagicalpet.ui.R as UiR

@Composable
fun ShopRoute(
    repository: GameRepository,
    onBack: () -> Unit,
) {
    val viewModel: ShopViewModel = viewModel { ShopViewModel(repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = state ?: return
    ShopScreen(
        state = loaded,
        actions = ShopActions(viewModel::onSelect, viewModel::onBuy, viewModel::onToggleWear, onBack),
        effects = viewModel.effects,
    )
}

class ShopActions(
    val onSelect: (String) -> Unit = {},
    val onBuy: () -> Unit = {},
    val onToggleWear: () -> Unit = {},
    val onBack: () -> Unit = {},
)

/**
 * The shop: the pet in a fitting room on top, every item underneath. Tapping an
 * item tries it on for free; the button under the pet buys, wears or takes off.
 */
@Composable
fun ShopScreen(
    state: ShopUiState,
    actions: ShopActions,
    modifier: Modifier = Modifier,
    effects: Flow<ShopEffect> = emptyFlow(),
    animate: Boolean = true,
) {
    val fx = rememberParticleField()
    var cheer by remember { mutableStateOf(false) }
    ShopEffectsPlayer(effects, fx, onCheer = { cheer = it })
    val fitting: @Composable (Modifier) -> Unit = { mod ->
        FittingRoom(state, actions, cheer, animate, mod) { ParticleLayer(fx, Modifier.fillMaxSize()) }
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.safeDrawingPadding()) {
            if (maxWidth >= WIDE_SCREEN) {
                Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ShopHeader(state, actions.onBack)
                        fitting(Modifier.fillMaxWidth())
                    }
                    ItemGrid(state, actions.onSelect, Modifier.weight(1f).verticalScroll(rememberScrollState()))
                }
            } else {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ShopHeader(state, actions.onBack)
                    fitting(Modifier.fillMaxWidth())
                    ItemGrid(state, actions.onSelect, Modifier.widthIn(max = 560.dp))
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(
    state: ShopUiState,
    onBack: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium) }
        Image(painterResource(UiR.drawable.ui_shop), contentDescription = null, modifier = Modifier.size(36.dp))
        Text(
            stringResource(R.string.shop_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        CoinChip(state.coins)
    }
}

@Composable
internal fun CoinChip(coins: Long) {
    val label = pluralStringResource(R.plurals.coins, coins.toInt(), coins.toInt())
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(painterResource(UiR.drawable.reward_coin), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(coins.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FittingRoom(
    state: ShopUiState,
    actions: ShopActions,
    cheer: Boolean,
    animate: Boolean,
    modifier: Modifier,
    overlay: @Composable () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(240.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Pet(
                genome = state.genome,
                expression = Expression.HAPPY,
                name = state.name,
                animate = animate,
                wearing = state.preview,
                modifier = Modifier.fillMaxSize(),
            )
            overlay()
        }
        val selected = state.selected
        val title =
            when {
                cheer -> stringResource(R.string.bought, state.name)
                selected != null -> stringResource(itemName(selected.id))
                else -> stringResource(R.string.try_on_hint)
            }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        selected?.let { ItemAction(it, state.coins, state.name, actions) }
    }
}

private val WIDE_SCREEN = 720.dp
