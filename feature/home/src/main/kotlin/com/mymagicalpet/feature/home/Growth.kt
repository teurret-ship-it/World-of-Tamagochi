package com.mymagicalpet.feature.home

import com.mymagicalpet.sim.CareAction
import com.mymagicalpet.sim.GrowthSource

/** Which care actions make the creature grow; strokes, naps and waking do not (nothing to farm). */
internal fun CareAction.growthSource(answeredNeed: Boolean): GrowthSource? =
    when (this) {
        CareAction.STROKE, CareAction.NAP, CareAction.WAKE -> {
            null
        }

        CareAction.FEED, CareAction.WASH, CareAction.PLAY, CareAction.TREAT -> {
            if (answeredNeed) GrowthSource.ANSWERED_NEED else GrowthSource.CARE
        }
    }
