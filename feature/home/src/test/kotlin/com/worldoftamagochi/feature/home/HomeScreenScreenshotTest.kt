package com.worldoftamagochi.feature.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reference screenshots of the home screen in the four variants required by
 * the quality gates: phone light, phone dark, 200% font, tablet.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
class HomeScreenScreenshotTest {
    @Test
    fun phoneLight() {
        captureRoboImage("src/test/screenshots/home_phone_light.png") {
            HomeScreenPreviewContent(darkTheme = false)
        }
    }

    @Test
    fun phoneDark() {
        captureRoboImage("src/test/screenshots/home_phone_dark.png") {
            HomeScreenPreviewContent(darkTheme = true)
        }
    }

    @Test
    fun phoneFontScale200() {
        captureRoboImage("src/test/screenshots/home_phone_font200.png") {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                HomeScreenPreviewContent(darkTheme = false)
            }
        }
    }

    @Test
    @Config(qualifiers = TABLET)
    fun tablet() {
        captureRoboImage("src/test/screenshots/home_tablet.png") {
            HomeScreenPreviewContent(darkTheme = false)
        }
    }
}

private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val TABLET = "w1280dp-h800dp-xhdpi"
