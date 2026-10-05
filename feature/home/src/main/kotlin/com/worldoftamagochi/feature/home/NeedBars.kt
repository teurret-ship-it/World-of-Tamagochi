package com.worldoftamagochi.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.designsystem.NeedColors
import com.worldoftamagochi.sim.Need

/** The five needs as labelled bars; the most urgent one is highlighted. */
@Composable
internal fun NeedBars(
    needs: Map<Need, Int>,
    urgent: Need?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        needs.forEach { (need, value) -> NeedBar(need, value, highlighted = need == urgent) }
    }
}

@Composable
private fun NeedBar(
    need: Need,
    value: Int,
    highlighted: Boolean,
) {
    val label = stringResource(need.labelRes())
    val description = stringResource(R.string.need_level, label, value)
    val container = if (highlighted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (highlighted) FontWeight.Black else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(LABEL_WEIGHT),
            )
            LinearProgressIndicator(
                progress = { value / FULL },
                color = need.color(),
                trackColor = MaterialTheme.colorScheme.background,
                drawStopIndicator = {},
                modifier = Modifier.weight(BAR_WEIGHT).height(14.dp).clip(RoundedCornerShape(7.dp)),
            )
        }
    }
}

private fun Need.labelRes(): Int =
    when (this) {
        Need.SATIETY -> R.string.need_satiety
        Need.ENERGY -> R.string.need_energy
        Need.HYGIENE -> R.string.need_hygiene
        Need.HAPPINESS -> R.string.need_happiness
        Need.HEALTH -> R.string.need_health
    }

/** One colour per need, consistent everywhere the need appears. */
internal fun Need.color(): Color =
    when (this) {
        Need.SATIETY -> NeedColors.Tummy
        Need.ENERGY -> NeedColors.Energy
        Need.HYGIENE -> NeedColors.Clean
        Need.HAPPINESS -> NeedColors.Fun
        Need.HEALTH -> NeedColors.Health
    }

internal fun Need.label(): Int = labelRes()

private const val FULL = 100f
private const val LABEL_WEIGHT = 0.9f
private const val BAR_WEIGHT = 2f
