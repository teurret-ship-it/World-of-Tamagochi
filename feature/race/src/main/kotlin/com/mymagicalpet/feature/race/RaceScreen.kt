package com.mymagicalpet.feature.race

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.network.OnlineRacing
import com.mymagicalpet.sim.Expression
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.race.Runner
import com.mymagicalpet.sim.race.SprintTracks
import com.mymagicalpet.sim.race.Tracks
import com.mymagicalpet.ui.pet.Pet
import com.mymagicalpet.ui.pet.PetPose
import com.mymagicalpet.ui.sound.LocalGameSounds
import com.mymagicalpet.ui.sound.Sfx
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import com.mymagicalpet.ui.R as UiR

@Composable
fun RaceRoute(
    repository: GameRepository,
    online: OnlineRacing?,
    trackId: String,
    onExit: () -> Unit,
) {
    val track = remember(trackId) { Tracks.byId(trackId) ?: SprintTracks.MEADOW }
    val viewModel: RaceViewModel = viewModel(key = trackId) { RaceViewModel(repository, track, online = online) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sounds = LocalGameSounds.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RaceEvent.COUNTDOWN -> {
                    sounds.play(Sfx.TAP)
                }

                RaceEvent.GO -> {
                    sounds.play(Sfx.POSITIVE)
                }

                RaceEvent.JUMP -> {
                    sounds.play(Sfx.SWIPE)
                }

                RaceEvent.HURDLE_HIT, RaceEvent.FAULT -> {
                    sounds.play(Sfx.CAUTION)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                }

                RaceEvent.BOOST -> {
                    sounds.play(Sfx.TOGGLE_ON)
                }

                RaceEvent.FINISH -> {
                    sounds.play(Sfx.BUTTON)
                }

                RaceEvent.MEDAL -> {
                    sounds.play(Sfx.CELEBRATION)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }

                RaceEvent.LEVEL_UP -> {
                    sounds.play(Sfx.REWARD)
                }
            }
        }
    }
    LaunchedEffect(viewModel) {
        while (true) withFrameNanos(viewModel::onFrame)
    }
    val loaded = state ?: return
    RaceScreen(
        state = loaded,
        controls = RaceControls(viewModel::onSprint, viewModel::onJump, viewModel::onRetry, onExit),
    )
}

class RaceControls(
    val onSprint: (Boolean) -> Unit = {},
    val onJump: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onExit: () -> Unit = {},
)

@Composable
fun RaceScreen(
    state: RaceUiState,
    controls: RaceControls,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Course(state, Modifier.weight(1f).fillMaxWidth())
                Pad(state, controls)
            }
            Hud(state, controls.onExit, Modifier.safeDrawingPadding().padding(12.dp))
            state.countdown?.let { Countdown(it) }
            state.result?.let { FinishCard(it, controls) }
        }
    }
}

@Composable
private fun Course(
    state: RaceUiState,
    modifier: Modifier,
) {
    val colors = remember(state.track.id) { courseColors(state.track.id) }
    val images =
        CourseImages(
            hurdle = ImageBitmap.imageResource(UiR.drawable.race_hurdle),
            puddle = ImageBitmap.imageResource(UiR.drawable.race_puddle),
            boost = ImageBitmap.imageResource(UiR.drawable.race_boost),
            finish = ImageBitmap.imageResource(UiR.drawable.race_finish),
            slow = ImageBitmap.imageResource(UiR.drawable.race_slow),
        )
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val size = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val camera = CourseCamera(size, state.runner.xMm)
        Canvas(Modifier.fillMaxSize()) { drawCourse(state.track, camera, colors, images) }
        state.ghost?.let { ghost ->
            val (ghostGenome, ghostWearing) =
                when (state.ghostKind) {
                    GhostKind.COACH -> COACH_GENOME to COACH_WEARING
                    GhostKind.RIVAL -> Genome.fromSeed(state.ghostName.hashCode().toLong()) to emptyList()
                    GhostKind.PERSONAL_BEST -> state.genome to state.wearing
                }
            RunnerSprite(ghost, ghostGenome, ghostWearing, camera, alpha = GHOST_ALPHA)
        }
        RunnerSprite(state.runner, state.genome, state.wearing, camera, alpha = 1f)
        Canvas(Modifier.fillMaxSize()) { drawCourseForeground(state.track, camera) }
    }
}

