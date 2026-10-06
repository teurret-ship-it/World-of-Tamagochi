package com.mymagicalpet.feature.home

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
import com.mymagicalpet.sim.CareAction
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Need
import com.mymagicalpet.sim.Refusal
import com.mymagicalpet.ui.fx.FxKind
import com.mymagicalpet.ui.fx.ParticleField
import com.mymagicalpet.ui.fx.ParticleLayer
import com.mymagicalpet.ui.fx.rememberParticleField
import com.mymagicalpet.ui.pet.Pet
import com.mymagicalpet.ui.sound.LocalGameSounds
import com.mymagicalpet.ui.sound.Sfx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.hypot
import kotlin.math.roundToInt
import com.mymagicalpet.ui.R as UiR

@Composable
internal fun EffectsPlayer(
    effects: Flow<HomeEffect>,
    fx: ParticleField,
    onBubble: (String?) -> Unit,
    onLevelUp: (Int) -> Unit,
    shake: Animatable<Float, AnimationVector1D>,
    evolvedLines: Map<LifeStage, String>,
) {
    val sounds = LocalGameSounds.current
    val haptics = LocalHapticFeedback.current
    val refusals = Refusal.entries.associateWith { stringResource(it.messageRes()) }
    val needColors = Need.entries.associateWith { it.color() }
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is HomeEffect.Refused -> {
                    sounds.play(Sfx.CAUTION)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    onBubble(refusals.getValue(effect.reason))
                    shake.animateTo(0f, SHAKE_SPEC)
                }

                is HomeEffect.Evolved -> {
                    sounds.play(Sfx.CELEBRATION)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    fx.burst(FxKind.STAR, CENTER, count = LEVEL_UP_STARS)
                    fx.burst(FxKind.SPARKLE, TOP, count = BURST_LARGE)
                    onBubble(evolvedLines[effect.stage])
                }

                is HomeEffect.Cared -> {
                    celebrate(effect, fx, needColors)
                    sounds.play(effect.action.sound())
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    if (effect.reward.coins > 0) {
                        fx.burst(FxKind.COIN, MOUTH, count = effect.reward.coins.coerceAtMost(MAX_COINS_SHOWN))
                        delay(REWARD_SOUND_DELAY_MILLIS)
                        sounds.play(Sfx.REWARD)
                    }
                    effect.levelUp?.let { level ->
                        delay(REWARD_SOUND_DELAY_MILLIS)
                        sounds.play(Sfx.CELEBRATION)
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        fx.burst(FxKind.STAR, CENTER, count = LEVEL_UP_STARS)
                        onLevelUp(level)
                    }
                }
            }
        }
    }
}

private fun celebrate(
    effect: HomeEffect.Cared,
    fx: ParticleField,
    needColors: Map<Need, Color>,
) {
    when (effect.action) {
        CareAction.FEED -> fx.burst(FxKind.SPARKLE, MOUTH, count = BURST_LARGE)
        CareAction.WASH -> fx.burst(FxKind.BUBBLE, CENTER, count = BURST_MEDIUM)
        CareAction.PLAY -> fx.burst(FxKind.HEART, CENTER, count = BURST_LARGE)
        CareAction.STROKE -> fx.burst(FxKind.HEART, TOP, count = BURST_SMALL)
        CareAction.NAP -> fx.burst(FxKind.ZZZ, TOP, count = BURST_MEDIUM)
        CareAction.WAKE -> fx.burst(FxKind.SPARKLE, TOP, count = BURST_MEDIUM)
        CareAction.TREAT -> fx.burst(FxKind.HEART, MOUTH, count = BURST_LARGE)
    }
    effect.changes.entries
        .filter { it.value > 0 && effect.action != CareAction.STROKE }
        .forEachIndexed { i, (need, points) ->
            fx.label("+$points", needColors.getValue(need), Offset(LABEL_X + i * LABEL_SPACING, LABEL_Y))
        }
}

private fun CareAction.sound(): Sfx =
    when (this) {
        CareAction.FEED -> Sfx.SELECT
        CareAction.WASH -> Sfx.SWIPE
        CareAction.PLAY -> Sfx.BUTTON
        CareAction.STROKE -> Sfx.TAP
        CareAction.NAP -> Sfx.TOGGLE_OFF
        CareAction.WAKE -> Sfx.TOGGLE_ON
        CareAction.TREAT -> Sfx.POSITIVE
    }

private fun Refusal.messageRes(): Int =
    when (this) {
        Refusal.FULL -> R.string.refused_full
        Refusal.ALREADY_CLEAN -> R.string.refused_clean
        Refusal.TOO_TIRED -> R.string.refused_tired
        Refusal.NOT_SLEEPY -> R.string.refused_not_sleepy
        Refusal.ASLEEP -> R.string.refused_asleep
        Refusal.NO_COINS -> R.string.refused_no_coins
        Refusal.NO_TREATS_LEFT -> R.string.refused_no_treats
        Refusal.NOT_NAPPING, Refusal.EGG -> R.string.refused_other
    }
