package com.mymagicalpet.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import com.mymagicalpet.sim.Species
import kotlin.math.roundToInt

// *Art.kt files hold shape coordinates: they are drawing data, not logic, so
// detekt's MagicNumber rule does not apply to them (config/detekt/detekt.yml).
// Positions are fractions of the body rect; [growth] (0 = hatchling, 1 =
// majestic) lengthens wings, horns, plumes and tails, and adds fox tails.

/** Parts drawn behind the body: wings, tails, ears, spikes. */
internal fun DrawScope.drawSpeciesBehind(
    species: Species,
    body: Rect,
    colors: PetColors,
    growth: Float,
) {
    val k = 0.6f + 0.7f * growth
    when (species) {
        Species.DRAGON -> {
            drawSpikes(body, colors, k)
            sides { drawBatWing(body, colors, it, k) }
            drawDragonTail(body, colors, k)
        }

        Species.GRIFFIN -> {
            sides { drawFeatherWing(body, Feathers(colors.bodyShade, colors.pattern), colors.outline, it, k) }
            drawTuftTail(body, colors, k)
            sides { drawEar(body, it, CREAM, colors.outline, EarSpec(height = 0.2f, width = 0.1f, spread = 0.3f)) }
        }

        Species.UNICORN -> {
            drawRainbowTail(body, colors, k)
            sides { drawEar(body, it, colors.bodyShade, colors.outline, EarSpec(height = 0.2f, width = 0.11f, spread = 0.26f)) }
        }

        Species.PHOENIX -> {
            drawPlumeTail(body, colors, k)
            sides { drawFeatherWing(body, Feathers(FLAME_CORAL, FLAME_GOLD), colors.outline, it, k) }
        }

        Species.KITSUNE -> {
            drawFoxTails(body, colors, growth, k)
            sides { drawFoxEar(body, colors, it) }
        }
    }
}

/** Parts drawn over the body, under the face: feathered heads, muzzles, manes. */
internal fun DrawScope.drawSpeciesOverBody(
    species: Species,
    body: Rect,
    colors: PetColors,
) {
    when (species) {
        Species.GRIFFIN -> drawFeatheredHead(body)
        Species.KITSUNE -> drawMuzzle(body)
        Species.DRAGON -> drawBellyPlates(body, colors)
        Species.UNICORN, Species.PHOENIX -> Unit
    }
}

/** Parts drawn on top of the face: horns, beaks, crests, manes. */
internal fun DrawScope.drawSpeciesFront(
    species: Species,
    body: Rect,
    colors: PetColors,
    growth: Float,
) {
    val k = 0.6f + 0.7f * growth
    when (species) {
        Species.DRAGON -> {
            sides { drawHorn(body, colors, it, k) }
        }

        Species.GRIFFIN -> {
            drawBeak(body, colors, size = 1f)
        }

        Species.UNICORN -> {
            drawMane(body, k)
            drawUnicornHorn(body, colors, k)
        }

        Species.PHOENIX -> {
            drawCrest(body, colors, k)
            drawBeak(body, colors, size = 0.75f)
        }

        Species.KITSUNE -> {
            drawForeheadMark(body, colors)
        }
    }
}

private inline fun sides(draw: (Float) -> Unit) {
    draw(-1f)
    draw(1f)
}

private fun DrawScope.outlined(
    path: Path,
    fill: Color,
    outline: Color,
    body: Rect,
) {
    drawPath(path, fill)
    drawPath(path, outline, style = Stroke(width = body.width * 0.022f))
}

// --- Dragon -------------------------------------------------------------

