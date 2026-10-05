package com.worldoftamagochi.ui.time

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.ui.R

/** A time with big earlier/later buttons, in half-hour steps: no fiddly clock dial. */
@Composable
fun TimeStepper(
    label: String,
    minuteOfDay: Int,
    onChange: (Int) -> Unit,
) {
    val earlier = stringResource(R.string.time_earlier, label)
    val later = stringResource(R.string.time_later, label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = { onChange(shiftTime(minuteOfDay, -STEP_MINUTES)) },
            modifier = Modifier.size(48.dp).semantics { contentDescription = earlier },
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
        }
        Text(
            clockTime(minuteOfDay),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(112.dp),
        )
        FilledTonalIconButton(
            onClick = { onChange(shiftTime(minuteOfDay, STEP_MINUTES)) },
            modifier = Modifier.size(48.dp).semantics { contentDescription = later },
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

private const val STEP_MINUTES = 30
