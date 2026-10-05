package com.worldoftamagochi.sim

import java.time.Instant
import java.time.ZoneId

/** The local calendar day of [epochMillis] in [zone], as days since 1970-01-01: daily caps count in these. */
fun localEpochDay(
    epochMillis: Long,
    zone: ZoneId,
): Long =
    Instant
        .ofEpochMilli(epochMillis)
        .atZone(zone)
        .toLocalDate()
        .toEpochDay()
