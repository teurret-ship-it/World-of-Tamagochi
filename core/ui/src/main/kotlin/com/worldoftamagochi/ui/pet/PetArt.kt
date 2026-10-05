package com.worldoftamagochi.ui.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.worldoftamagochi.sim.EarShape
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Pattern

// *Art.kt files hold shape coordinates: they are drawing data, not logic, so
// detekt's MagicNumber rule does not apply to them (config/detekt/detekt.yml).
// All coordinates are fractions of the draw area, so the rig scales to any size.

/** Draws the whole pet: tail and ears behind, body with markings, face on top. */
internal fun DrawScope.drawPet(
    genome: Genome,
    expression: Expression,
    pose: PetPose,
) {
    val colors = genome.colors()
    val unit = minOf(size.width, size.height)
    val bodyW = unit * 0.62f * genome.widthPerMille / 1000f
    val bodyH = unit * 0.56f * genome.heightPerMille / 1000f
    val breathe = 1f + 0.025f * pose.breath
    val squashX = 1f + 0.12f * pose.squash
    val squashY = 1f - 0.10f * pose.squash
    val center = Offset(size.width / 2f, size.height * 0.6f)
    val ground = center.y + bodyH / 2f

    withTransform({
        scale(scaleX = squashX / breathe.coerceAtLeast(1f) * breathe, scaleY = squashY * breathe, pivot = Offset(center.x, ground))
    }) {
        val body = Rect(center = center, radius = 0f).inflateTo(bodyW, bodyH)
        if (genome.hasTail) drawTail(body, colors)
        drawEars(genome.ears, body, colors)
        drawBody(body, genome.pattern, colors)
        drawFace(genome, expression, pose, body, colors)
    }
}

private fun Rect.inflateTo(
    width: Float,
    height: Float,
): Rect = Rect(center.x - width / 2f, center.y - height / 2f, center.x + width / 2f, center.y + height / 2f)

private fun bodyPath(body: Rect): Path =
    Path().apply {
        // A soft "gumdrop": rounder at the bottom than at the top.
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = body,
                topLeft = CornerRadius(body.width * 0.5f, body.height * 0.62f),
                topRight = CornerRadius(body.width * 0.5f, body.height * 0.62f),
                bottomRight = CornerRadius(body.width * 0.42f, body.height * 0.38f),
                bottomLeft = CornerRadius(body.width * 0.42f, body.height * 0.38f),
            ),
        )
    }

private fun DrawScope.drawBody(
    body: Rect,
    pattern: Pattern,
    colors: PetColors,
) {
    val path = bodyPath(body)
    drawPath(path, colors.body)
    clipPath(path) {
        when (pattern) {
            Pattern.PLAIN -> {
                Unit
            }

            Pattern.SPOTS -> {
                listOf(
                    Offset(0.18f, 0.35f) to 0.09f,
                    Offset(0.82f, 0.28f) to 0.07f,
                    Offset(0.75f, 0.78f) to 0.1f,
                    Offset(0.22f, 0.8f) to 0.06f,
                ).forEach { (at, r) -> drawCircle(colors.pattern, body.width * r, body.at(at)) }
            }

            Pattern.STRIPES -> {
                listOf(0.12f, 0.3f).forEach { y ->
                    drawRect(colors.pattern, Offset(body.left, body.top + body.height * y), Size(body.width, body.height * 0.07f))
                }
            }

            Pattern.PATCH -> {
                drawOval(colors.pattern, body.at(Offset(0.55f, -0.05f)), Size(body.width * 0.55f, body.height * 0.45f))
            }

            Pattern.BELLY -> {
                drawOval(
                    colors.pattern.copy(alpha = 0.55f),
                    body.at(Offset(0.22f, 0.55f)),
                    Size(
                        body.width * 0.56f,
                        body.height * 0.5f,
                    ),
                )
            }
        }
        // Soft shading on the lower half reads as volume.
        drawOval(colors.bodyShade.copy(alpha = 0.35f), body.at(Offset(-0.1f, 0.78f)), Size(body.width * 1.2f, body.height * 0.5f))
    }
    drawPath(path, colors.outline, style = Stroke(width = body.width * 0.025f))
}

