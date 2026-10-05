package com.worldoftamagochi.ui.pet

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.worldoftamagochi.sim.Genome

/** An egg in the colours of the pet inside; see [drawEgg] for the parameters. */
@Composable
fun Egg(
    genome: Genome,
    description: String,
    modifier: Modifier = Modifier,
    wobble: Float = 0f,
    crack: Float = 0f,
    open: Float = 0f,
) {
    Canvas(modifier.semantics { contentDescription = description }) {
        drawEgg(genome, wobble, crack, open)
    }
}
