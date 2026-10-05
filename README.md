# World of Tamagochi

An online virtual-pet game for Android: raise a pet that lives in real time,
watch it grow into a unique adult, decorate its world, visit friends, and race
other players' pets in sprints, agility courses and timed platformers.

The game is built iteratively. `CLAUDE.md` is the project constitution
(product, rules, architecture, quality gates, iteration protocol) and
`docs/ROADMAP.md` is the plan. Every `/goal` run in Claude Code delivers the
next iteration.

## What is in the game today

- A themed home screen with a rocking egg; the server answers `GET /health`.
- Game rules (`core/sim`): five needs (satiety, energy, hygiene, happiness,
  health) living in real time, a sleep window in the player's time zone, and
  exact offline catch-up. Not on screen yet: the living pet arrives in
  iteration 2.

## Build and run

Requirements: JDK 21 (17+), Android SDK 36 (`scripts/setup-android-sdk.sh`
installs it).

```bash
./gradlew check                        # format, detekt, lint, tests, coverage
./gradlew :app:installDebug            # run on a device or emulator
./gradlew :server:run                  # server on http://localhost:8080/health
docker compose up --build              # server + PostgreSQL
```

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
| `feature/home` | home screen |
| `app` | Android application |
| `server` | Ktor game server |
| `build-logic` | Gradle convention plugins |

## Playtesting

Write what you notice into `docs/FEEDBACK.md`. Items marked `[!]` are
handled before anything else in the next iteration.
