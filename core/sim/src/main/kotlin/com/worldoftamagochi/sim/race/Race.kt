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
    /** Agility faults so far; each adds [RacePhysics.faultPenaltyTicks] to the time. */
    val faults: Int = 0,
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
        val under = obstacleUnder(r.xMm)
        // In a tunnel or on a seesaw the pet cannot sprint, so it gets its breath back.
        val calm = under is Obstacle.Tunnel || under is Obstacle.Seesaw
        val sprinting = input.sprint && !r.exhausted && r.stumbleTicks == 0 && !calm
        var next =
            r.copy(
                tick = r.tick + 1,
                vx = horizontalSpeed(r, sprinting),
                stamina = staminaAfter(r.stamina, sprinting),
                stumbleTicks = (r.stumbleTicks - 1).coerceAtLeast(0),
                sprinting = sprinting,
            )
        next = airborne(next, jump = input.jump && r.grounded && under !is Obstacle.Tunnel)
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
            finishMicros = r.tick * MICROS_PER_TICK + crossedAt + next.faults * physics.faultPenaltyTicks * MICROS_PER_TICK
        }
        runner = next
        return next
    }

    private fun horizontalSpeed(
        r: Runner,
        sprinting: Boolean,
    ): Int {
        val under = obstacleUnder(r.xMm)
        val target =
            when {
                r.grounded && under is Obstacle.Puddle -> physics.puddleSpeed
                under is Obstacle.Tunnel -> physics.tunnelSpeed
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

    /**
     * Hurdles touched this tick trip the pet; boost pads crossed on the ground
     * fire once; agility obstacles are judged once, as the pet reaches them.
     */
    private fun collide(
        fromXMm: Int,
        r: Runner,
    ): Runner {
        var next = r
        track.obstacles.forEachIndexed { i, o ->
            if (!hit[i] && next.xMm >= o.startMm && fromXMm <= o.endMm) next = meet(i, o, fromXMm, next)
        }
        return next
    }

    /** What happens as the pet meets obstacle [i]; it is marked once it has been judged. */
    private fun meet(
        i: Int,
        o: Obstacle,
        fromXMm: Int,
        r: Runner,
    ): Runner {
        if (!judgedNow(o, fromXMm, r)) return r
        hit[i] = true
        return outcome(o, r)
    }

    /** Hurdles and pads count on contact, tyres in the middle of the ring, the rest on arrival. */
    private fun judgedNow(
        o: Obstacle,
        fromXMm: Int,
        r: Runner,
    ): Boolean =
        when (o) {
            is Obstacle.Hurdle -> r.yMm < o.heightMm
            is Obstacle.Boost -> r.yMm == 0
            is Obstacle.Tyre -> fromXMm < o.centerMm && r.xMm >= o.centerMm
            is Obstacle.Tunnel, is Obstacle.Seesaw -> true
            is Obstacle.Puddle -> false
        }

    private fun outcome(
        o: Obstacle,
        r: Runner,
    ): Runner =
        when (o) {
            is Obstacle.Hurdle -> {
                trip(r)
            }

            is Obstacle.Boost -> {
                r.copy(
                    vx = r.vx + physics.boostSpeed,
                    stamina = (r.stamina + physics.boostStamina).coerceAtMost(staminaMax),
                )
            }

            is Obstacle.Tyre -> {
                if (r.yMm in Obstacle.TYRE_BOTTOM_MM..Obstacle.TYRE_TOP_MM) r else fault(r, stumble = false)
            }

            is Obstacle.Tunnel -> {
                if (r.yMm > 0) fault(r, stumble = true) else r
            }

            is Obstacle.Seesaw -> {
                if (r.yMm > 0 || r.vx > physics.seesawSafeSpeed) fault(r, stumble = true) else r
            }

            is Obstacle.Puddle -> {
                r
            }
        }

    /** A touched hurdle: the pet stumbles; in agility it is also a fault. */
    private fun trip(r: Runner): Runner {
        val fault = if (track.discipline == Discipline.AGILITY) 1 else 0
        return r.copy(hurdlesHit = r.hurdlesHit + 1, faults = r.faults + fault, stumbleTicks = stumbleTicks, vx = r.vx / 2)
    }

    /** A fault costs time on the clock, and a moment of balance. */
    private fun fault(
        r: Runner,
        stumble: Boolean,
    ): Runner =
        r.copy(
            faults = r.faults + 1,
            vx = r.vx.coerceAtMost(physics.tiredSpeed),
            stumbleTicks = if (stumble) stumbleTicks / 2 else r.stumbleTicks,
        )

    /** Exact finish time in microseconds, or null while racing. */
    var finishMicros: Long? = null
        private set

    private fun obstacleUnder(xMm: Int): Obstacle? = track.obstacles.firstOrNull { xMm >= it.startMm && xMm < it.endMm }

    companion object {
        private const val PER_MILLE = 1_000
        const val MICROS_PER_TICK = 1_000_000L / RacePhysics.TICKS_PER_SECOND
    }
}
