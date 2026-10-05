package com.worldoftamagochi.feature.race

import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Reward
import com.worldoftamagochi.sim.race.Medal
import com.worldoftamagochi.sim.race.MedalTimes
import com.worldoftamagochi.sim.race.Runner
import com.worldoftamagochi.sim.race.Track

enum class RacePhase { COUNTDOWN, RUNNING, FINISHED }

/** Who the ghost is: the player's own best run, or the coach before there is one. */
enum class GhostKind { PERSONAL_BEST, COACH }

data class RaceUiState(
    val track: Track,
    val genome: Genome,
    val name: String,
    val phase: RacePhase,
    /** 3, 2, 1, then 0 = "Go!" for a moment; null once racing. */
    val countdown: Int?,
    val runner: Runner,
    val ghost: Runner?,
    val ghostKind: GhostKind,
    val elapsedMicros: Long,
    val bestMicros: Long?,
    val medals: MedalTimes,
    val staminaMax: Int,
    val result: RaceSummary? = null,
)

data class RaceSummary(
    val finishMicros: Long,
    val medal: Medal?,
    val newRecord: Boolean,
    val previousBestMicros: Long?,
    val reward: Reward,
    val levelUp: Int?,
    /** The next medal to chase and its time, or null when the author medal is won. */
    val nextMedal: Pair<Medal, Long>?,
)

/** One-off moments for sound and haptics. */
enum class RaceEvent { COUNTDOWN, GO, JUMP, HURDLE_HIT, BOOST, FINISH, MEDAL, LEVEL_UP }
