package com.worldoftamagochi.feature.race

import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Reward
import com.worldoftamagochi.sim.race.Medal
import com.worldoftamagochi.sim.race.MedalTimes
import com.worldoftamagochi.sim.race.Runner
import com.worldoftamagochi.sim.race.Track

enum class RacePhase { COUNTDOWN, RUNNING, FINISHED }

/** Who the ghost is: a real player just ahead of you online, your own best, or the coach. */
enum class GhostKind { RIVAL, PERSONAL_BEST, COACH }

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
    /** The rival's display name when racing a real player's ghost. */
    val ghostName: String? = null,
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
    /** Online result: today's rank once verified, or queued when offline; null in offline builds. */
    val online: OnlineOutcome? = null,
)

sealed interface OnlineOutcome {
    data object Sending : OnlineOutcome

    data class Ranked(
        val dailyRank: Int,
    ) : OnlineOutcome

    data object Queued : OnlineOutcome
}

/** One-off moments for sound and haptics. */
enum class RaceEvent { COUNTDOWN, GO, JUMP, HURDLE_HIT, BOOST, FINISH, MEDAL, LEVEL_UP }
