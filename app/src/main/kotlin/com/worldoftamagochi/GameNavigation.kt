package com.worldoftamagochi

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.feature.home.HomeRoute
import com.worldoftamagochi.feature.race.RaceRoute
import com.worldoftamagochi.feature.race.TrackSelectRoute
import com.worldoftamagochi.network.OnlineRacing

/**
 * Three places: home, the track list, a race. A navigation library arrives
 * with the next screens (shop, friends); for three it is only ceremony.
 */
@Composable
internal fun GameNavigation(
    repository: GameRepository,
    online: OnlineRacing?,
) {
    var route by rememberSaveable { mutableStateOf(HOME) }
    when {
        route == HOME -> {
            HomeRoute(repository, onOpenRaces = { route = TRACKS })
        }

        route == TRACKS -> {
            BackHandler { route = HOME }
            TrackSelectRoute(repository, online, onRace = { route = RACE_PREFIX + it }, onBack = { route = HOME })
        }

        route.startsWith(RACE_PREFIX) -> {
            BackHandler { route = TRACKS }
            RaceRoute(repository, online, route.removePrefix(RACE_PREFIX), onExit = { route = TRACKS })
        }
    }
}

private const val HOME = "home"
private const val TRACKS = "tracks"
private const val RACE_PREFIX = "race:"
