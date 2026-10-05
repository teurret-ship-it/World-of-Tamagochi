package com.worldoftamagochi.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.ui.fx.FxKind
import com.worldoftamagochi.ui.fx.ParticleField
import com.worldoftamagochi.ui.fx.ParticleLayer
import com.worldoftamagochi.ui.pet.Egg
import com.worldoftamagochi.ui.pet.Pet
import com.worldoftamagochi.ui.sound.LocalGameSounds
import com.worldoftamagochi.ui.sound.Sfx

/** The moving parts of the hatching: the egg's wobble, the shell lifting off, the pet popping out. */
private class HatchMotion(
    val wobble: Animatable<Float, AnimationVector1D>,
    val open: Animatable<Float, AnimationVector1D>,
    val petScale: Animatable<Float, AnimationVector1D>,
    val fx: ParticleField,
)

/**
 * The hatching: every tap makes the egg wobble and the crack grow; the last
 * tap pops the top of the shell off and the pet bounces out among stars.
 */
@Composable
internal fun HatchStep(
    state: OnboardingUiState,
    actions: OnboardingActions,
    animate: Boolean,
) {
    val seed = state.chosenEgg ?: return
    val genome = remember(seed) { Genome.fromSeed(seed) }
    val motion = rememberHatchMotion(state, animate)
    Text(
        text =
            when {
                state.hatched -> stringResource(R.string.hatch_done, state.name.orEmpty())
                state.taps > 0 -> stringResource(R.string.hatch_cracking)
                else -> stringResource(R.string.hatch_title)
            },
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    HatchStage(genome, state, motion, animate, actions.onTapEgg)
    if (state.hatched) NextButton(stringResource(R.string.hatch_go), enabled = true, onClick = actions.onDone)
}

@Composable
private fun rememberHatchMotion(
    state: OnboardingUiState,
    animate: Boolean,
): HatchMotion {
    val sounds = LocalGameSounds.current
    val haptics = LocalHapticFeedback.current
    val done = if (state.hatched && !animate) 1f else 0f
    val motion = remember { HatchMotion(Animatable(0f), Animatable(done), Animatable(done), ParticleField()) }
    LaunchedEffect(state.taps) {
        if (state.taps == 0 || state.hatched) return@LaunchedEffect
        sounds.play(if (state.taps % 2 == 0) Sfx.TAP else Sfx.SELECT)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        motion.fx.burst(FxKind.SPARKLE, CRACK_POINT, count = CRACK_SPARKLES)
        motion.wobble.animateTo(
            0f,
            keyframes {
                durationMillis = WOBBLE_STEP_MILLIS * (WOBBLE.size + 1)
                WOBBLE.forEachIndexed { i, angle -> angle at WOBBLE_STEP_MILLIS * (i + 1) }
            },
        )
    }
    LaunchedEffect(state.hatched) {
        if (!state.hatched || !animate) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        sounds.play(Sfx.CELEBRATION)
        motion.fx.burst(FxKind.STAR, CENTER, count = STARS)
        motion.fx.burst(FxKind.HEART, CENTER, count = HEARTS)
        motion.open.animateTo(1f, tween(OPEN_MILLIS))
        motion.petScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    return motion
}

@Composable
private fun HatchStage(
    genome: Genome,
    state: OnboardingUiState,
    motion: HatchMotion,
    animate: Boolean,
    onTap: () -> Unit,
) {
    val crack by animateFloatAsState(state.taps / OnboardingViewModel.TAPS_TO_HATCH.toFloat(), tween(CRACK_MILLIS), label = "crack")
    val idle = if (animate && state.taps == 0) idleWobble(0, excited = true) else 0f
    val eggLabel = stringResource(R.string.hatch_egg)
    Box(
        Modifier
            .size(300.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !state.hatched,
                role = Role.Button,
                onClickLabel = eggLabel,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (motion.petScale.value > 0f) {
            Pet(
                genome = genome,
                expression = Expression.HAPPY,
                name = state.name.orEmpty(),
                animate = animate,
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = motion.petScale.value
                        scaleY = motion.petScale.value
                        transformOrigin = PET_ORIGIN
                    },
            )
        }
        if (motion.open.value < 1f) {
            Egg(genome, eggLabel, Modifier.fillMaxSize(), wobble = motion.wobble.value + idle, crack = crack, open = motion.open.value)
        }
        ParticleLayer(motion.fx, Modifier.fillMaxSize(), labelStyle = MaterialTheme.typography.headlineMedium)
    }
}

/** A gentle rocking that never stops, so eggs look alive; [phase] keeps neighbours out of step. */
@Composable
internal fun idleWobble(
    phase: Int,
    excited: Boolean,
): Float {
    val transition = rememberInfiniteTransition(label = "egg")
    val t by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(IDLE_MILLIS + phase * IDLE_PHASE_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "rock",
    )
    return t * if (excited) EXCITED_DEGREES else IDLE_DEGREES
}

private val WOBBLE = listOf(14f, -12f, 9f, -5f, 2f)
private const val WOBBLE_STEP_MILLIS = 70
private const val CRACK_MILLIS = 250
private const val OPEN_MILLIS = 450
private const val STARS = 14
private const val CRACK_SPARKLES = 3
private val PET_ORIGIN = TransformOrigin(0.5f, 1f)
private const val HEARTS = 6
private const val IDLE_MILLIS = 1_400
private const val IDLE_PHASE_MILLIS = 230
private const val IDLE_DEGREES = 3f
private const val EXCITED_DEGREES = 7f
private val CENTER = Offset(0.5f, 0.55f)
private val CRACK_POINT = Offset(0.5f, 0.45f)
