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
