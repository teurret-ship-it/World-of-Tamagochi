package com.mymagicalpet.feature.race

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.data.RecordSave
import com.mymagicalpet.data.toProgress
import com.mymagicalpet.data.toSave
import com.mymagicalpet.data.toState
import com.mymagicalpet.data.toStats
import com.mymagicalpet.data.toWardrobe
import com.mymagicalpet.network.OnlineRacing
import com.mymagicalpet.network.Upload
import com.mymagicalpet.network.toStats
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.NeedsSimulation
import com.mymagicalpet.sim.ProgressUpdate
import com.mymagicalpet.sim.Reward
import com.mymagicalpet.sim.race.Autopilot
import com.mymagicalpet.sim.race.Discipline
import com.mymagicalpet.sim.race.InputLog
import com.mymagicalpet.sim.race.Medal
import com.mymagicalpet.sim.race.MedalTimes
import com.mymagicalpet.sim.race.Race
import com.mymagicalpet.sim.race.RaceForm
import com.mymagicalpet.sim.race.RacePhysics
import com.mymagicalpet.sim.race.RaceRewards
import com.mymagicalpet.sim.race.RaceStats
import com.mymagicalpet.sim.race.Replay
import com.mymagicalpet.sim.race.Runner
import com.mymagicalpet.sim.race.TickInput
import com.mymagicalpet.sim.race.Track
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId

/**
 * Runs one race at a fixed 60 Hz, whatever the display refresh rate: the UI
 * reports frame times, the view model steps the shared physics as many ticks
 * as have elapsed. Records the input log, races a ghost, and pays medals.
 */
