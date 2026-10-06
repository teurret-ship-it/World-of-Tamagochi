package com.mymagicalpet.feature.onboarding

import com.mymagicalpet.data.GameRepository
import com.mymagicalpet.data.GameSave
import com.mymagicalpet.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory save for tests. */
class FakeGameRepository : GameRepository {
    override val settings = MutableStateFlow(Settings())

    override val game = MutableStateFlow<GameSave?>(null)

    val saved: GameSave? get() = game.value

    override suspend fun loadGame(): GameSave? = game.value

    override suspend fun saveGame(game: GameSave) {
        this.game.value = game
    }

    override suspend fun updateGame(change: (GameSave) -> GameSave) {
        game.value = game.value?.let(change)
    }

    override suspend fun updateSettings(change: (Settings) -> Settings) {
        settings.value = change(settings.value)
    }
}
