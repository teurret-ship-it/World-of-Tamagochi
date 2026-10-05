package com.worldoftamagochi.sim

import com.worldoftamagochi.model.Gauge
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NeedsTest {
    @Test
    fun `display rounds up so zero means truly empty`() {
        val needs = Needs(satiety = 1, hygiene = Needs.UNITS_PER_POINT, happiness = 0)
        needs.gauge(Need.SATIETY) shouldBe Gauge.of(1)
        needs.gauge(Need.HYGIENE) shouldBe Gauge.of(1)
        needs.gauge(Need.HAPPINESS) shouldBe Gauge.EMPTY
        needs.gauge(Need.ENERGY) shouldBe Gauge.FULL
    }

    @Test
    fun `with clamps and reads back every need`() {
        Need.entries.forEach { need ->
            Needs().with(need, -5)[need] shouldBe 0L
            Needs().with(need, Needs.FULL * 2)[need] shouldBe Needs.FULL
            Needs().with(need, Needs.points(30))[need] shouldBe Needs.points(30)
        }
    }

    @Test
    fun `levels outside the scale are rejected`() {
        assertThrows<IllegalArgumentException> { Needs(satiety = -1) }
        assertThrows<IllegalArgumentException> { Needs(health = Needs.FULL + 1) }
        assertThrows<IllegalArgumentException> { NeedRates(-1, 0, 0, 0, 0, 0, 0, 0) }
    }
}
