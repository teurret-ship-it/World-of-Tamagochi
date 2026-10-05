package com.worldoftamagochi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.data.GameSave
import com.worldoftamagochi.data.Settings
import com.worldoftamagochi.data.toProgress
import com.worldoftamagochi.data.toSave
import com.worldoftamagochi.data.toState
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
 * care through the shared rules, pays the player for it and saves the game
 * after every action and every 15 seconds.
 */
class HomeViewModel(
    private val repository: GameRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val newPetSeed: () -> Long = clock,
    private val rules: HomeRules = HomeRules(),
) : ViewModel() {
    private val care = rules.care
    private val progression = rules.progression
    private val expressions = rules.expressions
    private var seed = 0L
    private var name = DEFAULT_NAME
    private var genome: Genome? = null
    private lateinit var pet: PetState
    private var progress = PlayerProgress()
    private var washing = false
    private var strokes = 0
    private var away: AwaySummary? = null
    private var settings = Settings()
    private var ticksSinceSave = 0

    /** Null until the saved game is loaded (a few milliseconds after start). */
    private val _state = MutableStateFlow<HomeUiState?>(null)
    val state: StateFlow<HomeUiState?> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            load()
            launch {
                repository.settings.collect {
                    settings = it
                    publish()
                }
            }
            // Races pay coins and XP too: keep the shown progress in step with the save.
            launch {
                repository.game.collect { game ->
                    game?.let {
                        progress = it.progress.toProgress()
                        publish()
                    }
                }
            }
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    /** Loads the saved pet (or hatches a new one) and catches it up to now. */
    internal suspend fun load() {
        val saved = repository.loadGame()
        if (saved == null) {
            seed = newPetSeed()
            pet = PetState(LifeStage.BABY, Needs(), SleepWindow(zone = zone), clock())
        } else {
            seed = saved.pet.seed
            name = saved.pet.name
            // The pet sleeps in the player's current time zone (travel, DST).
            pet = saved.pet.toState().let { it.copy(sleep = it.sleep.copy(zone = zone)) }
            progress = saved.progress.toProgress()
        }
        genome = Genome.fromSeed(seed)
        val before = pet
        pet = NeedsSimulation.advance(pet, clock())
        away = awaySummary(before, pet)
        save()
        publish()
    }

    /** Moves the pet to the current time. Called by the ticker; public for tests. */
    fun tick() {
        if (genome == null) return
        pet = NeedsSimulation.advance(pet, clock())
        if (pet.isAsleep()) washing = false
        if (++ticksSinceSave >= SAVE_EVERY_TICKS) saveAsync()
        publish()
    }

    fun onDismissAway() {
        away = null
        publish()
    }

    fun onSoundToggled(enabled: Boolean) = viewModelScope.launch { repository.updateSettings { it.copy(sound = enabled) } }

    fun onHapticsToggled(enabled: Boolean) = viewModelScope.launch { repository.updateSettings { it.copy(haptics = enabled) } }

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
        if (genome == null) return
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
                viewModelScope.launch { repository.updateGame { it.copy(progress = update.progress.toSave()) } }
                if (action == CareAction.STROKE) strokes++
                if (action == CareAction.NAP) washing = false
                _effects.trySend(HomeEffect.Cared(action, result.changes, update.earned, update.levelUp))
                saveAsync()
            }
        }
        publish()
    }

    private fun awaySummary(
        before: PetState,
        after: PetState,
    ): AwaySummary? {
        val minutes = (after.updatedAtEpochMillis - before.updatedAtEpochMillis) / MILLIS_PER_MINUTE
        if (minutes < AWAY_MINUTES) return null
        val changes =
            Need.entries
                .associateWith { after.needs.gauge(it).value - before.needs.gauge(it).value }
                .filterValues { it != 0 }
        return AwaySummary(minutes, changes)
    }

    private fun saveAsync() {
        ticksSinceSave = 0
        viewModelScope.launch { save() }
    }

    /** Saves the pet only; progress is written where it changes, so races are never overwritten. */
    private suspend fun save() {
        val petSave = pet.toSave(seed, name)
        if (repository.loadGame() == null) {
            repository.saveGame(GameSave(pet = petSave, progress = progress.toSave()))
        } else {
            repository.updateGame { it.copy(pet = petSave) }
        }
    }

    private fun isNapping(): Boolean = pet.napUntilEpochMillis?.let { it > pet.updatedAtEpochMillis } == true

    private fun publish() {
        val genome = genome ?: return
        _state.value = render(genome)
    }

    private fun render(genome: Genome): HomeUiState {
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
            away = away,
            sound = settings.sound,
            haptics = settings.haptics,
        )
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val SAVE_EVERY_TICKS = 15
        const val AWAY_MINUTES = 30L
        const val MILLIS_PER_MINUTE = 60_000L
        const val DEFAULT_NAME = "Mochi"
    }
}
