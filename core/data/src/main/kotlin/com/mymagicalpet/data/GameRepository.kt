package com.mymagicalpet.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Where the game is saved. One implementation on the device; fakes in tests. */
interface GameRepository {
    val settings: Flow<Settings>

    /** The saved game as it changes; null before the first pet hatches. */
    val game: Flow<GameSave?>

    suspend fun loadGame(): GameSave?

    suspend fun saveGame(game: GameSave)

    /**
     * Atomic read-modify-write. Screens change only their own part of the save
     * (the home screen the pet, a race its record), so they never overwrite
     * each other's progress. Does nothing while no game exists.
     */
    suspend fun updateGame(change: (GameSave) -> GameSave)

    suspend fun updateSettings(change: (Settings) -> Settings)
}

/** DataStore-backed save: atomic writes, one JSON document, corruption-safe. */
class DataStoreGameRepository(
    private val store: DataStore<SaveFile>,
) : GameRepository {
    override val settings: Flow<Settings> = store.data.map { it.settings }

    override val game: Flow<GameSave?> = store.data.map { it.game }

    override suspend fun loadGame(): GameSave? = store.data.first().game

    override suspend fun saveGame(game: GameSave) {
        store.updateData { it.copy(game = game) }
    }

    override suspend fun updateGame(change: (GameSave) -> GameSave) {
        store.updateData { file -> file.game?.let { file.copy(game = change(it)) } ?: file }
    }

    override suspend fun updateSettings(change: (Settings) -> Settings) {
        store.updateData { it.copy(settings = change(it.settings)) }
    }

    companion object {
        fun create(context: Context): DataStoreGameRepository =
            DataStoreGameRepository(dataStore(File(context.filesDir, "datastore/game.json")))

        fun dataStore(
            file: File,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ): DataStore<SaveFile> =
            DataStoreFactory.create(
                serializer = SaveFileSerializer,
                // A damaged save must never crash a child's game: start fresh instead.
                corruptionHandler = ReplaceFileCorruptionHandler { SaveFile() },
                scope = scope,
                produceFile = { file },
            )
    }
}

internal object SaveFileSerializer : Serializer<SaveFile> {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val defaultValue: SaveFile = SaveFile()

    override suspend fun readFrom(input: InputStream): SaveFile =
        try {
            json.decodeFromString(SaveFile.serializer(), input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Unreadable save", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Invalid save", e)
        }

    override suspend fun writeTo(
        t: SaveFile,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(SaveFile.serializer(), t).encodeToByteArray())
    }
}
