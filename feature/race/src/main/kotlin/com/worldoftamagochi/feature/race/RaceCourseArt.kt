package com.worldoftamagochi.feature.race

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.worldoftamagochi.sim.race.Obstacle
import com.worldoftamagochi.sim.race.Track
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// *Art.kt files hold shape coordinates: they are drawing data, not logic, so
// detekt's MagicNumber rule does not apply to them (config/detekt/detekt.yml).

/** Images used on the course. */
internal class CourseImages(
    val hurdle: ImageBitmap,
    val puddle: ImageBitmap,
    val boost: ImageBitmap,
    val finish: ImageBitmap,
    val slow: ImageBitmap,
)

/** Maps track millimetres to screen pixels around a camera that follows the runner. */
internal class CourseCamera(
    val size: Size,
    val cameraMm: Int,
) {
    val pxPerMm: Float = size.width / VIEW_WIDTH_MM
    val groundY: Float = size.height * GROUND_FRACTION

    /** Screen x of track position [mm]; the runner stays at [RUNNER_SCREEN_FRACTION] of the width. */
    fun x(mm: Int): Float = size.width * RUNNER_SCREEN_FRACTION + (mm - cameraMm) * pxPerMm

    fun height(mm: Int): Float = mm * pxPerMm

    companion object {
        const val VIEW_WIDTH_MM = 12_000f
        const val GROUND_FRACTION = 0.74f
        const val RUNNER_SCREEN_FRACTION = 0.3f
    }
}

internal fun DrawScope.drawCourse(
    track: Track,
    camera: CourseCamera,
    colors: CourseColors,
    images: CourseImages,
) {
    drawRect(Brush.verticalGradient(listOf(colors.skyTop, colors.skyBottom), endY = camera.groundY))
    drawHills(camera, colors.farHills, HillLayer(parallax = 0.15f, amplitude = 0.10f, wavelength = 0.9f, base = 0.58f))
    drawHills(camera, colors.nearHills, HillLayer(parallax = 0.4f, amplitude = 0.07f, wavelength = 0.55f, base = 0.66f))
    drawRect(colors.ground, Offset(0f, camera.groundY), Size(size.width, size.height - camera.groundY))
    drawRect(colors.groundEdge, Offset(0f, camera.groundY), Size(size.width, size.height * 0.015f))
    drawDistanceMarks(track, camera, colors.groundEdge)
    track.obstacles.forEach { drawObstacle(it, camera, images) }
    drawFinish(track, camera, images.finish)
}

/** One parallax hill line: scroll factor and shape, as fractions of the view. */
private data class HillLayer(
    val parallax: Float,
    val amplitude: Float,
    val wavelength: Float,
    val base: Float,
)

