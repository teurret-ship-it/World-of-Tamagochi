package com.mymagicalpet.sim

/**
 * Version of the game rules. The app and the server must agree on it: a replay
 * or a pet state produced under other rules is rejected, not reinterpreted.
 */
object SimVersion {
    const val CURRENT: Int = 1
}
