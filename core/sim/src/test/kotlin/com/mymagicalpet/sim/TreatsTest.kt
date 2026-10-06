package com.mymagicalpet.sim

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TreatsTest {
    private val rules = TreatRules.DEFAULT
    private val day = 20_000L

    @Test
    fun `a treat costs coins and three a day is the limit`() {
        var progress = PlayerProgress(coins = 100)
        repeat(3) {
            rules.refusal(progress, day) shouldBe null
            progress = rules.pay(progress, day)
        }
        progress.coins shouldBe 55
        rules.leftToday(progress, day) shouldBe 0
        rules.refusal(progress, day) shouldBe Refusal.NO_TREATS_LEFT
        rules.leftToday(progress, day + 1) shouldBe 3
        rules.refusal(progress, day + 1) shouldBe null
    }

    @Test
    fun `no coins, no treat - and a wound-back clock does not reset the limit`() {
        rules.refusal(PlayerProgress(coins = 14), day) shouldBe Refusal.NO_COINS
        val spent = PlayerProgress(coins = 100, treatsToday = 3, treatsDay = day)
        rules.refusal(spent, day - 1) shouldBe Refusal.NO_TREATS_LEFT
        rules.pay(PlayerProgress(coins = 100, treatsToday = 1, treatsDay = day), day - 1).treatsToday shouldBe 2
    }

    @Test
    fun `a treat cheers the pet up, is refused when full and never pays coins back`() {
        val hungry =
            PetState(
                LifeStage.CHILD,
                Needs(satiety = Needs.points(30), happiness = Needs.points(20)),
                SleepWindow(),
                12 * 3_600_000L,
            )
        val done = CareRules.DEFAULT.apply(hungry, CareAction.TREAT).shouldBeInstanceOf<CareResult.Done>()
        done.changes shouldBe mapOf(Need.SATIETY to 10, Need.HAPPINESS to 20)
        done.answeredNeed shouldBe true
        ProgressRules.DEFAULT
            .reward(PlayerProgress(), done, CareAction.TREAT, day)
            .earned.coins shouldBe 0
        val full = hungry.copy(needs = Needs())
        CareRules.DEFAULT.apply(full, CareAction.TREAT) shouldBe CareResult.Refused(full, Refusal.FULL)
    }
}