@Composable
private fun RunnerSprite(
    runner: Runner,
    genome: Genome,
    wearing: List<String>,
    camera: CourseCamera,
    alpha: Float,
) {
    val sizePx = PET_SIZE_MM * camera.pxPerMm
    val sizeDp = with(LocalDensity.current) { sizePx.toDp() }
    // A jelly hop while running: two hops per metre, flat in the air.
    val hopMm = if (runner.grounded) (abs(sin(runner.xMm * PI / HOP_STRIDE_MM)) * HOP_HEIGHT_MM).toInt() else 0
    val x = camera.x(runner.xMm) - sizePx / 2
    val y = camera.groundY - sizePx - camera.height(runner.yMm + hopMm)
    val expression =
        when {
            runner.stumbleTicks > 0 -> Expression.HUNGRY
            runner.exhausted -> Expression.SLEEPY
            else -> Expression.HAPPY
        }
    Pet(
        genome = genome,
        expression = expression,
        name = "",
        animate = false,
        pose = PetPose(look = Offset(1f, 0f), squash = if (runner.grounded) hopMm / HOP_HEIGHT_MM.toFloat() * SQUASH_ON_LANDING else 0f),
        wearing = wearing,
        modifier =
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(sizeDp)
                .graphicsLayer { this.alpha = alpha },
    )
}

@Composable
private fun Hud(
    state: RaceUiState,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onExit) { Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium) }
            Image(painterResource(UiR.drawable.race_timer), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(
                stringResource(R.string.seconds, formatSeconds(state.elapsedMicros)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = HUD_INK,
            )
            // Agility faults show at once as seconds added to the clock.
            if (state.runner.faults > 0) {
                Text(
                    stringResource(R.string.race_penalty, state.runner.faults * FAULT_SECONDS),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Box(Modifier.weight(1f))
            val ghostLabel =
                when (state.ghostKind) {
                    GhostKind.RIVAL -> stringResource(R.string.race_ghost_rival, state.ghostName.orEmpty())
                    GhostKind.PERSONAL_BEST -> stringResource(R.string.race_ghost_best)
                    GhostKind.COACH -> stringResource(R.string.race_ghost_coach)
                }
            Text(ghostLabel, style = MaterialTheme.typography.titleMedium, color = HUD_INK)
        }
        RaceProgress(state)
        StaminaBar(state)
    }
}

@Composable
private fun RaceProgress(state: RaceUiState) {
    val length = state.track.lengthMm.toFloat()
    Box(Modifier.fillMaxWidth().height(14.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                PROGRESS_TRACK,
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(size.height / 2),
            )
            state.ghost?.let {
                drawCircle(
                    GHOST_DOT,
                    size.height / 2,
                    Offset(
                        size.width * (it.xMm / length).coerceIn(0f, 1f),
                        size.height / 2,
                    ),
                )
            }
            drawCircle(
                PLAYER_DOT,
                size.height * PLAYER_DOT_SCALE,
                Offset(
                    size.width * (state.runner.xMm / length).coerceIn(0f, 1f),
                    size.height / 2,
                ),
            )
        }
    }
}

@Composable
private fun StaminaBar(state: RaceUiState) {
    val label = stringResource(if (state.runner.exhausted) R.string.race_tired else R.string.race_stamina)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = HUD_INK)
        LinearProgressIndicator(
            progress = { state.runner.stamina / state.staminaMax.toFloat() },
            color = if (state.runner.exhausted) STAMINA_EMPTY else STAMINA_FULL,
            trackColor = STAMINA_TRACK,
            drawStopIndicator = {},
            modifier = Modifier.weight(1f).height(12.dp),
        )
    }
}

@Composable
private fun Pad(
    state: RaceUiState,
    controls: RaceControls,
) {
    val enabled = state.result == null
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val sprintLabel = stringResource(R.string.race_sprint)
        HoldButton(
            label = sprintLabel,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f),
            onPress = { if (enabled) controls.onSprint(true) },
            onRelease = { controls.onSprint(false) },
        )
        HoldButton(
            label = stringResource(R.string.race_jump),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f),
            onPress = { if (enabled) controls.onJump() },
            onRelease = {},
        )
    }
}

/** A big button that acts on press (not release), as games need. */
@Composable
private fun HoldButton(
    label: String,
    color: Color,
    modifier: Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 4.dp,
        modifier =
            modifier
                .height(BUTTON_HEIGHT)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                }.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPress()
                            tryAwaitRelease()
                            onRelease()
                        },
                    )
                },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

private val COACH_GENOME = Genome.fromSeed(2_026)

/** The coach wears a cap, so a child can tell the coach from their own ghost. */
private val COACH_WEARING = listOf("cap")
private const val GHOST_ALPHA = 0.45f
private const val PET_SIZE_MM = 1_100f
private const val FAULT_SECONDS = 2
private const val HOP_STRIDE_MM = 700.0
private const val HOP_HEIGHT_MM = 110
private const val SQUASH_ON_LANDING = 0.35f
private val BUTTON_HEIGHT = 96.dp
private val HUD_INK = Color(0xFF1F1A17)
private val PROGRESS_TRACK = Color(0x55000000)
private val GHOST_DOT = Color(0x99FFFFFF)
private val PLAYER_DOT = Color(0xFFFF9E7A)
private val STAMINA_EMPTY = Color(0xFFE57373)
private val STAMINA_FULL = Color(0xFFFFB300)
private val STAMINA_TRACK = Color(0x55FFFFFF)
private const val PLAYER_DOT_SCALE = 0.62f
