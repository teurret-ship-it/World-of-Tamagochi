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

/** Everything the player can do on the home screen. */
class HomeActions(
    val onStroke: () -> Unit = {},
    val onFeed: () -> Unit = {},
    val onWash: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onLights: () -> Unit = {},
    val onDismissAway: () -> Unit = {},
    val onSound: (Boolean) -> Unit = {},
    val onHaptics: (Boolean) -> Unit = {},
    val onTreat: () -> Unit = {},
    val onSleepWindow: (bedtimeMinute: Int, wakeMinute: Int) -> Unit = { _, _ -> },
    val onOpenRaces: () -> Unit = {},
    val onOpenShop: () -> Unit = {},
)

/** Callbacks for the care bar; the food button can also be dragged onto the pet. */
internal class CareCallbacks(
    actions: HomeActions,
    val onFoodDrag: (rootPosition: Offset?) -> Unit,
    val onFoodDrop: (rootPosition: Offset) -> Unit,
) {
    val onFeed: () -> Unit = actions.onFeed
    val onWash: () -> Unit = actions.onWash
    val onPlay: () -> Unit = actions.onPlay
    val onLights: () -> Unit = actions.onLights
    val onTreat: () -> Unit = actions.onTreat
}