private fun DrawScope.drawEars(
    ears: EarShape,
    body: Rect,
    colors: PetColors,
) {
    val stroke = Stroke(width = body.width * 0.025f)
    listOf(-1f, 1f).forEach { side ->
        val base = body.at(Offset(0.5f + side * 0.27f, 0.1f))
        val path =
            when (ears) {
                EarShape.NONE -> {
                    return
                }

                EarShape.ROUND -> {
                    Path().apply {
                        addOval(
                            Rect(
                                center = base.copy(y = base.y - body.height * 0.08f),
                                radius =
                                    body.width * 0.13f,
                            ),
                        )
                    }
                }

                EarShape.POINTY -> {
                    Path().apply {
                        moveTo(base.x - side * body.width * 0.14f, base.y + body.height * 0.06f)
                        lineTo(base.x + side * body.width * 0.04f, base.y - body.height * 0.3f)
                        lineTo(base.x + side * body.width * 0.16f, base.y + body.height * 0.04f)
                        close()
                    }
                }

                EarShape.FLOPPY -> {
                    Path().apply {
                        moveTo(base.x - side * body.width * 0.1f, base.y)
                        quadraticTo(
                            base.x + side * body.width * 0.28f,
                            base.y - body.height * 0.12f,
                            base.x + side * body.width * 0.3f,
                            base.y + body.height * 0.25f,
                        )
                        quadraticTo(
                            base.x + side * body.width * 0.12f,
                            base.y + body.height * 0.2f,
                            base.x + side * body.width * 0.06f,
                            base.y + body.height * 0.08f,
                        )
                        close()
                    }
                }
            }
        drawPath(path, colors.bodyShade)
        drawPath(path, colors.outline, style = stroke)
    }
}

private fun DrawScope.drawTail(
    body: Rect,
    colors: PetColors,
) {
    val start = body.at(Offset(0.9f, 0.72f))
    val path =
        Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(
                start.x + body.width * 0.32f,
                start.y - body.height * 0.05f,
                start.x + body.width * 0.2f,
                start.y - body.height * 0.38f,
            )
        }
    drawPath(path, colors.outline, style = Stroke(width = body.width * 0.11f, cap = StrokeCap.Round))
    drawPath(path, colors.bodyShade, style = Stroke(width = body.width * 0.07f, cap = StrokeCap.Round))
}

private fun DrawScope.drawFace(
    genome: Genome,
    expression: Expression,
    pose: PetPose,
    body: Rect,
    colors: PetColors,
) {
    val eyeR = body.width * 0.1f * genome.eyeSizePerMille / 1000f
    val eyes = listOf(body.at(Offset(0.33f, 0.4f)), body.at(Offset(0.67f, 0.4f)))
    val lid = lidFor(expression, pose.blink)
    eyes.forEach { eye -> drawEye(Rect(center = eye, radius = eyeR), lid, expression, pose.look, colors) }

    val ink = colors.outline
    val mouth = body.at(Offset(0.5f, 0.64f))
    val w = body.width
    val line = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
    when (expression) {
        Expression.HAPPY -> {
            drawArc(ink, 0f, 180f, useCenter = true, topLeft = mouth - Offset(w * 0.1f, w * 0.06f), size = Size(w * 0.2f, w * 0.14f))
            listOf(0.18f, 0.82f).forEach { x -> drawOval(Color(0x66FF6F8E), body.at(Offset(x - 0.07f, 0.52f)), Size(w * 0.14f, w * 0.07f)) }
        }

        Expression.CONTENT -> {
            drawArc(
                ink,
                20f,
                140f,
                useCenter = false,
                topLeft = mouth - Offset(w * 0.08f, w * 0.07f),
                size =
                    Size(w * 0.16f, w * 0.1f),
                style = line,
            )
        }

        Expression.HUNGRY -> {
            drawOval(ink, mouth - Offset(w * 0.06f, w * 0.05f), Size(w * 0.12f, w * 0.11f))
            drawCircle(Color(0xFF8EC5FF), w * 0.025f, mouth + Offset(w * 0.07f, w * 0.08f))
        }

        Expression.SLEEPY, Expression.ASLEEP -> {
            drawLine(ink, mouth - Offset(w * 0.05f, 0f), mouth + Offset(w * 0.05f, 0f), strokeWidth = line.width, cap = StrokeCap.Round)
        }

        Expression.DIRTY -> {
            drawWavyMouth(mouth, w, ink, line)
            listOf(Offset(0.2f, 0.75f), Offset(0.7f, 0.85f), Offset(0.85f, 0.55f)).forEach { at ->
                drawOval(Color(0x995D4037), body.at(at) - Offset(w * 0.05f, w * 0.03f), Size(w * 0.1f, w * 0.06f))
            }
        }

        Expression.SAD -> {
            drawArc(
                ink,
                200f,
                140f,
                useCenter = false,
                topLeft = mouth - Offset(w * 0.08f, 0f),
                size = Size(w * 0.16f, w * 0.1f),
                style = line,
            )
            drawTear(eyes[0] + Offset(-eyeR * 0.6f, eyeR * 1.3f), w)
        }

        Expression.SICK -> {
            drawWavyMouth(mouth, w, ink, line)
            drawPath(bodyPath(body), Color(0x337CB342))
        }
    }
    if (expression == Expression.ASLEEP) drawSnore(body)
}

