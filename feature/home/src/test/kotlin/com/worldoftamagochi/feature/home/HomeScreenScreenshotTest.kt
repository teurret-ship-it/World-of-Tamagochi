package com.worldoftamagochi.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.worldoftamagochi.designsystem.WotTheme
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Need
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reference screenshots of the home screen in the four variants required by
 * the quality gates (phone light, phone dark, 200% font, tablet), plus the
 * hungry, washing, night and level-up states.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
class HomeScreenScreenshotTest {
    @Test
    fun phoneLight() = capture("home_phone_light") { Home(happy) }

    @Test
    fun phoneDark() = capture("home_phone_dark") { Home(happy, dark = true) }

    @Test
    fun phoneFontScale200() =
        capture("home_phone_font200") {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) { Home(happy) }
        }

    @Test
    @Config(qualifiers = TABLET)
    fun tablet() = capture("home_tablet") { Home(happy) }

    @Test
    fun hungry() = capture("home_phone_hungry") { Home(hungry) }

    @Test
    fun washing() =
        capture("home_phone_washing") { Home(happy.copy(washing = true, expression = Expression.DIRTY, urgentNeed = Need.HYGIENE)) }

    @Test
    fun nightNap() =
        capture("home_phone_nap") {
            Home(happy.copy(expression = Expression.ASLEEP, napping = true, urgentNeed = null), dark = true)
        }

    @Test
    fun welcomeBack() =
        capture("home_welcome_back") {
            Home(
                hungry.copy(
                    away =
                        AwaySummary(
                            312,
                            mapOf(
                                Need.SATIETY to -57,
                                Need.HAPPINESS to -40,
                                Need.ENERGY to 12,
                            ),
                        ),
                ),
            )
        }

    @Test
    fun levelUp() = capture("home_level_up") { WotTheme { LevelUpBanner(level = 3, onDone = {}) } }

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) = captureRoboImage("src/test/screenshots/$name.png", content = content)

    @Composable
    private fun Home(
        state: HomeUiState,
        dark: Boolean = false,
    ) = WotTheme(darkTheme = dark) { HomeScreen(state = state, actions = HomeActions(), animate = false) }

    private companion object {
        val happy =
            HomeUiState(
                name = "Mochi",
                genome = Genome.fromSeed(7),
                expression = Expression.HAPPY,
                needs = Need.entries.associateWith { 90 },
                urgentNeed = null,
                level = 4,
                levelProgress = 0.6f,
                coins = 125,
            )
        val hungry =
            happy.copy(
                expression = Expression.HUNGRY,
                needs = Need.entries.associateWith { 75 } + (Need.SATIETY to 18),
                urgentNeed = Need.SATIETY,
            )
    }
}

private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val TABLET = "w1280dp-h800dp-xhdpi"
