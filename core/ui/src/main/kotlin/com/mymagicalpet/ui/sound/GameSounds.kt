package com.mymagicalpet.ui.sound

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The game's sound vocabulary. Each cue is a slice of the SND sound sprite
 * (kit 03), in milliseconds from the start of the sprite (ADR-006).
 */
enum class Sfx(
    val startMillis: Int,
    val endMillis: Int,
) {
    TAP(30_000, 30_030),
    SELECT(18_000, 18_200),
    BUTTON(0, 400),
    SWIPE(20_000, 20_140),
    TOGGLE_ON(42_000, 42_270),
    TOGGLE_OFF(40_000, 40_220),
    REWARD(9_000, 9_750),
    POSITIVE(46_000, 46_620),
    CELEBRATION(4_000, 5_060),
    CAUTION(2_000, 2_250),
}

/** Plays sound cues. Implementations must never block the caller. */
fun interface GameSounds {
    fun play(sfx: Sfx)

    companion object {
        /** Silence: for previews, tests and when the player turns sound off. */
        val None = GameSounds { }
    }
}

val LocalGameSounds = staticCompositionLocalOf { GameSounds.None }
