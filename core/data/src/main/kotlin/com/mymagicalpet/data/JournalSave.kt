package com.mymagicalpet.data

import com.mymagicalpet.sim.journal.Deed
import com.mymagicalpet.sim.journal.Journal
import com.mymagicalpet.sim.journal.JournalRules
import com.mymagicalpet.sim.journal.Quest
import com.mymagicalpet.sim.journal.QuestKind
import com.mymagicalpet.sim.journal.Sticker
import com.mymagicalpet.sim.journal.Streak
import kotlinx.serialization.Serializable

/** Quests, streak and stickers in the save. Enum names are stored; unknown ones are dropped. */
@Serializable
data class JournalSave(
    val day: Long = Long.MIN_VALUE,
    val quests: List<QuestSave> = emptyList(),
    val allDoneClaimed: Boolean = false,
    val streakDays: Int = 0,
    val streakLastDay: Long = Long.MIN_VALUE,
    val streakBest: Int = 0,
    val freezes: Int = 1,
    val freezeWeek: Long = Long.MIN_VALUE,
    val counts: Map<String, Long> = emptyMap(),
    val stickers: List<String> = emptyList(),
)

@Serializable
data class QuestSave(
    val kind: String,
    val progress: Int,
)

fun JournalSave.toJournal(): Journal =
    Journal(
        day = day,
        quests = quests.mapNotNull { q -> enumOrNull<QuestKind>(q.kind)?.let { Quest(it, q.progress) } },
        allDoneClaimed = allDoneClaimed,
        streak = Streak(streakDays, streakLastDay, streakBest, freezes, freezeWeek),
        counts = counts.mapNotNull { (k, v) -> enumOrNull<Deed>(k)?.let { it to v } }.toMap(),
        stickers = stickers.mapNotNull { enumOrNull<Sticker>(it) }.toSet(),
    )

fun Journal.toSave(): JournalSave =
    JournalSave(
        day = day,
        quests = quests.map { QuestSave(it.kind.name, it.progress) },
        allDoneClaimed = allDoneClaimed,
        streakDays = streak.days,
        streakLastDay = streak.lastDay,
        streakBest = streak.best,
        freezes = streak.freezes,
        freezeWeek = streak.freezeWeek,
        counts = counts.mapKeys { it.key.name },
        stickers = stickers.map { it.name }.sorted(),
    )

/** What some deeds changed in the journal, for a screen to celebrate. */
data class JournalNews(
    val completed: List<QuestKind> = emptyList(),
    val allDone: Boolean = false,
    val stickers: List<Sticker> = emptyList(),
    val coins: Int = 0,
    val streakMilestone: Int? = null,
) {
    val isEmpty: Boolean get() = completed.isEmpty() && stickers.isEmpty() && coins == 0
}

/**
 * Records [deeds] done on [localEpochDay] in the journal and pays their rewards
 * into the coins, in one step, so every screen counts deeds the same way.
 */
fun GameSave.record(
    deeds: List<Deed>,
    localEpochDay: Long,
    rules: JournalRules = JournalRules.DEFAULT,
): Pair<GameSave, JournalNews> {
    var journal = journal.toJournal()
    var news = JournalNews()
    deeds.forEach { deed ->
        val update = rules.record(journal, pet.seed, deed, localEpochDay)
        journal = update.journal
        news =
            JournalNews(
                completed = news.completed + update.completed,
                allDone = news.allDone || update.allDone,
                stickers = news.stickers + update.newStickers,
                coins = news.coins + update.coins,
                streakMilestone = news.streakMilestone ?: update.streakMilestone,
            )
    }
    val paid = progress.copy(coins = progress.coins + news.coins)
    return copy(journal = journal.toSave(), progress = paid) to news
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
