package com.worldoftamagochi.data

import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.PlayerProgress
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.shop.Slot
import com.worldoftamagochi.sim.shop.Wardrobe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class GameRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val pet =
        PetState(
            stage = LifeStage.CHILD,
            needs = Needs(satiety = Needs.points(40), energy = 123_456_789, hygiene = 0, happiness = Needs.FULL, health = Needs.points(77)),
            sleep = SleepWindow(21 * 60, 6 * 60 + 30, ZoneId.of("Europe/Warsaw")),
            updatedAtEpochMillis = 1_790_000_000_000,
            napUntilEpochMillis = 1_790_000_100_000,
        )

    @Test
    fun `pet and progress survive a round trip exactly`() {
        assertEquals(pet, pet.toSave(seed = 42, name = "Mochi").toState())
        val progress = PlayerProgress(xp = 1234, coins = 56, careCoinsToday = 7, careCoinsDay = 20_000)
        assertEquals(progress, progress.toSave().toProgress())
    }

    @Test
    fun `the wardrobe survives a round trip and forgets items the catalog no longer has`() {
        val wardrobe = Wardrobe(owned = setOf("cap", "bell"), equipped = mapOf(Slot.HEAD to "cap"))
        assertEquals(wardrobe, wardrobe.toSave().toWardrobe())
        val old = WardrobeSave(owned = listOf("cap", "retired_hat"), worn = listOf("retired_hat", "cap", "bell"))
        assertEquals(Wardrobe(owned = setOf("cap"), equipped = mapOf(Slot.HEAD to "cap")), old.toWardrobe())
    }

    @Test
    fun `a saved game is there after reopening the store`() =
        runTest(UnconfinedTestDispatcher()) {
            val file = folder.newFile("game.json").also { it.delete() }
            val game = GameSave(pet = pet.toSave(42, "Mochi"), progress = PlayerProgress(xp = 99).toSave())
            // DataStore allows one active store per file: close the first like an app restart would.
            val firstJob = Job()
            val first =
                DataStoreGameRepository(
                    DataStoreGameRepository.dataStore(
                        file,
                        CoroutineScope(firstJob + UnconfinedTestDispatcher(testScheduler)),
                    ),
                )
            assertNull(first.loadGame())
            first.saveGame(game)
            first.updateSettings { it.copy(sound = false) }
            firstJob.cancelAndJoin()

            val reopened = DataStoreGameRepository(DataStoreGameRepository.dataStore(file, TestScope(testScheduler)))
            assertEquals(game, reopened.loadGame())
            assertFalse(reopened.settings.first().sound)
        }

    @Test
    fun `updates change one part of the game and leave the rest`() =
        runTest(UnconfinedTestDispatcher()) {
            val file = folder.newFile("update.json").also { it.delete() }
            val repository = DataStoreGameRepository(DataStoreGameRepository.dataStore(file, TestScope(testScheduler)))
            repository.updateGame { error("no game yet, nothing to update") }
            assertNull(repository.game.first())
            repository.saveGame(GameSave(pet = pet.toSave(42, "Mochi")))
            repository.updateGame { it.copy(records = mapOf("t" to RecordSave(1_000, listOf(4, 8)))) }
            repository.updateGame { it.copy(progress = it.progress.copy(coins = 7)) }
            val game = requireNotNull(repository.game.first())
            assertEquals(7L, game.progress.coins)
            assertEquals(RecordSave(1_000, listOf(4, 8)), game.records["t"])
            assertEquals(pet, game.pet.toState())
        }

    @Test
    fun `a corrupted save starts a fresh game instead of crashing`() =
        runTest(UnconfinedTestDispatcher()) {
            val file = folder.newFile("broken.json").apply { writeText("{ not json") }
            val repository = DataStoreGameRepository(DataStoreGameRepository.dataStore(file, TestScope(testScheduler)))
            assertNull(repository.loadGame())
            assertEquals(Settings(), repository.settings.first())
        }
}