private fun DrawScope.drawBatWing(
    body: Rect,
    colors: PetColors,
    side: Float,
    k: Float,
) {
    val root = body.at(Offset(0.5f + side * 0.36f, 0.4f))
    val w = body.width * k
    val h = body.height * k
    val tip = Offset(root.x + side * w * 0.5f, root.y - h * 0.42f)
    val points =
        listOf(
            Offset(root.x + side * w * 0.46f, root.y + h * 0.04f),
            Offset(root.x + side * w * 0.3f, root.y + h * 0.12f),
            Offset(root.x + side * w * 0.12f, root.y + h * 0.2f),
        )
    val path =
        Path().apply {
            moveTo(root.x, root.y - h * 0.05f)
            lineTo(tip.x, tip.y)
            var from = tip
            points.forEach { to ->
                // Scallops bow towards the root: the classic membrane edge.
                val ctrl = Offset((from.x + to.x) / 2f - side * w * 0.05f, (from.y + to.y) / 2f - h * 0.02f)
                quadraticTo(ctrl.x, ctrl.y, to.x, to.y)
                from = to
            }
            lineTo(root.x, root.y + h * 0.15f)
            close()
        }
    outlined(path, colors.bodyShade, colors.outline, body)
    (listOf(tip) + points.dropLast(1)).forEach { end ->
        drawLine(colors.outline.copy(alpha = 0.45f), root, end, strokeWidth = body.width * 0.014f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawDragonTail(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    val start = body.at(Offset(0.86f, 0.82f))
    val end = Offset(start.x + body.width * 0.36f * k, start.y - body.height * 0.34f * k)
    val path =
        Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(start.x + body.width * 0.38f * k, start.y + body.height * 0.05f, end.x, end.y)
        }
    drawPath(path, colors.outline, style = Stroke(width = body.width * 0.12f, cap = StrokeCap.Round))
    drawPath(path, colors.body, style = Stroke(width = body.width * 0.08f, cap = StrokeCap.Round))
    // A spade at the tip.
    val s = body.width * 0.09f
    val spade =
        Path().apply {
            moveTo(end.x, end.y - s * 1.3f)
            quadraticTo(end.x + s * 1.2f, end.y - s * 0.2f, end.x, end.y + s * 0.4f)
            quadraticTo(end.x - s * 1.2f, end.y - s * 0.2f, end.x, end.y - s * 1.3f)
            close()
        }
    outlined(spade, colors.pattern, colors.outline, body)
}

private fun DrawScope.drawSpikes(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    listOf(0.36f, 0.5f, 0.64f).forEach { x ->
        val base = body.at(Offset(x, 0.04f))
        val s = body.width * 0.07f * k
        val spike =
            Path().apply {
                moveTo(base.x - s, base.y + s * 0.6f)
                lineTo(base.x, base.y - s * 1.3f)
                lineTo(base.x + s, base.y + s * 0.6f)
                close()
            }
        outlined(spike, colors.pattern, colors.outline, body)
    }
}

private fun DrawScope.drawHorn(
    body: Rect,
    colors: PetColors,
    side: Float,
    k: Float,
) {
    val base = body.at(Offset(0.5f + side * 0.2f, 0.1f))
    val w = body.width * 0.06f
    val h = body.height * 0.22f * k
    val path =
        Path().apply {
            moveTo(base.x - w, base.y)
            quadraticTo(base.x - w * 0.2f, base.y - h * 0.6f, base.x + side * w * 1.8f, base.y - h)
            quadraticTo(base.x + w * 0.6f, base.y - h * 0.4f, base.x + w, base.y)
            close()
        }
    outlined(path, HORN, colors.outline, body)
}

private fun DrawScope.drawBellyPlates(
    body: Rect,
    colors: PetColors,
) {
    clipPath(bodyPath(body)) {
        val plate = colors.body.copy(alpha = 0.55f)
        listOf(0.72f, 0.82f, 0.92f).forEach { y ->
            val c = body.at(Offset(0.5f, y))
            drawOval(
                Color.White.copy(alpha = 0.28f),
                Offset(c.x - body.width * 0.2f, c.y - body.height * 0.04f),
                Size(
                    body.width * 0.4f,
                    body.height * 0.08f,
                ),
            )
            drawOval(plate, Offset(c.x - body.width * 0.18f, c.y - body.height * 0.03f), Size(body.width * 0.36f, body.height * 0.06f))
        }
    }
}

// --- Griffin and phoenix -------------------------------------------------

private class Feathers(
    val fill: Color,
    val tips: Color,
)

private fun DrawScope.drawFeatherWing(
    body: Rect,
    feathers: Feathers,
    outline: Color,
    side: Float,
    k: Float,
) {
    val root = body.at(Offset(0.5f + side * 0.34f, 0.48f))
    val w = body.width * k
    val h = body.height * k
    // Three layered feathers, longest at the back.
    listOf(0.5f to -0.38f, 0.42f to -0.22f, 0.32f to -0.06f).forEach { (len, rise) ->
        val tip = Offset(root.x + side * w * len, root.y + h * rise)
        val feather =
            Path().apply {
                moveTo(root.x, root.y - h * 0.08f)
                quadraticTo(root.x + side * w * len * 0.5f, tip.y - h * 0.12f, tip.x, tip.y)
                quadraticTo(root.x + side * w * len * 0.6f, tip.y + h * 0.16f, root.x, root.y + h * 0.1f)
                close()
            }
        outlined(feather, feathers.fill, outline, body)
        drawCircle(feathers.tips, body.width * 0.03f, Offset(tip.x - side * w * 0.04f, tip.y + h * 0.02f))
    }
}

private fun DrawScope.drawFeatheredHead(body: Rect) {
    clipPath(bodyPath(body)) {
        val path =
            Path().apply {
                moveTo(body.left, body.top)
                lineTo(body.right, body.top)
                lineTo(body.right, body.top + body.height * 0.5f)
                // A feathery lower edge.
                val points = 6
                for (i in points downTo 0) {
                    val x = body.left + body.width * i / points
                    val y = body.top + body.height * (if (i % 2 == 0) 0.5f else 0.58f)
                    lineTo(x, y)
                }
                close()
            }
        drawPath(path, CREAM)
    }
}

private fun DrawScope.drawBeak(
    body: Rect,
    colors: PetColors,
    size: Float,
) {
    val c = body.at(Offset(0.5f, 0.5f))
    val w = body.width * 0.09f * size
    val h = body.height * 0.13f * size
    val path =
        Path().apply {
            moveTo(c.x - w, c.y)
            quadraticTo(c.x, c.y - h * 0.35f, c.x + w, c.y)
            quadraticTo(c.x + w * 0.2f, c.y + h * 0.5f, c.x, c.y + h)
            quadraticTo(c.x - w * 0.2f, c.y + h * 0.5f, c.x - w, c.y)
            close()
        }
    outlined(path, BEAK, colors.outline, body)
}

private fun DrawScope.drawTuftTail(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    val start = body.at(Offset(0.88f, 0.8f))
    val end = Offset(start.x + body.width * 0.3f * k, start.y - body.height * 0.38f * k)
    val path =
        Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(start.x + body.width * 0.34f * k, start.y, end.x, end.y)
        }
    drawPath(path, colors.outline, style = Stroke(width = body.width * 0.06f, cap = StrokeCap.Round))
    drawPath(path, colors.bodyShade, style = Stroke(width = body.width * 0.035f, cap = StrokeCap.Round))
    drawCircle(colors.outline, body.width * 0.075f, end)
    drawCircle(colors.pattern, body.width * 0.06f, end)
}

private fun DrawScope.drawPlumeTail(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    val start = body.at(Offset(0.84f, 0.84f))
    val plumes = listOf(Triple(-15f, FLAME_GOLD, 0.62f), Triple(-40f, FLAME_CORAL, 0.7f), Triple(-65f, FLAME_PEACH, 0.58f))
    plumes.forEach { (angle, color, len) ->
        rotate(angle, start) {
            val l = body.width * len * k
            val w = body.height * 0.1f
            val plume =
                Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(start.x + l * 0.5f, start.y - w, start.x + l, start.y)
                    quadraticTo(start.x + l * 0.5f, start.y + w, start.x, start.y)
                    close()
                }
            outlined(plume, color, colors.outline, body)
            drawCircle(Color.White.copy(alpha = 0.5f), w * 0.4f, Offset(start.x + l * 0.8f, start.y))
        }
    }
}

