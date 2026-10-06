package com.mymagicalpet.sim

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * When the pet sleeps, in the player's local time: the pet sleeps when the
 * player sleeps (CLAUDE.md section 2). A window may cross midnight
 * (22:00-07:00); equal start and end means the pet never sleeps.
 */
data class SleepWindow(
    val startMinuteOfDay: Int = DEFAULT_BEDTIME,
    val endMinuteOfDay: Int = DEFAULT_WAKE_UP,
    val zone: ZoneId = ZoneId.of("UTC"),
) {
    init {
        require(startMinuteOfDay in 0 until MINUTES_PER_DAY) { "start must be a minute of the day" }
        require(endMinuteOfDay in 0 until MINUTES_PER_DAY) { "end must be a minute of the day" }
    }

    fun isAsleep(epochMillis: Long): Boolean {
        if (startMinuteOfDay == endMinuteOfDay) return false
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        val minute = time.hour * MINUTES_PER_HOUR + time.minute
        return if (startMinuteOfDay < endMinuteOfDay) {
            minute in startMinuteOfDay until endMinuteOfDay
        } else {
            minute >= startMinuteOfDay || minute < endMinuteOfDay
        }
    }

    /** The first moment strictly after [epochMillis] at which the pet falls asleep or wakes up. */
    fun nextBoundaryAfter(epochMillis: Long): Long {
        if (startMinuteOfDay == endMinuteOfDay) return Long.MAX_VALUE
        val today = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return (-1L..1L)
            .asSequence()
            .map { today.plusDays(it) }
            .flatMap { day -> sequenceOf(boundary(day, startMinuteOfDay), boundary(day, endMinuteOfDay)) }
            .filter { it > epochMillis }
            .min()
    }

    private fun boundary(
        day: LocalDate,
        minuteOfDay: Int,
    ): Long =
        day
            .atTime(LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    companion object {
        /** 22:00, the default from CLAUDE.md section 2; the player changes it in onboarding. */
        const val DEFAULT_BEDTIME = 22 * 60

        /** 07:00. */
        const val DEFAULT_WAKE_UP = 7 * 60

        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
