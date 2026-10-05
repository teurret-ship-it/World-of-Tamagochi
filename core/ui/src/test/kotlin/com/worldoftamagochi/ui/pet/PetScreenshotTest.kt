package com.worldoftamagochi.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Galleries for reviewing the rig: every expression, and a dozen genomes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w800dp-h1000dp-xhdpi")
class PetScreenshotTest {
    @Test
    fun everyExpression() {
        captureRoboImage("src/test/screenshots/pet_expressions.png") {
            Grid(Expression.entries.map { Genome.fromSeed(7) to it }) { it.second.name.lowercase() }
        }
    }

    @Test
    fun varietyOfGenomes() {
        captureRoboImage("src/test/screenshots/pet_genomes.png") {
            Grid((1L..12L).map { Genome.fromSeed(it) to Expression.HAPPY }) { "hue ${it.first.bodyHue}" }
        }
    }

    @Test
    fun midStroke() {
        captureRoboImage("src/test/screenshots/pet_squash.png") {
            Pet(
                genome = Genome.fromSeed(7),
                expression = Expression.HAPPY,
                name = "Mochi",
                animate = false,
                pose = PetPose(squash = 1f, look = Offset(1f, -1f)),
                modifier = Modifier.size(240.dp).background(Color(0xFFFFF8EE)),
            )
        }
    }

    @Composable
    private fun Grid(
        pets: List<Pair<Genome, Expression>>,
        label: (Pair<Genome, Expression>) -> String,
    ) {
        Column(Modifier.background(Color(0xFFFFF8EE)).padding(8.dp)) {
            pets.chunked(COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { pet ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Pet(pet.first, pet.second, "Mochi", animate = false, modifier = Modifier.size(180.dp))
                            Text(label(pet), fontSize = 14.sp, color = Color(0xFF2B2420))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val COLUMNS = 4
    }
}
