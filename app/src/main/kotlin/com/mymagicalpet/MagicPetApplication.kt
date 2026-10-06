package com.mymagicalpet

import android.app.Application
import com.mymagicalpet.data.DataStoreGameRepository
import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.data.SaveOnlineStore
import com.mymagicalpet.network.KtorRaceApi
import com.mymagicalpet.network.OnlineRacing
import io.ktor.client.engine.android.Android

/** Owns app-wide singletons until a DI framework earns its place (ADR-001). */
class MagicPetApplication : Application() {
    val repository: GameRepository by lazy { DataStoreGameRepository.create(this) }

    /** Null in offline builds (no server configured, ADR-008). */
    val online: OnlineRacing? by lazy {
        BuildConfig.SERVER_URL.takeIf { it.isNotBlank() }?.let { url ->
            OnlineRacing(KtorRaceApi(url, Android.create()), SaveOnlineStore(repository)) {
                repository.loadGame()?.pet?.name ?: com.mymagicalpet.sim.PetNames.DEFAULT
            }
        }
    }
}
