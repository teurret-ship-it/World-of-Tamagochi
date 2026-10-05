package com.worldoftamagochi.data

import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.PlayerProgress
import com.worldoftamagochi.sim.SleepWindow
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
    fun `a saved game is there after reopening the store`() =
        runTest(UnconfinedTestDispatcher()) {
            val file = folder.newFile("game.json").also { it.delete() }
            val game = GameSave(pet = pet.toSave(42, "Mochi"), progress = PlayerProgress(xp = 99).toSave())
            val first = DataStoreGameRepository(DataStoreGameRepository.dataStore(file, TestScope(testScheduler)))
            assertNull(first.loadGame())
            first.saveGame(game)
            first.updateSettings { it.copy(sound = false) }

            val reopened = DataStoreGameRepository(DataStoreGameRepository.dataStore(file, TestScope(testScheduler)))
            assertEquals(game, reopened.loadGame())
            assertFalse(reopened.settings.first().sound)
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