private fun DrawScope.drawCrest(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    val base = body.at(Offset(0.5f, 0.06f))
    listOf(Triple(-28f, FLAME_CORAL, 0.8f), Triple(28f, FLAME_CORAL, 0.8f), Triple(0f, FLAME_GOLD, 1f)).forEach { (angle, color, len) ->
        rotate(angle, base) {
            val l = body.height * 0.26f * k * len
            val w = body.width * 0.07f
            val plume =
                Path().apply {
                    moveTo(base.x, base.y)
                    quadraticTo(base.x - w, base.y - l * 0.6f, base.x, base.y - l)
                    quadraticTo(base.x + w, base.y - l * 0.6f, base.x, base.y)
                    close()
                }
            outlined(plume, color, colors.outline, body)
        }
    }
}

// --- Unicorn --------------------------------------------------------------

private fun DrawScope.drawUnicornHorn(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    val base = body.at(Offset(0.5f, 0.06f))
    val w = body.width * 0.065f
    val h = body.height * 0.3f * k
    val path =
        Path().apply {
            moveTo(base.x - w, base.y)
            lineTo(base.x, base.y - h)
            lineTo(base.x + w, base.y)
            close()
        }
    outlined(path, HORN_GOLD, colors.outline, body)
    // The spiral: three soft bands.
    clipPath(path) {
        listOf(0.25f, 0.5f, 0.75f).forEach { t ->
            val y = base.y - h * t
            drawLine(
                Color.White.copy(alpha = 0.6f),
                Offset(base.x - w, y + h * 0.06f),
                Offset(base.x + w, y - h * 0.04f),
                strokeWidth =
                    body.width * 0.018f,
            )
        }
    }
}

