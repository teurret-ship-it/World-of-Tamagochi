package com.mymagicalpet.model

/**
 * A pet need or mood level on a fixed 0..100 scale.
 *
 * Every arithmetic result is clamped, so no rule can push a need out of range
 * and no UI ever has to defend against a value of -3 or 140.
 */
@JvmInline
value class Gauge private constructor(
    val value: Int,
) : Comparable<Gauge> {
    operator fun plus(delta: Int): Gauge = of(value.toLong() + delta)

    operator fun minus(delta: Int): Gauge = of(value.toLong() - delta)

    override fun compareTo(other: Gauge): Int = value.compareTo(other.value)

    val isEmpty: Boolean get() = value == MIN_VALUE

    val isFull: Boolean get() = value == MAX_VALUE

    override fun toString(): String = "Gauge($value)"

    companion object {
        const val MIN_VALUE = 0
        const val MAX_VALUE = 100

        val EMPTY = Gauge(MIN_VALUE)
        val FULL = Gauge(MAX_VALUE)

        fun of(value: Int): Gauge = of(value.toLong())

        private fun of(value: Long): Gauge = Gauge(value.coerceIn(MIN_VALUE.toLong(), MAX_VALUE.toLong()).toInt())
    }
}
