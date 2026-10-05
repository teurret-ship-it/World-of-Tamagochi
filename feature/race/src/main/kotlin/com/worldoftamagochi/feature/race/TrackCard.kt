package com.worldoftamagochi.feature.race

import com.worldoftamagochi.sim.race.Medal
import com.worldoftamagochi.sim.race.MedalTimes
import com.worldoftamagochi.sim.race.Track

/** One course on the list: its medal times and the player's best. */
data class TrackCard(
    val track: Track,
    val medals: MedalTimes,
    val bestMicros: Long?,
) {
    val bestMedal: Medal? get() = medals.medalFor(bestMicros)
}
