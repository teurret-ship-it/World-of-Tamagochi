package com.mymagicalpet.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mymagicalpet.sim.journal.Journal
import com.mymagicalpet.sim.journal.QuestKind
import com.mymagicalpet.sim.journal.Sticker

/** Today's three quests, the streak and the sticker count. */
@Composable
internal fun QuestCard(
    journal: Journal,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.widthIn(max = 520.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(stringResource(R.string.quests_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    pluralStringResource(R.plurals.streak_days, journal.streak.days, journal.streak.days),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            journal.quests.forEach { quest ->
                val line =
                    stringResource(
                        if (quest.done) R.string.quest_done_line else R.string.quest_line,
                        stringResource(quest.kind.textRes()),
                        quest.progress.coerceAtMost(quest.kind.target),
                        quest.kind.target,
                    )
                Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics(mergeDescendants = true) {})
            }
            Text(
                stringResource(R.string.stickers_count, journal.stickers.size, Sticker.entries.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun QuestKind.textRes(): Int =
    when (this) {
        QuestKind.FEED_3 -> R.string.quest_feed
        QuestKind.WASH_2 -> R.string.quest_wash
        QuestKind.PLAY_3 -> R.string.quest_play
        QuestKind.STROKE_10 -> R.string.quest_stroke
        QuestKind.ANSWER_5 -> R.string.quest_answer
        QuestKind.RACE_2, QuestKind.RACE_4 -> R.string.quest_race
        QuestKind.MEDAL_1 -> R.string.quest_medal
        QuestKind.TRAIN_2 -> R.string.quest_train
    }

@StringRes
internal fun Sticker.nameRes(): Int =
    when (this) {
        Sticker.FIRST_MEAL -> R.string.sticker_first_meal
        Sticker.GOURMET -> R.string.sticker_gourmet
        Sticker.SQUEAKY_CLEAN -> R.string.sticker_squeaky_clean
        Sticker.BEST_FRIENDS -> R.string.sticker_best_friends
        Sticker.FIRST_RACE -> R.string.sticker_first_race
        Sticker.RACER -> R.string.sticker_racer
        Sticker.FIRST_MEDAL -> R.string.sticker_first_medal
        Sticker.GOLDEN -> R.string.sticker_golden
        Sticker.GOLD_RUSH -> R.string.sticker_gold_rush
        Sticker.ATHLETE -> R.string.sticker_athlete
        Sticker.SHOPPER -> R.string.sticker_shopper
        Sticker.GROWING_UP -> R.string.sticker_growing_up
        Sticker.LEGEND -> R.string.sticker_legend
    }
