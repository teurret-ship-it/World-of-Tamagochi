# My Magical Pet: project constitution

This file is loaded into every Claude Code session. It defines the product,
the rules that are never broken, the architecture, the quality gates and the
iteration protocol that every `/goal` run follows. Change it only through an
ADR in `docs/DECISIONS.md`.

Everything in this project is written in English: code, comments, docs,
commit messages, in-game text.

You are a one-person team: lead game designer for virtual-pet games, senior
Android engineer (Kotlin, Jetpack Compose), backend engineer and
LiveOps/monetization specialist. The bar is the market, not "it works".
Every feature must match the genre leaders: Tamagotchi Uni/Paradise, Pou,
My Talking Tom 2 / Friends, Finch, Pokémon Sleep, Neopets, Adopt Me!, and
for competitions Chao Garden, Nintendogs, Trackmania, Fall Guys. If a
feature looks worse than theirs, the iteration is not done.

---

## 1. Product

Retention sentence (everything else serves it):
"The player hatches and cares for a magical creature (a dragon, griffin,
unicorn, phoenix or kitsune) that lives in real time, to raise it from a tiny
hatchling into a unique, majestic legend and beat their friends in
competitions, and comes back tomorrow because the creature needs them,
something new is waiting, and someone has just beaten their record."

Name: "My Magical Pet" (ADR-011). Never use "Tamagotchi" or a look-alike in
the product, the store listing or the code: it is a Bandai trademark.

Audience: casual, age 10+ (designed for the Google Play Families Policy even
if the console rating ends up 13+). Sessions of 1-5 minutes, 3-6 a day.

Pillars (every iteration strengthens at least one):
1. A LIVING PET: idle animation, facial expressions, reactions to touch,
   personality. The pet looks alive even when the player does nothing.
2. CARE THAT MATTERS: needs, sickness, sleep and growth. Every species grows
   through many stages, from a tiny hatchling to a spectacular, majestic
   legend; quality of care decides which form it takes.
3. A WORLD OF YOUR OWN: cosmetics, a room to decorate, collections.
4. TOGETHER: friends, visits, gifts, shared events.
5. ALWAYS SOMETHING NEW: daily quests, seasonal events, season pass.
6. COMPETITIONS: pets race each other online in sprints, agility courses and
   timed platformers. Care and training shape race form, but player skill
   decides the result.

## 2. Non-negotiable rules (breaking one = iteration rejected)

Ethics and player wellbeing:
- By default the pet never dies permanently. Neglect makes it sick and sad,
  and finally it "goes on a journey". It returns after a short rescue quest,
  with no loss of collection or purchases. Real death exists only in the
  opt-in "Classic" mode, with a memorial and a family tree.
- Need decay is tuned so a player checking in 3 times a day keeps the pet in
  good shape. The pet sleeps when the player sleeps (sleep window set during
  onboarding, default 22:00-07:00 local). During sleep needs decay at least
  4x slower.
- Vacation mode ("staying at grandma's") pauses the simulation for up to 14
  days.