private fun DrawScope.drawHills(
    camera: CourseCamera,
    color: Color,
    layer: HillLayer,
) {
    val shift = camera.cameraMm * camera.pxPerMm * layer.parallax
    val path = Path()
    path.moveTo(0f, size.height)
    val steps = 48
    for (i in 0..steps) {
        val x = size.width * i / steps
        val phase = (x + shift) / (size.width * layer.wavelength) * 2 * PI
        val y = size.height * (layer.base - layer.amplitude * (0.6f * sin(phase).toFloat() + 0.4f * sin(phase * 2.3).toFloat()))
        path.lineTo(x, y)
    }
    path.lineTo(size.width, size.height)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawDistanceMarks(
    track: Track,
    camera: CourseCamera,
    color: Color,
) {
    var mark = 0
    while (mark <= track.lengthMm) {
        val x = camera.x(mark)
        if (x in -20f..size.width + 20f) {
            drawRect(color.copy(alpha = 0.6f), Offset(x - 2f, camera.groundY), Size(4f, size.height * 0.05f))
        }
        mark += 10_000
    }
}

private fun DrawScope.drawObstacle(
    obstacle: Obstacle,
    camera: CourseCamera,
    images: CourseImages,
) {
    val left = camera.x(obstacle.startMm)
    val right = camera.x(obstacle.endMm)
    if (right < -size.width * 0.2f || left > size.width * 1.2f) return
    when (obstacle) {
        is Obstacle.Hurdle -> {
            val h = camera.height(obstacle.heightMm) * 1.35f
            drawImage(
                images.hurdle,
                dstOffset = IntOffset((left - h * 0.35f).roundToInt(), (camera.groundY - h).roundToInt()),
                dstSize = IntSize(h.roundToInt(), h.roundToInt()),
            )
        }

        is Obstacle.Puddle -> {
            drawOval(Color(0xCC3FA7D6), Offset(left, camera.groundY - size.height * 0.01f), Size(right - left, size.height * 0.05f))
            val s = size.height * 0.07f
            drawImage(
                images.puddle,
                dstOffset = IntOffset(((left + right) / 2 - s / 2).roundToInt(), (camera.groundY - s * 0.9f).roundToInt()),
                dstSize = IntSize(s.roundToInt(), s.roundToInt()),
            )
        }

        is Obstacle.Boost -> {
            drawRect(Color(0x99FFD54F), Offset(left, camera.groundY), Size(right - left, size.height * 0.03f))
            val s = size.height * 0.08f
            drawImage(
                images.boost,
                dstOffset = IntOffset(((left + right) / 2 - s / 2).roundToInt(), (camera.groundY - s).roundToInt()),
                dstSize = IntSize(s.roundToInt(), s.roundToInt()),
            )
        }

        is Obstacle.Tyre -> {
            drawTyre(obstacle, camera)
        }

        is Obstacle.Tunnel -> {
            // The inside of the tube; the translucent outside is drawn over the pet.
            drawRect(
                TUNNEL_INSIDE,
                Offset(left, camera.groundY - camera.height(TUNNEL_HEIGHT_MM)),
                Size(right - left, camera.height(TUNNEL_HEIGHT_MM)),
            )
        }

        is Obstacle.Seesaw -> {
            drawSeesaw(obstacle, camera, images.slow)
        }
    }
}

/** An agility tyre: a striped ring hanging in a frame, sized so the pet fits through. */
private fun DrawScope.drawTyre(
    tyre: Obstacle.Tyre,
    camera: CourseCamera,
) {
    val cx = camera.x(tyre.centerMm)
    val cy = camera.groundY - camera.height(TYRE_CENTER_MM)
    val radius = camera.height(TYRE_RADIUS_MM)
    val band = camera.height(TYRE_BAND_MM)
    val postW = camera.height(60)
    val top = camera.groundY - camera.height(TYRE_FRAME_MM)
    listOf(-1f, 1f).forEach { side ->
        val x = cx + side * camera.height(TYRE_FRAME_HALF_MM)
        drawRect(FRAME, Offset(x - postW / 2, top), Size(postW, camera.groundY - top))
        drawLine(ROPE, Offset(x, top + postW), Offset(cx + side * radius * 0.6f, cy - radius * 0.8f), strokeWidth = postW * 0.4f)
    }
    drawRect(FRAME, Offset(cx - camera.height(TYRE_FRAME_HALF_MM), top), Size(camera.height(TYRE_FRAME_HALF_MM) * 2, postW))
    drawCircle(TYRE_DARK, radius + band / 2, Offset(cx, cy), style = Stroke(band))
    for (i in 0 until 8) {
        drawArc(
            TYRE_STRIPE,
            startAngle = i * 45f,
            sweepAngle = 22f,
            useCenter = false,
            topLeft = Offset(cx - radius - band / 2, cy - radius - band / 2),
            size = Size((radius + band / 2) * 2, (radius + band / 2) * 2),
            style = Stroke(band * 0.7f),
        )
    }
}

/** A seesaw that tips as the pet walks over it, with a turtle sign: slow down! */
private fun DrawScope.drawSeesaw(
    seesaw: Obstacle.Seesaw,
    camera: CourseCamera,
    slow: ImageBitmap,
) {
    val left = camera.x(seesaw.startMm)
    val right = camera.x(seesaw.endMm)
    val mid = (left + right) / 2
    val pivotTop = camera.groundY - camera.height(SEESAW_PIVOT_MM)
    val base = camera.height(SEESAW_PIVOT_MM)
    drawPath(
        Path().apply {
            moveTo(mid, pivotTop)
            lineTo(mid + base * 0.7f, camera.groundY)
            lineTo(mid - base * 0.7f, camera.groundY)
            close()
        },
        FRAME,
    )
    // Tilt from "left end down" to "right end down" as the pet crosses it.
    val progress = ((camera.cameraMm - seesaw.startMm).toFloat() / seesaw.lengthMm).coerceIn(0f, 1f)
    val rise = camera.height(SEESAW_RISE_MM) * (1f - 2f * progress)
    val leftEnd = Offset(left, pivotTop + rise)
    val rightEnd = Offset(right, pivotTop - rise)
    val plank = camera.height(SEESAW_PLANK_MM)
    drawLine(SEESAW_BLUE, leftEnd, rightEnd, strokeWidth = plank, cap = StrokeCap.Round)
    drawLine(SEESAW_YELLOW, leftEnd, lerp(leftEnd, rightEnd, 0.18f), strokeWidth = plank, cap = StrokeCap.Round)
    drawLine(SEESAW_YELLOW, lerp(leftEnd, rightEnd, 0.82f), rightEnd, strokeWidth = plank, cap = StrokeCap.Round)
    // The sign stands before the seesaw, where braking has to start.
    val signX = camera.x(seesaw.startMm - SIGN_BEFORE_MM)
    val signTop = camera.groundY - camera.height(SIGN_HEIGHT_MM)
    drawRect(FRAME, Offset(signX - plank / 4, signTop), Size(plank / 2, camera.groundY - signTop))
    val s = camera.height(SIGN_SIZE_MM)
    drawImage(
        slow,
        dstOffset = IntOffset((signX - s / 2).roundToInt(), (signTop - s * 0.8f).roundToInt()),
        dstSize = IntSize(s.roundToInt(), s.roundToInt()),
    )
}

private fun lerp(
    a: Offset,
    b: Offset,
    t: Float,
): Offset = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** Drawn over the runners: the outside of tunnels, see-through so the pet shows inside. */
internal fun DrawScope.drawCourseForeground(
    track: Track,
    camera: CourseCamera,
) {
    track.obstacles.filterIsInstance<Obstacle.Tunnel>().forEach { tunnel ->
        val left = camera.x(tunnel.startMm)
        val right = camera.x(tunnel.endMm)
        if (right < 0f || left > size.width) return@forEach
        val h = camera.height(TUNNEL_HEIGHT_MM)
        val top = camera.groundY - h
        drawRoundRect(TUNNEL_OUTSIDE, Offset(left, top), Size(right - left, h), CornerRadius(h / 2, h / 2))
        var ring = tunnel.startMm + TUNNEL_RING_MM / 2
        while (ring < tunnel.endMm) {
            val x = camera.x(ring)
            drawLine(TUNNEL_RING, Offset(x, top + h * 0.05f), Offset(x, camera.groundY), strokeWidth = camera.height(80))
            ring += TUNNEL_RING_MM
        }
        drawOval(TUNNEL_INSIDE, Offset(left - h * 0.18f, top), Size(h * 0.36f, h))
    }
}

private val FRAME = Color(0xFF8D6E63)
private val ROPE = Color(0xFF5D4037)
private val TYRE_DARK = Color(0xFF37474F)
private val TYRE_STRIPE = Color(0xFFFFC107)
private val TUNNEL_INSIDE = Color(0xFF283593)
private val TUNNEL_OUTSIDE = Color(0xB35C6BC0)
private val TUNNEL_RING = Color(0x993949AB)
private val SEESAW_BLUE = Color(0xFF1E88E5)
private val SEESAW_YELLOW = Color(0xFFFFCA28)
private const val TYRE_CENTER_MM = 840
private const val TYRE_RADIUS_MM = 430
private const val TYRE_BAND_MM = 130
private const val TYRE_FRAME_MM = 1_560
private const val TYRE_FRAME_HALF_MM = 760
private const val TUNNEL_HEIGHT_MM = 1_050
private const val TUNNEL_RING_MM = 700
private const val SEESAW_PIVOT_MM = 350
private const val SEESAW_RISE_MM = 290
private const val SEESAW_PLANK_MM = 130
private const val SIGN_BEFORE_MM = 2_400
private const val SIGN_HEIGHT_MM = 700
private const val SIGN_SIZE_MM = 520

private fun DrawScope.drawFinish(
    track: Track,
    camera: CourseCamera,
    flag: ImageBitmap,
) {
    val x = camera.x(track.lengthMm)
    if (x > size.width * 1.3f) return
    val square = size.height * 0.03f
    for (row in 0 until 8) {
        for (col in 0 until 2) {
            val dark = (row + col) % 2 == 0
            drawRect(
                if (dark) Color(0xFF222222) else Color.White,
                Offset(x + col * square, camera.groundY - (row + 1) * square),
                Size(square, square),
            )
        }
    }
    val s = size.height * 0.14f
    drawImage(
        flag,
        dstOffset = IntOffset((x - s * 0.2f).roundToInt(), (camera.groundY - 8 * square - s).roundToInt()),
        dstSize = IntSize(s.roundToInt(), s.roundToInt()),
    )
}

/** Colours of a course: sky, two hill layers and the running surface. */
internal data class CourseColors(
    val skyTop: Color,
    val skyBottom: Color,
    val farHills: Color,
    val nearHills: Color,
    val ground: Color,
    val groundEdge: Color,
)

internal fun courseColors(trackId: String): CourseColors =
    if (trackId.startsWith("agility")) agilityColors(trackId) else sprintColors(trackId)

private fun sprintColors(trackId: String): CourseColors =
    when (trackId) {
        "sprint-beach" -> {
            CourseColors(
                skyTop = Color(0xFF6EC6FF),
                skyBottom = Color(0xFFFFE6B3),
                farHills = Color(0xFF4FA3D9),
                nearHills = Color(0xFF2E86C1),
                ground = Color(0xFFF4D58D),
                groundEdge = Color(0xFFE0B65C),
            )
        }

        "sprint-snow" -> {
            CourseColors(
                skyTop = Color(0xFF9EC5E8),
                skyBottom = Color(0xFFEAF4FF),
                farHills = Color(0xFFCFE0F2),
                nearHills = Color(0xFFB4CDE6),
                ground = Color(0xFFFFFFFF),
                groundEdge = Color(0xFFC9DBEE),
            )
        }

        else -> {
            CourseColors(
                skyTop = Color(0xFF7CC8FF),
                skyBottom = Color(0xFFDFF4FF),
                farHills = Color(0xFF9BD08A),
                nearHills = Color(0xFF6DBB5A),
                ground = Color(0xFF8BD16E),
                groundEdge = Color(0xFF5DA846),
            )
        }
    }

private fun agilityColors(trackId: String): CourseColors =
    when (trackId) {
        "agility-park" -> {
            CourseColors(
                skyTop = Color(0xFF8FD3FF),
                skyBottom = Color(0xFFE8F7FF),
                farHills = Color(0xFFA8D8A0),
                nearHills = Color(0xFF7CC46E),
                ground = Color(0xFF9CCC65),
                groundEdge = Color(0xFF7CB342),
            )
        }

        "agility-hills" -> {
            CourseColors(
                skyTop = Color(0xFFFFB074),
                skyBottom = Color(0xFFFFE3C2),
                farHills = Color(0xFFE59A6B),
                nearHills = Color(0xFFC97B4E),
                ground = Color(0xFFE8C07D),
                groundEdge = Color(0xFFCC9A52),
            )
        }

        else -> {
            CourseColors(
                skyTop = Color(0xFF7FB8C8),
                skyBottom = Color(0xFFD6EEE6),
                farHills = Color(0xFF5E9E6E),
                nearHills = Color(0xFF3F7D52),
                ground = Color(0xFFA1887F),
                groundEdge = Color(0xFF7B5E57),
            )
        }
    }
