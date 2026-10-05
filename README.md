# World of Tamagochi

An online virtual-pet game for Android: raise a pet that lives in real time,
watch it grow into a unique adult, decorate its world, visit friends, and race
other players' pets in sprints, agility courses and timed platformers.

The game is built iteratively. `CLAUDE.md` is the project constitution
(product, rules, architecture, quality gates, iteration protocol) and
`docs/ROADMAP.md` is the plan. Every `/goal` run in Claude Code delivers the
next iteration.

## What is in the game today

- A living pet on the home screen: unique look from a genome, breathing,
  blinking, eight faces, eyes that follow your finger; rub it to stroke it.
- Five need bars; the most urgent need is highlighted.
- Care: drag the apple to feed, pick up the soap and rub to wash, play,
  lights off for a nap. Every action has sound, haptics, particles and a
  "+N" label; a full or sleepy pet says so.
- Rewards: XP and levels with a level-up fanfare, coins for answering a
  real need (capped per day).
- The game is saved on the device; coming back shows a "Welcome back!"
  card. Settings: sound and vibration toggles, credits.
- Races: three sprint courses, your pet against the coach's or your best
  ghost, hold Sprint and tap Jump, bronze/silver/gold/trophy medals, records
  and coins.
- Online (with a server): race the ghost of the real player just ahead of
  you, today's top three per track, your online rank after each race.
- The server answers `GET /health`.
- Game rules (`core/sim`): five needs (satiety, energy, hygiene, happiness,
  health) living in real time, a sleep window in the player's time zone, and
  exact offline catch-up.

## Build and run

Requirements: JDK 21 (17+), Android SDK 36 (`scripts/setup-android-sdk.sh`
installs it).

```bash
./gradlew check                        # format, detekt, lint, tests, coverage
./gradlew :app:installDebug            # run on a device or emulator
./gradlew :server:run                  # server on http://localhost:8080/health
docker compose up --build              # server + PostgreSQL
```

Online races need the racing server (ADR-008): run `docker compose up`
and build the app with `./gradlew :app:installDebug -Pwot.serverUrl=http://10.0.2.2:8080`
(the emulator's address for your computer). Without `wot.serverUrl` the app
is fully offline.

Without the Android SDK (e.g. no access to dl.google.com):

```bash
./gradlew jvmCheck -Pwot.jvmOnly=true # rules and server only (ADR-002)
```

Screenshots: `./gradlew recordRoborazziDebug` records reference images into
`*/src/test/screenshots`, `verifyRoborazziDebug` compares against them.

## Layout

| Module | What |
|---|---|
| `core/sim` | game rules, pure Kotlin, shared by app and server |
| `core/model` | shared value types |
| `core/designsystem` | theme, palette, typography |
| `core/ui` | the pet rig, particles, sounds, shared game UI |
| `core/data` | the save (DataStore) and settings |
| `core/api` | wire format shared by app and server |
| `core/network` | racing server client, offline-first upload queue |
| `feature/home` | home screen: the pet, care, rewards, settings |
| `feature/race` | track list and the race screen |
| `app` | Android application |
| `server` | Ktor game server |
| `build-logic` | Gradle convention plugins |

## Playtesting

Write what you notice into `docs/FEEDBACK.md`. Items marked `[!]` are
handled before anything else in the next iteration.