private fun DrawScope.drawMane(
    body: Rect,
    k: Float,
) {
    // Curls in soft rainbow pastels along the top of the head.
    val curls = 7
    for (i in 0 until curls) {
        val t = i / (curls - 1f)
        val at = body.at(Offset(0.78f - 0.5f * t, 0.02f + 0.1f * t * t))
        drawCircle(MANE[i % MANE.size], body.width * 0.075f * k.coerceAtMost(1.1f), at)
    }
}

private fun DrawScope.drawRainbowTail(
    body: Rect,
    colors: PetColors,
    k: Float,
) {
    val start = body.at(Offset(0.86f, 0.8f))
    MANE.take(4).forEachIndexed { i, color ->
        val path =
            Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(
                    start.x + body.width * (0.3f + i * 0.04f) * k,
                    start.y - body.height * 0.05f,
                    start.x + body.width * (0.18f + i * 0.05f) * k,
                    start.y - body.height * (0.4f - i * 0.06f) * k,
                )
            }
        drawPath(path, colors.outline.copy(alpha = 0.6f), style = Stroke(width = body.width * 0.075f, cap = StrokeCap.Round))
        drawPath(path, color, style = Stroke(width = body.width * 0.055f, cap = StrokeCap.Round))
    }
}

// --- Kitsune --------------------------------------------------------------