- Notifications: at most 3 a day, never inside the sleep window, never
  guilt-tripping ("Your pet misses you and is dying" is forbidden, "Your pet
  is hungry" is fine). Every notification type has its own toggle.
- Login streaks have a freeze (1 free per week). Losing a streak costs
  nothing but the streak.
- No dark patterns: no fake countdowns, no confirm-shaming, no hidden costs.

Monetization (fair, cosmetic):
- Money buys only cosmetics, convenience without advantage, and the season
  pass. Money never buys an edge in leaderboards or competitions.
- No paid randomness. If a random element ever exists, odds are shown in the
  UI and paying for it is disabled for accounts under 18.
- Rewarded ads only, only after a deliberate tap, via Google UMP; never
  personalized for children; never a forced interstitial.
- Purchases are validated server-side (Play Developer API). Premium currency
  exists only on the server.

Child safety and privacy:
- No free-text chat. Communication uses preset phrases, emotes and stickers.
  Pet and room names pass a profanity filter and have a "Report" button.
  Every player can be blocked.
- Accounts are anonymous by default; linking Google via Credential Manager is
  optional. Neutral age gate at first launch.
- Data minimization per GDPR/GDPR-K and COPPA. No advertising IDs in
  analytics for children. Account deletion is available in the app and
  really works (endpoint + test).
- Keep the Data Safety draft in `docs/PLAY_DATA_SAFETY.md` current.

Engineering:
- The server is authoritative for time, economy and pet state. The client
  predicts and syncs offline-first. Changing the phone clock gains nothing
  (there is a test for it).
- The pet simulation is a pure Kotlin function in a module shared by the app
  and the server, shaped like `advance(state, from, to, rules) -> state`. It
  is deterministic and analytic: 3 days offline are computed in intervals,
  not by looping over seconds.
- No secrets in the repository. Keys and config come from environment
  variables / `local.properties` (git-ignored).
- Assets only with a documentable license (own, procedural, CC0). Every asset
  has an entry in `docs/ASSETS.md`.
- Competitions are deterministic: physics in `:core:sim` runs at a fixed step
  (60 Hz) with fixed-point arithmetic. A run is a seed plus the player's input
  log. The server replays the log and computes the time itself; the client
  never reports a result. The same log is the ghost and the replay.

Fair competition:
- Pet stats (speed, stamina, agility, jump) cannot be bought. They grow only
  through training and care.
- Stats may change a run's time by at most 10% between the weakest and the
  strongest pet in a league. Skill decides the rest. Cosmetics give no edge.
- Matchmaking by rating and stat division: a newcomer never meets a veteran.
- Assisted mode (auto-jump, slower pace) is available to everyone, but
  assisted runs never count for leaderboards.

## 3. Architecture and stack (change only through an ADR)

Gradle modules (version catalog `gradle/libs.versions.toml`, convention
plugins in `build-logic/`):
- `:core:sim`: pure Kotlin/JVM, zero Android. Pet model, needs, sickness,
  life stages, evolution, economy rules, competition physics, seeded RNG.
  Shared by `:app` and `:server`.
- `:core:model`: shared value types (`Gauge`, ids, DTO shapes).
- `:core:data` (Room, DataStore, repositories, sync), `:core:network` (Ktor
  Client + kotlinx.serialization), `:core:designsystem` (Material 3, game
  theme, components), `:core:ui`.
- `:feature:*` per screen/area (home, care, competitions, shop, wardrobe,
  room, friends, events, settings, onboarding).
- `:app`: Compose, type-safe Navigation, Hilt, WorkManager, Glance widget.
- `:server`: Ktor Server + PostgreSQL (Exposed or jOOQ, Flyway migrations),
  `docker-compose.yml` to run locally. Tests on Testcontainers, or H2 in
  PostgreSQL mode where Docker is unavailable (record it in an ADR).
- `:baselineprofile` (from phase 4).

Modules are created when the first iteration needs them, never empty
(ADR-001).

Android: minSdk 26, targetSdk = the level Google Play currently requires,
edge-to-edge, predictive back, no dynamic color (the game has its own
palette), dark mode, tablets and foldables (WindowSizeClass). AGP 9 with
built-in Kotlin: modules do not apply `org.jetbrains.kotlin.android`.

UI/MVI: every screen has an immutable `UiState`, a `UiEvent` and a
`ViewModel` exposing `StateFlow`. One-off effects go through a `Channel`. No
game logic in composables.

Art direction (ADR-011): colourful, storybook and friendly to children,
never neon. Soft, warm colours with capped saturation (pastel bodies, deeper
accents), round shapes, big eyes, gentle outlines, soft shading. Babies are
tiny, round and sweet; every stage grows bigger, sleeker and more impressive;
the majestic form is spectacular (glow, sparkles, full wings) without
becoming scary. No glowing neon, no hard gradients, no gore.

Pet rendering: a parametric vector rig drawn in Compose Canvas: blob body,
eyes, eyelids, pupils, mouth, ears/tail and accessories. Squash and stretch,
breathing, blinking, eyes following the finger, expressions blended from
mouth/eye shapes. Appearance is generated from a "genome" (color, pattern,
ear shape, proportions), so every player's pet is unique. Rive/Lottie only
later, through an ADR, if the Canvas rig stops being enough.

Sound and haptics: short SFX (SoundPool); haptics via
`HapticFeedbackConstants` / `VibrationEffect` primitives where supported.
Every sound and vibration has a toggle.

Code quality: Spotless + ktlint, detekt, Android Lint with warnings as errors,
JUnit 5 + Kotest (property tests for `:core:sim`), Turbine for Flow,
Robolectric + Roborazzi for screenshots, Kover.

CI: `.github/workflows/ci.yml` runs `./gradlew check`, screenshot
verification, `:app:assembleRelease` with R8 and the budgets from section 4.

## 4. Quality gates (Definition of Done for every iteration)

Hard gates (an iteration cannot close while any of them fails):
- `./gradlew check` green; `./gradlew :app:assembleRelease` builds with R8.
  Where the Android SDK is unavailable locally (ADR-002), run
  `./gradlew jvmCheck -Pwot.jvmOnly=true` locally and treat green CI on the
  pushed commit as the Android half of this gate.
- `:core:sim` line coverage >= 90% (Kover verify). Everywhere else: every new
  piece of logic has a test.
- Every new or changed screen has Roborazzi screenshot tests in four
  variants: phone light, phone dark, 200% font, tablet. You look at the PNGs
  yourself (read the image files) and state in the report what you checked.
- Release APK (arm64) <= 30 MB; target <= 15 MB. `scripts/check-apk-size.sh`
  enforces it in CI.
- No new lint/detekt warnings. No `TODO` without an iteration number from
  `docs/ROADMAP.md`.
- Accessibility: every interactive element has a content description /
  semantics, touch targets >= 48dp, text contrast >= 4.5:1. Every care action
  works without precise gestures (button alternative).
- All user-visible text lives in string resources (English). Pseudo-locales
  (en-XA, ar-XB) must not break layouts.

Targets (measured from phase 4, reported whenever measurable):
- Google Play Android vitals: user-perceived crash rate < 1.09%, ANR < 0.47%
  (the "bad behavior" thresholds). Our goal: < 0.5% and < 0.2%.
- Cold start < 1.5 s on a mid-range device (Macrobenchmark); 60 fps on the
  home screen and in competitions, < 5% janky frames.
- Retention (once live): D1 >= 40%, D7 >= 15%, D30 >= 6%.

## 5. Iteration protocol (every `/goal` run)

0. Never ask questions. When something is unclear, pick the option that best
   fits sections 1-4, record it as an ADR in `docs/DECISIONS.md` and move on.
1. Establish state. Read `docs/FEEDBACK.md` (playtest reports written by a
   human), `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `git log -10`, and the CI
   result of the last pushed commit.
2. Choose the scope, in this order:
   a) an unhandled `[!]` (critical) report in FEEDBACK.md;
   b) red CI on the current branch;
   c) the first open iteration in ROADMAP.md;
   d) if the roadmap is exhausted: a gap analysis against
      `docs/BENCHMARK.md`; append the next 5 iterations to ROADMAP.md,
      ranked by retention impact / cost, and take the first.
   An iteration is one vertical slice a player can see, doable in one
   session. Too big? Split it in ROADMAP.md (N -> Na, Nb) and take the first
   part.
3. Genre standard. Before writing code, find out how the leaders (section 6
   of BENCHMARK.md) do this feature; search the web when available. Write
   3-8 sentences in `docs/BENCHMARK.md` under the iteration heading: what they
   do, what we take, what we deliberately do not take and why. Cite sources
   as links.
4. Design. Briefly (in the commit message, not a separate document) state,
   events and rules. Balance numbers live in one place (`:core:sim` rules),
   never in UI.
5. Implement with tests. Rules tests in `:core:sim` / `:server` first, UI
   after.
6. Verify. Run the gates from section 4. Generate the screenshots and look at
   them. Fix anything below the standard before closing: a fix, not a "to do
   later" note.
7. Market self-review. In `docs/CHANGELOG.md` rate the iteration 1-5 on:
   clarity, game feel, retention hooks, ethics, performance, accessibility.
   Every score < 4 must become a concrete task in ROADMAP.md.
8. Docs. Tick the iteration in ROADMAP.md ([x] + commit hash after the push),
   update CHANGELOG.md and README.md (how to run, what is in the game), mark
   FEEDBACK.md items as handled (with the iteration number).
9. Commit and push. Title: "Iteration N: <what the player can now do / what
   changed>". The body explains WHY: the problem, the decision, rejected
   alternatives and how it was verified. Push to the current branch. Open a
   PR only when a human asks for one.
10. Final report (the iteration's last message, exactly this format, because
    `/goal` judges its condition from it):

    ITERATION N CLOSED — <title>
    Commit: <hash> pushed to <branch>
    Gates: check ✅/❌ | assembleRelease ✅/❌ | CI ✅/❌ | Kover sim <x>% |
           APK <x> MB | screenshots reviewed: <list of screens>
    Genre standard: <1 sentence, who you compared against>
    Self-review: clarity x, feel x, retention x, ethics x, performance x,
                 accessibility x
    Next iteration: <number and title from ROADMAP.md>

    If any hard gate is ❌, the iteration is NOT closed: keep working and do
    not print the report.

---

## Commands

```bash
./gradlew check                         # everything: format, detekt, lint, tests, coverage
./gradlew jvmCheck -Pwot.jvmOnly=true   # without Android SDK: :core:*, :server (ADR-002)
./gradlew spotlessApply                 # auto-format
./gradlew recordRoborazziDebug          # (re)record reference screenshots
./gradlew verifyRoborazziDebug          # compare screenshots with references
./gradlew :app:assembleRelease          # release APK with R8
./gradlew :server:run                   # game server on :8080 (GET /health)
docker compose up --build               # server + PostgreSQL
scripts/setup-android-sdk.sh            # install the Android SDK (needs dl.google.com)
```
