package com.mymagicalpet.feature.race

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mymagicalpet.sim.race.Medal
import com.mymagicalpet.sim.race.Track
import java.util.Locale
import com.mymagicalpet.ui.R as UiR

/** "32.45" from microseconds, in the player's locale. */
internal fun formatSeconds(micros: Long): String = String.format(Locale.getDefault(), "%.2f", micros / MICROS_PER_SECOND)

@StringRes
internal fun Track.titleRes(): Int =
    when (id) {
        "sprint-meadow" -> R.string.track_meadow
        "sprint-beach" -> R.string.track_beach
        "sprint-snow" -> R.string.track_snow
        "agility-park" -> R.string.track_park
        "agility-hills" -> R.string.track_hills
        "agility-trail" -> R.string.track_trail
        else -> R.string.track_unknown
    }

@StringRes
internal fun Medal.titleRes(): Int =
    when (this) {
        Medal.BRONZE -> R.string.medal_bronze
        Medal.SILVER -> R.string.medal_silver
        Medal.GOLD -> R.string.medal_gold
        Medal.AUTHOR -> R.string.medal_author
    }

@DrawableRes
internal fun Medal.imageRes(): Int =
    when (this) {
        Medal.BRONZE -> UiR.drawable.medal_bronze
        Medal.SILVER -> UiR.drawable.medal_silver
        Medal.GOLD -> UiR.drawable.medal_gold
        Medal.AUTHOR -> UiR.drawable.medal_author
    }

private const val MICROS_PER_SECOND = 1_000_000.0
