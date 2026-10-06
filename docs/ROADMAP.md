# Roadmap

Each iteration is one vertical slice a player can see. Every item lists the
player goal, acceptance criteria (checkable) and the tests that prove them.
Tick an item with `[x]` and the commit hash once it is pushed. Split items
that are too big (N -> Na, Nb). When the list runs out, the iteration
protocol (CLAUDE.md 5.2d) appends the next five from a gap analysis.

Order follows ADR-004: the game must be fun for kids, engaging, rewarding
and competitive online, so the first online competition comes right after
the core care loop, and every iteration from 3 on pays the player something.

## Phase A: Core loop and first online competition

- [x] **0. Skeleton.** (ddb40a6, CI green at b5469a2) Goal: a project anyone can build, test and ship.
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
- [x] **1. Needs simulation.** (d8eb6a9) Goal: the pet's needs live in real time.
  - `:core:sim`: hunger, energy, hygiene, fun, health (Gauge 0..100); decay
    rates per life stage; sleep window with >= 4x slower decay; health drops
    only while another need is empty.
  - Analytic `advance(state, from, to, rules)`: cost independent of elapsed
    time; 3 days offline computed in intervals.
  - Property tests: needs stay in 0..100; needs never rise without an
    action (energy only while asleep); `advance(a->c) == advance(b->c) after
    advance(a->b)`; a 3-check-ins-a-day player never lets any need hit zero
    (ethics rule).
- [x] **2. Living pet.** Goal: the pet looks alive.
  - Vector rig from a genome (color, pattern, ear shape, proportions);
    breathing, blinking, eyes follow the finger; 6 expressions (happy,
    hungry, sleepy, dirty, sick, sad) chosen from needs.
  - Stroking gesture with reaction and haptics.
  - Screenshot of every expression, 4 variants for the home screen.
- [x] **3. Care with juice and rewards.** Goal: caring feels good and pays.
  - Feed (drag food to the mouth, button alternative), wash (scrub with
    foam), lights off (sleep), play (ball). Rules in `:core:sim`
    (`CareAction` -> state), one test per action.
  - Every action: animation, sound (SND, ADR-006), haptics, "+N"
    floating over the pet, particles (hearts, bubbles, sparkles, stars,
    coins) drawn with Fluent Emoji 3D.
  - Player XP and level with a level-up celebration and jingle; coins for
    caring for a pet in need (capped per day, so caring is never grinding).
- [x] **4. Persistence and "while you were away".** Goal: the pet is still
  there tomorrow and greets you.
  - DataStore save (ADR-007); catch-up on launch; a friendly card
    summarizing what happened; clock-rollback protection.
  - Settings: sound and haptics toggles (CLAUDE.md section 2), credits for
    SND, Fluent Emoji and Fredoka.
- [x] **5a. Competition engine.** (f6c4b8a) Goal: races that are fair and provable.
  - `:core:sim`: 60 Hz integer physics (millimetres, ticks, per-mille),
    input log of changes, replay that reproduces a run frame by frame,
    sub-tick finish times, seeded tracks, autopilot, medals vs. author time.
  - Tests: determinism, replay = live run, every track clearable, stats
    within 10%, tampered logs cannot claim a time, physics pinned.
- [x] **5b. Sprint race screen.** Goal: the first race.
  - Side-scrolling course (hurdles, puddles, boost pads), the player's pet
    running with the rig, big Sprint (hold) and Jump (tap) buttons, stamina
    bar, countdown, own-record ghost, medal reveal, coin and XP rewards
    (daily cap), records and best ghosts saved per track, finish replay.
- [x] **6a. Racing server.** (7394c93) Goal: online races that cannot be faked.
  - Anonymous players (random token stored as a SHA-256 hash), display
    names from the safe pet-name list plus a number.
  - `POST /v1/runs`: the server replays the input log with `:core:sim` and
    stores the time it computed; malformed, unknown-track, wrong-version and
    unfinished runs are rejected; 12 runs a minute per player.
  - Daily (UTC), weekly (Monday) and all-time leaderboards with each
    player's best; ghosts of the players just ahead of you.
  - PostgreSQL via JDBC + Flyway; H2 in PostgreSQL mode for tests.
- [x] **6b. Online races in the app.** (1758013, CI green at 4b8f69a) Goal: race other players today.
  - `:core:network` (Ktor client), anonymous registration on first online
    race, run upload after each finish, leaderboards (daily/weekly) on the
    track list, race a real player's ghost; works offline and syncs later.
  - Needs a deployed server (ADR-008): until then the app hides online
    features.
- [x] **7. Shop and wardrobe.** (f15bfb0, CI green) Goal: spend what you earn. (Moved ahead of
  agility: coins had nothing to buy, so rewards felt empty.)
  - 14 cosmetics (head, face, neck) drawn on the rig from Fluent Emoji 3D,
    following breath and squash; worn at home, in races and by your ghost.
  - Shop with a free fitting room, buy/wear/take off, celebrations, a
    gentle "N more coins to go" with where coins come from.
  - Economy test: a first item on day one, something new at least weekly,
    the whole wardrobe in about five weeks.
- [x] **7b. Treats.** Coins buy treats that cheer the pet up: a repeatable
  sink once the wardrobe is complete. 15 coins, 3 a day, refused when
  full, never paid back in coins; a badge shows how many are left today.
