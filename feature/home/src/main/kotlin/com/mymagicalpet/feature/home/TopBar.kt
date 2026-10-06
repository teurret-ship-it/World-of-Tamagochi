package com.mymagicalpet.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymagicalpet.ui.R as UiR

/** Level with its progress ring on the left, the pet's name, coins on the right. */
@Composable
internal fun TopBar(
    state: HomeUiState,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val levelLabel = stringResource(R.string.level_badge, state.level)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).clearAndSetSemantics { contentDescription = levelLabel },
        ) {
            CircularProgressIndicator(
                progress = { state.levelProgress },
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Text(state.level.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = state.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val coinsLabel = pluralStringResource(R.plurals.coins, state.coins.toInt(), state.coins.toInt())
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clearAndSetSemantics { contentDescription = coinsLabel },
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(painterResource(UiR.drawable.reward_coin), contentDescription = null, modifier = Modifier.size(32.dp))
                Text(state.coins.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
            Image(
                painterResource(UiR.drawable.ui_gear),
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
