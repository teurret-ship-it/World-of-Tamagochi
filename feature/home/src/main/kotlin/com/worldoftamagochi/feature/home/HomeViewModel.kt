package com.worldoftamagochi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldoftamagochi.sim.CareAction
import com.worldoftamagochi.sim.CareResult
import com.worldoftamagochi.sim.CareRules
import com.worldoftamagochi.sim.ExpressionRules
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.NeedsSimulation
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.PlayerProgress
import com.worldoftamagochi.sim.ProgressRules
import com.worldoftamagochi.sim.SleepWindow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** The game rules the home screen runs on; tests swap them. */
data class HomeRules(
    val care: CareRules = CareRules.DEFAULT,
    val progression: ProgressRules = ProgressRules.DEFAULT,
    val expressions: ExpressionRules = ExpressionRules.DEFAULT,
)

/**
 * Drives the home screen: keeps the pet's needs moving in real time, applies
 * care through the shared rules and pays the player for it. The pet lives in
 * memory until persistence arrives (roadmap iteration 4).
 */
class HomeViewModel(
    private val clock: () -> Long = System::currentTimeMillis,
    seed: Long = clock(),
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val name: String = DEFAULT_NAME,
    private val rules: HomeRules = HomeRules(),
) : ViewModel() {
    private val care = rules.care
    private val progression = rules.progression
    private val expressions = rules.expressions
    private val genome = Genome.fromSeed(seed)
    private var pet =
        PetState(
            stage = LifeStage.BABY,
            needs = Needs(),
            sleep = SleepWindow(zone = zone),
            updatedAtEpochMillis = clock(),
        )
    private var progress = PlayerProgress()
    private var washing = false
    private var strokes = 0

    private val _state = MutableStateFlow(render())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    /** Moves the pet to the current time. Called by the ticker; public for tests. */
    fun tick() {
        pet = NeedsSimulation.advance(pet, clock())
        if (pet.isAsleep()) washing = false
        publish()
    }

    /** The player rubbed the pet: a stroke, or a scrub while holding the soap. */
    fun onStroke() {
        if (washing) {
            perform(CareAction.WASH)
            if (pet.needs.gauge(Need.HYGIENE).isFull) washing = false
            publish()
        } else {
            perform(CareAction.STROKE)
        }
    }

    fun onFeed() = perform(CareAction.FEED)

    fun onPlay() = perform(CareAction.PLAY)

    /** Picks up or puts down the soap. */
    fun onToggleSoap() {
        if (!washing && pet.needs.gauge(Need.HYGIENE).isFull) {
            perform(CareAction.WASH) // refused: "already clean"
            return
        }
        washing = !washing && !pet.isAsleep()
        publish()
    }

    /** Lights off starts a nap; during a nap, lights on wakes the pet. */
    fun onLights() = perform(if (isNapping()) CareAction.WAKE else CareAction.NAP)

    private fun perform(action: CareAction) {
        pet = NeedsSimulation.advance(pet, clock())
        when (val result = care.apply(pet, action)) {
            is CareResult.Refused -> {
                _effects.trySend(HomeEffect.Refused(action, result.reason))
            }

            is CareResult.Done -> {
                pet = result.pet
                val day =
                    Instant
                        .ofEpochMilli(clock())
                        .atZone(zone)
                        .toLocalDate()
                        .toEpochDay()
                val update = progression.reward(progress, result, action, day)
                progress = update.progress
                if (action == CareAction.STROKE) strokes++
                if (action == CareAction.NAP) washing = false
                _effects.trySend(HomeEffect.Cared(action, result.changes, update.earned, update.levelUp))
            }
        }
        publish()
    }

    private fun isNapping(): Boolean = pet.napUntilEpochMillis?.let { it > pet.updatedAtEpochMillis } == true

    private fun publish() {
        _state.value = render()
    }

    private fun render(): HomeUiState {
        val asleep = pet.isAsleep()
        val level = progress.level
        val start = progression.xpAtLevel(level)
        val span = progression.xpAtLevel(level + 1) - start
        return HomeUiState(
            name = name,
            genome = genome,
            expression = expressions.expressionOf(pet.needs, asleep),
            needs = Need.entries.associateWith { pet.needs.gauge(it).value },
            urgentNeed = if (asleep) null else expressions.mostUrgentNeed(pet.needs),
            level = level,
            levelProgress = ((progress.xp - start).toFloat() / span).coerceIn(0f, 1f),
            coins = progress.coins,
            napping = isNapping(),
            washing = washing,
            strokes = strokes,
        )
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val DEFAULT_NAME = "Mochi"
    }
}
