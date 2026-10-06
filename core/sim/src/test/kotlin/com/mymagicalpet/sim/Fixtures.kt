package com.mymagicalpet.sim

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

internal val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

internal fun utc(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int = 0,
): Long = LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

internal fun local(
    zone: ZoneId,
    isoLocalDateTime: String,
): Long =
    LocalDateTime
        .parse(isoLocalDateTime)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

internal const val HOUR: Long = 3_600_000L
internal const val MINUTE: Long = 60_000L
