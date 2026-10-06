package com.mymagicalpet.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.mymagicalpet.ui.R as UiR

/** The pet is away: on a journey (rescue steps) or at grandma's (vacation). */
@Composable
internal fun AwayScreen(
    state: HomeUiState,
    onAway: (AwayAction) -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar(state, onSettings)
        Column(
            Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.onJourney) Journey(state, onAway) else Vacation(state, onAway)
        }
    }
}

@Composable
private fun Journey(
    state: HomeUiState,
    onAway: (AwayAction) -> Unit,
) {
    Image(painterResource(UiR.drawable.ui_journey), contentDescription = null, modifier = Modifier.size(120.dp))
    Text(stringResource(R.string.journey_title, state.name), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Text(stringResource(R.string.journey_text, state.name), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    val steps =
        listOf(
            stringResource(R.string.journey_step_1),
            stringResource(R.string.journey_step_2),
            stringResource(R.string.journey_step_3, state.name),
        )
    steps.forEachIndexed { i, step ->
        val done = i < state.rescueSteps
        val next = i == state.rescueSteps
        Button(
            onClick = { onAway(AwayAction.Rescue) },
            enabled = next,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (done) "✓ $step" else step, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun Vacation(
    state: HomeUiState,
    onAway: (AwayAction) -> Unit,
) {
    val until =
        state.vacationUntilEpochMillis
            ?.let {
                Instant
                    .ofEpochMilli(
                        it,
                    ).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
            }.orEmpty()
    Image(painterResource(UiR.drawable.ui_home), contentDescription = null, modifier = Modifier.size(120.dp))
    Text(stringResource(R.string.vacation_title, state.name), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Text(stringResource(R.string.vacation_text, until), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    OutlinedButton(onClick = { onAway(AwayAction.EndVacation) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(stringResource(R.string.vacation_end), style = MaterialTheme.typography.titleMedium)
    }
}
