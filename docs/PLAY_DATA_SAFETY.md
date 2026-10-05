# Google Play Data Safety: draft

Kept current by every iteration that touches data (CLAUDE.md section 2).

| Data type | Collected | Shared | Purpose | Optional | Notes |
|---|---|---|---|---|---|
| (none) | No | No | n/a | n/a | The app stores the game only on the device (DataStore) and transmits nothing. |

When online races are enabled (iteration 6b, server configured):

| Data type | Collected | Shared | Purpose | Optional | Notes |
|---|---|---|---|---|---|
| App activity: race inputs and times | Yes | No | App functionality (leaderboards, ghosts) | Yes (online races only) | Shown to other players with the display name. |
| Device or other IDs: random install token | Yes | No | App functionality (anonymous account) | Yes | Random, not derived from the device; stored hashed on the server. |
| Display name | Yes | No | App functionality | Yes | Chosen from a fixed list of pet names plus a number; no free text. |

- Encryption in transit: HTTPS (required for the production server).
- Account deletion: n/a (no accounts yet; arrives with iteration 21/24).
- Families Policy: no ads SDK, no analytics SDK, no identifiers.
