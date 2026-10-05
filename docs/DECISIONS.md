# Architecture decision records

Newest last. An ADR is never edited after the fact; a later ADR supersedes it.

## ADR-001: Stack and module layout

**Context.** A real-time pet game with online competitions needs one source
of truth for rules on the phone (offline play, prediction) and on the server
(authority, anti-cheat).

**Decision.**
- Android: Kotlin, Jetpack Compose, Material 3 with an own palette, AGP 9
  with built-in Kotlin, minSdk 26, compile/target SDK 36 (Google Play
  requirement in 2026).
- Rules: `:core:sim`, a pure Kotlin/JVM module with no Android dependency,
  shared by `:app` and `:server`. Shared value types live in `:core:model`.
- Server: Ktor 3 on the JVM, PostgreSQL via docker-compose.
- Build: Gradle 9.6 wrapper, version catalog, convention plugins in
  `build-logic/` (`wot.jvm.library`, `wot.android.*`, `wot.quality`,
  `wot.coverage`, `wot.server`).
- Quality: Spotless + ktlint (official style), detekt, Android Lint with
  warnings as errors, JUnit 5 + Kotest property tests, Robolectric +
  Roborazzi screenshots, Kover (>= 90% lines on `:core:sim`).
- Dependency freshness is Dependabot's job, so Lint's "newer version"
  checks are disabled.
- Modules and libraries are added by the first iteration that needs them,
  never as empty placeholders: `:core:data`, `:core:network`, `:core:ui`,
  further `:feature:*` modules, Hilt, Room, Navigation, WorkManager and
  Glance arrive with iterations 2-9.

**Rejected.** Kotlin Multiplatform for the shared rules: the server and the
app are both JVM, so a plain JVM module gives the same sharing without KMP's
build cost. Firebase as the backend: it cannot replay competition input logs
with our own physics, which is the core of fair competitions.

## ADR-002: JVM-only build mode

**Context.** Some development environments (cloud sessions) cannot reach
`dl.google.com`, which hosts the Android SDK and Google's Maven repository.
There the Android modules cannot even be configured.

**Decision.** `-Pwot.jvmOnly=true` (or `WOT_JVM_ONLY=true`) makes
`settings.gradle.kts` include only `:core:sim`, `:core:model` and `:server`,
and makes build-logic skip the Android convention plugins. `./gradlew
jvmCheck -Pwot.jvmOnly=true` runs every gate that needs no SDK. In such an
environment green CI on the pushed commit is the Android half of the
iteration gate. Google's repository is restricted to `com.android*`,
`com.google*` and `androidx*` groups, so everything else resolves from Maven
Central without touching the blocked host.

**Consequence.** Game rules belong in `:core:sim` anyway (CLAUDE.md
section 2), so the most important tests run everywhere.

## ADR-003: Screenshot review through a branch, not artifacts

**Context.** Development sessions cannot download GitHub Actions artifacts
(blob storage is outside their network policy), yet every screen change has
to be looked at (CLAUDE.md section 4).

**Decision.** Reference screenshots are committed in
`*/src/test/screenshots`. CI verifies against them; when they are missing
or differ, CI records the new images and force-pushes them to
`screenshots/<branch>`. The reviewer fetches that branch, looks at the PNGs
and, if they are right, copies them into the working branch in a normal
commit. Differences fail CI until that commit lands, so references never
change unreviewed. Robolectric test failures print full stack traces to the
log for the same reason.

## ADR-004: Competitions and rewards first

**Context.** The product owner's direction (2026-10-05): the game must be
pleasant for children, engaging, rewarding and competitive online. The
original roadmap put online competitions at iteration 28, after months of
offline care features, and paid the player nothing until iteration 10.

**Decision.** Re-order the roadmap: the care loop (iterations 1-4), then the
competition engine and the first race (5), then online ghost races with
server-verified times and leaderboards (6), then more disciplines, a shop to
spend rewards, training that links care to race form, and daily quests.
Every iteration from 3 on gives the player something: coins, XP, medals,
stickers or cosmetics. Asynchronous ghost races come before live races
because they work with any number of players, never make a child wait in a
lobby, and cannot lag.

