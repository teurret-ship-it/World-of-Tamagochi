package com.mymagicalpet.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.data.GameSave
import com.mymagicalpet.data.StatsSave
import com.mymagicalpet.data.TrainingSave
import com.mymagicalpet.data.toSave
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Needs
import com.mymagicalpet.sim.PetNames
import com.mymagicalpet.sim.PetState
import com.mymagicalpet.sim.SleepWindow
import com.mymagicalpet.sim.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.random.Random

/** The first-launch story: pick an egg, name the pet inside, say when you sleep, hatch it. */
enum class OnboardingStep { CHOOSE_EGG, NAME, SLEEP, HATCH }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CHOOSE_EGG,
    /** Genome seeds of the eggs on offer. */
    val eggs: List<Long>,
    val chosenEgg: Long? = null,
    val names: List<String>,
    val name: String? = null,
    val bedtimeMinute: Int = SleepWindow.DEFAULT_BEDTIME,
    val wakeMinute: Int = SleepWindow.DEFAULT_WAKE_UP,
    /** Taps on the egg so far; it hatches at [OnboardingViewModel.TAPS_TO_HATCH]. */
    val taps: Int = 0,
    val hatched: Boolean = false,
) {
    val canContinue: Boolean
        get() =
            when (step) {
                OnboardingStep.CHOOSE_EGG -> chosenEgg != null
                OnboardingStep.NAME -> name != null
                OnboardingStep.SLEEP -> true
                OnboardingStep.HATCH -> hatched
            }
}

class OnboardingViewModel(
    private val repository: GameRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val random: Random = Random.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState(eggs = newEggs(), names = newNames()))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onChooseEgg(seed: Long) = _state.update { it.copy(chosenEgg = seed.takeIf { s -> s in it.eggs }) }

    /** Other eggs, please. */
    fun onMoreEggs() = _state.update { it.copy(eggs = newEggs(), chosenEgg = null) }

    fun onPickName(name: String) = _state.update { it.copy(name = name.takeIf(PetNames::isAllowed)) }

    fun onMoreNames() = _state.update { it.copy(names = newNames(), name = null) }

    fun onSleepWindow(
        bedtimeMinute: Int,
        wakeMinute: Int,
    ) = _state.update { it.copy(bedtimeMinute = bedtimeMinute, wakeMinute = wakeMinute) }

    fun onNext() =
        _state.update {
            if (!it.canContinue) return@update it
            val next = OnboardingStep.entries.getOrNull(it.step.ordinal + 1) ?: return@update it
            it.copy(step = next)
        }

    fun onBack() =
        _state.update {
            val previous = OnboardingStep.entries.getOrNull(it.step.ordinal - 1)
            if (previous == null || (it.step == OnboardingStep.HATCH && it.taps > 0)) it else it.copy(step = previous)
        }

    /** A tap on the egg: it wobbles and cracks, and the last tap hatches it (and saves the new pet). */
    fun onTapEgg() {
        val now = _state.value
        if (now.step != OnboardingStep.HATCH || now.hatched) return
        val taps = now.taps + 1
        _state.value = now.copy(taps = taps, hatched = taps >= TAPS_TO_HATCH)
        if (taps >= TAPS_TO_HATCH) save(_state.value)
    }

    private fun save(state: OnboardingUiState) {
        val seed = requireNotNull(state.chosenEgg)
        val name = requireNotNull(state.name)
        val pet = PetState(LifeStage.BABY, Needs(), SleepWindow(state.bedtimeMinute, state.wakeMinute, zone), clock())
        viewModelScope.launch {
            // Normally there is no game yet; if there is, only the pet (and its stats) is new.
            if (repository.loadGame() == null) {
                repository.saveGame(GameSave(pet = pet.toSave(seed, name)))
            } else {
                repository.updateGame { it.copy(pet = pet.toSave(seed, name), stats = StatsSave(), training = TrainingSave()) }
            }
        }
    }

    /** One egg of every species, in fresh colours each time. */
    private fun newEggs(): List<Long> = Species.entries.map { Species.seedFor(it, random.nextLong()) }

    private fun newNames(): List<String> = PetNames.ALL.shuffled(random).take(NAMES)

    companion object {
        const val TAPS_TO_HATCH = 5
        private const val NAMES = 8
    }
}
