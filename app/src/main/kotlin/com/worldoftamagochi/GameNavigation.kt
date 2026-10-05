package com.worldoftamagochi

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.feature.home.HomeRoute
import com.worldoftamagochi.feature.onboarding.OnboardingRoute
import com.worldoftamagochi.feature.onboarding.TitleActions
import com.worldoftamagochi.feature.onboarding.TitleRoute
import com.worldoftamagochi.feature.race.RaceRoute
import com.worldoftamagochi.feature.race.TrackSelectRoute
import com.worldoftamagochi.feature.shop.ShopRoute
import com.worldoftamagochi.network.OnlineRacing

/**
 * The places of the game: the title screen (main menu), the first-launch
 * story, home, the shop, the track list and a race. A navigation library
 * arrives with the next screens (friends, album).
 */
@Composable
internal fun GameNavigation(
    repository: GameRepository,
    online: OnlineRacing?,
) {
    var route by rememberSaveable { mutableStateOf(TITLE) }
    when {
        route == TITLE -> {
            TitleRoute(
                repository,
                TitleActions(
                    onPlay = { route = HOME },
                    onNewPet = { route = ONBOARDING },
                    onRaces = { route = TRACKS },
                    onShop = { route = SHOP },
                ),
            )
        }

        route == ONBOARDING -> {
            BackHandler { route = TITLE }
            OnboardingRoute(repository, onDone = { route = HOME })
        }

        route == HOME -> {
            BackHandler { route = TITLE }
            HomeRoute(repository, onOpenRaces = { route = TRACKS }, onOpenShop = { route = SHOP })
        }

        route == SHOP -> {
            BackHandler { route = HOME }
            ShopRoute(repository, onBack = { route = HOME })
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

private const val TITLE = "title"
private const val ONBOARDING = "onboarding"
private const val HOME = "home"
private const val TRACKS = "tracks"
private const val SHOP = "shop"
private const val RACE_PREFIX = "race:"
