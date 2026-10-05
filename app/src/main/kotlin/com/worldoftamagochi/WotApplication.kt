package com.worldoftamagochi

import android.app.Application
import com.worldoftamagochi.data.DataStoreGameRepository
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.data.SaveOnlineStore
import com.worldoftamagochi.network.KtorRaceApi
import com.worldoftamagochi.network.OnlineRacing
import io.ktor.client.engine.okhttp.OkHttp

/** Owns app-wide singletons until a DI framework earns its place (ADR-001). */
class WotApplication : Application() {
    val repository: GameRepository by lazy { DataStoreGameRepository.create(this) }

    /** Null in offline builds (no server configured, ADR-008). */
    val online: OnlineRacing? by lazy {
        BuildConfig.SERVER_URL.takeIf { it.isNotBlank() }?.let { url ->
            OnlineRacing(KtorRaceApi(url, OkHttp.create()), SaveOnlineStore(repository)) {
                repository.loadGame()?.pet?.name ?: com.worldoftamagochi.sim.PetNames.DEFAULT
            }
        }
    }
}
