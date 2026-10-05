package com.worldoftamagochi.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

// *Art.kt files hold shape coordinates: they are drawing data, not logic, so
// detekt's MagicNumber rule does not apply to them (config/detekt/detekt.yml).

/** Speckled egg filling the draw area; speckle positions are fractions of its size. */
internal fun DrawScope.drawEgg(
    shell: Color,
    speckle: Color,
) {
    drawOval(color = shell, topLeft = Offset.Zero, size = Size(size.width, size.height))
    val spots =
        listOf(
            Offset(0.32f, 0.30f) to 0.07f,
            Offset(0.62f, 0.22f) to 0.05f,
            Offset(0.55f, 0.52f) to 0.09f,
            Offset(0.28f, 0.66f) to 0.06f,
            Offset(0.70f, 0.74f) to 0.05f,
        )
    spots.forEach { (center, radius) ->
        drawCircle(
            color = speckle,
            radius = size.width * radius,
            center = Offset(size.width * center.x, size.height * center.y),
        )
    }
}
