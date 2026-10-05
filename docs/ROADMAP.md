# Roadmap

Each iteration is one vertical slice a player can see. Every item lists the
player goal, acceptance criteria (checkable) and the tests that prove them.
Tick an item with `[x]` and the commit hash once it is pushed. Split items
that are too big (N -> Na, Nb). When the list runs out, the iteration
protocol (CLAUDE.md 5.2d) appends the next five from a gap analysis.

## Phase 0: Foundation and offline core

Phase gate: "I want to come back tomorrow" (protocol in `docs/PLAYTEST.md`,
written in iteration 9).

- [ ] **0. Skeleton.** Goal: a project anyone can build, test and ship.
  - Modules `:core:sim`, `:core:model`, `:core:designsystem`, `:feature:home`,
    `:app`, `:server`; build-logic convention plugins; version catalog.
  - Spotless/ktlint, detekt, Android Lint (warnings as errors), Kover >= 90%
    on `:core:sim`, Roborazzi screenshots in 4 variants.
  - CI runs check, screenshots, release build with R8, APK size budget.
  - `:server` answers `GET /health` with the rules version (test).
  - `docker-compose.yml`, `scripts/setup-android-sdk.sh`, README, ADR-001,
    ADR-002.
  - Done when: `./gradlew jvmCheck -Pwot.jvmOnly=true` green locally and CI
    green on the pushed commit.
- [ ] **1. Needs simulation.** Goal: the pet's needs live in real time.
  - `:core:sim`: hunger, energy, hygiene, fun, health (Gauge 0..100); decay
    rates per life stage; sleep window with >= 4x slower decay; health drops
    only while another need is empty.
  - Analytic `advance(state, from, to, rules)`: cost independent of elapsed
    time; 3 days offline computed in intervals.
  - Property tests: needs stay in 0..100; needs never rise without an
    action; `advance(a->c) == advance(b->c) after advance(a->b)`; a
    3-check-ins-a-day player never lets any need hit zero (ethics rule).
- [ ] **2. Living pet.** Goal: the pet looks alive.
  - Vector rig from a genome (color, pattern, ear shape, proportions);
    breathing, blinking, eyes follow the finger; 6 expressions (happy,
    hungry, sleepy, dirty, sick, sad) chosen from needs.
  - Stroking gesture with reaction and haptics.
  - Screenshot of every expression, 4 variants for the home screen.
- [ ] **3. Care actions.** Goal: caring feels good.
  - Feed by dragging food to the mouth (button alternative), wash by
    scrubbing with foam, sleep by switching the light off, play with a ball.
  - Every action: animation, sound, haptics, a "+N" over the need bar.
  - Rules in `:core:sim` (`CareAction` -> state), tests per action.
- [ ] **4. Persistence and time.** Goal: the pet is still there tomorrow.
  - Room + DataStore; catch-up on return with a "while you were away" card.
  - Clock rollback protection (monotonic time + last known server time).
- [ ] **5. Life cycle and evolution.** Goal: the pet grows up into someone.
  - Egg -> baby -> child -> teen -> adult; at least 6 adult forms decided by
    care mistakes and dominant activity; transformation animation; album.
- [ ] **6. Sickness without cruelty.** Goal: neglect has consequences, never
  cruelty.
  - Sickness and medicine; "journey" instead of death; rescue quest;
    vacation mode (up to 14 days).
- [ ] **7. Respectful notifications.** Goal: reminders that help.
  - WorkManager, channels, POST_NOTIFICATIONS asked in context (after the
    first hunger, not at launch), limits from CLAUDE.md section 2.
- [ ] **8. Onboarding (FTUE).** Goal: love at first tap.
  - Hatching as the tutorial, naming, sleep window. < 60 s to the first
    stroke, no walls of text. Screenshot test of the whole path.
- [ ] **9. Home-screen widget (Glance).** Goal: the pet on the home screen.
  - Pet and its most urgent need. Phase gate: `docs/PLAYTEST.md` (5
    people, do they come back on day 2 unprompted).

## Phase 1: Content, economy and offline competitions

- [ ] **10. Soft currency and economy model** in `:core:sim` (sources/sinks,
  30-day simulation in a test, inflation under a set threshold).
- [ ] **11. Competition engine** in `:core:sim`: fixed 60 Hz step,
  fixed-point physics, input log, replay reproduces the result frame by
  frame (test), ghost from the log. Pet stats with capped influence (test
  for the 10% cap).
- [ ] **12. Sprint race.** Stamina management and lane changes, 3 tracks of
  45-60 s, ghost of your own record, start countdown, finish replay.
