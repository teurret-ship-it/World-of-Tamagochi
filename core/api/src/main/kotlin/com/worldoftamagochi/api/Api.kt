package com.worldoftamagochi.api

import kotlinx.serialization.Serializable

// Wire format of the public API (v1). Field names are part of the contract.

@Serializable
data class RegisterRequest(
    val petName: String,
)

@Serializable
data class RegisterResponse(
    val playerId: String,
    val token: String,
    val displayName: String,
)

/** The race stats a run was raced with (after race-day form). */
@Serializable
data class StatsDto(
    val speed: Int = 0,
    val stamina: Int = 0,
    val agility: Int = 0,
    val jump: Int = 0,
)

@Serializable
data class RunRequest(
    val trackId: String,
    val simVersion: Int,
    val log: List<Int>,
    val stats: StatsDto = StatsDto(),
)

@Serializable
data class RunResponse(
    val finishMicros: Long,
    val personalBestMicros: Long,
    val dailyRank: Int,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val finishMicros: Long,
    val you: Boolean,
)

@Serializable
data class LeaderboardResponse(
    val trackId: String,
    val period: String,
    val entries: List<LeaderboardEntry>,
)

@Serializable
data class GhostRun(
    val displayName: String,
    val finishMicros: Long,
    val log: List<Int>,
    val stats: StatsDto = StatsDto(),
)

@Serializable
data class GhostsResponse(
    val trackId: String,
    val ghosts: List<GhostRun>,
)

@Serializable
data class ErrorResponse(
    val error: String,
)
