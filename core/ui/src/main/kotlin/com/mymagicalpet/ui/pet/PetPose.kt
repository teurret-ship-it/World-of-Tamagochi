package com.mymagicalpet.ui.pet

import androidx.compose.ui.geometry.Offset

/**
 * The animated part of the pet, separate from who it is ([com.mymagicalpet.sim.Genome])
 * and how it feels ([com.mymagicalpet.sim.Expression]).
 *
 * @property breath 0..1 phase of the breathing cycle.
 * @property blink 0 = eyes open, 1 = eyelids fully closed.
 * @property look where the pupils point, as a unit-ish vector (-1..1 on each axis).
 * @property squash 0 = resting, 1 = fully squashed (a stroke or a bounce).
 */
data class PetPose(
    val breath: Float = 0f,
    val blink: Float = 0f,
    val look: Offset = Offset.Zero,
    val squash: Float = 0f,
) {
    companion object {
        val REST = PetPose()
    }
}
