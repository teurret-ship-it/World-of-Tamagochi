package com.worldoftamagochi.ui.pet

import androidx.compose.ui.graphics.Color
import com.worldoftamagochi.sim.Genome

/** Colours derived from a [Genome]. Saturation and value are fixed so every pet stays soft and readable. */
internal data class PetColors(
    val body: Color,
    val bodyShade: Color,
    val pattern: Color,
    val outline: Color,
)

internal fun Genome.colors(): PetColors =
    PetColors(
        body = Color.hsv(bodyHue.toFloat(), BODY_SATURATION, BODY_VALUE),
        bodyShade = Color.hsv(bodyHue.toFloat(), BODY_SATURATION + SHADE_EXTRA_SATURATION, BODY_VALUE - SHADE_DARKER),
        pattern = Color.hsv(patternHue.toFloat(), PATTERN_SATURATION, PATTERN_VALUE),
        outline = Color.hsv(bodyHue.toFloat(), OUTLINE_SATURATION, OUTLINE_VALUE),
    )

private const val BODY_SATURATION = 0.42f
private const val BODY_VALUE = 0.97f
private const val SHADE_EXTRA_SATURATION = 0.12f
private const val SHADE_DARKER = 0.12f
private const val PATTERN_SATURATION = 0.55f
private const val PATTERN_VALUE = 0.82f
private const val OUTLINE_SATURATION = 0.6f
private const val OUTLINE_VALUE = 0.3f
