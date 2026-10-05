package com.worldoftamagochi.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * Fredoka (SIL OFL): rounded, friendly and very legible for young readers.
 * One variable font file serves every weight.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Fredoka: FontFamily =
    FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
            Font(
                resId = R.font.fredoka,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

private val Base = Typography()

private fun TextStyle.fredoka(weight: FontWeight): TextStyle = copy(fontFamily = Fredoka, fontWeight = weight)

/** The whole game speaks in Fredoka; headings are heavier. */
internal val WotTypography: Typography =
    Typography(
        displayLarge = Base.displayLarge.fredoka(FontWeight.Bold),
        displayMedium = Base.displayMedium.fredoka(FontWeight.Bold),
        displaySmall = Base.displaySmall.fredoka(FontWeight.Bold),
        headlineLarge = Base.headlineLarge.fredoka(FontWeight.Bold),
        headlineMedium = Base.headlineMedium.fredoka(FontWeight.SemiBold),
        headlineSmall = Base.headlineSmall.fredoka(FontWeight.SemiBold),
        titleLarge = Base.titleLarge.fredoka(FontWeight.SemiBold),
        titleMedium = Base.titleMedium.fredoka(FontWeight.SemiBold),
        titleSmall = Base.titleSmall.fredoka(FontWeight.Medium),
        bodyLarge = Base.bodyLarge.fredoka(FontWeight.Normal),
        bodyMedium = Base.bodyMedium.fredoka(FontWeight.Normal),
        bodySmall = Base.bodySmall.fredoka(FontWeight.Normal),
        labelLarge = Base.labelLarge.fredoka(FontWeight.SemiBold),
        labelMedium = Base.labelMedium.fredoka(FontWeight.Medium),
        labelSmall = Base.labelSmall.fredoka(FontWeight.Medium),
    )
