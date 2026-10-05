package com.worldoftamagochi.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldoftamagochi.designsystem.WotTheme

/**
 * Iteration 0 home: the game's look and a living egg. The pet itself
 * (vector rig, needs, care actions) arrives in iterations 1-3.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            RockingEgg(animate = animate)
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.home_coming_soon),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RockingEgg(animate: Boolean) {
    val shell = MaterialTheme.colorScheme.primaryContainer
    val speckle = MaterialTheme.colorScheme.secondary
    val description = stringResource(R.string.home_egg_description)
    val tilt =
        if (animate) {
            val transition = rememberInfiniteTransition(label = "egg")
            val angle by transition.animateFloat(
                initialValue = -EGG_MAX_TILT_DEGREES,
                targetValue = EGG_MAX_TILT_DEGREES,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(EGG_ROCK_MILLIS, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "tilt",
            )
            angle
        } else {
            0f
        }
    Canvas(
        modifier =
            Modifier
                .size(width = 160.dp, height = 200.dp)
                .semantics { contentDescription = description },
    ) {
        rotate(degrees = tilt, pivot = Offset(size.width / 2, size.height)) {
            drawEgg(shell, speckle)
        }
    }
}

private const val EGG_MAX_TILT_DEGREES = 6f
private const val EGG_ROCK_MILLIS = 900

@Composable
internal fun HomeScreenPreviewContent(darkTheme: Boolean) {
    WotTheme(darkTheme = darkTheme) { HomeScreen(animate = false) }
}
