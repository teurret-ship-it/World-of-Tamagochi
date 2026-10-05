# Changelog

Newest first. Each iteration ends with a market self-review (CLAUDE.md 5.7):
1-5 on clarity, game feel, retention hooks, ethics, performance,
accessibility. Every score below 4 has a matching task in ROADMAP.md.

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
