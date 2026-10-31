# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-31 — Bundle H

## Trigger

The first mythic-focused live-play pass reported multiple player-facing issues at once: Area Acceleration Lv.1 was too far below the new Lv.2 strength; Gigantification and Domination displayed misleading Double Charge pips; Gigantification's cooldown was therefore unreadable; dominated mobs could wander into caves chasing distant targets and had no release interaction; giant body size did not enlarge sweep geometry; powered flight shook visually, used an unstable swim-like pose and excluded neutral mobs from body-impact damage; Time Stop lacked a gray temporal-field identity; Divine Punishment lacked satisfying vanilla-lightning staging; and Auto Refill unexpectedly populated an intentionally selected empty hotbar slot.

The supplied Auto Refill recording was reviewed and reproduced the symptom directly: moving selection to an empty hotbar slot caused the previously selected item type to be pulled from main inventory despite no stack depletion.

## Area Acceleration balance

- Lv.1: **x15**.
- Lv.2: **x30**.
- Duration remains **60 seconds**.
- Radius remains **24 blocks**.
- Base cooldown remains **120 seconds**.
- Growth sampling and machine/production consumers use the same authoritative multiplier.
- Player-facing text now states the actual 15x / 30x values without describing an invented "felt multiplier".

## Mythic cooldown / charge presentation

`Double Charge` remains restricted to cooldown-bearing **non-mythic** actives.

- `gigantification` no longer reads fabricated `#charge0/#charge1` cooldown keys. It displays its real single mythic cooldown.
- `domination` has no cooldown and therefore displays **no charge pips**.
- Existing compatible non-mythic actives retain Double Charge behavior.

## Gigantification sweep geometry

Gigantification now enlarges sweep geometry with the player's actual `Attributes.SCALE` value.

- Vanilla sweep AABB expands horizontally with body scale.
- Better Combat server range validation scales with the giant body before ordinary Sweeping Enhancement bonuses are added.
- The change enlarges hit reach/geometry, not the secondary-damage multiplier by itself.

## Domination ownership UX and leash

Domination now has an explicit release interaction and owner-centered behavior.

- Aim at one of your own dominated permanent companions and use Domination again to release that exact entity.
- Release clears domination ownership, glow, current combat target/navigation and occupancy count.
- Idle dominated companions follow the owner when more than 10 blocks away.
- Passive hostile pursuit is bounded to 24 blocks from the owner.
- A target explicitly attacked by the owner may be commanded only while it remains within 32 blocks of the owner.
- Beyond 24 blocks from the owner, the companion drops combat and returns.
- Beyond 40 blocks in the same dimension, the runtime attempts a collision-safe recall near the owner; if no safe recall point is found it paths back instead.
- If the owner is offline, dead or in another dimension, the dominated mob stops active combat/navigation rather than continuing an unbounded chase.

The player-facing card explains the release interaction and owner-centered fighting behavior.

## Powered flight

### Camera / pose presentation

- Powered-flight FOV now smooths the speed-derived bonus instead of feeding every server velocity correction directly into the camera.
- The avatar uses one continuous prone `FALL_FLYING` silhouette for the entire powered-flight state rather than switching around a speed threshold.
- The old whole-body flying-yaw/banking presentation was removed so the avatar stays in a stable forward-facing prone pose.

This is a presentation-side mitigation for the reported shake. Actual in-game feel still requires manual retest; CI boot success is not treated as proof that all perceived camera jitter is gone.

### Body-impact target scope

- Existing hostile collision damage is preserved.
- Neutral/non-hostile living mobs are now also valid powered-flight body-impact targets.
- Players and allied entities remain excluded.
- Damage/provenance/collision authority remains in the existing server-owned flight runtime.

## Time Stop presentation

The server-authoritative Time Stop radius now has a gray temporal-field identity:

- the same real sphere radius is traced with gray `ASH` / `WHITE_ASH` particles;
- sparse gray particles also occupy the interior;
- the field is refreshed at 4 Hz for the active duration.

This is a world-space presentation matched to the gameplay sphere, not a whole-world shader or fake radius.

## Divine Punishment presentation

Divine Punishment keeps one gameplay damage/stun application per affected target but stages the strike with vanilla Minecraft lightning.

- Visual-only vanilla lightning bolts are scheduled at 0 / +4 / +8 ticks at the affected target position.
- `LightningBolt#setVisualOnly(true)` prevents the presentation bolts from adding separate vanilla lightning damage or fire.
- The canonical percent-health damage remains applied exactly once by the existing server-owned damage source.

## Auto Refill — video-confirmed root cause and repair

Root cause: the old runtime remembered the previous selected-item template but did not require the selected hotbar slot itself to be the same slot that had just depleted. Selecting an intentionally empty hotbar slot could therefore satisfy the generic "selected item is empty" condition and pull the previous item type out of main inventory.

New rule:

- changing selected hotbar slot is always treated as an explicit player choice and never as a depletion event;
- automatic refill is eligible only when the **same selected slot** contained exactly **1 stackable item** on the immediately preceding tick and is empty on the next tick;
- non-stackable tools/weapons and inventory-management clears do not trigger refill;
- if no matching reserve exists at the actual depletion moment, no delayed refill request is retained.

A regression test preserves the intended real `1 -> 0` stack-depletion refill while separately proving that selecting an empty hotbar slot does not pull reserve items.

## Time Acceleration focus clarification

No gameplay behavior changed here. The existing 3-second focus already requires the player to remain stationary and not use an item. The player-facing description now states this explicitly.

## Validation

Final Bundle H runtime checkpoint: `664e5d70ac4ebaf74794e7a654aa63e562630a2b`.

Workflow **#541** completed successfully:

- clean build / pack dependency checks: **PASS**
- required GameTests: **140 / 140 PASS**
- World Bosses localization capture: **PASS**
- external Korean localization audit: **PASS**
- dedicated-server boot smoke: **PASS**
- client boot smoke: **PASS**
- verified MRPack generation: **PASS**
- production JAR structure/checksum: **PASS**

Artifacts:

- production JAR SHA-256: `b8a4411de70f5b0734734a367bdbb9caa454d133af877793ba14707b5144cb1c`
- MRPack SHA-256: `806e079c5b9e60576f9ba0ff4b8d1d977945668138c6252ec021415baec411f4`

Validation boundary:

- CODE REVIEWED: YES
- TESTED: YES — 140 required automated GameTests
- BUILD VERIFIED: YES — workflow #541
- JAR PRODUCED: YES
- MRPACK PRODUCED / VERIFIED: YES
- DEDICATED SERVER SMOKE: YES
- CLIENT BOOT SMOKE: YES
- PLAYTESTED AFTER FIX: NO
- MULTIPLAYER TESTED: NO

## Separate deferred work

- allied-player Revive remains a dedicated multiplayer death/respawn-authority pass.
- `aerial_combo` air-jump strength 1.5x remains a separate requested change and was not mixed into Bundle H.
