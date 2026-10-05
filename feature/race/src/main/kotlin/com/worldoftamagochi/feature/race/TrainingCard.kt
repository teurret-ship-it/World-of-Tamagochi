package com.worldoftamagochi.feature.race

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.sim.training.Exercise
import com.worldoftamagochi.sim.training.TrainingRefusal
import com.worldoftamagochi.sim.training.of
import com.worldoftamagochi.ui.R as UiR

/** The training ground: four stats, one exercise each, and today's form. */
@Composable
internal fun TrainingCard(
    state: TrainingUiState,
    onTrain: (Exercise) -> Unit,
    message: TrainingEffect?,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.training_title, state.name), style = MaterialTheme.typography.headlineSmall)
            FormLine(state)
            Exercise.entries.forEach { exercise ->
                ExerciseRow(exercise, state.stats.of(exercise), enabled = state.sessionsLeft > 0, onTrain = { onTrain(exercise) })
            }
            val line =
                when (message) {
                    is TrainingEffect.Trained -> {
                        stringResource(
                            R.string.training_gain,
                            stringResource(message.exercise.statRes()),
                            message.gained,
                        )
                    }

                    is TrainingEffect.Refused -> {
                        stringResource(message.reason.messageRes())
                    }

                    null -> {
                        pluralStringResource(R.plurals.training_left, state.sessionsLeft, state.sessionsLeft)
                    }
                }
            Text(
                text = line,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun FormLine(state: TrainingUiState) {
    val (text, color) =
        when (state.formHint) {
            FormHint.TOP -> stringResource(R.string.form_top) to MaterialTheme.colorScheme.tertiary
            FormHint.HUNGRY -> stringResource(R.string.form_hungry, state.name) to MaterialTheme.colorScheme.error
            FormHint.TIRED -> stringResource(R.string.form_tired, state.name) to MaterialTheme.colorScheme.error
        }
    Text(text, style = MaterialTheme.typography.titleMedium, color = color)
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    value: Int,
    enabled: Boolean,
    onTrain: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(exercise.imageRes()), contentDescription = null, modifier = Modifier.size(40.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.stat_value, stringResource(exercise.statRes()), value),
                style = MaterialTheme.typography.titleSmall,
            )
            LinearProgressIndicator(
                progress = { value / MAX_STAT },
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
        }
        FilledTonalButton(onClick = onTrain, enabled = enabled, modifier = Modifier.width(132.dp)) {
            Text(stringResource(exercise.nameRes()), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@DrawableRes
private fun Exercise.imageRes(): Int =
    when (this) {
        Exercise.SPRINTS -> UiR.drawable.train_speed
        Exercise.JOGGING -> UiR.drawable.train_stamina
        Exercise.HOOPS -> UiR.drawable.race_tyre
        Exercise.JUMP_ROPE -> UiR.drawable.train_jump
    }

@StringRes
internal fun Exercise.statRes(): Int =
    when (this) {
        Exercise.SPRINTS -> R.string.stat_speed
        Exercise.JOGGING -> R.string.stat_stamina
        Exercise.HOOPS -> R.string.stat_agility
        Exercise.JUMP_ROPE -> R.string.stat_jump
    }

@StringRes
private fun Exercise.nameRes(): Int =
    when (this) {
        Exercise.SPRINTS -> R.string.exercise_sprints
        Exercise.JOGGING -> R.string.exercise_jogging
        Exercise.HOOPS -> R.string.exercise_hoops
        Exercise.JUMP_ROPE -> R.string.exercise_jump_rope
    }

@StringRes
internal fun TrainingRefusal.messageRes(): Int =
    when (this) {
        TrainingRefusal.EGG -> R.string.training_refused_egg
        TrainingRefusal.ASLEEP -> R.string.training_refused_asleep
        TrainingRefusal.TOO_TIRED -> R.string.training_refused_tired
        TrainingRefusal.TOO_HUNGRY -> R.string.training_refused_hungry
        TrainingRefusal.DONE_FOR_TODAY -> R.string.training_refused_done
    }

private const val MAX_STAT = 100f
