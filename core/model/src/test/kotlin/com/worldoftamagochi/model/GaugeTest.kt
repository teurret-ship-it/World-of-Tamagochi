package com.worldoftamagochi.model

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GaugeTest {
    @Test
    fun `values outside 0 to 100 are clamped`() {
        Gauge.of(-5) shouldBe Gauge.EMPTY
        Gauge.of(250) shouldBe Gauge.FULL
        Gauge.of(42).value shouldBe 42
    }

    @Test
    fun `arithmetic never leaves the range, even at integer extremes`(): Unit =
        runBlocking {
            checkAll(Arb.int(), Arb.int()) { start, delta ->
                val plus = Gauge.of(start) + delta
                val minus = Gauge.of(start) - delta
                (plus.value in 0..100) shouldBe true
                (minus.value in 0..100) shouldBe true
            }
        }

    @Test
    fun `empty and full are recognised`() {
        (Gauge.FULL - 100).isEmpty shouldBe true
        (Gauge.EMPTY + 100).isFull shouldBe true
        (Gauge.of(50) < Gauge.of(60)) shouldBe true
        Gauge.of(7).toString() shouldBe "Gauge(7)"
    }
}
