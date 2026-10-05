package com.worldoftamagochi.server

import com.worldoftamagochi.sim.PetNames
import com.worldoftamagochi.sim.SimVersion
import com.worldoftamagochi.sim.race.InputLog
import com.worldoftamagochi.sim.race.RaceStats
import com.worldoftamagochi.sim.race.Replay
import com.worldoftamagochi.sim.race.Tracks
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class Period { DAILY, WEEKLY, ALL_TIME }

/** Why a run was not accepted. */
class RejectedRun(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

private fun reject(
    whenTrue: Boolean,
    message: () -> String,
) {
    if (whenTrue) throw RejectedRun(message())
}

data class NewPlayer(
    val playerId: String,
    val token: String,
    val displayName: String,
)

data class AcceptedRun(
    val finishMicros: Long,
    val personalBestMicros: Long,
    val dailyRank: Int,
)

data class Ranked(
    val rank: Int,
    val run: StoredRun,
)

/**
 * The rules of online racing. A client sends only its input log; the server
 * replays it with the shared physics and decides the time (CLAUDE.md
 * section 2). Leaderboards keep each player's best per period.
 */
class RaceService(
    private val store: GameStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
) {
    fun register(petName: String): NewPlayer {
        val name = petName.takeIf(PetNames::isAllowed) ?: PetNames.DEFAULT
        val token = ByteArray(TOKEN_BYTES).also(random::nextBytes).toHex()
        val player = NewPlayer(UUID.randomUUID().toString(), token, "$name #${random.nextInt(TAG_RANGE) + TAG_MIN}")
        store.createPlayer(player.playerId, hash(token), player.displayName, clock())
        return player
    }

    fun authenticate(token: String): Player? = store.playerByTokenHash(hash(token))

    fun submit(
        player: Player,
        trackId: String,
        simVersion: Int,
        codes: List<Int>,
    ): AcceptedRun {
        reject(simVersion != SimVersion.CURRENT) { "Rules version $simVersion is not ${SimVersion.CURRENT}" }
        val track = Tracks.byId(trackId)
        reject(track == null) { "Unknown track" }
        reject(codes.size > MAX_EVENTS) { "Log too long" }
        reject(codes.any { it < 0 || it % CODE_BASE >= InputLog.Kind.entries.size }) { "Unknown input" }
        val log = parseLog(codes)
        reject(log.events.any { it.tick < 0 || it.tick > Replay.MAX_TICKS }) { "Events outside the race" }
        // Stats will come from the server's copy of the pet (iteration 22); until then everyone is a rookie.
        val finish = Replay.run(requireNotNull(track), RaceStats.ROOKIE, log).finishMicros
        reject(finish == null) { "The run does not finish" }
        requireNotNull(finish)
        store.insertRun(NewRun(player.id, trackId, finish, codes, simVersion, clock()))
        val allTime = store.bestRuns(trackId, 0, SimVersion.CURRENT)
        val daily = store.bestRuns(trackId, periodStart(Period.DAILY), SimVersion.CURRENT)
        return AcceptedRun(
            finishMicros = finish,
            personalBestMicros = allTime.first { it.playerId == player.id }.finishMicros,
            dailyRank = daily.indexOfFirst { it.playerId == player.id } + 1,
        )
    }

    private fun parseLog(codes: List<Int>): InputLog =
        try {
            InputLog.fromCodes(codes)
        } catch (e: IllegalArgumentException) {
            throw RejectedRun("Malformed log", e)
        }

    fun leaderboard(
        trackId: String,
        period: Period,
        limit: Int,
    ): List<Ranked> =
        store
            .bestRuns(trackId, periodStart(period), SimVersion.CURRENT)
            .take(limit.coerceIn(1, MAX_LEADERBOARD))
            .mapIndexed { i, run -> Ranked(i + 1, run) }

    /**
     * Ghosts worth racing: the players just ahead of you this week (the next
     * people to beat), or the top of the board when you have no time yet or
     * are already first.
     */
    fun ghosts(
        player: Player,
        trackId: String,
    ): List<StoredRun> {
        val board = store.bestRuns(trackId, periodStart(Period.WEEKLY), SimVersion.CURRENT).filter { it.playerId != player.id }
        val mine = store.bestRuns(trackId, 0, SimVersion.CURRENT).firstOrNull { it.playerId == player.id }?.finishMicros
        val ahead = if (mine == null) emptyList() else board.filter { it.finishMicros < mine }
        // The fastest player (or a newcomer) races the top of the board instead.
        return if (ahead.isEmpty()) board.take(GHOSTS) else ahead.takeLast(GHOSTS)
    }

    /** Daily boards start at 00:00 UTC, weekly boards on Monday 00:00 UTC. */
    fun periodStart(period: Period): Long {
        val today = Instant.ofEpochMilli(clock()).atZone(ZoneOffset.UTC).toLocalDate()
        return when (period) {
            Period.DAILY -> today
            Period.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Period.ALL_TIME -> return 0
        }.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.encodeToByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val TOKEN_BYTES = 32
        const val TAG_MIN = 1_000
        const val TAG_RANGE = 9_000
        const val MAX_EVENTS = 20_000
        const val MAX_LEADERBOARD = 100
        const val GHOSTS = 3
        const val CODE_BASE = 4
    }
}
