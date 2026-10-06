package com.mymagicalpet.feature.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mymagicalpet.ui.fx.FxKind
import com.mymagicalpet.ui.fx.ParticleField
import com.mymagicalpet.ui.pet.Wearables
import com.mymagicalpet.ui.sound.LocalGameSounds
import com.mymagicalpet.ui.sound.Sfx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import com.mymagicalpet.ui.R as UiR

/** Every item as a big tile, four to a row. */
@Composable
internal fun ItemGrid(
    state: ShopUiState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.items.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    ItemTile(item, selected = item.id == state.selectedId, onClick = { onSelect(item.id) }, modifier = Modifier.weight(1f))
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ItemTile(
    item: ShopItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val name = stringResource(itemName(item.id))
    val status =
        when {
            item.worn -> stringResource(R.string.wearing)
            item.owned -> stringResource(R.string.yours)
            else -> pluralStringResource(R.plurals.coins, item.cosmetic.price, item.cosmetic.price)
        }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.secondaryContainer else colors.surfaceVariant,
        border = if (selected) BorderStroke(3.dp, colors.primary) else null,
        modifier =
            modifier.aspectRatio(TILE_ASPECT).clearAndSetSemantics {
                contentDescription = "$name, $status"
                role = Role.Button
                this.selected = selected
            },
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Wearables.byId(item.id)?.let { Image(painterResource(it.drawable), contentDescription = null, modifier = Modifier.size(52.dp)) }
            when {
                item.worn -> Text(status, style = MaterialTheme.typography.labelLarge, color = colors.primary)
                item.owned -> Text(status, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                else -> Price(item.cosmetic.price, dim = !item.affordable)
            }
        }
    }
}

@Composable
private fun Price(
    price: Int,
    dim: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Image(painterResource(UiR.drawable.reward_coin), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            price.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DIM_ALPHA) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Buy, wear or take off: one big button for the selected item. */
@Composable
internal fun ItemAction(
    item: ShopItem,
    coins: Long,
    petName: String,
    actions: ShopActions,
) {
    val wide = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    when {
        item.worn -> {
            OutlinedButton(onClick = actions.onToggleWear, modifier = wide) {
                Text(stringResource(R.string.take_off), style = MaterialTheme.typography.titleMedium)
            }
        }

        item.owned -> {
            Button(onClick = actions.onToggleWear, modifier = wide) {
                Text(stringResource(R.string.wear), style = MaterialTheme.typography.titleMedium)
            }
        }

        item.affordable -> {
            Button(onClick = actions.onBuy, modifier = wide) {
                Image(painterResource(UiR.drawable.reward_coin), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                    stringResource(R.string.buy_for, item.cosmetic.price),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        else -> {
            val missing = (item.cosmetic.price - coins).toInt()
            // Still tappable: a gentle "not yet" with a hint where coins come from.
            FilledTonalButton(onClick = actions.onBuy, modifier = wide) {
                Text(pluralStringResource(R.plurals.coins_to_go, missing, missing), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(R.string.earn_hint, petName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Sound, haptics and sparkles for shop moments. */
@Composable
internal fun ShopEffectsPlayer(
    effects: Flow<ShopEffect>,
    fx: ParticleField,
    onCheer: (Boolean) -> Unit,
) {
    val sounds = LocalGameSounds.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                ShopEffect.TriedOn -> {
                    sounds.play(Sfx.SELECT)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    fx.burst(FxKind.SPARKLE, HEAD, count = SPARKLES_SMALL)
                }

                is ShopEffect.Bought -> {
                    sounds.play(Sfx.CELEBRATION)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    fx.burst(FxKind.STAR, CENTER, count = STARS)
                    fx.burst(FxKind.SPARKLE, HEAD, count = SPARKLES_LARGE)
                    onCheer(true)
                    delay(CHEER_MILLIS)
                    onCheer(false)
                }

                ShopEffect.NotEnoughCoins -> {
                    sounds.play(Sfx.CAUTION)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                }

                is ShopEffect.Changed -> {
                    sounds.play(if (effect.wearing) Sfx.TOGGLE_ON else Sfx.TOGGLE_OFF)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    if (effect.wearing) fx.burst(FxKind.SPARKLE, HEAD, count = SPARKLES_SMALL)
                }
            }
        }
    }
}

private const val COLUMNS = 4
private const val TILE_ASPECT = 0.82f
private const val DIM_ALPHA = 0.6f
private const val SPARKLES_SMALL = 5
private const val SPARKLES_LARGE = 12
private const val STARS = 10
private const val CHEER_MILLIS = 2_500L
private val HEAD = Offset(0.5f, 0.3f)
private val CENTER = Offset(0.5f, 0.55f)
