package com.mymagicalpet.sim.journal

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class JournalTest {
    private val rules = JournalRules.DEFAULT
    private val seed = 42L
    private val day = 20_000L

    /** Does every quest of the day once over, completing all three. */
    private fun completeAll(
        journal: Journal,
        d: Long,
    ): Pair<Journal, List<JournalUpdate>> {
        var j = rules.today(journal, seed, d)
        val updates = mutableListOf<JournalUpdate>()
        j.quests.forEach { q ->
            repeat(q.kind.target) {
                rules.record(j, seed, q.kind.deed, d).also {
                    j = it.journal
                    updates += it
                }
            }
        }
        return j to updates
    }

    @Test
    fun `three different quests a day, the same all day and new tomorrow`() {
        val today = rules.questsFor(seed, day)
        today.size shouldBe 3
        today.map { it.kind }.toSet().size shouldBe 3
        rules.questsFor(seed, day) shouldBe today
        (1L..10L).map { rules.questsFor(seed, day + it) }.toSet().size shouldNotBe 1
        rules.questsFor(seed + 1, day) shouldNotBe today
    }

    @Test
    fun `finishing quests pays coins at once, and all three pay a bonus`() {
        val (journal, updates) = completeAll(Journal(), day)
        journal.quests.all { it.done } shouldBe true
        updates.flatMap { it.completed }.toSet() shouldBe journal.quests.map { it.kind }.toSet()
        updates.single { it.allDone }.coins shouldBe (updates.single { it.allDone }.completed.sumOf { it.coins } + 25)
        updates.sumOf { it.coins } shouldBe journal.quests.sumOf { it.kind.coins } + 25
        // Doing more after everything is done pays nothing more.
        rules
            .record(
                journal,
                seed,
                journal.quests
                    .first()
                    .kind.deed,
                day,
            ).coins shouldBe 0
    }

    @Test
    fun `a streak grows by one a day, pays at milestones, and a freeze covers one missed day a week`() {
        var journal = Journal()
        val milestones = mutableListOf<Int>()
        for (d in 0L until 3L) {
            val (j, updates) = completeAll(journal, day + d)
            journal = j
            updates.mapNotNull { it.streakMilestone }.forEach { milestones += it }
        }
        journal.streak.days shouldBe 3
        milestones shouldBe listOf(3)
        // Skip one day: the weekly freeze keeps the streak alive.
        journal = completeAll(journal, day + 4).first
        journal.streak.days shouldBe 4
        // Skip two more days in the same week: the freeze is used up, the streak starts over.
        journal = completeAll(journal, day + 7).first
        journal.streak.days shouldBe 1
        journal.streak.best shouldBe 4
    }

    @Test
    fun `stickers unlock once, from lifetime counts`() {
        var journal = Journal()
        val first = rules.record(journal, seed, Deed.FEED, day)
        first.newStickers shouldBe listOf(Sticker.FIRST_MEAL)
        journal = first.journal
        rules.record(journal, seed, Deed.FEED, day).newStickers shouldBe emptyList()
        repeat(99) { journal = rules.record(journal, seed, Deed.FEED, day + it / 10).journal }
        journal.stickers shouldContain Sticker.GOURMET
        journal.counts[Deed.FEED] shouldBe 100
    }

    @Test
    fun `a wound-back clock gives no new quests`() {
        val journal = rules.today(Journal(), seed, day)
        rules.today(journal, seed, day - 1) shouldBe journal
    }
}
