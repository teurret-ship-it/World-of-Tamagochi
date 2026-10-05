package com.worldoftamagochi.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Base = Typography()

/** Rounded, heavier headings read as playful; body text stays the platform default. */
internal val WotTypography: Typography =
    Base.copy(
        displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Black),
        headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
