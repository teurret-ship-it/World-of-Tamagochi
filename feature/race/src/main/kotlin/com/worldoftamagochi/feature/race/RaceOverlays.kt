package com.worldoftamagochi.feature.race

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 3, 2, 1, Go! - each number pops in. */
@Composable
internal fun Countdown(value: Int) {
    val scale = remember(value) { Animatable(POP_FROM) }
    LaunchedEffect(value) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (value == 0) stringResource(R.string.race_go_banner) else value.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = MaterialTheme.typography.displayLarge,
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }.semantics { liveRegion = LiveRegionMode.Assertive },
        )
    }
}

/** The finish card: time, medal, record, rewards and the next goal. */
@Composable
internal fun FinishCard(
    summary: RaceSummary,
    controls: RaceControls,
) {
    Box(Modifier.fillMaxSize().background(SCRIM), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(32.dp), shadowElevation = 12.dp, modifier = Modifier.padding(24.dp).widthIn(max = 440.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).semantics { liveRegion = LiveRegionMode.Assertive },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.finish_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.seconds, formatSeconds(summary.finishMicros)), style = MaterialTheme.typography.displaySmall)
                if (summary.medal != null) {
                    Image(painterResource(summary.medal.imageRes()), contentDescription = null, modifier = Modifier.size(112.dp))
                    Text(stringResource(summary.medal.titleRes()), style = MaterialTheme.typography.titleLarge)
                } else {
                    Text(stringResource(R.string.finish_no_medal), style = MaterialTheme.typography.titleLarge)
                }
                if (summary.newRecord) {
                    Text(
                        stringResource(R.string.finish_new_record),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.finish_xp, summary.reward.xp), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.finish_coins, summary.reward.coins), style = MaterialTheme.typography.titleMedium)
                }
                val next = summary.nextMedal
                Text(
                    text =
                        if (next == null) {
                            stringResource(R.string.finish_all_medals)
                        } else {
                            stringResource(
                                R.string.finish_next,
                                stringResource(next.first.titleRes()),
                                stringResource(R.string.seconds, formatSeconds(next.second)),
                            )
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = controls.onExit,
                    ) { Text(stringResource(R.string.finish_done), style = MaterialTheme.typography.titleMedium) }
                    Button(
                        onClick = controls.onRetry,
                    ) { Text(stringResource(R.string.finish_retry), style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}

private const val POP_FROM = 0.4f
private val SCRIM = Color(0x99000000)
