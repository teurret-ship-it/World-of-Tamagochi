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
internal fun PetStage(
    state: HomeUiState,
    onStroke: () -> Unit,
    animate: Boolean,
    fx: ParticleField,
    bubble: String?,
    shake: Float,
    modifier: Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val strokeDistancePx = with(LocalDensity.current) { STROKE_DISTANCE.toPx() }
    var look by remember { mutableStateOf(Offset.Zero) }
    val squash = remember { Animatable(0f) }

    // One bouncy squash per accepted stroke.
    LaunchedEffect(state.strokes) {
        if (state.strokes == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        squash.animateTo(1f, tween(SQUASH_IN_MILLIS))
        squash.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    val strokeLabel = stringResource(R.string.stroke_action, state.name)
    Box(
        modifier =
            modifier
                .widthIn(max = 420.dp)
                .aspectRatio(1f)
                .graphicsLayer { translationX = shake * SHAKE_PX }
                .strokeable(state.asleep, strokeLabel, strokeDistancePx, onStroke) { look = it },
        contentAlignment = Alignment.Center,
    ) {
        Pet(
            genome = state.genome,
            expression = state.expression,
            name = state.name,
            animate = animate,
            look = look,
            squash = squash.value,
            modifier = Modifier.fillMaxSize(),
        )
        if (state.asleep) {
            Box(Modifier.fillMaxSize().background(NIGHT_DIM, RoundedCornerShape(32.dp)))
        }
        ParticleLayer(fx, Modifier.fillMaxSize(), labelStyle = MaterialTheme.typography.headlineMedium)
        AnimatedVisibility(
            visible = bubble != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            SpeechBubble(bubble.orEmpty())
        }
    }
}

private fun lookToward(
    point: Offset,
    size: IntSize,
): Offset {
    if (size.width == 0 || size.height == 0) return Offset.Zero
    // Map the touch point to -1..1 around the pet's centre.
    val dx = (point.x / size.width - HALF) / HALF
    val dy = (point.y / size.height - HALF) / HALF
    return Offset(dx.coerceIn(-1f, 1f), dy.coerceIn(-1f, 1f))
}

/** Rubbing strokes the pet (every [distancePx] of travel); TalkBack gets a "Stroke" action instead. */
private fun Modifier.strokeable(
    asleep: Boolean,
    label: String,
    distancePx: Float,
    onStroke: () -> Unit,
    onLook: (Offset) -> Unit,
): Modifier =
    semantics {
        // Stroking without a precise gesture (CLAUDE.md section 4).
        customActions =
            listOf(
                CustomAccessibilityAction(label) {
                    onStroke()
                    true
                },
            )
    }.pointerInput(asleep) {
        var travelled = 0f
        detectDragGestures(
            onDragEnd = { onLook(Offset.Zero) },
            onDragCancel = { onLook(Offset.Zero) },
        ) { change, drag ->
            onLook(lookToward(change.position, size))
            travelled += hypot(drag.x, drag.y)
            if (travelled >= distancePx) {
                travelled = 0f
                onStroke()
            }
        }
    }
