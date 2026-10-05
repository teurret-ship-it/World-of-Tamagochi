package com.worldoftamagochi.sim

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.ZoneId

class LocalDayTest {
    @Test
    fun `the local day follows the player's time zone`() {
        val lateEveningInWarsaw = 1_790_000_000_000L - 1_790_000_000_000L % 86_400_000L + 22 * 3_600_000L + 30 * 60_000L
        localEpochDay(lateEveningInWarsaw, ZoneId.of("UTC")) shouldBe lateEveningInWarsaw / 86_400_000L
        // 22:30 UTC is already tomorrow in Warsaw.
        localEpochDay(lateEveningInWarsaw, ZoneId.of("Europe/Warsaw")) shouldBe lateEveningInWarsaw / 86_400_000L + 1
    }
}
