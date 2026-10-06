package com.mymagicalpet.feature.home

import com.mymagicalpet.sim.CareAction
import com.mymagicalpet.sim.GrowthSource
import com.mymagicalpet.sim.journal.Deed

/** Which care actions make the creature grow; strokes, naps and waking do not (nothing to farm). */
internal fun CareAction.growthSource(answeredNeed: Boolean): GrowthSource? =
    when (this) {
        CareAction.STROKE, CareAction.NAP, CareAction.WAKE -> {
            null
        }

        CareAction.FEED, CareAction.WASH, CareAction.PLAY, CareAction.TREAT, CareAction.MEDICINE -> {
            if (answeredNeed) GrowthSource.ANSWERED_NEED else GrowthSource.CARE
        }
    }

/** What a care action counts for in quests and stickers. */
internal fun CareAction.deeds(answeredNeed: Boolean): List<Deed> {
    val deed =
        when (this) {
            CareAction.FEED -> Deed.FEED
            CareAction.WASH -> Deed.WASH
            CareAction.PLAY -> Deed.PLAY
            CareAction.STROKE -> Deed.STROKE
            CareAction.TREAT -> Deed.TREAT
            CareAction.NAP, CareAction.WAKE, CareAction.MEDICINE -> null
        }
    return listOfNotNull(deed, Deed.ANSWERED_NEED.takeIf { answeredNeed })
}