private fun lidFor(
    expression: Expression,
    blink: Float,
): Float {
    val resting =
        when (expression) {
            Expression.ASLEEP -> 1f
            Expression.SLEEPY, Expression.SICK -> 0.55f
            Expression.SAD -> 0.3f
            else -> 0f
        }
    return maxOf(resting, blink)
}

private fun DrawScope.drawEye(
    eyeRect: Rect,
    lid: Float,
    expression: Expression,
    look: Offset,
    colors: PetColors,
) {
    val center = eyeRect.center
    val radius = eyeRect.width / 2f
    if (lid >= 0.99f) {
        // Closed: a gentle downward curve.
        drawArc(
            colors.outline,
            0f,
            180f,
            false,
            center - Offset(radius, radius * 0.5f),
            Size(radius * 2f, radius),
            style =
                Stroke(radius * 0.25f, cap = StrokeCap.Round),
        )
        return
    }
    drawOval(Color.White, eyeRect.topLeft, eyeRect.size)
    val pupilR = radius * if (expression == Expression.HAPPY) 0.62f else 0.55f
    val travel = radius - pupilR
    val lookAt = if (expression == Expression.HUNGRY) Offset(look.x * 0.3f, -0.6f) else look
    val pupil = center + Offset(lookAt.x.coerceIn(-1f, 1f) * travel, lookAt.y.coerceIn(-1f, 1f) * travel)
    drawCircle(Color(0xFF1F1A17), pupilR, pupil)
    drawCircle(Color.White, pupilR * 0.32f, pupil + Offset(-pupilR * 0.3f, -pupilR * 0.35f))
    if (lid > 0f) {
        clipPath(Path().apply { addOval(eyeRect) }) {
            drawRect(colors.body, eyeRect.topLeft, Size(eyeRect.width, eyeRect.height * lid))
            drawLine(
                colors.outline,
                Offset(eyeRect.left, eyeRect.top + eyeRect.height * lid),
                Offset(
                    eyeRect.right,
                    eyeRect.top + eyeRect.height * lid,
                ),
                radius * 0.18f,
            )
        }
    }
    drawOval(colors.outline, eyeRect.topLeft, eyeRect.size, style = Stroke(radius * 0.14f))
}

private fun DrawScope.drawWavyMouth(
    mouth: Offset,
    w: Float,
    ink: Color,
    line: Stroke,
) {
    val path =
        Path().apply {
            moveTo(mouth.x - w * 0.08f, mouth.y)
            quadraticTo(mouth.x - w * 0.04f, mouth.y - w * 0.03f, mouth.x, mouth.y)
            quadraticTo(mouth.x + w * 0.04f, mouth.y + w * 0.03f, mouth.x + w * 0.08f, mouth.y)
        }
    drawPath(path, ink, style = line)
}

private fun DrawScope.drawTear(
    at: Offset,
    w: Float,
) {
    val path =
        Path().apply {
            moveTo(at.x, at.y - w * 0.05f)
            quadraticTo(at.x + w * 0.035f, at.y + w * 0.01f, at.x, at.y + w * 0.03f)
            quadraticTo(at.x - w * 0.035f, at.y + w * 0.01f, at.x, at.y - w * 0.05f)
        }
    drawPath(path, Color(0xFF8EC5FF))
}

private fun DrawScope.drawSnore(body: Rect) {
    val ink = Color(0xFF6B7FD7)
    listOf(Offset(0.95f, -0.1f) to 0.09f, Offset(1.08f, -0.32f) to 0.12f).forEach { (at, s) ->
        val o = body.at(at)
        val z = body.width * s
        val path =
            Path().apply {
                moveTo(o.x, o.y)
                lineTo(o.x + z, o.y)
                lineTo(o.x, o.y + z)
                lineTo(o.x + z, o.y + z)
            }
        drawPath(path, ink, style = Stroke(width = z * 0.18f, cap = StrokeCap.Round))
    }
}

/** A point inside [this] rect given as fractions of its width and height. */
private fun Rect.at(fraction: Offset): Offset = Offset(left + width * fraction.x, top + height * fraction.y)
