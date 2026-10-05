package com.worldoftamagochi.network

import com.worldoftamagochi.api.GhostRun
import com.worldoftamagochi.api.LeaderboardEntry
import com.worldoftamagochi.api.RunRequest
import com.worldoftamagochi.api.RunResponse
import com.worldoftamagochi.sim.SimVersion
import com.worldoftamagochi.sim.race.InputLog

/** The anonymous online identity of this install. */
data class OnlineAccount(
    val token: String,
    val displayName: String,
)

/** A finished run waiting to be uploaded. */
data class PendingRun(
    val trackId: String,
    val log: List<Int>,
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
    ): Upload {
        store.savePendingRuns(store.pendingRuns() + PendingRun(trackId, log.toCodes()))
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
                    Upload.Verified(api.submitRun(account.token, RunRequest(run.trackId, SimVersion.CURRENT, run.log)))
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

    private suspend fun account(): OnlineAccount =
        store.account() ?: api.register(petName()).let { registered ->
            OnlineAccount(registered.token, registered.displayName).also { store.saveAccount(it) }
        }
}
