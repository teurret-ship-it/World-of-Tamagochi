package com.worldoftamagochi.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.ui.pet.Pet

/**
 * The welcome-back card: the pet, how long the player was gone and what
 * changed. Friendly, never guilt-tripping (CLAUDE.md section 2).
 */
@Composable
internal fun AwayCard(
    state: HomeUiState,
    away: AwaySummary,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(SCRIM), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(32.dp), shadowElevation = 12.dp, modifier = Modifier.padding(24.dp).widthIn(max = 420.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.away_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Pet(state.genome, state.expression, state.name, animate = false, wearing = state.wearing, modifier = Modifier.size(140.dp))
                val hours = away.minutes / MINUTES_PER_HOUR
                Text(
                    text =
                        if (hours == 0L) {
                            pluralStringResource(R.plurals.away_minutes, away.minutes.toInt(), away.minutes.toInt())
                        } else {
                            pluralStringResource(R.plurals.away_hours, hours.toInt(), hours.toInt())
                        },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                away.changes.forEach { (need, points) ->
                    Text(
                        stringResource(R.string.away_change, stringResource(need.label()), points),
                        style = MaterialTheme.typography.titleLarge,
                        color = need.color(),
                    )
                }
                Button(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.away_ok), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private val SCRIM = Color(0x99000000)
private const val MINUTES_PER_HOUR = 60L
