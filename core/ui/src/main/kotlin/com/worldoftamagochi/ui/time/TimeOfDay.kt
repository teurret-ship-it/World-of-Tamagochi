package com.worldoftamagochi.ui.time

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "7:00 AM" or "07:00", in the player's locale, from minutes after midnight. */
fun clockTime(minuteOfDay: Int): String =
    LocalTime
        .of(
            minuteOfDay / MINUTES_PER_HOUR,
            minuteOfDay % MINUTES_PER_HOUR,
        ).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** Moves a time of day by [deltaMinutes], wrapping around midnight. */
fun shiftTime(
    minuteOfDay: Int,
    deltaMinutes: Int,
): Int = Math.floorMod(minuteOfDay + deltaMinutes, MINUTES_PER_DAY)

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
