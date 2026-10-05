package com.worldoftamagochi.sim

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SleepWindowTest {
    private val night = SleepWindow(zone = WARSAW)

    @Test
    fun `default window crosses midnight in local time`() {
        night.isAsleep(local(WARSAW, "2026-06-01T21:59")) shouldBe false
        night.isAsleep(local(WARSAW, "2026-06-01T22:00")) shouldBe true
        night.isAsleep(local(WARSAW, "2026-06-02T03:00")) shouldBe true
        night.isAsleep(local(WARSAW, "2026-06-02T07:00")) shouldBe false
    }

    @Test
    fun `a daytime window for night-shift players`() {
        val day = SleepWindow(startMinuteOfDay = 9 * 60, endMinuteOfDay = 17 * 60, zone = WARSAW)
        day.isAsleep(local(WARSAW, "2026-06-01T08:00")) shouldBe false
        day.isAsleep(local(WARSAW, "2026-06-01T12:00")) shouldBe true
        day.nextBoundaryAfter(local(WARSAW, "2026-06-01T12:00")) shouldBe local(WARSAW, "2026-06-01T17:00")
    }

    @Test
    fun `next boundary is the next fall-asleep or wake-up moment`() {
        night.nextBoundaryAfter(local(WARSAW, "2026-06-01T12:00")) shouldBe local(WARSAW, "2026-06-01T22:00")
        night.nextBoundaryAfter(local(WARSAW, "2026-06-01T22:00")) shouldBe local(WARSAW, "2026-06-02T07:00")
        night.nextBoundaryAfter(local(WARSAW, "2026-06-02T01:00")) shouldBe local(WARSAW, "2026-06-02T07:00")
    }

    @Test
    fun `daylight saving change keeps bedtime at 22 local`() {
        // Europe/Warsaw switches to summer time on 2026-03-29.
        night.nextBoundaryAfter(local(WARSAW, "2026-03-29T12:00")) shouldBe local(WARSAW, "2026-03-29T22:00")
        (local(WARSAW, "2026-03-29T22:00") - local(WARSAW, "2026-03-28T22:00")) shouldBe 23 * HOUR
    }

    @Test
    fun `equal start and end means the pet never sleeps`() {
        val never = SleepWindow(startMinuteOfDay = 600, endMinuteOfDay = 600)
        never.isAsleep(utc(2026, 1, 1, 10)) shouldBe false
        never.nextBoundaryAfter(0) shouldBe Long.MAX_VALUE
    }

    @Test
    fun `minutes outside a day are rejected`() {
        assertThrows<IllegalArgumentException> { SleepWindow(startMinuteOfDay = 24 * 60) }
        assertThrows<IllegalArgumentException> { SleepWindow(endMinuteOfDay = -1) }
    }
}