**Rejected.** Live races first (needs a player base and netcode before
anyone has a reason to play); paid boosts for races (CLAUDE.md section 2).

## ADR-005: Asset sources (superseded by ADR-006)

**Context.** Kids' games live on polish: sound on every tap, medals, a
readable world. Drawing and composing everything ourselves is slow and
worse than what exists under CC0. The development environment's network
policy blocks kenney.nl, opengameart.org and freesound.org, but public
GitHub repositories are readable.

**Decision.**
- The pet stays procedural (vector rig + genome, iteration 2): uniqueness
  per player is a feature no sprite pack gives.
- Sound, medals, UI pieces and platformer tiles come from Kenney's CC0
  packs: "Kenney Asset Pack 1" mirrored at github.com/iwenzhou/kenney (UI,
  Casino, RPG, Digital and Jingle sounds; Platformer assets; Onscreen
  controls; UI pack; Medals) and "Interface Sounds" at
  github.com/Calinou/kenney-interface-sounds. Both carry Kenney's CC0
  licence file; every imported file is listed in `docs/ASSETS.md` with its
  source path.
- Only the files a feature uses are copied into the app, re-encoded where
  that shrinks them, to stay inside the APK budget.
- If kenney.nl is allowed in the environment later, newer Kenney packs
  (e.g. Animal Pack, Food Kit) may be added the same way.

**Rejected.** AI-generated art and audio (unclear licensing for a
children's product); paid asset stores (licence terms vary per asset and
cannot be checked from here).

## ADR-006: Production-quality assets only

**Context.** The product owner rejected Kenney's packs (ADR-005): a
children's game competes on polish, so assets must be production quality,
not the first free pack found. The development environment's network
policy blocks most asset sites (kenney.nl, opengameart.org, freesound.org,
archive.org, itch.io, pixabay.com, sonniss.com); GitHub, npm, PyPI and
Maven Central are reachable.

**Decision.**
- **Pet:** stays procedural (vector rig + genome): a unique pet per player.
- **Items, rewards, icons:** Microsoft Fluent Emoji, 3D style (MIT,
  github.com/microsoft/fluentui-emoji): food, toys, soap, medals, trophies,
  coins, stars in one consistent, high-end style. Converted to lossless
  WebP at the sizes the app draws, listed in `docs/ASSETS.md`.
- **UI and reward sounds:** SND (snd.dev, designed by professional sound
  designers; free for commercial use, credited in-app). Its terms forbid
  redistributing the unmodified files as standalone assets, so the sounds
  are not committed: the build downloads the pinned npm package
  `snd-lib@1.2.4` (SHA-256 checked) and embeds the sound sprite in the APK,
  where it is part of the product. The app decodes the sprite once and plays
  slices.
- **Typeface:** Fredoka (SIL OFL), rounded and highly legible for children.
- **Still wanted, waiting on network access:** Google's Material Design
  sound resources (CC-BY 4.0, archive.org) for richer game sounds (eating,
  bubbles, coins); professional GDC bundles from Sonniss. Allowing
  archive.org and sonniss.com in the environment unblocks them.

**Rejected.** Kenney (product owner: not premium enough); AI-generated art
and audio (unclear licensing for a children's product); itch.io packs
(per-pack licences, unreachable).

## ADR-007: One DataStore document for the save, Room later

**Context.** Iteration 4 needs the pet, progress and settings to survive
restarts. The save is one small document today (a pet, a progress record,
two toggles).

**Decision.** `:core:data` stores a versioned JSON document
(`SaveFile`, kotlinx.serialization) in a typed DataStore: atomic writes, no
schema, trivial migrations by `version`. A corrupt file starts a fresh game
instead of crashing. The ViewModel saves after every care action and every
15 seconds. Room arrives when the data becomes collections that need
queries (inventory, album, race history); the repository interface stays.
No DI framework yet: `WotApplication` owns the repository and passes it to
the screen; Hilt arrives with the second screen that needs shared objects.

**Clock.** Time never runs backwards for the pet (iteration 1), and the
daily coin cap ignores a date wound back (it keeps the latest day seen). A
date wound forward cannot be detected offline; it only makes the pet
hungrier, and server time takes over in iteration 6.
