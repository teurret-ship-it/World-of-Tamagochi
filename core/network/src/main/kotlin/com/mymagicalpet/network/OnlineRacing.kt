package com.mymagicalpet.network

import com.mymagicalpet.api.GhostRun
import com.mymagicalpet.api.LeaderboardEntry
import com.mymagicalpet.api.RunRequest
import com.mymagicalpet.api.RunResponse
import com.mymagicalpet.api.StatsDto
import com.mymagicalpet.sim.SimVersion
import com.mymagicalpet.sim.race.InputLog
import com.mymagicalpet.sim.race.RaceStats

/** The anonymous online identity of this install. */
data class OnlineAccount(
    val token: String,
    val displayName: String,
)

/** A finished run waiting to be uploaded, with the stats it was raced with. */
data class PendingRun(
    val trackId: String,
    val log: List<Int>,
    val stats: RaceStats = RaceStats.ROOKIE,
)

/** Where the online state is saved on the device (implemented by the save file). */
interface OnlineStore {
    suspend fun account(): OnlineAccount?

    suspend fun saveAccount(account: OnlineAccount)

    suspend fun pendingRuns(): List<PendingRun>

    suspend fun savePendingRuns(runs: List<PendingRun>)
}

/** What happened to a run after the race. */
sealed interface Upload {
    /** The server verified it: its time and today's rank. */
    data class Verified(
        val response: RunResponse,
    ) : Upload

    /** No connection: kept and sent next time. */
    data object Queued : Upload

    /** The server refused it (it will not change its mind). */
    data class Refused(
        val reason: String,
    ) : Upload
}

/**
 * Online racing for the app, offline-first: every finished run is queued and
 * uploaded when the server can be reached; the anonymous account is created
 * on the first upload. Nothing here ever blocks playing.
 */
class OnlineRacing(
    private val api: RaceApi,
    private val store: OnlineStore,
    private val petName: suspend () -> String,
) {
    suspend fun submit(
        trackId: String,
        log: InputLog,
        stats: RaceStats = RaceStats.ROOKIE,
    ): Upload {
        store.savePendingRuns(store.pendingRuns() + PendingRun(trackId, log.toCodes(), stats))
        return flush().lastOrNull() ?: Upload.Queued
    }

    /** Uploads queued runs in order; stops at the first connection problem. */
    suspend fun flush(): List<Upload> {
        val results = mutableListOf<Upload>()
        var pending = store.pendingRuns()
        while (pending.isNotEmpty()) {
            val run = pending.first()
            val result =
                try {
                    val account = account()
                    Upload.Verified(api.submitRun(account.token, RunRequest(run.trackId, SimVersion.CURRENT, run.log, run.stats.toDto())))
                } catch (expected: ApiException.Unavailable) {
                    results += Upload.Queued
                    return results
                } catch (e: ApiException.Rejected) {
                    Upload.Refused(e.message.orEmpty())
                }
            results += result
            pending = pending.drop(1)
            store.savePendingRuns(pending)
        }
        return results
    }

    /** Today's or this week's board, or null when offline. */
    suspend fun leaderboard(
        trackId: String,
        period: String,
    ): List<LeaderboardEntry>? =
        try {
            api.leaderboard(trackId, period, store.account()?.token).entries
        } catch (expected: ApiException) {
            null // offline: the screen simply shows no board / no rival
        }

    /** The player just ahead of you this week, to race against; null when offline or none. */
    suspend fun rivalGhost(trackId: String): GhostRun? =
        try {
            api.ghosts(account().token, trackId).ghosts.lastOrNull()
        } catch (expected: ApiException) {
            null // offline: the screen simply shows no board / no rival
        }

    /** Creates the anonymous account early, so the server's training clock starts (ADR-010). */
    suspend fun warmUp() {
        try {
            account()
        } catch (expected: ApiException) {
            // offline: tried again next time
        }
    }

    private suspend fun account(): OnlineAccount =
        store.account() ?: api.register(petName()).let { registered ->
            OnlineAccount(registered.token, registered.displayName).also { store.saveAccount(it) }
        }
}

fun RaceStats.toDto(): StatsDto = StatsDto(speed, stamina, agility, jump)

/** Stats as sent by the server; anything out of range is treated as a rookie's. */
fun StatsDto.toStats(): RaceStats = runCatching { RaceStats(speed, stamina, agility, jump) }.getOrDefault(RaceStats.ROOKIE)
