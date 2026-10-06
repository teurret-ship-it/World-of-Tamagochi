package com.mymagicalpet.data

import com.mymagicalpet.sim.Form
import com.mymagicalpet.sim.Growth
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Needs
import com.mymagicalpet.sim.PetState
import com.mymagicalpet.sim.PlayerProgress
import com.mymagicalpet.sim.SleepWindow
import com.mymagicalpet.sim.race.RaceStats
import com.mymagicalpet.sim.shop.Catalog
import com.mymagicalpet.sim.shop.Wardrobe
import com.mymagicalpet.sim.training.TrainingLog
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
    /** The anonymous online identity and runs waiting to be uploaded. */
    val online: OnlineSave = OnlineSave(),
    /** Cosmetics the player owns and the ones the pet wears. */
    val wardrobe: WardrobeSave = WardrobeSave(),
    /** Race stats grown by training, and today's sessions. */
    val stats: StatsSave = StatsSave(),
    val training: TrainingSave = TrainingSave(),
    /** Daily quests, the streak and the sticker album. */
    val journal: JournalSave = JournalSave(),
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
    val awakeUntilEpochMillis: Long? = null,
    val growthPoints: Int = 0,
    val growthToday: Int = 0,
    val growthDay: Long = 0,
    val growthSport: Int = 0,
    /** Form name, fixed at adulthood; unknown names are ignored. */
    val form: String? = null,
    val onJourney: Boolean = false,
    val rescueSteps: Int = 0,
    val vacationUntilEpochMillis: Long? = null,
)

@Serializable
data class ProgressSave(
    val xp: Long = 0,
    val coins: Long = 0,
    val careCoinsToday: Int = 0,
    val careCoinsDay: Long = 0,
    val raceCoinsToday: Int = 0,
    val raceCoinsDay: Long = 0,
    val treatsToday: Int = 0,
    val treatsDay: Long = 0,
)

/** The best run on one track: its exact time and the inputs that made it (the ghost). */
@Serializable
data class RecordSave(
    val finishMicros: Long,
    val log: List<Int>,
    /** The stats the record was raced with: the ghost must replay with them. */
    val stats: StatsSave = StatsSave(),
)

@Serializable
data class StatsSave(
    val speed: Int = 0,
    val stamina: Int = 0,
    val agility: Int = 0,
    val jump: Int = 0,
)

@Serializable
data class TrainingSave(
    val sessionsToday: Int = 0,
    val day: Long = 0,
)

@Serializable
data class OnlineSave(
    val token: String? = null,
    val displayName: String? = null,
    val pending: List<PendingRunSave> = emptyList(),
)

@Serializable
data class PendingRunSave(
    val trackId: String,
    val log: List<Int>,
    val stats: StatsSave = StatsSave(),
)

/** Item ids; the slot of a worn item comes from the catalog, so a save never disagrees with it. */
@Serializable
data class WardrobeSave(
    val owned: List<String> = emptyList(),
    val worn: List<String> = emptyList(),
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
        awakeUntilEpochMillis = awakeUntilEpochMillis,
        growth = Growth(growthPoints, growthToday, growthDay, growthSport),
        form = Form.entries.firstOrNull { it.name == form },
        onJourney = onJourney,
        rescueSteps = rescueSteps,
        vacationUntilEpochMillis = vacationUntilEpochMillis,
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
        awakeUntilEpochMillis = awakeUntilEpochMillis,
        growthPoints = growth.points,
        growthToday = growth.today,
        growthDay = growth.day,
        growthSport = growth.sport,
        form = form?.name,
        onJourney = onJourney,
        rescueSteps = rescueSteps,
        vacationUntilEpochMillis = vacationUntilEpochMillis,
    )

fun ProgressSave.toProgress(): PlayerProgress =
    PlayerProgress(xp, coins, careCoinsToday, careCoinsDay, raceCoinsToday, raceCoinsDay, treatsToday, treatsDay)

fun PlayerProgress.toSave(): ProgressSave =
    ProgressSave(xp, coins, careCoinsToday, careCoinsDay, raceCoinsToday, raceCoinsDay, treatsToday, treatsDay)

/** Unknown ids (an item removed from the catalog) are dropped instead of crashing. */
fun WardrobeSave.toWardrobe(): Wardrobe {
    val owned = owned.filter { Catalog.byId(it) != null }.toSet()
    val equipped = worn.filter { it in owned }.mapNotNull { Catalog.byId(it) }.associate { it.slot to it.id }
    return Wardrobe(owned, equipped)
}

fun Wardrobe.toSave(): WardrobeSave = WardrobeSave(owned.sorted(), equipped.values.sorted())

/** A damaged or hand-edited save can hold anything: clamp into the allowed range. */
fun StatsSave.toStats(): RaceStats {
    fun of(value: Int) = value.coerceIn(RaceStats.MIN, RaceStats.MAX)
    return RaceStats(of(speed), of(stamina), of(agility), of(jump))
}

fun RaceStats.toSave(): StatsSave = StatsSave(speed, stamina, agility, jump)

fun TrainingSave.toLog(): TrainingLog = TrainingLog(sessionsToday, day)

fun TrainingLog.toSave(): TrainingSave = TrainingSave(sessionsToday, day)
