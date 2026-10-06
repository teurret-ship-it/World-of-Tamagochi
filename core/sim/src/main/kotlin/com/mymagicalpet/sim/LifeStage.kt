package com.mymagicalpet.sim

/**
 * Life stages in order, from the egg to the majestic legend (ADR-011).
 * Names are part of the save format: add new stages, never rename them.
 * Players see them as: egg, hatchling, baby, junior, young, adult, majestic.
 */
enum class LifeStage {
    EGG,
    BABY,
    CHILD,
    JUNIOR,
    TEEN,
    ADULT,
    MAJESTIC,
}
