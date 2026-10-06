package com.mymagicalpet.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.Species

// *Art.kt files hold shape coordinates: they are drawing data, not logic, so
// detekt's MagicNumber rule does not apply to them (config/detekt/detekt.yml).

/**
 * An egg wearing the colours of the pet inside: its body colour as the shell
 * and its markings as spots, so choosing an egg is choosing a pet.
 *
 * @param wobble rotation in degrees around the bottom of the egg.
 * @param crack 0 = whole, 1 = cracked all the way round.
 * @param open 0 = closed, 1 = the top of the shell has flown off.
 */
internal fun DrawScope.drawEgg(
    genome: Genome,
    wobble: Float,
    crack: Float,
    open: Float,
) {
    val colors = genome.colors()
    val egg = eggRect()
    val shell = eggPath(egg)
    val line = crackLine(egg)
    val pivot = Offset(egg.center.x, egg.bottom)
    rotate(wobble, pivot) {
        // Shadow on the ground.
        drawOval(
            Color(0x22000000),
            Offset(egg.left + egg.width * 0.1f, egg.bottom - egg.height * 0.03f),
            Size(
                egg.width * 0.8f,
                egg.height * 0.08f,
            ),
        )
        if (open <= 0f) {
            drawShell(shell, egg, genome, colors)
        } else {
            clipPath(below(line, egg)) { drawShell(shell, egg, genome, colors) }
            val lift = egg.height * 0.7f * open
            translate(left = -egg.width * 0.35f * open, top = -lift) {
                rotate(-40f * open, Offset(egg.center.x, egg.top + egg.height * 0.45f)) {
                    clipPath(above(line, egg)) { drawShell(shell, egg, genome, colors, alpha = 1f - open) }
                }
            }
        }
        if (crack > 0f && open <= 0f) drawCrack(line, crack, colors.outline, egg)
    }
}

private fun DrawScope.eggRect(): Rect {
    val h = size.minDimension * 0.82f
    val w = h * 0.76f
    return Rect(Offset((size.width - w) / 2f, size.height - h - size.height * 0.06f), Size(w, h))
}

/** Narrow at the top, round at the bottom. */
private fun eggPath(r: Rect): Path =
    Path().apply {
        moveTo(r.center.x, r.top)
        cubicTo(r.left + r.width * 0.82f, r.top, r.right, r.top + r.height * 0.52f, r.right, r.top + r.height * 0.64f)
        cubicTo(r.right, r.bottom - r.height * 0.06f, r.left + r.width * 0.72f, r.bottom, r.center.x, r.bottom)
        cubicTo(r.left + r.width * 0.28f, r.bottom, r.left, r.bottom - r.height * 0.06f, r.left, r.top + r.height * 0.64f)
        cubicTo(r.left, r.top + r.height * 0.52f, r.left + r.width * 0.18f, r.top, r.center.x, r.top)
        close()
    }

private fun DrawScope.drawShell(
    shell: Path,
    egg: Rect,
    genome: Genome,
    colors: PetColors,
    alpha: Float = 1f,
) {
    val base = colors.body.copy(alpha = alpha)
    drawPath(shell, base)
    clipPath(shell) {
        drawShellMarks(egg, genome.species, colors.pattern.copy(alpha = alpha))
        // Shade at the bottom, a shine at the top left: it reads as round.
        drawOval(
            colors.bodyShade.copy(alpha = 0.35f * alpha),
            Offset(egg.left - egg.width * 0.1f, egg.top + egg.height * 0.72f),
            Size(egg.width * 1.2f, egg.height * 0.4f),
        )
        drawOval(
            Color.White.copy(alpha = 0.45f * alpha),
            Offset(egg.left + egg.width * 0.2f, egg.top + egg.height * 0.14f),
            Size(egg.width * 0.16f, egg.height * 0.22f),
        )
    }
    drawPath(shell, colors.outline.copy(alpha = alpha), style = Stroke(width = egg.width * 0.03f))
}

/** The zigzag the egg breaks along, from the left edge to the right, a little above the middle. */
private fun crackLine(egg: Rect): List<Offset> =
    (0..8).map { i ->
        val x = egg.left - egg.width * 0.05f + egg.width * 1.1f * i / 8f
        val y = egg.top + egg.height * (0.42f + if (i % 2 == 0) 0f else 0.07f)
        Offset(x, y)
    }

