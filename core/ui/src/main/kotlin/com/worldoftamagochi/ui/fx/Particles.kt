package com.worldoftamagochi.ui.fx

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.worldoftamagochi.ui.R
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Celebration bits, each a Fluent Emoji image with its own motion. */
enum class FxKind(
    @param:DrawableRes val image: Int,
    internal val motion: Motion,
) {
    HEART(R.drawable.fx_heart, Motion.FLOAT),
    BUBBLE(R.drawable.fx_bubbles, Motion.FLOAT),
    SPARKLE(R.drawable.fx_sparkles, Motion.BURST),
    STAR(R.drawable.reward_star, Motion.BURST),
    COIN(R.drawable.reward_coin, Motion.ARC),
    ZZZ(R.drawable.fx_zzz, Motion.FLOAT),
}

internal enum class Motion { FLOAT, BURST, ARC }

internal data class Particle(
    val kind: FxKind,
    val position: Offset,
    val velocity: Offset,
    val spin: Float,
    val size: Float,
    val lifeMillis: Long,
    val ageMillis: Long = 0,
    val rotation: Float = 0f,
)

internal data class FloatingLabel(
    val text: String,
    val color: Color,
    val position: Offset,
    val ageMillis: Long = 0,
)

/**
 * Short-lived celebration effects. Positions are fractions of the layer
 * (0..1), so callers do not need to know its pixel size.
 */
@Stable
class ParticleField(
    private val random: Random = Random.Default,
) {
    internal val particles = mutableStateListOf<Particle>()
    internal val labels = mutableStateListOf<FloatingLabel>()

    val isIdle: Boolean get() = particles.isEmpty() && labels.isEmpty()

    fun burst(
        kind: FxKind,
        at: Offset,
        count: Int = DEFAULT_COUNT,
    ) {
        repeat(count) {
            val angle = random.nextDouble(0.0, 2 * PI)
            val speed = random.nextDouble(SPEED_MIN, SPEED_MAX).toFloat()
            val velocity =
                when (kind.motion) {
                    Motion.FLOAT -> Offset(random.nextFloat() * DRIFT - DRIFT / 2, -speed * FLOAT_SLOWDOWN)
                    Motion.BURST -> Offset(cos(angle).toFloat() * speed, sin(angle).toFloat() * speed)
                    Motion.ARC -> Offset(random.nextFloat() * DRIFT - DRIFT / 2, -speed * ARC_LAUNCH)
                }
            particles +=
                Particle(
                    kind = kind,
                    position = at,
                    velocity = velocity,
                    spin = random.nextFloat() * SPIN - SPIN / 2,
                    size = random.nextDouble(SIZE_MIN, SIZE_MAX).toFloat(),
                    lifeMillis = random.nextLong(LIFE_MIN_MILLIS, LIFE_MAX_MILLIS),
                )
        }
    }

    /** A "+35" style label that rises and fades. */
    fun label(
        text: String,
        color: Color,
        at: Offset,
    ) {
        labels += FloatingLabel(text, color, at)
    }

    internal fun step(deltaMillis: Long) {
        val dt = deltaMillis / MILLIS_PER_SECOND
        val next =
            particles.mapNotNull { p ->
                val age = p.ageMillis + deltaMillis
                if (age >= p.lifeMillis) return@mapNotNull null
                val gravity = if (p.kind.motion == Motion.ARC) GRAVITY else 0f
                val velocity = Offset(p.velocity.x, p.velocity.y + gravity * dt)
                p.copy(position = p.position + velocity * dt, velocity = velocity, ageMillis = age, rotation = p.rotation + p.spin * dt)
            }
        particles.clear()
        particles.addAll(next)
        val nextLabels =
            labels.mapNotNull { l ->
                val age = l.ageMillis + deltaMillis
                if (age >= LABEL_LIFE_MILLIS) null else l.copy(ageMillis = age, position = l.position + Offset(0f, -LABEL_RISE * dt))
            }
        labels.clear()
        labels.addAll(nextLabels)
    }

    private companion object {
        const val DEFAULT_COUNT = 8
        const val SPEED_MIN = 0.25
        const val SPEED_MAX = 0.6
        const val FLOAT_SLOWDOWN = 0.6f
        const val ARC_LAUNCH = 1.6f
        const val DRIFT = 0.3f
        const val SPIN = 240f
        const val SIZE_MIN = 0.07
        const val SIZE_MAX = 0.12
        const val LIFE_MIN_MILLIS = 700L
        const val LIFE_MAX_MILLIS = 1_300L
        const val GRAVITY = 2.4f
        const val LABEL_RISE = 0.18f
        const val MILLIS_PER_SECOND = 1_000f
    }
}

@Composable
fun rememberParticleField(): ParticleField = remember { ParticleField() }

/** Draws and animates a [ParticleField]; sleeps when nothing is flying. */
@Composable
fun ParticleLayer(
    field: ParticleField,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = TextStyle.Default,
) {
    val images = FxKind.entries.associateWith { ImageBitmap.imageResource(it.image) }
    val measurer = rememberTextMeasurer()
    LaunchedEffect(field) {
        while (true) {
            snapshotFlow { field.isIdle }.first { !it }
            var last = withFrameMillis { it }
            while (!field.isIdle) {
                val now = withFrameMillis { it }
                field.step(now - last)
                last = now
            }
        }
    }
    Canvas(modifier) {
        field.particles.forEach { p ->
            val image = images.getValue(p.kind)
            val side = size.minDimension * p.size
            val life = p.ageMillis.toFloat() / p.lifeMillis
            val pop = (life / POP_IN).coerceAtMost(1f)
            val fade = if (life > FADE_FROM) 1f - (life - FADE_FROM) / (1f - FADE_FROM) else 1f
            val center = Offset(p.position.x * size.width, p.position.y * size.height)
            translate(center.x - side / 2, center.y - side / 2) {
                rotate(p.rotation, Offset(side / 2, side / 2)) {
                    scale(pop, Offset(side / 2, side / 2)) {
                        drawImage(
                            image = image,
                            dstSize = IntSize(side.toInt(), side.toInt()),
                            alpha = fade,
                        )
                    }
                }
            }
        }
        field.labels.forEach { l -> drawLabel(measurer, l, labelStyle) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    label: FloatingLabel,
    style: TextStyle,
) {
    val layout = measurer.measure(label.text, style.copy(color = label.color))
    val alpha = 1f - (label.ageMillis.toFloat() / LABEL_LIFE_MILLIS).let { if (it > FADE_FROM) (it - FADE_FROM) / (1f - FADE_FROM) else 0f }
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(label.position.x * size.width - layout.size.width / 2f, label.position.y * size.height),
        alpha = alpha,
    )
}

private const val POP_IN = 0.15f
private const val FADE_FROM = 0.6f
private const val LABEL_LIFE_MILLIS = 1_100L
