package com.worldoftamagochi.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.ui.R as UiR

/** The ways out of the room: races and the shop, big, bright and always visible. */
@Composable
internal fun PlayButtons(
    onOpenRaces: () -> Unit,
    onOpenShop: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton(
            label = stringResource(R.string.races_button),
            image = UiR.drawable.medal_author,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenRaces,
            modifier = Modifier.weight(1f),
        )
        BigButton(
            label = stringResource(R.string.shop_button),
            image = UiR.drawable.ui_shop,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onOpenShop,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BigButton(
    label: String,
    @DrawableRes image: Int,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = container, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(image), contentDescription = null, modifier = Modifier.size(44.dp))
            Text(label, style = MaterialTheme.typography.headlineSmall, color = content, maxLines = 1)
        }
    }
}
