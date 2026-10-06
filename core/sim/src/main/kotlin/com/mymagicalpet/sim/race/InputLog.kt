package com.mymagicalpet.sim.race

/**
 * A run as the player played it: only the moments the input changed. This is
 * what the server receives and replays (it never trusts a reported time), and
 * what ghosts and replays are made of. Compact: a 40 s race is a few hundred
 * numbers.
 */
data class InputLog(
    val events: List<Event>,
) {
    enum class Kind { SPRINT_ON, SPRINT_OFF, JUMP }

    data class Event(
        val tick: Int,
        val kind: Kind,
    )

    init {
        require(events.zipWithNext().all { (a, b) -> a.tick <= b.tick }) { "Events must be in tick order" }
    }

    /** Compact form for saves and the network: one int per event, tick * 4 + kind. */
    fun toCodes(): List<Int> = events.map { it.tick * CODE_BASE + it.kind.ordinal }

    /** Expands the log into one input per tick, starting at tick 0. */
    fun inputs(): Sequence<TickInput> =
        sequence {
            var sprint = false
            var index = 0
            var tick = 0
            while (true) {
                var jump = false
                while (index < events.size && events[index].tick == tick) {
                    when (events[index].kind) {
                        Kind.SPRINT_ON -> sprint = true
                        Kind.SPRINT_OFF -> sprint = false
                        Kind.JUMP -> jump = true
                    }
                    index++
                }
                yield(TickInput(sprint, jump))
                tick++
            }
        }

    companion object {
        private const val CODE_BASE = 4

        fun fromCodes(codes: List<Int>): InputLog = InputLog(codes.map { Event(it / CODE_BASE, Kind.entries[it % CODE_BASE]) })
    }

    /** Records per-tick inputs as change events. */
    class Recorder {
        private val events = mutableListOf<Event>()
        private var sprint = false
        private var tick = 0

        fun record(input: TickInput) {
            if (input.sprint != sprint) {
                events += Event(tick, if (input.sprint) Kind.SPRINT_ON else Kind.SPRINT_OFF)
                sprint = input.sprint
            }
            if (input.jump) events += Event(tick, Kind.JUMP)
            tick++
        }

        fun build(): InputLog = InputLog(events.toList())
    }
}
