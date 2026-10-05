package com.worldoftamagochi.data

import com.worldoftamagochi.network.OnlineAccount
import com.worldoftamagochi.network.OnlineStore
import com.worldoftamagochi.network.PendingRun

/** Keeps the online identity and the upload queue inside the save file. */
class SaveOnlineStore(
    private val repository: GameRepository,
) : OnlineStore {
    override suspend fun account(): OnlineAccount? =
        repository.loadGame()?.online?.let { online ->
            val token = online.token ?: return null
            OnlineAccount(token, online.displayName.orEmpty())
        }

    override suspend fun saveAccount(account: OnlineAccount) =
        repository.updateGame { it.copy(online = it.online.copy(token = account.token, displayName = account.displayName)) }

    override suspend fun pendingRuns(): List<PendingRun> =
        repository
            .loadGame()
            ?.online
            ?.pending
            .orEmpty()
            .map { PendingRun(it.trackId, it.log, it.stats.toStats()) }

    override suspend fun savePendingRuns(runs: List<PendingRun>) =
        repository.updateGame {
            it.copy(
                online =
                    it.online.copy(
                        pending =
                            runs.map { r ->
                                PendingRunSave(r.trackId, r.log, r.stats.toSave())
                            },
                    ),
            )
        }
}
