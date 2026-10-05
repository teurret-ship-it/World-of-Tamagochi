package com.worldoftamagochi.data

import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.PlayerProgress
import com.worldoftamagochi.sim.SleepWindow
import kotlinx.serialization.Serializable
import java.time.ZoneId

/**
 * Everything saved on the device, as one versioned document. Field names are
 * part of the save format: rename only with a migration and a new [version].
 */
@Serializable
data class GameSave(
    val version: Int = CURRENT_VERSION,
    val pet: PetSave,
    val progress: ProgressSave = ProgressSave(),
    /** Personal bests per track id. */
    val records: Map<String, RecordSave> = emptyMap(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class PetSave(
    val seed: Long,
    val name: String,
    val stage: LifeStage,
    val satiety: Long,
    val energy: Long,
    val hygiene: Long,
    val happiness: Long,
    val health: Long,
    val sleepStartMinute: Int,
    val sleepEndMinute: Int,
    val zoneId: String,
    val updatedAtEpochMillis: Long,
    val napUntilEpochMillis: Long? = null,
)

@Serializable
data class ProgressSave(
    val xp: Long = 0,
    val coins: Long = 0,
    val careCoinsToday: Int = 0,
    val careCoinsDay: Long = 0,
    val raceCoinsToday: Int = 0,
    val raceCoinsDay: Long = 0,
)

/** The best run on one track: its exact time and the inputs that made it (the ghost). */
@Serializable
data class RecordSave(
    val finishMicros: Long,
    val log: List<Int>,
)

/** Player preferences (CLAUDE.md section 2: every sound and vibration has a toggle). */
@Serializable
data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
)

/** The whole file: the game (absent before the first pet) and the settings. */
@Serializable
data class SaveFile(
    val game: GameSave? = null,
    val settings: Settings = Settings(),
)

fun PetSave.toState(): PetState =
    PetState(
        stage = stage,
        needs = Needs(satiety, energy, hygiene, happiness, health),
        sleep = SleepWindow(sleepStartMinute, sleepEndMinute, ZoneId.of(zoneId)),
        updatedAtEpochMillis = updatedAtEpochMillis,
        napUntilEpochMillis = napUntilEpochMillis,
    )

fun PetState.toSave(
    seed: Long,
    name: String,
): PetSave =
    PetSave(
        seed = seed,
        name = name,
        stage = stage,
        satiety = needs.satiety,
        energy = needs.energy,
        hygiene = needs.hygiene,
        happiness = needs.happiness,
        health = needs.health,
        sleepStartMinute = sleep.startMinuteOfDay,
        sleepEndMinute = sleep.endMinuteOfDay,
        zoneId = sleep.zone.id,
        updatedAtEpochMillis = updatedAtEpochMillis,
        napUntilEpochMillis = napUntilEpochMillis,
    )

fun ProgressSave.toProgress(): PlayerProgress = PlayerProgress(xp, coins, careCoinsToday, careCoinsDay, raceCoinsToday, raceCoinsDay)

fun PlayerProgress.toSave(): ProgressSave = ProgressSave(xp, coins, careCoinsToday, careCoinsDay, raceCoinsToday, raceCoinsDay)
