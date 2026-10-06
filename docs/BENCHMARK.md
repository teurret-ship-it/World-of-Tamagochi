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

## Iteration 3: care with juice and rewards

Pou's signature is feeding by dragging food into the pet's mouth and
washing by scrubbing until foam appears; My Talking Tom answers every touch
with sound, particles and a bouncy body; Tamagotchi refuses a snack when
full rather than punishing the player. Mobile pet games pay small, frequent
rewards (coins, XP bars, level-up fanfares) and cap them so caring never
becomes a grind ([Pou](https://en.wikipedia.org/wiki/Pou_(video_game))).
We take: drag-to-mouth feeding with a tap alternative, soap mode where
rubbing scrubs, lights-off naps, play that costs energy and food, polite
refusals in a speech bubble with a head shake, "+N" labels, particles, a
haptic tick and a sound on every action, XP with a level ring and a
level-up banner, and coins only for answering a real need, capped at 60 a
day. We do not take Pou's toilet and food-poop loop (a chore with no joy
for younger children) or punishment for overfeeding.

## Iteration 4: persistence and "while you were away"

Tamagotchi Uni and Pou keep living while the app is closed; Finch greets the
player with what happened and never scolds; Pokémon Sleep opens with a
morning summary. We take: the save survives restarts, the pet is caught up
on launch, and after 30 minutes away a friendly "Welcome back!" card shows
the pet and what changed, per need, in that need's colour. We do not take
guilt-tripping wording ("your pet missed you so much it got sick"): the
card states facts and offers "Let's go!". Sound and vibration toggles and
credits for every asset author live in a settings sheet behind the gear.

## Iteration 5a: competition engine

Trackmania is the reference for fair time trials: deterministic physics,
runs stored as inputs, servers that re-simulate a run before accepting a
record, ghosts of other players and author/gold/silver/bronze medals
([Trackmania](https://en.wikipedia.org/wiki/Trackmania)). Chao Garden ties
stats grown through care to race performance. Mario Kart time trials show
that racing a ghost is engaging even with no one online. We take: integer
physics at 60 Hz (bit-identical on phone and server), an input log of
changes as the only thing a client sends, sub-tick finish times so ties are
rare, an autopilot that sets author times and proves every generated track
clearable, and stats capped so they move a run by at most 10% (measured:
3-4%; skill alone is worth 22-26%). We do not take floating-point physics
engines (Box2D and similar give different results across devices and
versions, which would make server verification impossible).

## Iteration 5b: the sprint race screen

Mobile runners for children (Subway Surfers, Talking Tom Gold Run) use big
readable obstacles, one-thumb controls and constant feedback; Trackmania
and Mario Kart make the ghost and the medal targets the reason to replay;
Fall Guys shows that failing should look funny, not punishing. We take: a
3-2-1-Go countdown, big press-to-act Sprint (hold) and Jump buttons, a
stamina bar that says "Tired!" in words, a progress bar with the ghost's
position, the coach ghost on a first run and your own best afterwards, a
jelly-hop run cycle and a surprised face on a stumble, and a finish card
with the medal, "New record!", rewards and the next medal's time. We do not
take tilt or swipe controls (harder for small hands and for accessibility)
or lives and game-over screens: every run finishes.

## Iteration 6a: the racing server

Trackmania's leaderboards only accept runs the server can replay; Mario
Kart Tour and Clash Royale show daily and weekly boards keep competition
fresh for players who are not top 100; Adopt Me! and Roblox games for
children avoid free-text identity. We take: replay verification (the
client sends inputs, never a time), each player's best per period, daily
boards that reset at 00:00 UTC and weekly boards on Monday, ghosts of the
players just ahead (the next people to beat, not the unreachable top), and
anonymous players whose names come from a fixed list. We do not take global
chat, friend search by name or any free-text field.

## Iteration 6b: online races in the app

Asynchronous multiplayer is how casual mobile games stay competitive with a
small or scattered player base: Mario Kart Tour and Trackmania show other
players' ghosts and daily boards, Clash Royale's daily rewards bring players
back. Offline-first is the norm for children's devices (tablets without
mobile data, car journeys). We take: the rival ghost is the real player just
ahead of you this week, today's top three on every track card, "Today's
online rank: #4" on the finish card, and a queue that keeps runs raced
offline and uploads them later. We do not take login walls, push to sign
in, or any online requirement to play.

## Iteration 7: shop and wardrobe

Pou, My Talking Tom and Adopt Me! all turn earned coins into visible
self-expression: hats and glasses on the pet, tried on before buying. Animal
Crossing's fitting room and Fall Guys' costumes show that cosmetics are the
reward children talk about, and that they must never change how well you
play. We take: a free try-on that shows the item on your own pet, one big
button that buys, wears or takes off, a celebration on every purchase, and
the outfit everywhere the pet appears (home, welcome-back card, races, your
ghost; the coach wears a cap so a child can tell it apart). Prices follow a
test of the economy: something new on day one, a new goal at least weekly,
the whole wardrobe in about five weeks. We do not take loot boxes, timed
"only today" offers or premium-only items: everything is bought with coins
earned by caring and racing, at a price shown up front.

## Iteration 7b: treats

Pou sells snacks that are both food and fun, and Tamagotchi's snack raises
happiness but can be overdone; both give a reason to keep spending after
the wardrobe is full. We take a cheap treat that cheers the pet up, refused
when the pet is full, and a visible daily limit of three. We do not take
paid energy refills or snacks that unlock anything: a treat is a kindness,
not a shortcut.

## Iteration 8: agility

Nintendogs and real dog agility judge a clean round as much as a fast one:
tyre, tunnel, seesaw and hurdles, with faults added to the time. Trackmania
shows that a penalty shown at the moment it happens teaches without a
lecture. We take: four classic obstacles, two seconds per fault displayed at
once on the clock, a "Clean run!" on the finish card, a turtle sign before
the seesaw so the rule is readable without words, and hand-made courses with
a gentle first one. We do not take disqualification or refusals (a child
always finishes), and we keep the two buttons from sprint rather than adding
a control per obstacle.

## Iteration 9: training and race form

Chao Garden is the reference: stats grow by training and feeding, and they
show in races. Pokémon's effort values show that diminishing returns keep
early progress exciting and late progress a long goal; Duolingo-style daily
caps keep sessions short. We take: four exercises mapped to four stats,
gains that shrink as a stat grows, three sessions a day that cost energy
and food, and race-day form that makes a hungry or tired pet visibly
slower, with the reason and the fix spelled out. We do not take paid stat
boosts or training energy refills: stats are earned only, and their effect
on a race is capped at 10%.

## Iteration 13: title screen and hatching

Tamagotchi starts every life with an egg that hatches after a short wait;
Tamagotchi Uni and Pokémon let the player choose among eggs or starters and
name them; Pou and Talking Tom open on a title with the character greeting
the player. Apple's and Google's onboarding guidance favours one decision
per screen and learning by doing. We take: a title screen that shows your
own pet, four eggs whose shells hint at the pet inside (the choice matters
and stays a surprise), naming from a safe list, the sleep window asked in
plain words, and hatching as the first interaction (tap, wobble, crack,
pop), so the very first thing a child does is play. We do not take a timed
wait for the egg (a first session must end with a pet), free-text names
(child safety), or a long tutorial before play.

## Iteration 10: daily quests, streaks and stickers

Duolingo made the daily streak with a "streak freeze" the reference
retention mechanic; Pokémon GO and Clash Royale pay small daily quests
(three to five) that steer players to different parts of the game;
Animal Crossing's Nook Miles and Neopets' trophies show that a collection of
achievements keeps long-term players going. We take: three short quests a
day across care, racing and training, paid at once (no claim button to
forget), a streak with a free weekly freeze, milestone rewards, and a
sticker album. We do not take paid streak repairs, quest rerolls for money
or loss-aversion messages ("your streak will die!"): losing a streak costs
nothing but the number.

## Iteration 14: sickness without cruelty

Classic Tamagotchi pets die within a day of neglect, which hurts children
and drives them away; Tamagotchi Uni and Finch replaced death with gentler
consequences, and Nintendogs dogs run away and come back. We take: visible
sickness with a free cure, a "journey" instead of death with a short,
kind rescue that restores everything, and a vacation mode for planned
absences (CLAUDE.md section 2). We do not take paid revival, lost progress,
or any guilt-tripping text.

