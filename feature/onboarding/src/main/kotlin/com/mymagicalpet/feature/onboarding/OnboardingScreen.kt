package com.mymagicalpet.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.ui.pet.Egg
import com.mymagicalpet.ui.time.TimeStepper

@Composable
fun OnboardingRoute(
    repository: GameRepository,
    onDone: () -> Unit,
) {
    val viewModel: OnboardingViewModel = viewModel { OnboardingViewModel(repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        actions =
            OnboardingActions(
                onChooseEgg = viewModel::onChooseEgg,
                onMoreEggs = viewModel::onMoreEggs,
                onPickName = viewModel::onPickName,
                onMoreNames = viewModel::onMoreNames,
                onSleepWindow = viewModel::onSleepWindow,
                onNext = viewModel::onNext,
                onBack = viewModel::onBack,
                onTapEgg = viewModel::onTapEgg,
                onDone = onDone,
            ),
    )
}

class OnboardingActions(
    val onChooseEgg: (Long) -> Unit = {},
    val onMoreEggs: () -> Unit = {},
    val onPickName: (String) -> Unit = {},
    val onMoreNames: () -> Unit = {},
    val onSleepWindow: (Int, Int) -> Unit = { _, _ -> },
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onTapEgg: () -> Unit = {},
    val onDone: () -> Unit = {},
)

/** First launch: four short steps, one decision each, big buttons, no typing. */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(state, actions.onBack)
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (state.step) {
                    OnboardingStep.CHOOSE_EGG -> EggStep(state, actions, animate)
                    OnboardingStep.NAME -> NameStep(state, actions)
                    OnboardingStep.SLEEP -> SleepStep(state, actions)
                    OnboardingStep.HATCH -> HatchStep(state, actions, animate)
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    state: OnboardingUiState,
    onBack: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val canGoBack = state.step != OnboardingStep.CHOOSE_EGG && !(state.step == OnboardingStep.HATCH && state.taps > 0)
        if (canGoBack) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium) }
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.step, state.step.ordinal + 1, OnboardingStep.entries.size),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Title(
    text: String,
    hint: String? = null,
) {
    Text(text, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    hint?.let { Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
}

@Composable
private fun EggStep(
    state: OnboardingUiState,
    actions: OnboardingActions,
    animate: Boolean,
) {
    Title(stringResource(R.string.egg_title), stringResource(R.string.egg_hint))
    state.eggs.withIndex().chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            row.forEach { (i, seed) ->
                EggTile(
                    seed = seed,
                    description = stringResource(R.string.egg_description, i + 1, state.eggs.size),
                    chosen = seed == state.chosenEgg,
                    phase = i,
                    animate = animate,
                    onClick = { actions.onChooseEgg(seed) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    OutlinedButton(onClick = actions.onMoreEggs) { Text(stringResource(R.string.egg_more), style = MaterialTheme.typography.titleMedium) }
    NextButton(stringResource(R.string.egg_next), state.canContinue, actions.onNext)
}

@Composable
private fun EggTile(
    seed: Long,
    description: String,
    chosen: Boolean,
    phase: Int,
    animate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (chosen) colors.secondaryContainer else colors.surfaceVariant,
        border = if (chosen) BorderStroke(4.dp, colors.primary) else null,
        modifier =
            modifier.aspectRatio(1f).semantics {
                role = Role.RadioButton
                selected = chosen
            },
    ) {
        val wobble = if (animate) idleWobble(phase, excited = chosen) else 0f
        Egg(Genome.fromSeed(seed), description, Modifier.fillMaxSize().padding(12.dp), wobble = wobble)
    }
}

@Composable
private fun NameStep(
    state: OnboardingUiState,
    actions: OnboardingActions,
) {
    state.chosenEgg?.let { Egg(Genome.fromSeed(it), stringResource(R.string.egg_chosen), Modifier.size(120.dp)) }
    Title(stringResource(R.string.name_title))
    state.names.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { name ->
                val picked = name == state.name
                val colors = MaterialTheme.colorScheme
                Surface(
                    onClick = { actions.onPickName(name) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (picked) colors.primary else colors.surfaceVariant,
                    modifier =
                        Modifier.weight(1f).height(56.dp).semantics {
                            role = Role.RadioButton
                            selected = picked
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (picked) colors.onPrimary else colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = actions.onMoreNames) { Text(stringResource(R.string.name_more), style = MaterialTheme.typography.titleMedium) }
    NextButton(stringResource(R.string.name_next), state.canContinue, actions.onNext)
}

@Composable
private fun SleepStep(
    state: OnboardingUiState,
    actions: OnboardingActions,
) {
    Title(stringResource(R.string.sleep_title), stringResource(R.string.sleep_hint, state.name.orEmpty()))
    TimeStepper(stringResource(R.string.sleep_bedtime), state.bedtimeMinute) { actions.onSleepWindow(it, state.wakeMinute) }
    TimeStepper(stringResource(R.string.sleep_wake_up), state.wakeMinute) { actions.onSleepWindow(state.bedtimeMinute, it) }
    NextButton(stringResource(R.string.sleep_next), state.canContinue, actions.onNext)
}

@Composable
internal fun NextButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(64.dp)) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}
