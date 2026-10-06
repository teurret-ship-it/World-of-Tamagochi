# Changelog

Newest first. Each iteration ends with a market self-review (CLAUDE.md 5.7):
1-5 on clarity, game feel, retention hooks, ethics, performance,
accessibility. Every score below 4 has a matching task in ROADMAP.md.

## Iterations M1-M2: My Magical Pet and five species

- The game is now "My Magical Pet" (ADR-011): name, logo, application id
  and code packages. Art direction: colourful and storybook, never neon.
- Five magical species, each with its own look drawn on the rig: dragon
  (horns, bat wings, back spikes, spade tail, belly plates), griffin (white
  feathered head, beak, feathered wings, tufted tail), unicorn (pearly body,
  golden horn, pastel rainbow mane and tail), phoenix (flame crest and
  plumes, warm wings, small beak) and kitsune (big fox ears, white muzzle,
  forehead mark, more tails as it grows: one to nine).
- Each species has its own colours and its own egg: scales, feather
  chevrons, a rainbow band with stars, rising flames, misty swirls.
- A new game offers one egg of every species, named, so the player picks
  the creature they want.

Self-review: clarity 4, game feel 4, retention 5, ethics 5, performance 4,
accessibility 4.

## Iteration 13: title screen and hatching (pulled forward by playtest)

- A title screen opens the game: "World of Tamagochi" with bouncing,
  many-coloured letters, your pet (in its outfit) saying hello, and Play,
  Races and Shop. A new player sees three rocking eggs and "Get your pet!".
- First launch tells a short story in four steps: choose one of four eggs
  (each shell wears the colours and markings of the pet inside, "Show me
  other eggs" for more), pick a name from the safe list (no typing), say
  when you go to sleep, then tap the egg: it wobbles, a crack grows with
  every tap, and on the fifth the top of the shell flies off and the pet
  bounces out among stars and hearts with a fanfare.
- Back from home returns to the title screen.

Self-review: clarity 5, game feel 4, retention 4, ethics 5, performance 4,
accessibility 4.
- Game feel 4: the first-session guide (arrows to the first feed, stroke and
  race) is iteration 13b.

## Iteration 9b: playable at night

- Playtest: opening the game in the evening showed a sleeping pet and
  nothing to do. Now Wake up works at night too: the pet stays up for an
  hour (needs drain at the daytime pace), then dozes off; Lights off puts
  it back to bed at once.
- The night hint says "Mochi is asleep until 7:00. Tap Wake up to play
  now!"; up late it says how to send the pet back to bed.
- Sleep time in settings: bedtime and wake-up in half-hour steps, so the
  pet sleeps when the player sleeps.
- Test builds: every green main is published as an installable APK
  (pre-release "test-build").

Self-review: clarity 5, game feel 4, retention 4, ethics 5, performance 5,
accessibility 4.

## Iteration 9: training and race form

- A training card on the races screen: Sprints (speed), Jogging (stamina),
  Hoops (agility) and Jump rope (jump), with bars and "+8 Speed!"; three
  sessions a day, each costing energy and food. Rookies learn fast,
  champions slowly.
- Race-day form: "Top form! Ready to race." or "Mochi is hungry and will
  run slower. Feed first for top form!"; a pet in poor form is about 2-3%
  slower, enough to cost a medal.
- Trained stats go online with every run; the server replays with them and
  rejects stats no amount of daily training could reach (ADR-010). Ghosts
  and records replay with the stats they were raced with.
- The home screen keeps changes made elsewhere (a training session) instead
  of overwriting them.
- Bigger pets in races on tall phones (the camera zooms in), with more of
  the course ahead of the runner.

Self-review: clarity 4, game feel 4, retention 5, ethics 5, performance 4,
accessibility 4.

## Iteration 8: agility

- A second discipline: agility courses with tyres to jump through, tunnels
  to crawl through and seesaws to walk over, plus hurdles. Every fault adds
  two seconds, shown in red next to the clock as it happens.
- Three hand-made courses: Puppy Park (gentle), Hoop Hills (tyres galore)
  and Twisty Trail (everything, with puddles and boost pads).
- Same Sprint and Jump buttons: let go of Sprint before a seesaw (a turtle
  sign marks the spot); tunnels refill stamina.
- The coach clears every course without a fault; medals, ghosts, records,
  online verification and leaderboards work as for sprint.
- Pets wear their outfits in every race; the coach wears a cap.

Self-review: clarity 4, game feel 4, retention 4, ethics 5, performance 4,
accessibility 3.
- Accessibility 3: obstacles are told apart by shape and colour only; an
  audio cue before each obstacle is planned with the accessibility audit
  (iteration 32).

## Iteration 7b: treats

- A Treat button (cookie) in the care bar: 15 coins for a little food and a
  lot of joy, with hearts and a happy jingle; three a day, shown on a badge.
- Friendly refusals: "Race for coins, then treats!" and "Yum! No more
  treats today"; a full pet still says "I'm full!".
- Treats never pay coins back (no earn-spend loop); the daily limit
  survives a wound-back clock.

Self-review: clarity 4, game feel 4, retention 4, ethics 5, performance 5,
accessibility 4 (the button announces how many treats are left).

## Iteration 7: shop and wardrobe

- 14 cosmetics in three slots (head, face, neck), drawn on the rig with
  Fluent Emoji 3D and anchored to the body so they follow every shape,
  breath and squash; bells and pendants hang on a drawn collar.
- New Shop (button next to Races): a fitting room with a free try-on, buy,
  wear and take off, stars and sparkles with a fanfare on every purchase,
  "N more coins to go" and a hint where coins come from.
- The outfit shows at home, on the welcome-back card, in races and on your
  own ghost; the coach wears a cap.
- Saved in the game file; items that leave the catalog are dropped safely.
- Economy test: first item on day one, never more than a week without
  something new, whole wardrobe in about five weeks.
- CI: online client uses the platform HTTP engine (OkHttp needed SDK 37);
  failed runs print what went wrong at the end of the log.

Self-review: clarity 4, game feel 4, retention 5, ethics 5, performance 4,
accessibility 3.
- Accessibility 3: tiles announce name and price, but the fitting room has
  no audio description of the outfit yet (iteration 32).
- Rival ghosts do not show their owner's outfit yet (needs the outfit on the
  server; with live races, iteration 18).

