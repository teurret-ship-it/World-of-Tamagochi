package com.mymagicalpet.feature.race

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mymagicalpet.api.LeaderboardEntry
import com.mymagicalpet.designsystem.MagicTheme
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.Reward
import com.mymagicalpet.sim.race.AgilityTracks
import com.mymagicalpet.sim.race.Autopilot
import com.mymagicalpet.sim.race.Medal
import com.mymagicalpet.sim.race.MedalTimes
import com.mymagicalpet.sim.race.Obstacle
import com.mymagicalpet.sim.race.Race
import com.mymagicalpet.sim.race.RaceStats
import com.mymagicalpet.sim.race.Replay
import com.mymagicalpet.sim.race.SprintTracks
import com.mymagicalpet.sim.race.Tracks
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class RaceScreenshotTest {
    private val track = SprintTracks.MEADOW
    private val medals = MedalTimes.of(track)
    private val frames = Replay.frames(track, RaceStats.ROOKIE, Autopilot(track).play(RaceStats.ROOKIE).second)

    private fun state(
        tick: Int,
        countdown: Int? = null,
        result: RaceSummary? = null,
    ) = RaceUiState(
        track = track,
        genome = Genome.fromSeed(7),
        name = "Mochi",
        phase = if (result != null) RacePhase.FINISHED else RacePhase.RUNNING,
        countdown = countdown,
        runner = frames[tick],
        ghost = frames[(tick + 40).coerceAtMost(frames.lastIndex)],
        ghostKind = GhostKind.COACH,
        elapsedMicros = tick * Race.MICROS_PER_TICK,
        bestMicros = null,
        medals = medals,
        staminaMax = frames[0].stamina,
        result = result,
    )

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) = captureRoboImage("src/test/screenshots/$name.png") { MagicTheme { content() } }

    @Test
    fun midRaceJumpingAHurdle() {
        val airborne = frames.indexOfFirst { it.yMm > 300 }
        capture("race_mid_jump") { RaceScreen(state(airborne), RaceControls()) }
    }

    @Test
    fun countdown() = capture("race_countdown") { RaceScreen(state(0, countdown = 3), RaceControls()) }

    @Test
    fun finishWithGold() =
        capture("race_finish_gold") {
            val summary =
                RaceSummary(
                    finishMicros = medals.gold - 120_000,
                    medal = Medal.GOLD,
                    newRecord = true,
                    previousBestMicros = medals.silver,
                    reward = Reward(xp = 30, coins = 23),
                    levelUp = null,
                    nextMedal = Medal.AUTHOR to medals.authorMicros,
                    online = OnlineOutcome.Ranked(7),
                )
            RaceScreen(state(frames.lastIndex, result = summary), RaceControls())
        }

    @Test
    fun trackList() =
        capture("race_tracks") {
            val cards =
                SprintTracks.ALL.mapIndexed { i, t ->
                    val m = MedalTimes.of(t)
                    val today =
                        listOf(
                            LeaderboardEntry(1, "Comet #4821", m.authorMicros - 400_000, you = false),
                            LeaderboardEntry(2, "Pip #1234", m.gold, you = true),
                            LeaderboardEntry(3, "Bean #7310", m.silver, you = false),
                        )
                    TrackCard(t, m, bestMicros = listOf(m.silver, m.authorMicros, null)[i], today = if (i == 0) today else emptyList())
                }
            TrackSelectScreen(cards, onRace = {}, onBack = {})
        }

    @Test
    fun trackListWithTraining() =
        capture("race_tracks_training") {
            val cards = Tracks.ALL.map { TrackCard(it, MedalTimes.of(it), bestMicros = null) }
            val training = TrainingUiState("Mochi", RaceStats(speed = 34, stamina = 21, agility = 8, jump = 15), 2, 600, FormHint.HUNGRY)
            TrackSelectScreen(cards, onRace = {}, onBack = {}, training = TrainingSlot(training, onTrain = {}))
        }

    @Test
    fun agilityTrackList() =
        capture("race_tracks_agility") {
            val cards =
                AgilityTracks.ALL.mapIndexed { i, t ->
                    TrackCard(
                        t,
                        MedalTimes.of(t),
                        bestMicros =
                            if (i ==
                                0
                            ) {
                                MedalTimes.of(t).gold
                            } else {
                                null
                            },
                    )
                }
            TrackSelectScreen(cards, onRace = {}, onBack = {})
        }

    private val park = AgilityTracks.PARK
    private val parkFrames = Replay.frames(park, RaceStats.ROOKIE, Autopilot(park).play(RaceStats.ROOKIE).second)

    private fun agility(
        tick: Int,
        result: RaceSummary? = null,
    ) = state(tick, result = result).copy(
        track = park,
        runner = parkFrames[tick],
        ghost = parkFrames[(tick + 40).coerceAtMost(parkFrames.lastIndex)],
        medals = MedalTimes.of(park),
        wearing = listOf("cap"),
    )

    @Test
    fun agilityThroughTheTyre() {
        val tyre = park.obstacles.filterIsInstance<Obstacle.Tyre>().first()
        capture("race_agility_tyre") { RaceScreen(agility(parkFrames.indexOfFirst { it.xMm >= tyre.centerMm - 600 }), RaceControls()) }
    }

    @Test
    fun agilityInTheTunnel() {
        val tunnel = park.obstacles.filterIsInstance<Obstacle.Tunnel>().first()
        capture("race_agility_tunnel") { RaceScreen(agility(parkFrames.indexOfFirst { it.xMm >= tunnel.startMm + 2_000 }), RaceControls()) }
    }

    @Test
    fun agilityOnTheSeesaw() {
        val seesaw = park.obstacles.filterIsInstance<Obstacle.Seesaw>().first()
        capture("race_agility_seesaw") { RaceScreen(agility(parkFrames.indexOfFirst { it.xMm >= seesaw.startMm + 600 }), RaceControls()) }
    }

    @Test
    fun agilityFinishWithFaults() =
        capture("race_agility_finish_faults") {
            val medals = MedalTimes.of(park)
            val summary =
                RaceSummary(
                    finishMicros = medals.bronze - 300_000,
                    medal = Medal.BRONZE,
                    newRecord = false,
                    previousBestMicros = medals.silver,
                    reward = Reward(xp = 20, coins = 8),
                    levelUp = null,
                    nextMedal = Medal.SILVER to medals.silver,
                    faults = 2,
                )
            RaceScreen(agility(parkFrames.lastIndex, result = summary), RaceControls())
        }
}
