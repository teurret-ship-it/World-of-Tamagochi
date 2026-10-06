package com.mymagicalpet.sim

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class ElapsedTest {
    @Test
    fun `forward time is measured exactly`() {
        val elapsed = Elapsed.between(previousEpochMillis = 0, nowEpochMillis = 3.hours.inWholeMilliseconds)
        elapsed.duration shouldBe 3.hours
        elapsed.clockWentBackwards shouldBe false
    }

    @Test
    fun `a clock wound back never produces negative time`() {
        val elapsed = Elapsed.between(previousEpochMillis = 10_000, nowEpochMillis = 4_000)
        elapsed.duration shouldBe Duration.ZERO
        elapsed.clockWentBackwards shouldBe true
    }

    @Test
    fun `elapsed time is never negative for any pair of timestamps`(): Unit =
        runBlocking {
            val range = Arb.long(0L..4_000_000_000_000L)
            checkAll(range, range) { previous, now ->
                (Elapsed.between(previous, now).duration >= Duration.ZERO) shouldBe true
            }
        }

    @Test
    fun `rules version is positive`() {
        (SimVersion.CURRENT > 0) shouldBe true
    }
}
