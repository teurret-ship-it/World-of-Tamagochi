package com.worldoftamagochi.sim

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProgressTest {
    private val rules = ProgressRules.DEFAULT
    private val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), 0)

    private fun result(answered: Boolean) = CareResult.Done(pet, emptyMap(), answered)

    @Test
    fun `levels need 50, 100, 150 more XP each`() {
        rules.levelFor(0) shouldBe 1
        rules.levelFor(49) shouldBe 1
        rules.levelFor(50) shouldBe 2
        rules.levelFor(149) shouldBe 2
        rules.levelFor(150) shouldBe 3
        rules.xpAtLevel(1) shouldBe 0
        rules.xpAtLevel(3) shouldBe 150
        rules.xpAtLevel(10) shouldBe 2250
        PlayerProgress(xp = 150).level shouldBe 3
    }

    @Test
    fun `answering a need pays a bonus, plain care pays XP only`() {
        rules.reward(PlayerProgress(), result(answered = true), CareAction.FEED, 1).earned shouldBe Reward(15, 5)
        rules.reward(PlayerProgress(), result(answered = false), CareAction.FEED, 1).earned shouldBe Reward(5, 0)
        rules.reward(PlayerProgress(), result(answered = false), CareAction.STROKE, 1).earned shouldBe Reward(1, 0)
    }

    @Test
    fun `care coins are capped per day and the cap resets tomorrow`() {
        var progress = PlayerProgress()
        repeat(20) { progress = rules.reward(progress, result(true), CareAction.FEED, 10).progress }
        progress.coins shouldBe 60
        progress.careCoinsToday shouldBe 60
        progress = rules.reward(progress, result(true), CareAction.FEED, 11).progress
        progress.coins shouldBe 65
        progress.careCoinsToday shouldBe 5
    }

    @Test
    fun `a level-up is reported once, on the action that crossed it`() {
        val almost = PlayerProgress(xp = 45)
        val update = rules.reward(almost, result(answered = false), CareAction.PLAY, 1)
        update.levelUp shouldBe 2
        rules.reward(update.progress, result(answered = false), CareAction.PLAY, 1).levelUp shouldBe null
    }
}
