package com.mymagicalpet.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mymagicalpet.ui.R as UiR

@Composable
internal fun CareBar(
    state: HomeUiState,
    callbacks: CareCallbacks,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FoodButton(callbacks, Modifier.weight(1f))
        CareButton(
            UiR.drawable.item_soap,
            stringResource(R.string.care_wash),
            callbacks.onWash,
            Modifier.weight(1f),
            selected = state.washing,
        )
        CareButton(UiR.drawable.item_ball, stringResource(R.string.care_play), callbacks.onPlay, Modifier.weight(1f))
        if (state.sick) {
            // Sick: free medicine takes the treat's place, where a child looks first.
            CareButton(
                UiR.drawable.item_medicine,
                stringResource(R.string.care_medicine),
                callbacks.onMedicine,
                Modifier.weight(1f),
                selected = true,
            )
        } else {
            CareButton(
                UiR.drawable.item_cookie,
                stringResource(R.string.care_treat),
                callbacks.onTreat,
                Modifier.weight(1f),
                badge = state.treatsLeft,
            )
        }
        if (state.asleep) {
            CareButton(UiR.drawable.item_light, stringResource(R.string.care_lights_on), callbacks.onLights, Modifier.weight(1f))
        } else {
            CareButton(UiR.drawable.item_moon, stringResource(R.string.care_lights_off), callbacks.onLights, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FoodButton(
    callbacks: CareCallbacks,
    modifier: Modifier,
) {
    val drag = remember { FoodDrag() }
    CareButton(
        image = UiR.drawable.item_apple,
        label = stringResource(R.string.care_feed),
        onClick = callbacks.onFeed,
        modifier =
            modifier
                .onGloballyPositioned { drag.origin = it.boundsInRoot().center }
                .pointerInput(Unit) {
                    // Drag the apple to the pet's mouth: the Pou gesture. Tapping still works.
                    detectDragGestures(
                        onDragStart = {
                            drag.at = drag.origin
                            callbacks.onFoodDrag(drag.at)
                        },
                        onDragEnd = {
                            callbacks.onFoodDrag(null)
                            callbacks.onFoodDrop(drag.at)
                        },
                        onDragCancel = { callbacks.onFoodDrag(null) },
                    ) { change, delta ->
                        change.consume()
                        drag.at += delta
                        callbacks.onFoodDrag(drag.at)
                    }
                },
    )
}

@Composable
private fun CareButton(
    @DrawableRes image: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    badge: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val description = badge?.let { pluralStringResource(R.plurals.treats_left, it, label, it) } ?: label
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (selected) colors.secondaryContainer else colors.surfaceVariant,
        border = if (selected) BorderStroke(3.dp, colors.secondary) else null,
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
            },
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier =
                        Modifier.size(52.dp).graphicsLayer {
                            if (selected) rotationZ = SELECTED_TILT
                            if (badge == 0) alpha = SPENT_ALPHA
                        },
                )
                badge?.let { Badge(it, Modifier.align(Alignment.TopEnd)) }
            }
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Where the dragged food started and where it is now, in root coordinates. */
private class FoodDrag {
    var origin: Offset = Offset.Zero
    var at: Offset = Offset.Zero
}

@Composable
private fun Badge(
    count: Int,
    modifier: Modifier,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary, modifier = modifier.size(22.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiary)
        }
    }
}

private const val SELECTED_TILT = -12f
private const val SPENT_ALPHA = 0.45f
