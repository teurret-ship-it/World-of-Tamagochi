package com.worldoftamagochi.ui.pet

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
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Pattern

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
        val spots =
            when (genome.pattern) {
                Pattern.PLAIN -> emptyList()
                Pattern.STRIPES -> null
                else -> SPOTS
            }
        if (spots == null) {
            listOf(0.3f, 0.55f).forEach { y ->
                drawRect(
                    colors.pattern.copy(alpha = alpha),
                    Offset(egg.left, egg.top + egg.height * y),
                    Size(egg.width, egg.height * 0.07f),
                )
            }
        } else {
            spots.forEach { (at, r) ->
                drawCircle(
                    colors.pattern.copy(alpha = alpha),
                    egg.width * r,
                    Offset(
                        egg.left + egg.width * at.x,
                        egg.top + egg.height * at.y,
                    ),
                )
            }
        }
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

private val SPOTS =
    listOf(
        Offset(0.3f, 0.3f) to 0.1f,
        Offset(0.7f, 0.22f) to 0.07f,
        Offset(0.75f, 0.6f) to 0.11f,
        Offset(0.25f, 0.7f) to 0.08f,
        Offset(0.5f, 0.85f) to 0.06f,
    )
