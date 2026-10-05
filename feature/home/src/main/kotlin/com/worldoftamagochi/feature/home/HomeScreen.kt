package com.worldoftamagochi.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldoftamagochi.ui.pet.Pet
import kotlin.math.hypot

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel { HomeViewModel() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, onStroke = viewModel::onStroke)
}

/**
 * The pet's room: the pet in the middle, its needs underneath. Rubbing the pet
 * strokes it; the pet's eyes follow the finger.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStroke: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints { HomeLayout(state, onStroke, animate, wide = maxWidth >= WIDE_SCREEN) }
    }
}

@Composable
private fun HomeLayout(
    state: HomeUiState,
    onStroke: () -> Unit,
    animate: Boolean,
    wide: Boolean,
) {
    if (wide) {
        Row(
            modifier = Modifier.safeDrawingPadding().padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PetStage(state, onStroke, animate, Modifier.weight(1f))
            Column(Modifier.weight(1f)) { Status(state) }
        }
    } else {
        Column(
            modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PetStage(state, onStroke, animate, Modifier.fillMaxWidth())
            Status(state)
        }
    }
}

@Composable
private fun Status(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = state.name,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val hint =
            when {
                state.asleep -> stringResource(R.string.asleep_hint, state.name)
                state.urgentNeed != null -> stringResource(R.string.need_urgent, stringResource(state.urgentNeed.label()))
                else -> stringResource(R.string.stroke_hint, state.name)
            }
        Text(
            text = hint,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        NeedBars(needs = state.needs, urgent = state.urgentNeed, modifier = Modifier.widthIn(max = 520.dp))
    }
}

@Composable
private fun PetStage(
    state: HomeUiState,
    onStroke: () -> Unit,
    animate: Boolean,
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
                .semantics {
                    // Stroking without a precise gesture (CLAUDE.md section 4).
                    customActions =
                        listOf(
                            CustomAccessibilityAction(strokeLabel) {
                                onStroke()
                                true
                            },
                        )
                }.pointerInput(state.asleep) {
                    var travelled = 0f
                    detectDragGestures(
                        onDragEnd = { look = Offset.Zero },
                        onDragCancel = { look = Offset.Zero },
                    ) { change, drag ->
                        look = lookToward(change.position, this.size)
                        travelled += hypot(drag.x, drag.y)
                        if (travelled >= strokeDistancePx) {
                            travelled = 0f
                            onStroke()
                        }
                    }
                },
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

private const val HALF = 0.5f
private val STROKE_DISTANCE = 140.dp
private const val SQUASH_IN_MILLIS = 90
private val WIDE_SCREEN = 840.dp
