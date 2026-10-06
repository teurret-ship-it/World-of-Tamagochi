package com.mymagicalpet.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mymagicalpet.ui.fx.FxKind
import com.mymagicalpet.ui.fx.ParticleField
import com.mymagicalpet.ui.fx.ParticleLayer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w400dp-h400dp-xhdpi")
class ParticleScreenshotTest {
    @Test
    fun everyKindMidFlight() {
        val field = ParticleField(Random(3))
        FxKind.entries.forEachIndexed { i, kind -> field.burst(kind, Offset(0.15f + i * 0.14f, 0.6f), count = 3) }
        field.label("+35", Color(0xFFF08A4B), Offset(0.5f, 0.2f))
        field.step(250)
        captureRoboImage("src/test/screenshots/fx_particles.png") {
            ParticleLayer(field, Modifier.size(400.dp).background(Color(0xFFFFF8EE)), TextStyle(fontSize = 32.sp))
        }
    }

    @Test
    fun particlesExpire() {
        val field = ParticleField(Random(1))
        field.burst(FxKind.COIN, Offset(0.5f, 0.5f), count = 5)
        field.label("+5", Color.Black, Offset(0.5f, 0.5f))
        repeat(40) { field.step(50) }
        assertTrue(field.isIdle)
    }
}
