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
