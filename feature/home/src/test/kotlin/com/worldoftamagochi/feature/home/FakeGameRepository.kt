package com.worldoftamagochi.feature.home

import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.data.GameSave
import com.worldoftamagochi.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory save for tests. */
class FakeGameRepository(
    var game: GameSave? = null,
) : GameRepository {
    override val settings = MutableStateFlow(Settings())

    var saves = 0

    override suspend fun loadGame(): GameSave? = game

    override suspend fun saveGame(game: GameSave) {
        this.game = game
        saves++
    }

    override suspend fun updateSettings(change: (Settings) -> Settings) {
        settings.value = change(settings.value)
    }
}
