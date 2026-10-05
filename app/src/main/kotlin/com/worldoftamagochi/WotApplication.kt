package com.worldoftamagochi

import android.app.Application
import com.worldoftamagochi.data.DataStoreGameRepository
import com.worldoftamagochi.data.GameRepository

/** Owns app-wide singletons until a DI framework earns its place (ADR-001). */
class WotApplication : Application() {
    val repository: GameRepository by lazy { DataStoreGameRepository.create(this) }
}