## Iteration 6b: online races in the app

- New `:core:api` (the wire format shared by app and server) and
  `:core:network` (Ktor client, offline-first `OnlineRacing`).
- Every finished race is queued and uploaded; the anonymous account is
  created on the first upload; runs raced offline go up later; refused runs
  never block the queue.
- Racing online: the ghost is the real player just ahead of you ("Rival:
  Comet #4821"); the finish card shows today's online rank; each track card
  shows today's top three.
- The server address comes from the build (`-Pwot.serverUrl=...`); without
  it the app is fully offline and hides online parts (ADR-008).
- End-to-end tests run the app's client against the real server in memory.
- Fix: the first frame after "Go!" stepped one physics tick too many.
- CI prints failing tests, lint and detekt findings at the end of a failed
  log.

Self-review: clarity 4, game feel 4, retention 5, ethics 5, performance 4,
accessibility 3.
- The online parts need a deployed server (ADR-008): a product-owner step.

## Iteration 6a: the racing server

- Anonymous players with safe display names ("Pip #4821").
- Runs are verified by replaying the input log with the shared physics;
  bad logs, unknown tracks, other rules versions and unfinished runs are
  rejected; uploads are rate limited.
- Daily, weekly and all-time leaderboards (each player's best) and ghosts of
  the players just ahead of you.
- PostgreSQL with Flyway migrations; docker compose runs server + database.

Self-review: clarity 4, game feel n/a (server), retention 5, ethics 5,
performance 4, accessibility n/a.

## Iteration 5b: the first race

- Races button on the home screen, a track list with medal times and
  personal bests, and three sprint courses with their own skies, parallax
  hills, hurdles, puddles, boost pads and a chequered finish.
- The player's own pet runs (a jelly hop; surprised when it stumbles,
  sleepy when exhausted) next to a ghost: the coach on a first run, then the
  player's best.
- Sprint (hold) and Jump (tap) buttons act on press; countdown with sounds;
  finish card with time, medal, "New record!", XP and coins and the next
  medal to chase.
- Rewards (`RaceRewards` in `:core:sim`): 3 coins per finish (30 a day),
  one-time coins for each new medal tier on a track (5/10/20/30), XP.
- Records and best ghosts are saved per track; the save is updated
  atomically so the home screen never overwrites race rewards.
- CI fixes: DataStore reopen test, nullable state in a home test; Android
  test failures now print in full in the CI log.

Self-review: clarity 4, game feel 4, retention 5, ethics 5, performance 4,
accessibility 3.
- Accessibility 3: races need timing; an assisted mode (auto-jump, not
  ranked) is planned in iteration 32 and moves earlier if playtests ask.

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
