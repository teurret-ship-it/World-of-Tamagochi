package com.worldoftamagochi.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.worldoftamagochi.designsystem.WotTheme
import com.worldoftamagochi.sim.Genome
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingScreenshotTest {
    private val eggs = listOf(3L, 14L, 15L, 92L)
    private val names = listOf("Mochi", "Pip", "Biscuit", "Noodle", "Maple", "Comet", "Tofu", "Kiwi")
    private val base = OnboardingUiState(eggs = eggs, names = names)

    private fun capture(
        name: String,
        dark: Boolean = false,
        content: @Composable () -> Unit,
    ) = captureRoboImage("src/test/screenshots/$name.png") { WotTheme(darkTheme = dark) { content() } }

    @Test
    fun titleNewPlayer() = capture("title_new") { TitleScreen(null, TitleActions(), animate = false) }

    @Test
    fun titleReturning() =
        capture("title_returning") { TitleScreen(TitlePet("Mochi", Genome.fromSeed(7), listOf("crown")), TitleActions(), animate = false) }

    @Test
    fun titleReturningDark() =
        capture("title_returning_dark", dark = true) {
            TitleScreen(TitlePet("Mochi", Genome.fromSeed(7), listOf("crown")), TitleActions(), animate = false)
        }

    @Test
    fun titleFont200() =
        capture("title_font200") {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                TitleScreen(TitlePet("Mochi", Genome.fromSeed(7)), TitleActions(), animate = false)
            }
        }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun eggsTablet() =
        capture("onboarding_eggs_tablet") { OnboardingScreen(base.copy(chosenEgg = 14L), OnboardingActions(), animate = false) }

    @Test
    fun eggs() = capture("onboarding_eggs") { OnboardingScreen(base.copy(chosenEgg = 14L), OnboardingActions(), animate = false) }

    @Test
    fun eggsDark() = capture("onboarding_eggs_dark", dark = true) { OnboardingScreen(base, OnboardingActions(), animate = false) }

    @Test
    fun name() =
        capture("onboarding_name") {
            OnboardingScreen(base.copy(step = OnboardingStep.NAME, chosenEgg = 14L, name = "Biscuit"), OnboardingActions(), animate = false)
        }

    @Test
    fun sleep() =
        capture("onboarding_sleep") {
            OnboardingScreen(
                base.copy(step = OnboardingStep.SLEEP, chosenEgg = 14L, name = "Biscuit"),
                OnboardingActions(),
                animate = false,
            )
        }

    @Test
    fun hatchCracking() =
        capture("onboarding_hatch_cracking") {
            OnboardingScreen(
                base.copy(step = OnboardingStep.HATCH, chosenEgg = 14L, name = "Biscuit", taps = 3),
                OnboardingActions(),
                animate = false,
            )
        }

    @Test
    fun hatched() =
        capture("onboarding_hatched") {
            OnboardingScreen(
                base.copy(step = OnboardingStep.HATCH, chosenEgg = 14L, name = "Biscuit", taps = 5, hatched = true),
                OnboardingActions(),
                animate = false,
            )
        }
}
