package com.worldoftamagochi.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.sim.CareAction
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Refusal
import com.worldoftamagochi.ui.fx.FxKind
import com.worldoftamagochi.ui.fx.ParticleField
import com.worldoftamagochi.ui.fx.ParticleLayer
import com.worldoftamagochi.ui.fx.rememberParticleField
import com.worldoftamagochi.ui.pet.Pet
import com.worldoftamagochi.ui.sound.LocalGameSounds
import com.worldoftamagochi.ui.sound.Sfx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.hypot
import kotlin.math.roundToInt
import com.worldoftamagochi.ui.R as UiR

@Composable
fun HomeRoute(
    repository: GameRepository,
    onOpenRaces: () -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Nothing to draw until the saved pet is loaded (milliseconds after start).
    val loaded = state ?: return
    HomeScreen(
        state = loaded,
        actions =
            HomeActions(
                onStroke = viewModel::onStroke,
                onFeed = viewModel::onFeed,
                onWash = viewModel::onToggleSoap,
                onPlay = viewModel::onPlay,
                onLights = viewModel::onLights,
                onDismissAway = viewModel::onDismissAway,
                onSound = viewModel::onSoundToggled,
                onHaptics = viewModel::onHapticsToggled,
                onOpenRaces = onOpenRaces,
            ),
        effects = viewModel.effects,
    )
}

/**
 * The pet's room: level and coins on top, the pet in the middle with its
 * effects, needs and the care bar underneath.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    effects: Flow<HomeEffect> = emptyFlow(),
    animate: Boolean = true,
) {
    val fx = rememberParticleField()
    var bubble by remember { mutableStateOf<String?>(null) }
    var levelUp by remember { mutableStateOf<Int?>(null) }
    val shake = remember { Animatable(0f) }
    var petBounds by remember { mutableStateOf(Rect.Zero) }
    var draggedFood by remember { mutableStateOf<Offset?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }

    EffectsPlayer(effects, fx, onBubble = { bubble = it }, onLevelUp = { levelUp = it }, shake = shake)

    val stage: @Composable (Modifier) -> Unit = { mod ->
        PetStage(
            state = state,
            onStroke = actions.onStroke,
            animate = animate,
            fx = fx,
            bubble = bubble,
            shake = shake.value,
            modifier = mod.onGloballyPositioned { petBounds = it.boundsInRoot() },
        )
    }
    val care =
        CareCallbacks(
            onFeed = actions.onFeed,
            onWash = actions.onWash,
            onPlay = actions.onPlay,
            onLights = actions.onLights,
            onFoodDrag = { draggedFood = it },
            onFoodDrop = { if (petBounds.contains(it)) actions.onFeed() },
        )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box {
            HomeLayout(state, stage, care, onSettings = { settingsOpen = true }, onOpenRaces = actions.onOpenRaces)
            draggedFood?.let { FoodInHand(it) }
            LevelUpBanner(levelUp, onDone = { levelUp = null })
            state.away?.let { AwayCard(state, it, actions.onDismissAway) }
            if (settingsOpen) SettingsSheet(state, actions, onClose = { settingsOpen = false })
        }
    }
}

@Composable
private fun HomeLayout(
    state: HomeUiState,
    stage: @Composable (Modifier) -> Unit,
    care: CareCallbacks,
    onSettings: () -> Unit,
    onOpenRaces: () -> Unit,
) {
    BoxWithConstraints {
        if (maxWidth >= WIDE_SCREEN) {
            Row(
                modifier = Modifier.safeDrawingPadding().padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stage(Modifier.weight(1f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TopBar(state, onSettings)
                    Status(state)
                    CareBar(state, care)
                    RacesButton(onOpenRaces)
                }
            }
        } else {
            Column(
                modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TopBar(state, onSettings)
                stage(Modifier.fillMaxWidth())
                CareBar(state, care)
                RacesButton(onOpenRaces)
                Status(state)
            }
        }
    }
}

@Composable
private fun Status(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val hint =
            when {
                state.napping -> stringResource(R.string.napping_hint, state.name)
                state.asleep -> stringResource(R.string.asleep_hint, state.name)
                state.washing -> stringResource(R.string.washing_hint, state.name)
                state.urgentNeed == Need.SATIETY -> stringResource(R.string.feed_hint, state.name)
                state.urgentNeed != null -> stringResource(R.string.need_urgent, stringResource(state.urgentNeed.label()))
                else -> stringResource(R.string.stroke_hint, state.name)
            }
        Text(
            text = hint,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        )
        NeedBars(needs = state.needs, urgent = state.urgentNeed, modifier = Modifier.widthIn(max = 520.dp))
    }
}