private fun DrawScope.drawFoxTails(
    body: Rect,
    colors: PetColors,
    growth: Float,
    k: Float,
) {
    // One tail as a hatchling, nine as a majestic kitsune.
    val count = 1 + (growth * 8f).roundToInt()
    val start = body.at(Offset(0.8f, 0.82f))
    for (i in 0 until count) {
        val angle = if (count == 1) -35f else -5f - 80f * i / (count - 1)
        rotate(angle, start) {
            val l = body.width * 0.5f * k
            val w = body.height * 0.13f
            val tail =
                Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(start.x + l * 0.5f, start.y - w * 1.4f, start.x + l, start.y)
                    quadraticTo(start.x + l * 0.5f, start.y + w * 1.4f, start.x, start.y)
                    close()
                }
            outlined(tail, colors.body, colors.outline, body)
            clipPath(tail) { drawCircle(CREAM, w * 1.1f, Offset(start.x + l, start.y)) }
            drawPath(tail, colors.outline, style = Stroke(width = body.width * 0.022f))
        }
    }
}

private fun DrawScope.drawFoxEar(
    body: Rect,
    colors: PetColors,
    side: Float,
) {
    val base = body.at(Offset(0.5f + side * 0.27f, 0.14f))
    val outer =
        Path().apply {
            moveTo(base.x - side * body.width * 0.17f, base.y + body.height * 0.08f)
            lineTo(base.x + side * body.width * 0.06f, base.y - body.height * 0.42f)
            lineTo(base.x + side * body.width * 0.2f, base.y + body.height * 0.04f)
            close()
        }
    outlined(outer, colors.body, colors.outline, body)
    val inner =
        Path().apply {
            moveTo(base.x - side * body.width * 0.08f, base.y + body.height * 0.02f)
            lineTo(base.x + side * body.width * 0.05f, base.y - body.height * 0.28f)
            lineTo(base.x + side * body.width * 0.12f, base.y)
            close()
        }
    drawPath(inner, CREAM)
}

private fun DrawScope.drawMuzzle(body: Rect) {
    clipPath(bodyPath(body)) {
        drawOval(CREAM, body.at(Offset(0.18f, 0.5f)), Size(body.width * 0.64f, body.height * 0.6f))
    }
}

private fun DrawScope.drawForeheadMark(
    body: Rect,
    colors: PetColors,
) {
    val c = body.at(Offset(0.5f, 0.2f))
    val s = body.width * 0.05f
    val path =
        Path().apply {
            moveTo(c.x, c.y - s * 1.4f)
            quadraticTo(c.x + s, c.y, c.x, c.y + s)
            quadraticTo(c.x - s, c.y, c.x, c.y - s * 1.4f)
            close()
        }
    drawPath(path, colors.pattern)
}

/** A simple ear behind the head. */
private class EarSpec(
    val height: Float,
    val width: Float,
    val spread: Float,
)

private fun DrawScope.drawEar(
    body: Rect,
    side: Float,
    fill: Color,
    outline: Color,
    ear: EarSpec,
) {
    val base = body.at(Offset(0.5f + side * ear.spread, 0.1f))
    val path =
        Path().apply {
            moveTo(base.x - side * body.width * ear.width, base.y + body.height * 0.05f)
            lineTo(base.x + side * body.width * 0.03f, base.y - body.height * ear.height)
            lineTo(base.x + side * body.width * ear.width, base.y + body.height * 0.03f)
            close()
        }
    outlined(path, fill, outline, body)
}

// Storybook accents: warm and soft, never neon.
private val CREAM = Color(0xFFFFF6E6)
private val HORN = Color(0xFFF5E3BE)
private val HORN_GOLD = Color(0xFFF2CF79)
private val BEAK = Color(0xFFF3BC5C)
private val FLAME_GOLD = Color(0xFFF7C866)
private val FLAME_CORAL = Color(0xFFF08C6C)
private val FLAME_PEACH = Color(0xFFF9B28A)
private val MANE =
    listOf(
        Color(0xFFF9B4C8),
        Color(0xFFFCD0A4),
        Color(0xFFFBEAA6),
        Color(0xFFB8E6CF),
        Color(0xFFB9D6FA),
        Color(0xFFD4C4F6),
    )