class RaceViewModel(
    private val repository: GameRepository,
    private val track: Track,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val rewards: RaceRewards = RaceRewards.DEFAULT,
    private val online: OnlineRacing? = null,
) : ViewModel() {
    /** Trained stats after race-day form; a rookie's until the save is loaded. */
    private var stats = RaceStats.ROOKIE
    private val medals = MedalTimes.of(track)
    private var staminaMax = Race(track, stats).runner.stamina

    private var race = Race(track, stats)
    private var recorder = InputLog.Recorder()
    private var ghostFrames: List<Runner> = emptyList()
    private var ghostKind = GhostKind.COACH
    private var ghostName: String? = null
    private var bestMicros: Long? = null
    private var bestMedal: Medal? = null
    private var genome = Genome.fromSeed(0)
    private var wearing: List<String> = emptyList()
    private var name = ""

    private var sprintHeld = false
    private var jumpQueued = false
    private var startNanos: Long? = null
    private var phase = RacePhase.COUNTDOWN
    private var summary: RaceSummary? = null

    private val _state = MutableStateFlow<RaceUiState?>(null)
    val state: StateFlow<RaceUiState?> = _state.asStateFlow()

    private val _events = Channel<RaceEvent>(Channel.BUFFERED)
    val events: Flow<RaceEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val game = repository.loadGame()
            game?.let {
                genome = Genome.fromSeed(it.pet.seed)
                name = it.pet.name
                val pet = NeedsSimulation.advance(it.pet.toState(), clock())
                stats = RaceForm.effective(it.stats.toStats(), RaceForm.perMille(pet.needs))
                race = Race(track, stats)
                staminaMax = race.runner.stamina
                wearing =
                    it.wardrobe
                        .toWardrobe()
                        .equipped.values
                        .toList()
            }
            val record = game?.records?.get(track.id)
            bestMicros = record?.finishMicros
            bestMedal = medals.medalFor(record?.finishMicros)
            ghostKind = if (record == null) GhostKind.COACH else GhostKind.PERSONAL_BEST
            // Every ghost replays with the stats it was raced with; the coach is a rookie.
            ghostFrames =
                if (record != null) {
                    Replay.frames(track, record.stats.toStats(), InputLog.fromCodes(record.log))
                } else {
                    Replay.frames(track, RaceStats.ROOKIE, Autopilot(track).play(RaceStats.ROOKIE).second)
                }
            publish(countdown = COUNTDOWN_SECONDS)
            // Online: race the player just ahead of you instead, if the server answers in time.
            online?.let { racing ->
                val rival = withTimeoutOrNull(RIVAL_TIMEOUT_MILLIS) { racing.rivalGhost(track.id) }
                if (rival != null && phase == RacePhase.COUNTDOWN) {
                    ghostFrames = Replay.frames(track, rival.stats.toStats(), InputLog.fromCodes(rival.log))
                    ghostKind = GhostKind.RIVAL
                    ghostName = rival.displayName
                    publish(countdown = _state.value?.countdown)
                }
            }
        }
    }

    fun onSprint(held: Boolean) {
        sprintHeld = held
    }

    fun onJump() {
        jumpQueued = true
    }

    /** Called once per display frame with its timestamp. */
    fun onFrame(frameNanos: Long) {
        if (_state.value == null || phase == RacePhase.FINISHED) return
        val start = startNanos ?: frameNanos.also { startNanos = it }
        val sinceStart = frameNanos - start - COUNTDOWN_SECONDS * NANOS_PER_SECOND
        if (sinceStart < 0) {
            val countdown = (-sinceStart / NANOS_PER_SECOND + 1).toInt()
            if (countdown != _state.value?.countdown) _events.trySend(RaceEvent.COUNTDOWN)
            publish(countdown)
            return
        }
        if (phase == RacePhase.COUNTDOWN) {
            phase = RacePhase.RUNNING
            _events.trySend(RaceEvent.GO)
        }
        val dueTicks = (sinceStart / NANOS_PER_TICK).toInt()
        while (race.runner.tick < dueTicks && !race.finished) step()
        if (race.finished) finish() else publish(countdown = if (sinceStart < GO_SHOWN_NANOS) 0 else null)
    }

    /** Starts over on the same track: new log, same ghost. */
    fun onRetry() {
        race = Race(track, stats)
        recorder = InputLog.Recorder()
        startNanos = null
        phase = RacePhase.COUNTDOWN
        summary = null
        sprintHeld = false
        jumpQueued = false
        publish(countdown = COUNTDOWN_SECONDS)
    }

    private fun step() {
        val input = TickInput(sprint = sprintHeld, jump = jumpQueued)
        jumpQueued = false
        recorder.record(input)
        val before = race.runner
        val after = race.step(input)
        when {
            after.faults > before.faults -> _events.trySend(RaceEvent.FAULT)
            after.hurdlesHit > before.hurdlesHit -> _events.trySend(RaceEvent.HURDLE_HIT)
            after.vx - before.vx > RacePhysics.DEFAULT.acceleration -> _events.trySend(RaceEvent.BOOST)
            input.jump && after.yMm > 0 && before.grounded -> _events.trySend(RaceEvent.JUMP)
        }
    }

    private fun finish() {
        phase = RacePhase.FINISHED
        val finishMicros = requireNotNull(race.finishMicros)
        val medal = medals.medalFor(finishMicros)
        val previousBest = bestMicros
        val newRecord = previousBest == null || finishMicros < previousBest
        val log = recorder.build()
        val day =
            Instant
                .ofEpochMilli(clock())
                .atZone(zone)
                .toLocalDate()
                .toEpochDay()
        viewModelScope.launch {
            var update: ProgressUpdate? = null
            repository.updateGame { game ->
                val best = game.records[track.id]
                val reward = rewards.reward(game.progress.toProgress(), true, medal, medals.medalFor(best?.finishMicros), day)
                update = reward
                val records =
                    if (best == null || finishMicros < best.finishMicros) {
                        game.records + (track.id to RecordSave(finishMicros, log.toCodes(), stats.toSave()))
                    } else {
                        game.records
                    }
                game.copy(progress = reward.progress.toSave(), records = records)
            }
            val paid = update
            summary =
                RaceSummary(
                    finishMicros = finishMicros,
                    medal = medal,
                    newRecord = newRecord,
                    previousBestMicros = previousBest,
                    reward = paid?.earned ?: Reward.NONE,
                    levelUp = paid?.levelUp,
                    nextMedal = nextMedal(medal),
                    faults = race.runner.faults.takeIf { track.discipline == Discipline.AGILITY },
                )
            if (newRecord) {
                bestMicros = finishMicros
                ghostFrames = Replay.frames(track, stats, log)
                ghostKind = GhostKind.PERSONAL_BEST
            }
            if (medal != null && bestMedal.let { it == null || medal > it }) bestMedal = medal
            _events.trySend(RaceEvent.FINISH)
            if (medal != null) _events.trySend(RaceEvent.MEDAL)
            if (paid?.levelUp != null) _events.trySend(RaceEvent.LEVEL_UP)
            publish(countdown = null)
            online?.let { racing -> uploadRun(racing, log) }
        }
        publish(countdown = null)
    }

    private suspend fun uploadRun(
        racing: OnlineRacing,
        log: InputLog,
    ) {
        summary = summary?.copy(online = OnlineOutcome.Sending)
        publish(countdown = null)
        val outcome =
            when (val upload = racing.submit(track.id, log, stats)) {
                is Upload.Verified -> OnlineOutcome.Ranked(upload.response.dailyRank)
                Upload.Queued -> OnlineOutcome.Queued
                is Upload.Refused -> null
            }
        summary = summary?.copy(online = outcome)
        publish(countdown = null)
    }

    private fun nextMedal(medal: Medal?): Pair<Medal, Long>? {
        val next = Medal.entries.firstOrNull { medal == null || it > medal } ?: return null
        val time =
            when (next) {
                Medal.BRONZE -> medals.bronze
                Medal.SILVER -> medals.silver
                Medal.GOLD -> medals.gold
                Medal.AUTHOR -> medals.authorMicros
            }
        return next to time
    }

    private fun publish(countdown: Int?) {
        val runner = race.runner
        _state.value =
            RaceUiState(
                track = track,
                genome = genome,
                name = name,
                wearing = wearing,
                phase = phase,
                countdown = countdown,
                runner = runner,
                ghost = ghostFrames.getOrNull(runner.tick) ?: ghostFrames.lastOrNull(),
                ghostKind = ghostKind,
                ghostName = ghostName,
                elapsedMicros = race.finishMicros ?: (runner.tick * Race.MICROS_PER_TICK),
                bestMicros = bestMicros,
                medals = medals,
                staminaMax = staminaMax,
                result = summary,
            )
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val NANOS_PER_TICK = NANOS_PER_SECOND / RacePhysics.TICKS_PER_SECOND
        const val GO_SHOWN_NANOS = 700_000_000L
        const val RIVAL_TIMEOUT_MILLIS = 2_500L
    }
}
