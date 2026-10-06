package com.mymagicalpet.feature.race

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.data.GameSave
import com.mymagicalpet.data.record
import com.mymagicalpet.data.toLog
import com.mymagicalpet.data.toSave
import com.mymagicalpet.data.toState
import com.mymagicalpet.data.toStats
import com.mymagicalpet.sim.GrowthRules
import com.mymagicalpet.sim.GrowthSource
import com.mymagicalpet.sim.Need
import com.mymagicalpet.sim.NeedsSimulation
import com.mymagicalpet.sim.grown
import com.mymagicalpet.sim.journal.Deed
import com.mymagicalpet.sim.race.RaceForm
import com.mymagicalpet.sim.race.RaceStats
import com.mymagicalpet.sim.training.Exercise
import com.mymagicalpet.sim.training.TrainingRefusal
import com.mymagicalpet.sim.training.TrainingResult
import com.mymagicalpet.sim.training.TrainingRules
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Why the pet is not in top form, so the screen can say what helps. */
enum class FormHint { TOP, HUNGRY, TIRED }

data class TrainingUiState(
    val name: String,
    val stats: RaceStats,
    val sessionsLeft: Int,
    val formPerMille: Int,
    val formHint: FormHint,
)

sealed interface TrainingEffect {
    data class Trained(
        val exercise: Exercise,
        val gained: Int,
    ) : TrainingEffect

    data class Refused(
        val reason: TrainingRefusal,
    ) : TrainingEffect
}

/** The training ground on the races screen: one tap, one session. Rules live in [TrainingRules]. */
class TrainingViewModel(
    private val repository: GameRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val rules: TrainingRules = TrainingRules.DEFAULT,
    private val growth: GrowthRules = GrowthRules.DEFAULT,
) : ViewModel() {
    val state: StateFlow<TrainingUiState?> =
        repository.game
            .map { game -> game?.let(::stateOf) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _effects = Channel<TrainingEffect>(Channel.BUFFERED)
    val effects: Flow<TrainingEffect> = _effects.receiveAsFlow()

    fun onTrain(exercise: Exercise) {
        viewModelScope.launch {
            var outcome: TrainingResult? = null
            repository.updateGame { game ->
                val pet = NeedsSimulation.advance(game.pet.toState(), clock())
                val result = rules.train(pet, game.stats.toStats(), game.training.toLog(), exercise, today())
                outcome = result
                if (result is TrainingResult.Done) {
                    val grown = result.pet.grown(growth.grow(result.pet, GrowthSource.TRAINING, today()))
                    game
                        .copy(
                            pet = grown.toSave(game.pet.seed, game.pet.name),
                            stats = result.stats.toSave(),
                            training = result.log.toSave(),
                        ).record(listOf(Deed.TRAINING), today())
                        .first
                } else {
                    game
                }
            }
            when (val result = outcome) {
                is TrainingResult.Done -> _effects.send(TrainingEffect.Trained(exercise, result.gained))
                is TrainingResult.Refused -> _effects.send(TrainingEffect.Refused(result.reason))
                null -> Unit
            }
        }
    }

    private fun stateOf(game: GameSave): TrainingUiState {
        val needs = NeedsSimulation.advance(game.pet.toState(), clock()).needs
        val form = RaceForm.perMille(needs)
        val hint =
            when {
                form >= TOP_FORM -> FormHint.TOP
                needs.gauge(Need.SATIETY).value <= needs.gauge(Need.ENERGY).value -> FormHint.HUNGRY
                else -> FormHint.TIRED
            }
        return TrainingUiState(
            name = game.pet.name,
            stats = game.stats.toStats(),
            sessionsLeft = rules.sessionsLeft(game.training.toLog(), today()),
            formPerMille = form,
            formHint = hint,
        )
    }

    private fun today(): Long =
        Instant
            .ofEpochMilli(clock())
            .atZone(zone)
            .toLocalDate()
            .toEpochDay()

    private companion object {
        const val TOP_FORM = 1_000
    }
}
