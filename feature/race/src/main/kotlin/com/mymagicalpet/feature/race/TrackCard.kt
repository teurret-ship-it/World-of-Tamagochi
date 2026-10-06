package com.mymagicalpet.feature.race

import com.mymagicalpet.api.LeaderboardEntry
import com.mymagicalpet.sim.race.Medal
import com.mymagicalpet.sim.race.MedalTimes
import com.mymagicalpet.sim.race.Track

/** One course on the list: its medal times and the player's best. */
data class TrackCard(
    val track: Track,
    val medals: MedalTimes,
    val bestMicros: Long?,
    /** Today's online top, empty offline. */
    val today: List<LeaderboardEntry> = emptyList(),
) {
    val bestMedal: Medal? get() = medals.medalFor(bestMicros)
}
