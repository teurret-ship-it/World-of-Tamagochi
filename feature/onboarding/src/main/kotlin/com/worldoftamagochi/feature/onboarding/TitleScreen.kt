package com.worldoftamagochi.feature.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.data.toWardrobe
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.ui.pet.Egg
import com.worldoftamagochi.ui.pet.Pet
import kotlin.math.PI
import kotlin.math.sin

/** The player's pet as the title screen shows it. */
data class TitlePet(
    val name: String,
    val genome: Genome,
    val wearing: List<String> = emptyList(),
)

class TitleActions(
    val onPlay: () -> Unit = {},
    val onNewPet: () -> Unit = {},
    val onRaces: () -> Unit = {},
    val onShop: () -> Unit = {},
)

@Composable
fun TitleRoute(
    repository: GameRepository,
    actions: TitleActions,
) {
    // Loaded once: Unit until the save has been read, so a returning player never sees "Get your pet!".
    val pet by produceState<Any?>(initialValue = Unit, repository) {
        value =
            repository.loadGame()?.let { game ->
                TitlePet(
                    game.pet.name,
                    Genome.fromSeed(game.pet.seed),
                    game.wardrobe
                        .toWardrobe()
                        .equipped.values
                        .toList(),
                )
            }
    }
    if (pet == Unit) return
    TitleScreen(pet as? TitlePet, actions)
}

/** The main menu: the logo, your pet saying hello, and the way into every part of the game. */
@Composable
fun TitleScreen(
    pet: TitlePet?,
    actions: TitleActions,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val sky = MaterialTheme.colorScheme.tertiaryContainer
    val ground = MaterialTheme.colorScheme.background
    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(sky, ground)))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Logo(animate)
        Text(
            stringResource(R.string.title_tagline),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (pet != null) {
            Pet(pet.genome, Expression.HAPPY, pet.name, Modifier.size(220.dp), animate = animate, wearing = pet.wearing)
            Text(
                stringResource(R.string.title_greeting, pet.name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TITLE_EGGS.forEachIndexed { i, seed ->
                    val wobble = if (animate) idleWobble(i, excited = false) else 0f
                    Egg(Genome.fromSeed(seed), "", Modifier.size(96.dp).clearAndSetSemantics {}, wobble = wobble)
                }
            }
        }
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pet != null) {
                Button(onClick = actions.onPlay, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    Text(stringResource(R.string.title_play), style = MaterialTheme.typography.headlineMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = actions.onRaces, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(stringResource(R.string.title_races), style = MaterialTheme.typography.titleLarge)
                    }
                    FilledTonalButton(onClick = actions.onShop, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(stringResource(R.string.title_shop), style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else {
                Button(onClick = actions.onNewPet, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    Text(stringResource(R.string.title_start), style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

/** "World of Tamagochi" with bouncing, many-coloured letters. */
@Composable
private fun Logo(animate: Boolean) {
    val small = stringResource(R.string.app_title_small)
    val big = stringResource(R.string.app_title_big)
    val transition = rememberInfiniteTransition(label = "logo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BOUNCE_MILLIS), RepeatMode.Restart),
        label = "bounce",
    )
    val phase = if (animate) t else 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.padding(top = 16.dp).semantics(mergeDescendants = true) {
                heading()
                contentDescription = "$small $big"
            },
    ) {
        Text(small, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Row {
            big.forEachIndexed { i, letter ->
                val lift = if (animate) (sin((phase + i * LETTER_PHASE) * 2 * PI) * LIFT_PX).toFloat() else 0f
                Text(
                    letter.toString(),
                    style =
                        MaterialTheme.typography.displayMedium.merge(
                            TextStyle(shadow = LOGO_SHADOW),
                        ),
                    color = LETTER_COLORS[i % LETTER_COLORS.size],
                    modifier = Modifier.graphicsLayer { translationY = lift },
                )
            }
        }
    }
}

private val LETTER_COLORS =
    listOf(
        Color(0xFFE8603C),
        Color(0xFF2E9E7E),
        Color(0xFF3B7DD8),
        Color(0xFFD94C8A),
        Color(0xFFB57A00),
        Color(0xFF7E57C2),
    )
private val LOGO_SHADOW = Shadow(Color(0x55000000), Offset(0f, 4f), 6f)
private val TITLE_EGGS = listOf(11L, 23L, 37L)
private const val BOUNCE_MILLIS = 1_800
private const val LETTER_PHASE = 0.09
private const val LIFT_PX = 8.0
