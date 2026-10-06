package com.mymagicalpet.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DayColors: ColorScheme =
    lightColorScheme(
        primary = Palette.PeachDeep,
        onPrimary = Palette.Cream,
        primaryContainer = Palette.Peach,
        onPrimaryContainer = Palette.Ink,
        secondary = Palette.MintDeep,
        secondaryContainer = Palette.Mint,
        onSecondaryContainer = Palette.Ink,
        tertiary = Palette.SkyDeep,
        tertiaryContainer = Palette.Sky,
        background = Palette.Cream,
        onBackground = Palette.Ink,
        surface = Palette.Cream,
        onSurface = Palette.Ink,
        error = Palette.Error,
    )

private val NightColors: ColorScheme =
    darkColorScheme(
        primary = Palette.Peach,
        onPrimary = Palette.Ink,
        primaryContainer = Palette.PeachDeep,
        onPrimaryContainer = Palette.Cream,
        secondary = Palette.Mint,
        secondaryContainer = Palette.MintDeep,
        onSecondaryContainer = Palette.Cream,
        tertiary = Palette.Sky,
        tertiaryContainer = Palette.SkyDeep,
        background = Palette.Night,
        onBackground = Palette.Moon,
        surface = Palette.NightSurface,
        onSurface = Palette.Moon,
        error = Palette.ErrorNight,
    )

/** The game's theme. Dark mode is the pet's "night room", not just inverted colours. */
@Composable
fun MagicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightColors else DayColors,
        typography = MagicTypography,
        content = content,
    )
}
