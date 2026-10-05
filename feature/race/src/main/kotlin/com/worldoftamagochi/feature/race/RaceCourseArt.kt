package com.worldoftamagochi.feature.race

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    }
}

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
