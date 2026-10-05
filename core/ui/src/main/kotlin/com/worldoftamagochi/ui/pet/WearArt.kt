package com.worldoftamagochi.ui.pet

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.worldoftamagochi.sim.shop.Slot
import com.worldoftamagochi.ui.R
import kotlin.math.roundToInt

/**
 * Where a cosmetic sits on the rig, in fractions of the body rect so it
 * follows every body shape, breath and squash.
 *
 * @property anchor the point on the body the item is attached to.
 * @property width the item's width as a fraction of the body width.
 * @property pivot the point of the image that lands on [anchor] (fractions of the image).
 * @property collar a band around the neck drawn under the item (bells and pendants hang on it).
 * @property collarWidth the band's thickness as a fraction of the body width.
 */
data class Placement(
    val anchor: Offset,
    val width: Float,
    val pivot: Offset = Offset(0.5f, 0.5f),
    val rotation: Float = 0f,
    val collar: Color? = null,
    val collarWidth: Float = 0.07f,
)

/** The art for a shop item: a Fluent Emoji 3D image and where it goes. */
data class Wearable(
    val id: String,
    @param:DrawableRes val drawable: Int,
    val placement: Placement,
)

object Wearables {
    private val RED_COLLAR = Color(0xFFE53950)
    private val GOLD_COLLAR = Color(0xFFF2B33D)
    private val SCARF_RED = Color(0xFFE8344E)

    val ALL: List<Wearable> =
        listOf(
            Wearable("ribbon", R.drawable.wear_ribbon, Placement(Offset(0.76f, 0.06f), 0.4f, rotation = 18f)),
            Wearable("blossom", R.drawable.wear_blossom, Placement(Offset(0.24f, 0.08f), 0.34f, rotation = -14f)),
            Wearable("bell", R.drawable.wear_bell, Placement(Offset(0.5f, 0.84f), 0.2f, Offset(0.5f, 0.12f), collar = RED_COLLAR)),
            Wearable("glasses", R.drawable.wear_glasses, Placement(Offset(0.5f, 0.4f), 0.8f, Offset(0.5f, 0.42f))),
            Wearable("cap", R.drawable.wear_cap, Placement(Offset(0.48f, 0.14f), 0.86f, Offset(0.42f, 0.78f))),
            Wearable(
                "scarf",
                R.drawable.wear_scarf,
                Placement(Offset(0.27f, 0.84f), 0.36f, Offset(0.5f, 0.3f), rotation = -10f, collar = SCARF_RED, collarWidth = 0.14f),
            ),
            Wearable("butterfly", R.drawable.wear_butterfly, Placement(Offset(0.7f, 0.02f), 0.38f, rotation = 14f)),
            Wearable("sunglasses", R.drawable.wear_sunglasses, Placement(Offset(0.5f, 0.4f), 0.84f, Offset(0.5f, 0.45f))),
            Wearable("grad_cap", R.drawable.wear_grad_cap, Placement(Offset(0.5f, 0.12f), 0.84f, Offset(0.5f, 0.66f))),
            Wearable("goggles", R.drawable.wear_goggles, Placement(Offset(0.5f, 0.41f), 0.8f, Offset(0.5f, 0.68f))),
            Wearable("headphones", R.drawable.wear_headphones, Placement(Offset(0.5f, 0.44f), 1.32f, Offset(0.5f, 0.74f))),
            Wearable("gem", R.drawable.wear_gem, Placement(Offset(0.5f, 0.84f), 0.2f, Offset(0.5f, 0.2f), collar = GOLD_COLLAR)),
            Wearable("top_hat", R.drawable.wear_top_hat, Placement(Offset(0.5f, 0.12f), 0.62f, Offset(0.5f, 0.86f))),
            Wearable("crown", R.drawable.wear_crown, Placement(Offset(0.5f, 0.1f), 0.62f, Offset(0.5f, 0.84f))),
        )

    fun byId(id: String): Wearable? = ALL.firstOrNull { it.id == id }

    /** Drawing order: neck under the face items, hats on top. */
    internal fun layer(slot: Slot): Int =
        when (slot) {
            Slot.NECK -> 0
            Slot.FACE -> 1
            Slot.HEAD -> 2
        }
}

/** A worn item ready to draw. */
class WornItem(
    val placement: Placement,
    val image: ImageBitmap,
)

internal fun DrawScope.drawOutfit(
    outfit: List<WornItem>,
    body: Rect,
) {
    outfit.forEach { item ->
        val p = item.placement
        p.collar?.let { drawCollar(body, it, p.collarWidth) }
        val width = body.width * p.width
        val height = width * item.image.height / item.image.width
        val at = body.at(p.anchor)
        val topLeft = Offset(at.x - width * p.pivot.x, at.y - height * p.pivot.y)
        rotate(p.rotation, pivot = at) {
            drawImage(
                image = item.image,
                dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                dstSize = IntSize(width.roundToInt(), height.roundToInt()),
            )
        }
    }
}

/** A soft band around the neck, clipped to the body so it wraps the round shape. */
private fun DrawScope.drawCollar(
    body: Rect,
    color: Color,
    width: Float,
) {
    clipPath(bodyPath(body)) {
        // The neck line sits under the mouth and sags a little: it reads as round.
        val sag = body.height * 0.05f
        val left = body.at(Offset(-0.05f, 0.78f))
        val right = body.at(Offset(1.05f, 0.78f))
        val path =
            Path().apply {
                moveTo(left.x, left.y)
                quadraticTo(body.center.x, left.y + sag * 2f, right.x, right.y)
            }
        drawPath(path, color, style = Stroke(width = body.width * width, cap = StrokeCap.Round))
        drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(width = body.width * 0.018f, cap = StrokeCap.Round))
    }
}