- [ ] **13. Agility course.** Slalom, tunnel, seesaw, hurdle, tyre; time
  penalties for faults; one-hand controls.
- [ ] **14. Timed platformer.** 30-90 s levels, jump and double jump,
  checkpoints, instant restart, bronze/silver/gold/author medals.
- [ ] **15. Training and form.** Exercises raise stats at the cost of energy
  and hunger, with a daily cap. A hungry, tired or sick pet races worse, so
  care matters in competitions.
- [ ] **16. Shop and pantry.** Food with different effects; the pet's
  favourite flavours (personality).
- [ ] **17. Wardrobe.** Cosmetics on the rig (hats, glasses, scarves), also
  visible in competitions; preview before buying.
- [ ] **18. Room.** Grid decorating, furniture affects mood, wallpapers, a
  trophy shelf, several rooms (kitchen, bathroom, bedroom, playroom).
- [ ] **19. Daily and weekly quests**, login streak with freeze,
  achievements.
- [ ] **20. Personality.** Traits from care history that shape reactions,
  dialogue (picture speech bubbles) and running style.

## Phase 2: Online and networked competitions

- [ ] **21. Anonymous server account**, tokens, migration of local state.
- [ ] **22. Offline-first sync.** Authoritative server, action queue with
  idempotency keys, conflict resolution. Test "two devices, one pet".
- [ ] **23. Server economy.** Transaction validation, time anti-cheat (test
  with the client clock wound back).
- [ ] **24. Google account linking** (Credential Manager), account deletion.
- [ ] **25. Friends.** Codes/QR, invitations, list, block and report.
- [ ] **26. Visits.** A friend's room, joint stroking/feeding with a bonus
  for both, guest book with stickers.
- [ ] **27. Gifts and preset phrases** (safe communication).
- [ ] **28. Asynchronous competitions.** Race other players' ghosts matched
  by rating (e.g. Glicko-2) and stat division. The server replays the input
  log and confirms the time; mismatching logs are rejected (test). Weekly
  leaderboards per track: friends, country, world.
- [ ] **29. Leagues and cups.** Divisions with weekly promotion/relegation,
  weekend cups, cosmetic-only rewards + a trophy for the room.
- [ ] **30. Live races.** Lobbies of 4-8 pets (friends or matchmaking; after
  10 s ghosts fill the gaps). WebSocket, authoritative server, client
  prediction and reconciliation, smooth at 150 ms latency and 2% packet loss
  (test with simulated network). Emote reactions instead of chat.
- [ ] **31. Replays and spectating.** Watch the record holder's and friends'
  runs, learn from ghosts, share a run clip.
- [ ] **32. Push via FCM** (behind an interface, WorkManager fallback),
  including "a friend beat your record", within the limits of section 2.

## Phase 3: LiveOps and monetization

- [ ] **33. Remote config and feature flags** from the server (with in-app
  defaults).
- [ ] **34. Seasonal event calendar** (server side). First event: a seasonal
  Grand Prix with a temporary track, themed cosmetics and mini-quests.
- [ ] **35. Season pass** (free track + paid track, cosmetics only).
- [ ] **36. Google Play Billing** (latest library), server-side receipt
  validation, purchase restore, parental purchase controls.
- [ ] **37. Rewarded ads** with UMP, child mode, daily cap.
- [ ] **38. Privacy-respecting analytics.** FTUE funnel, retention, economy,
  competition participation. KPI board in `docs/KPI.md`, A/B tests via
  flags.
- [ ] **39. Generations.** An adult pet can become a parent; the offspring
  inherits genes and some competition aptitude; family tree.

## Phase 4: Premium quality and release

- [ ] **40. Baseline Profiles + Macrobenchmark**, budgets from section 4 in
  CI (competitions: steady 60 fps on a mid-range device).
- [ ] **41. Accessibility.** TalkBack audit, color-blind mode, reduced
  motion, large buttons, assisted mode in competitions.
- [ ] **42. Tablets and foldables.** Two-pane layouts.
- [ ] **43. Localization.** First additional languages (DE, ES, PT-BR, PL)
  and date/number formats; the game itself is authored in English.
- [ ] **44. Crash reporting**, privacy policy, Data Safety, Families Policy
  compliance (checklist in docs).
- [ ] **45. Release pipeline.** Signed AAB, Gradle Play Publisher to the
  internal track, versioning, release notes from CHANGELOG.

Next: the gap analysis against BENCHMARK.md picks the following iterations
(protocol 5.2d), e.g. a track editor with moderation, team relays and clubs,
new disciplines (swimming, frisbee), Wear OS, AR "pet in your room".
