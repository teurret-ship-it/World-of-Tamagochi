package com.worldoftamagochi.feature.race

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldoftamagochi.api.LeaderboardEntry
import com.worldoftamagochi.data.GameRepository
import com.worldoftamagochi.network.OnlineRacing
import com.worldoftamagochi.sim.race.Discipline
import com.worldoftamagochi.sim.race.Medal
import com.worldoftamagochi.sim.race.MedalTimes
import com.worldoftamagochi.sim.race.Track
import com.worldoftamagochi.sim.race.Tracks
import com.worldoftamagochi.ui.R as UiR

@Composable
fun TrackSelectRoute(
    repository: GameRepository,
    online: OnlineRacing?,
    onRace: (String) -> Unit,
    onBack: () -> Unit,
) {
    val game by repository.game.collectAsStateWithLifecycle(initialValue = null)
    val medals = remember { Tracks.ALL.associateWith { MedalTimes.of(it) } }
    var boards by remember { mutableStateOf<Map<String, List<LeaderboardEntry>>>(emptyMap()) }
    LaunchedEffect(online) {
        online ?: return@LaunchedEffect
        online.flush() // runs raced offline go up first, so the board includes them
        boards =
            Tracks.ALL
                .mapNotNull { track -> online.leaderboard(track.id, "daily")?.let { track.id to it.take(TOP_SHOWN) } }
                .toMap()
    }
    val cards =
        Tracks.ALL.map {
            TrackCard(it, medals.getValue(it), game?.records?.get(it.id)?.finishMicros, boards[it.id].orEmpty())
        }
    TrackSelectScreen(cards, onRace, onBack)
}

@Composable
fun TrackSelectScreen(
    cards: List<TrackCard>,
    onRace: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium) }
            }
            Text(
                stringResource(R.string.races_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.races_subtitle), style = MaterialTheme.typography.titleMedium)
            cards.groupBy { it.track.discipline }.forEach { (discipline, group) ->
                SectionHeader(discipline)
                group.forEach { TrackCardView(it, onRace) }
            }
        }
    }
}

@Composable
private fun SectionHeader(discipline: Discipline) {
    val (title, hint, image) =
        when (discipline) {
            Discipline.SPRINT -> Triple(R.string.section_sprint, R.string.section_sprint_hint, UiR.drawable.race_boost)
            Discipline.AGILITY -> Triple(R.string.section_agility, R.string.section_agility_hint, UiR.drawable.race_tyre)
        }
    Row(
        Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(image), contentDescription = null, modifier = Modifier.size(40.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(hint), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TrackCardView(
    card: TrackCard,
    onRace: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(card.track.titleRes()), style = MaterialTheme.typography.headlineMedium)
            Text(
                text =
                    card.bestMicros?.let { stringResource(R.string.track_best, stringResource(R.string.seconds, formatSeconds(it))) }
                        ?: stringResource(R.string.track_no_best),
                style = MaterialTheme.typography.titleMedium,
            )
            if (card.today.isNotEmpty()) TodayBoard(card.today)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Medal.entries.forEach { medal ->
                    val earned = card.bestMedal?.let { it >= medal } == true
                    Image(
                        painter = painterResource(medal.imageRes()),
                        contentDescription = stringResource(medal.titleRes()),
                        modifier = Modifier.size(44.dp).graphicsLayer { alpha = if (earned) 1f else LOCKED_ALPHA },
                    )
                }
                Column(Modifier.weight(1f)) {}
                Button(
                    onClick = { onRace(card.track.id) },
                ) { Text(stringResource(R.string.race_go), style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
private fun TodayBoard(entries: List<LeaderboardEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.online_today),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        entries.forEach { entry ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("#${entry.rank}", style = MaterialTheme.typography.bodyLarge, fontWeight = if (entry.you) FontWeight.Bold else null)
                Text(
                    entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (entry.you) FontWeight.Bold else null,
                )
                Text(stringResource(R.string.seconds, formatSeconds(entry.finishMicros)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private const val LOCKED_ALPHA = 0.3f
private const val TOP_SHOWN = 3
