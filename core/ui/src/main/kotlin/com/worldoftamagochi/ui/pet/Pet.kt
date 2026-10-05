package com.worldoftamagochi.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.shop.Catalog
import com.worldoftamagochi.ui.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * A living pet: breathes, blinks at irregular intervals and looks where it is
 * told to. With [animate] false it renders [pose] exactly, which keeps
 * screenshot tests deterministic.
 */
@Composable
fun Pet(
    genome: Genome,
    expression: Expression,
    name: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    look: Offset = Offset.Zero,
    squash: Float = 0f,
    pose: PetPose = PetPose.REST,
    wearing: Collection<String> = emptyList(),
) {
    val description = stringResource(R.string.pet_description, name, stringResource(expression.descriptionRes()))
    val live = if (animate) livePose(expression, look, squash) else pose
    val outfit = rememberOutfit(wearing)
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        drawPet(genome, expression, live, outfit)
    }
}

/** Loads the images of the worn items, in drawing order (neck, face, head). */
@Composable
private fun rememberOutfit(wearing: Collection<String>): List<WornItem> {
    val items =
        wearing
            .mapNotNull { id -> Catalog.byId(id)?.let { item -> Wearables.byId(id)?.let { it to item.slot } } }
            .sortedBy { (_, slot) -> Wearables.layer(slot) }
    return items.map { (wearable, _) ->
        key(wearable.id) { WornItem(wearable.placement, ImageBitmap.imageResource(wearable.drawable)) }
    }
}

@Composable
private fun livePose(
    expression: Expression,
    look: Offset,
    squash: Float,
): PetPose {
    val asleep = expression == Expression.ASLEEP
    val transition = rememberInfiniteTransition(label = "pet")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(if (asleep) SLEEP_BREATH_MILLIS else BREATH_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breath",
    )
    val blink = remember { Animatable(0f) }
    LaunchedEffect(asleep) {
        if (asleep) return@LaunchedEffect
        while (true) {
            // Irregular intervals read as alive; a metronome reads as a machine.
            delay(Random.nextLong(BLINK_MIN_GAP_MILLIS, BLINK_MAX_GAP_MILLIS))
            blink.animateTo(1f, tween(BLINK_HALF_MILLIS))
            blink.animateTo(0f, tween(BLINK_HALF_MILLIS))
        }
    }
    return PetPose(breath = breath, blink = blink.value, look = look, squash = squash)
}

private fun Expression.descriptionRes(): Int =
    when (this) {
        Expression.HAPPY -> R.string.expression_happy
        Expression.CONTENT -> R.string.expression_content
        Expression.HUNGRY -> R.string.expression_hungry
        Expression.SLEEPY -> R.string.expression_sleepy
        Expression.DIRTY -> R.string.expression_dirty
        Expression.SAD -> R.string.expression_sad
        Expression.SICK -> R.string.expression_sick
        Expression.ASLEEP -> R.string.expression_asleep
    }

private const val BREATH_MILLIS = 1_600
private const val SLEEP_BREATH_MILLIS = 2_600
private const val BLINK_MIN_GAP_MILLIS = 2_200L
private const val BLINK_MAX_GAP_MILLIS = 5_500L
private const val BLINK_HALF_MILLIS = 70