private fun DrawScope.drawCrack(
    line: List<Offset>,
    progress: Float,
    color: Color,
    egg: Rect,
) {
    // The crack grows from the middle outwards.
    val half = (line.size - 1) / 2f
    val reach = half * progress
    val path = Path()
    var started = false
    line.forEachIndexed { i, p ->
        if (kotlin.math.abs(i - half) <= reach + 0.01f) {
            if (started) path.lineTo(p.x, p.y) else path.moveTo(p.x, p.y)
            started = true
        }
    }
    clipPath(eggPath(egg)) {
        drawPath(path, color, style = Stroke(width = egg.width * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun above(
    line: List<Offset>,
    egg: Rect,
): Path =
    Path().apply {
        moveTo(line.first().x, egg.top - egg.height)
        line.forEach { lineTo(it.x, it.y) }
        lineTo(line.last().x, egg.top - egg.height)
        close()
    }

private fun below(
    line: List<Offset>,
    egg: Rect,
): Path =
    Path().apply {
        moveTo(line.first().x, egg.bottom + egg.height)
        line.forEach { lineTo(it.x, it.y) }
        lineTo(line.last().x, egg.bottom + egg.height)
        close()
    }

/** Every species has its own shell: scales, feathers, stars, flames or swirls. */
private fun DrawScope.drawShellMarks(
    egg: Rect,
    species: Species,
    ink: Color,
) {
    when (species) {
        Species.DRAGON -> drawScales(egg, ink)
        Species.GRIFFIN -> drawFeatherMarks(egg, ink)
        Species.UNICORN -> drawRainbowAndStars(egg, ink)
        Species.PHOENIX -> drawFlames(egg, ink)
        Species.KITSUNE -> drawSwirls(egg, ink)
    }
}

private fun Rect.point(
    x: Float,
    y: Float,
) = Offset(left + width * x, top + height * y)

private fun DrawScope.drawScales(
    egg: Rect,
    ink: Color,
) {
    for (row in 0 until 6) {
        val y = 0.22f + row * 0.12f
        val shift = if (row % 2 == 0) 0f else 0.1f
        for (col in 0 until 6) {
            val c = egg.point(shift + col * 0.2f, y)
            drawArc(
                ink,
                0f,
                180f,
                useCenter = false,
                topLeft = Offset(c.x - egg.width * 0.08f, c.y - egg.width * 0.06f),
                size = Size(egg.width * 0.16f, egg.width * 0.12f),
                style = Stroke(egg.width * 0.025f),
            )
        }
    }
}

private fun DrawScope.drawFeatherMarks(
    egg: Rect,
    ink: Color,
) {
    listOf(0.3f, 0.5f, 0.7f).forEach { y ->
        val path =
            Path().apply {
                moveTo(egg.left + egg.width * 0.2f, egg.top + egg.height * y)
                lineTo(egg.left + egg.width * 0.5f, egg.top + egg.height * (y + 0.07f))
                lineTo(egg.left + egg.width * 0.8f, egg.top + egg.height * y)
            }
        drawPath(path, ink, style = Stroke(egg.width * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    listOf(0.25f to 0.2f, 0.72f to 0.38f, 0.3f to 0.85f, 0.68f to 0.8f).forEach { (x, y) ->
        drawCircle(ink, egg.width * 0.035f, egg.point(x, y))
    }
}

private fun DrawScope.drawRainbowAndStars(
    egg: Rect,
    ink: Color,
) {
    RAINBOW.forEachIndexed { i, color ->
        drawRect(color, Offset(egg.left, egg.top + egg.height * (0.55f + i * 0.035f)), Size(egg.width, egg.height * 0.035f))
    }
    listOf(Triple(0.3f, 0.28f, 0.07f), Triple(0.7f, 0.4f, 0.05f), Triple(0.62f, 0.82f, 0.05f)).forEach { (x, y, r) ->
        drawStar(egg.point(x, y), egg.width * r, ink)
    }
}

private fun DrawScope.drawFlames(
    egg: Rect,
    ink: Color,
) {
    listOf(0.18f to 0.55f, 0.5f to 0.42f, 0.82f to 0.55f).forEach { (x, top) ->
        val base = egg.point(x, 1.02f)
        val tip = egg.point(x, top)
        val w = egg.width * 0.2f
        val flame =
            Path().apply {
                moveTo(base.x - w, base.y)
                quadraticTo(base.x - w, tip.y + (base.y - tip.y) * 0.4f, tip.x, tip.y)
                quadraticTo(base.x + w, tip.y + (base.y - tip.y) * 0.4f, base.x + w, base.y)
                close()
            }
        drawPath(flame, ink)
    }
}

private fun DrawScope.drawSwirls(
    egg: Rect,
    ink: Color,
) {
    listOf(Triple(0.32f, 0.3f, 0.12f), Triple(0.7f, 0.55f, 0.14f), Triple(0.35f, 0.78f, 0.1f)).forEach { (x, y, r) ->
        val c = egg.point(x, y)
        drawArc(
            ink,
            200f,
            250f,
            useCenter = false,
            topLeft = Offset(c.x - egg.width * r, c.y - egg.width * r),
            size = Size(egg.width * r * 2, egg.width * r * 2),
            style = Stroke(egg.width * 0.03f, cap = StrokeCap.Round),
        )
        drawCircle(ink, egg.width * 0.025f, c)
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val p = Offset(center.x + (r * kotlin.math.cos(angle)).toFloat(), center.y + (r * kotlin.math.sin(angle)).toFloat())
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color)
}

private val RAINBOW =
    listOf(
        Color(0xFFF9B4C8),
        Color(0xFFFCD0A4),
        Color(0xFFFBEAA6),
        Color(0xFFB8E6CF),
        Color(0xFFB9D6FA),
        Color(0xFFD4C4F6),
    )