- [x] **8. Agility course.** Goal: a second discipline that rewards
  precision, not just speed (ADR-009).
  - Tyres (jump through the ring), tunnels (crawl, catch your breath; no
    jumping in), seesaws (arrive at a trot, a turtle sign says when), and
    hurdles; every fault adds 2 s, shown at once on the clock.
  - Three hand-made courses (Puppy Park, Hoop Hills, Twisty Trail), each
    cleared by the autopilot without a fault; medals, ghosts, records,
    online upload and leaderboards reuse 5 and 6.
  - Track list in two sections, Sprint and Agility; "Clean run!" or
    "2 faults (+4 s)" on the finish card.
- [ ] **8b. Weave poles.** The slalom from the original plan: a rhythm
  challenge (alternating taps), needs a third input kind in the log.
- [x] **9. Training and race form.** Goal: care and training matter in
  competitions (ADR-010).
  - Four exercises (sprints, jogging, hoops, jump rope) raise speed,
    stamina, agility and jump; gains shrink as a stat grows; three sessions
    a day, each costing energy and food (and a little fun gained).
  - Race-day form: a hungry or tired pet runs below its stats (about 2-3%
    slower at worst, enough to cost a medal); the races screen says why and
    what helps.
  - Runs, records, queued uploads and ghosts carry the stats they were
    raced with; the server checks them against what training allows.
  - Tall phones zoom the race camera in so the pet is twice as big.
- [x] **9b. Playable at night** (playtest [!], f2c0a04). Wake up works during the
  sleep window (up for an hour, needs drain at the daytime pace), Lights
  off sends the pet back to bed, the hint names the wake-up time, and the
  sleep time is set in settings (half-hour steps).
- [ ] **10. Daily quests, streaks and achievements.** Three short daily
  quests (care + race), streak with a weekly freeze, sticker album of
  achievements.

## Phase B: Grow, belong, compete more

- [ ] **11. Timed platformer.** 30-90 s levels, jump and double jump,
  checkpoints, instant restart, medals, online ghosts; art from a
  production-quality source per ADR-006.
- [ ] **12. Life cycle and evolution.** Egg -> baby -> child -> teen ->
  adult; at least 6 adult forms from care quality and favourite discipline;
  transformation animation; album.
- [x] **13. Title screen and onboarding (FTUE).** (25465da) Pulled ahead by playtest
  feedback ("no pet choice, no main menu, no intro, no hatching").
  - Title screen as the main menu: bouncing logo, your pet saying hello,
    Play / Races / Shop; "Get your pet!" for a new player.
  - Four steps, one decision each: choose one of four eggs (shell in the
    colours of the pet inside, "show me other eggs"), pick a name from the
    safe list, set bedtime and wake-up, tap the egg five times to hatch it
    (wobble, growing crack, shell flies off, the pet bounces out among
    stars, fanfare).
- [ ] **13b. First-session guide.** Arrows and speech bubbles for the first
  feed, stroke and race; < 60 s to the first stroke; first race within the
  first session.
- [ ] **14. Sickness without cruelty.** Sickness and medicine, "journey"
  instead of death, rescue quest, vacation mode.
- [ ] **15. Leagues and weekend cups.** Divisions with weekly promotion and
  relegation, matchmaking by rating and stat division, cosmetic trophies.
- [ ] **16. Friends.** Friend codes/QR, friend leaderboards, "a friend beat
  your record" (in-app), block and report.
- [ ] **17. Visits and gifts.** A friend's room, joint stroking/feeding
  bonus, gifts, preset phrases and stickers only (no free text).
- [ ] **18. Live races.** Lobbies of 4-8 (friends or matchmaking, ghosts
  fill gaps after 10 s), WebSocket, authoritative server, prediction and
  reconciliation, smooth at 150 ms / 2% loss (simulated-network test),
  emote reactions.
- [ ] **19. Respectful notifications.** WorkManager, channels, permission
  asked in context, max 3 a day, never in the sleep window.
- [ ] **20. Home-screen widget (Glance).** The pet and its most urgent need.
- [ ] **21. Room decorating.** Grid placement, furniture affects mood,
  trophy shelf, several rooms.
- [ ] **22. Personality.** Traits from care history shaping reactions,
  picture speech bubbles and running style.

## Phase C: LiveOps, monetization, release

- [ ] **23. Google account linking** (Credential Manager), cloud save across
  devices ("two devices, one pet" test), account deletion.
- [ ] **24. Remote config and feature flags.**
- [ ] **25. Seasonal events.** Seasonal Grand Prix with a temporary track
  and themed cosmetics.
- [ ] **26. Season pass** (free + paid track, cosmetics only).
- [ ] **27. Google Play Billing** with server-side validation, restore,
  parental purchase controls.
- [ ] **28. Rewarded ads** (UMP, child mode, daily cap).
- [ ] **29. Privacy-respecting analytics** (FTUE funnel, retention, race
  participation; KPI board; A/B via flags).
- [ ] **30. Generations.** Offspring inherit genes and some race aptitude;
  family tree.
- [ ] **31. Performance.** Baseline Profiles, Macrobenchmark, 60 fps races
  on a mid-range device.
- [ ] **32. Accessibility audit.** TalkBack, colour-blind mode, reduced
  motion, assisted race mode (not ranked).
- [ ] **33. Localization** (the game is authored in English; DE, ES, PT-BR,
  PL first).
- [ ] **34. Release readiness.** Crash reporting, privacy policy, Data
  Safety, Families Policy checklist, signed AAB, Play internal track.

Next: the gap analysis against BENCHMARK.md picks the following iterations
(protocol 5.2d), e.g. a track editor with moderation, team relays and clubs,
new disciplines (swimming, frisbee), Wear OS, AR "pet in your room".
