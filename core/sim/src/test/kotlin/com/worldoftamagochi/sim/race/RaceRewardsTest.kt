package com.worldoftamagochi.sim.race

import com.worldoftamagochi.sim.PlayerProgress
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RaceRewardsTest {
    private val rules = RaceRewards.DEFAULT

    private fun pay(
        medal: Medal?,
        bestBefore: Medal? = null,
        today: Int = 0,
        day: Long = 100,
        paidDay: Long = 100,
        finished: Boolean = true,
    ) = rules.reward(PlayerProgress(raceCoinsToday = today, raceCoinsDay = paidDay), finished, medal, bestBefore, day)

    @Test
    fun `a first gold pays every tier up to gold, once`() {
        val first = pay(Medal.GOLD)
        first.earned.coins shouldBe 3 + 5 + 10 + 20
        first.earned.xp shouldBe 10 + 3 * 10
        pay(Medal.GOLD, bestBefore = Medal.GOLD).earned.coins shouldBe 3
        pay(Medal.AUTHOR, bestBefore = Medal.GOLD).earned.coins shouldBe 3 + 30
        pay(Medal.BRONZE, bestBefore = Medal.SILVER).earned.coins shouldBe 3
    }

    @Test
    fun `finishing without a medal still pays a little, a DNF pays nothing`() {
        pay(null).earned.coins shouldBe 3
        pay(null).earned.xp shouldBe 10
        pay(Medal.GOLD, finished = false).earned.coins shouldBe 0
    }

    @Test
    fun `finish coins are capped per day and reset tomorrow, medal coins are not`() {
        pay(null, today = 30).earned.coins shouldBe 0
        pay(null, today = 29).progress.raceCoinsToday shouldBe 30
        pay(Medal.BRONZE, today = 30).earned.coins shouldBe 5
        pay(null, today = 30, day = 101, paidDay = 100).earned.coins shouldBe 3
        // A clock wound back to yesterday keeps today's count.
        pay(null, today = 30, day = 99, paidDay = 100).earned.coins shouldBe 0
    }

    @Test
    fun `logs survive the compact encoding`() {
        val log = Autopilot(SprintTracks.MEADOW).play(RaceStats.ROOKIE).second
        InputLog.fromCodes(log.toCodes()) shouldBe log
    }
}
