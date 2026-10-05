package com.worldoftamagochi.sim.race

/** What the player does in one tick: hold sprint, and/or press jump. */
data class TickInput(
    val sprint: Boolean = false,
    val jump: Boolean = false,
) {
    companion object {
        val IDLE = TickInput()
    }
}

/** The runner at the end of a tick. */
data class Runner(
    val tick: Int = 0,
    val xMm: Int = 0,
    val yMm: Int = 0,
    val vx: Int = 0,
    val vy: Int = 0,
    val stamina: Int,
    val stumbleTicks: Int = 0,
    val sprinting: Boolean = false,
    val finishTick: Int? = null,
    val hurdlesHit: Int = 0,
    /** Ran out of stamina: no sprinting until it recovers. */
    val exhausted: Boolean = false,
) {
    val grounded: Boolean get() = yMm == 0 && vy == 0
    val finished: Boolean get() = finishTick != null
}

/**
 * The deterministic race: one call to [step] per 1/60 s. Same track, stats
 * and inputs always give the same runner, on every device and on the server.
 */
class Race(
    val track: Track,
    val stats: RaceStats,
    private val physics: RacePhysics = RacePhysics.DEFAULT,
) {
    private val staminaMax = physics.scaled(physics.staminaMax, stats.stamina, physics.staminaStatBonus)
    private val sprintSpeed = physics.scaled(physics.sprintSpeed, stats.speed, physics.speedStatBonus)
    private val cruiseSpeed = physics.scaled(physics.cruiseSpeed, stats.speed, physics.speedStatBonus)
    private val jumpImpulse = physics.scaled(physics.jumpImpulse, stats.jump, physics.jumpStatBonus)
    private val stumbleTicks = physics.stumbleTicks * PER_MILLE / (PER_MILLE + physics.agilityStatBonus * stats.agility / RaceStats.MAX)
    private val hit = BooleanArray(track.obstacles.size)

    var runner: Runner = Runner(stamina = staminaMax)
        private set

    val finished: Boolean get() = runner.finished

    fun step(input: TickInput): Runner {
        if (finished) return runner
        val r = runner
        val sprinting = input.sprint && !r.exhausted && r.stumbleTicks == 0
        var next =
            r.copy(
                tick = r.tick + 1,
                vx = horizontalSpeed(r, sprinting),
                stamina = staminaAfter(r.stamina, sprinting),
                stumbleTicks = (r.stumbleTicks - 1).coerceAtLeast(0),
                sprinting = sprinting,
            )
        next = airborne(next, jump = input.jump && r.grounded)
        next = next.copy(xMm = r.xMm + next.vx)
        next = collide(fromXMm = r.xMm, next)
        next =
            next.copy(
                exhausted = next.stamina == 0 || (r.exhausted && next.stamina < physics.recoverTo),
                finishTick = if (next.xMm >= track.lengthMm) next.tick else null,
            )
        if (next.finished) {
            // Sub-tick finish time, so two runs a tick apart still rank exactly.
            val crossedAt = (track.lengthMm - r.xMm).toLong() * MICROS_PER_TICK / next.vx.coerceAtLeast(1)
            finishMicros = r.tick * MICROS_PER_TICK + crossedAt
        }
        runner = next
        return next
    }

    private fun horizontalSpeed(
        r: Runner,
        sprinting: Boolean,
    ): Int {
        val target =
            when {
                r.grounded && obstacleUnder(r.xMm) is Obstacle.Puddle -> physics.puddleSpeed
                sprinting -> sprintSpeed
                r.exhausted -> physics.tiredSpeed
                else -> cruiseSpeed
            }
        return when {
            r.stumbleTicks > 0 -> r.vx.coerceAtMost(physics.tiredSpeed)
            r.vx < target -> (r.vx + physics.acceleration).coerceAtMost(target)
            else -> (r.vx - physics.deceleration).coerceAtLeast(target)
        }
    }

    private fun staminaAfter(
        stamina: Int,
        sprinting: Boolean,
    ): Int =
        if (sprinting) {
            (stamina - physics.sprintDrain).coerceAtLeast(0)
        } else {
            (stamina + physics.staminaRegen).coerceAtMost(staminaMax)
        }

    /** Vertical motion: take off on [jump], then a parabola back to the ground. */
    private fun airborne(
        r: Runner,
        jump: Boolean,
    ): Runner {
        var vy = if (jump) jumpImpulse else r.vy
        var y = r.yMm
        if (y > 0 || vy > 0) {
            y += vy
            vy -= physics.gravity
            if (y <= 0) {
                y = 0
                vy = 0
            }
        }
        return r.copy(yMm = y, vy = vy)
    }

    /** Hurdles touched this tick trip the pet; boost pads crossed on the ground fire once. */
    private fun collide(
        fromXMm: Int,
        r: Runner,
    ): Runner {
        var next = r
        track.obstacles.forEachIndexed { i, o ->
            if (hit[i] || next.xMm < o.startMm || fromXMm > o.endMm) return@forEachIndexed
            if (o is Obstacle.Hurdle && next.yMm < o.heightMm) {
                hit[i] = true
                next = next.copy(hurdlesHit = next.hurdlesHit + 1, stumbleTicks = stumbleTicks, vx = next.vx / 2)
            } else if (o is Obstacle.Boost && next.yMm == 0) {
                hit[i] = true
                next =
                    next.copy(vx = next.vx + physics.boostSpeed, stamina = (next.stamina + physics.boostStamina).coerceAtMost(staminaMax))
            }
        }
        return next
    }

    /** Exact finish time in microseconds, or null while racing. */
    var finishMicros: Long? = null
        private set

    private fun obstacleUnder(xMm: Int): Obstacle? = track.obstacles.firstOrNull { xMm >= it.startMm && xMm < it.endMm }

    companion object {
        private const val PER_MILLE = 1_000
        const val MICROS_PER_TICK = 1_000_000L / RacePhysics.TICKS_PER_SECOND
    }
}
