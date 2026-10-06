package com.mymagicalpet.ui.pet

import androidx.annotation.StringRes
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
