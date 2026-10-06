package com.mymagicalpet.ui.pet

import androidx.annotation.StringRes
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Species
import com.mymagicalpet.ui.R

/** The name of a species, as players read it. */
@StringRes
fun Species.nameRes(): Int =
    when (this) {
        Species.DRAGON -> R.string.species_dragon
        Species.GRIFFIN -> R.string.species_griffin
        Species.UNICORN -> R.string.species_unicorn
        Species.PHOENIX -> R.string.species_phoenix
        Species.KITSUNE -> R.string.species_kitsune
    }

/** The name of a life stage, as players read it. */
@StringRes
fun LifeStage.labelRes(): Int =
    when (this) {
        LifeStage.EGG -> R.string.stage_egg
        LifeStage.BABY -> R.string.stage_baby
        LifeStage.CHILD -> R.string.stage_child
        LifeStage.JUNIOR -> R.string.stage_junior
        LifeStage.TEEN -> R.string.stage_teen
        LifeStage.ADULT -> R.string.stage_adult
        LifeStage.MAJESTIC -> R.string.stage_majestic
    }

/** How grown-up the rig draws a stage: 0 hatchling .. 1 majestic. */
fun LifeStage.growth(): Float =
    (ordinal - LifeStage.BABY.ordinal).coerceAtLeast(0) / (LifeStage.MAJESTIC.ordinal - LifeStage.BABY.ordinal).toFloat()
