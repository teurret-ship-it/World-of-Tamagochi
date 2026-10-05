# Changelog

Newest first. Each iteration ends with a market self-review (CLAUDE.md 5.7):
1-5 on clarity, game feel, retention hooks, ethics, performance,
accessibility. Every score below 4 has a matching task in ROADMAP.md.

## Iteration 5a: the competition engine

The rules for racing, in `:core:sim`, shared with the future server.

- 60 Hz integer physics: speed, sprint and stamina (running dry means
  resting to 25% before sprinting again), jumps, hurdles (touching one means
  a stumble), puddles (slow unless jumped), boost pads.
- Runs are input logs of changes; replay reproduces them frame by frame;
  finish times are sub-tick exact.
- Three launch sprint tracks (Meadow ~32 s, Beach ~38 s, Snow ~43 s),
  seeded and provably clearable; medals measured against an autopilot's
  author time (gold +4%, silver +12%, bronze +30%).
- Stats (speed, stamina, agility, jump) change a run by 3-4%; skill by
  22-26% (balance table printed by the tests).

Self-review: clarity 4, game feel n/a (no screen yet: 5b), retention 4,
ethics 5, performance 5, accessibility n/a.

## Iteration 4: the pet is still there tomorrow

- The game is saved (DataStore JSON, ADR-007) after every care action and
  every 15 seconds, and loaded on start; a corrupt save starts fresh instead
  of crashing.
- Coming back after 30+ minutes shows a "Welcome back!" card with the pet
  and what changed per need.
- Settings (gear in the top bar): sound and vibration toggles that apply to
  the whole game, and credits for SND, Fluent Emoji and Fredoka.
- The pet always sleeps in the player's current time zone; a date wound
  back no longer resets the daily coin cap.
- Fixes from CI on iteration 3: "coins" is a plural resource; a test helper
  no longer counts buffered effects from earlier actions.

Self-review: clarity 4, game feel 4, retention 4, ethics 5, performance 4,
accessibility 4.

## Iteration 3: care with juice and rewards

Caring for the pet now feels good and pays.

- Feed (drag the apple to the pet or tap), wash (pick up the soap and rub),
  play (costs a little energy and food), lights off for a nap and back on.
  Rules in `:core:sim` (`CareRules`), one test per action.
- A full, clean, tired or sleeping pet says so in a speech bubble with a
  head shake: never a scolding.
- Every action: a sound (SND, ADR-006), a haptic tick, particles (hearts,
  bubbles, sparkles, stars, coins: Fluent Emoji 3D) and "+N" labels.
- Player XP and levels (50, 100, 150... XP per level) with a progress ring
  and a level-up banner and fanfare; coins for answering a real need,
  capped at 60 a day.
- Fredoka typeface across the game.
- Assets: Microsoft Fluent Emoji 3D (MIT), Fredoka (OFL), SND sounds
  downloaded at build time (never committed).

Self-review: clarity 4, game feel 4, retention 3, ethics 5, performance 4,
accessibility 3.
- Retention 3: coins have nothing to buy yet (iteration 8, shop) and
  progress is lost on restart (iteration 4, persistence).
- Accessibility 3: no sound/haptics toggle yet; added to iteration 4.

## Iteration 2: a living pet

The egg is replaced by a pet that looks alive and is unique to each player.

- Vector rig drawn in Compose Canvas: body with markings and shading, ears
  (round, pointy, floppy, none), optional tail, eyes with lids and
  highlights, mouth; everything scales with the screen.
- Genome from a seed (SplitMix64, pinned by a reference-value test): hue,
  contrasting marking hue, pattern, ears, tail, proportions, eye size.
- Eight faces (happy, content, hungry, sleepy, dirty, sad, sick, asleep),
  picked by urgency in `:core:sim`; the most urgent need is highlighted.
- Breathing, irregular blinking, eyes following the finger; rubbing the pet
  strokes it with a bouncy squash and a haptic tick; TalkBack users get a
  "Stroke Mochi" action.
- Home screen: pet plus five need bars (Tummy, Energy, Clean, Fun, Health),
  two-pane on tablets, scrolls at 200% font.

Self-review: clarity 4, game feel 3, retention 3, ethics 5, performance 4,
accessibility 4.
- Game feel 3: strokes react, but nothing else does yet; care actions with
  juice (particles, sounds) are iteration 3.
- Retention 3: still no reward loop; see ADR-004 and the re-ordered roadmap.

## Iteration 1: needs that live in real time

The pet now has five needs (satiety, energy, hygiene, happiness, health)
that change in real time, in `:core:sim`, shared by the app and the server.

- Exact integer arithmetic: 1 tenth of a point per hour is 1 unit per ms, so
  simulating 3 days at once equals simulating them minute by minute (property
  test), and a year offline takes a few hundred steps.
- The pet sleeps in the player's sleep window in their time zone (DST-safe);
  asleep, needs fall at least 4x slower, happiness not at all, and energy
  recovers.
- Health falls only while satiety or hygiene is empty.
- A rewound clock changes nothing.
- Ethics test: morning, midday and evening care keeps every need above zero
  at every life stage for a week.

Self-review: clarity 4, game feel n/a (no UI yet), retention 3, ethics 5,
performance 5, accessibility n/a.
- Retention 3: needs alone give a reason to come back, not a reward for
  coming back. Covered by iterations 2-3 (living pet, satisfying care) and 7
  (respectful reminders).

## Iteration 0: project skeleton

Modules, convention plugins, quality gates wired into `check`, CI with
release build and APK budget, server health endpoint, JVM-only mode for
environments without the Android SDK, constitution, roadmap, benchmark,
ADRs.

Self-review: clarity 4, game feel 2, retention 1, ethics 5, performance 4,
accessibility 4.
- Game feel 2 and retention 1: there is no game yet, by design. Iterations
  1-3 (needs, living pet, care actions) are the answer.
