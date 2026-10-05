# Genre benchmark

Who sets the bar, and what we take from them. Every iteration adds a section
below (CLAUDE.md 5.3): what the leaders do, what we take, what we
deliberately do not take and why, with sources.

## Reference titles

- **Tamagotchi (Uni / Paradise / On):** care mistakes decide the adult form;
  life stages; generations and matchmaking; character collecting; a shared
  online world (Tamaverse).
- **Pou:** feeding by dragging food to the mouth, washing by scrubbing,
  switching the light off, mini-games as the currency source, wardrobe and
  rooms, visiting friends.
- **My Talking Tom 2 / Friends:** reactions to touch and voice, very high
  "juice" (squash and stretch, particles, sound), sickness, toilet, toys.
- **Finch:** the pet grows with the player's own wellbeing, gentle
  notifications, excellent widgets, no punishment, friends sending "good
  vibes".
- **Pokémon Sleep:** a daily rhythm aligned with the player's life, morning
  report.
- **Neopets / Adopt Me!:** economy, collections, seasonal events, safe
  communication through preset phrases, trading (ours: gifts only, no
  trading, because of children).
- **Chao Garden (Sonic Adventure 2):** the model for linking care to
  competition. Feeding and training change stats, and the pet enters races
  and tournaments.
- **Nintendogs:** agility, disc and obedience as competitions with classes
  and trophies.
- **Trackmania:** timed runs, ghosts, bronze/silver/gold/author medals,
  server-side replay validation, weekly leaderboards.
- **Fall Guys:** multiplayer obstacle races, readable and funny failures,
  short rounds, eliminations.

---

## Iteration 0: skeleton

The genre leaders ship on the same foundations: a Compose-first Android app,
a design system before screens, and an automated quality pipeline. Android's
own reference app ([Now in Android](https://github.com/android/nowinandroid))
uses build-logic convention plugins, a version catalog and screenshot tests
with Roborazzi. We take that layout as is. Google Play's
[Android vitals](https://developer.android.com/topic/performance/vitals)
"bad behavior" thresholds (crash rate 1.09%, ANR rate 0.47%) become our
upper bounds from day one, with stricter internal targets. We deliberately
do not take Hilt, Room and Navigation in iteration 0: nothing needs them
yet, and empty wiring would only be noise to review (ADR-001).

## Iteration 1: needs simulation

Tamagotchi measures hunger and happiness in hearts that drain over time;
when the pet sleeps the meters pause or drain much more slowly, and a meter
left empty becomes a care mistake that shapes the adult form
([Tamagotchi Wiki: Care](https://tamagotchi.fandom.com/wiki/Care)). Pou
keeps four needs (hunger, health, energy, fun) that keep draining while the
app is closed and refills energy by sleeping. We take five needs (satiety,
energy, hygiene, happiness, health), the slow-drain sleep, and health that
falls only as a consequence of neglect rather than on its own. We do not take
the classic Tamagotchi's fixed, character-defined bedtime: our pet sleeps
when the player sleeps (default 22:00-07:00 local, set in onboarding), the
same daily-rhythm idea Pokémon Sleep builds on. Balance is checked by a test
of the ethics rule: three check-ins a day keep every need above zero at
every life stage.

## Iteration 2: a living pet

My Talking Tom and Pou set the bar for "alive": constant idle motion,
blinking, eyes that follow the finger, and an exaggerated squash-and-stretch
reaction to every touch. Disney's principles of animation (squash and
stretch, anticipation, follow-through) are why it reads as alive rather than
as a sprite. Tamagotchi Uni and Pokémon make every individual feel unique.
We take: irregular blinking (a metronome looks mechanical), slower breathing
while asleep, pupils that track the finger, a bouncy squash on every stroke
with a haptic tick, and a procedural genome (colour, markings, ears, tail,
proportions, eye size) so no two players share a pet. Faces show exactly one
need at a time, the most urgent, so a child reads the pet at a glance
instead of scanning five bars. We do not take voice repetition (Talking
Tom's microphone needs a privacy review for children, not worth it yet) and
we do not use bitmaps: the rig is vector, so every size and skin works
without new art.
