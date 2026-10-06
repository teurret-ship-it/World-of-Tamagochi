package com.mymagicalpet.sim

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Time that passed between two observations of the clock.
 *
 * The pet simulation only ever moves forward. A clock that goes backwards
 * (time zone change, a player winding the phone clock back to undo a missed
 * meal) yields zero elapsed time and is flagged, never a negative duration
 * that could "un-starve" a pet.
 */
data class Elapsed(
    val duration: Duration,
    val clockWentBackwards: Boolean,
) {
    companion object {
        fun between(
            previousEpochMillis: Long,
            nowEpochMillis: Long,
        ): Elapsed {
            val delta = nowEpochMillis - previousEpochMillis
            return if (delta < 0) {
                Elapsed(Duration.ZERO, clockWentBackwards = true)
            } else {
                Elapsed(delta.milliseconds, clockWentBackwards = false)
            }
        }
    }
}
