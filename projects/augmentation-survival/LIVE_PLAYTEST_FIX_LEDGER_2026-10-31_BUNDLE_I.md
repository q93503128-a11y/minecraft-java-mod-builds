# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-31 — Bundle I

## Trigger

The second mythic-focused live-play pass reported or requested the following changes:

- clarify `second_choice` when only one augment remains;
- simplify the Gigantification card wording;
- broaden Time Stop beyond hostile-only targets and give the field a much stronger stopped-time visual identity;
- clarify vanilla boss classification, including Warden;
- restore the pre-Bundle-H powered-flight presentation;
- repair Retrace, which appeared not to apply;
- repair Lightning, which could silently consume a proc with no chain target and had no real lightning presentation;
- make Divine Punishment lightning substantially more mythic/readable.

## Second Choice exhaustion behavior

No runtime change was required.

The acquisition engine already computes `selectionCount = min(2, candidateCount)`. Therefore, when only one valid unowned augment remains, the offer contains that one augment and only one selection is required. Choosing it completes the offer normally. The game does not demand a nonexistent second choice.

## Gigantification card wording

The player-facing detail now groups the proportional properties together:

- size / maximum health / current health / sweep range: **1.75x / 2.5x**;
- melee reach: **+3 / +6 blocks**;
- melee damage: **+35% / +70%**;
- knockback: **+100%**;
- cooldown: **60 seconds**.

This keeps the card mechanically accurate while removing repeated wording.

## Time Stop — broad sphere control

Time Stop is no longer hostile-only.

Within the authoritative 32-block sphere, the caster is the only ordinary moving entity intentionally exempt from the stop. The field now covers:

- hostile, neutral and allied living entities;
- boss entities that allow Time Stop through compatibility policy;
- projectiles;
- ordinary ticking entities such as dropped items, experience, vehicles, primed TNT and falling blocks.

Living entities and projectiles keep the existing restore-aware time-stop paths. Other non-living entities have their server entity tick cancelled while they remain inside an active sphere.

### Boundary of the rule

This does **not** stop the entire Minecraft server clock, chunks outside the sphere, or globally suspend block/random ticks. Doing so would also stop the ability's own expiry clock and unrelated world simulation outside the field. The implemented contract is therefore "all dynamic entities inside the sphere, except the caster," not a global server pause.

## Time Stop presentation

The weak sparse gray outline was replaced by a stronger field identity while preserving the real spherical gameplay radius:

- three great-circle traces plus additional latitude rings define the sphere;
- denser `ASH`, `WHITE_ASH` and `SMOKE` populate the field;
- the caster receives a full-screen cool-gray scene wash/vignette and a brighter inner edge while Time Stop is active.

The visual direction takes the general lesson from JoJo-style stopped-time scenes — a conspicuous special-scene color treatment plus complete motion arrest — without copying a specific copyrighted frame or palette.

## Vanilla boss classification

- Wither: already `boss=true`.
- Ender Dragon: already `boss=true`.
- Warden: now explicitly `boss=true` for Augmentation Survival combat/control compatibility.

Warden now uses boss branches such as Divine Punishment's boss health fraction and remains non-dominatable. Boss identity still does **not** automatically grant a boss-core reward; reward bands remain separately authored.

## Powered flight presentation rollback

The Bundle-H presentation-only flight changes were reverted to the pre-H behavior:

- original speed-derived FOV behavior restored;
- original speed-threshold gliding pose restored;
- original restrained body banking restored.

The Bundle-H gameplay change that allows powered-flight body-impact damage against non-allied neutral living mobs is retained.

## Retrace repair

Two separate defects made Retrace appear non-functional.

1. The old presentation hook sampled current position before and after the proc, producing a zero-length visible line instead of the stored two-second path.
2. Runtime history stored the player's feet position while collision checks compared the path against target bounding-box centers. A normal standing target could therefore be outside the narrow path radius even when the player's body had crossed it.

Repairs:

- path history is sampled from the player's bounding-box center so path and target hitboxes share the same vertical reference frame;
- VFX is emitted from every authoritative historical segment actually checked for path damage;
- the regression test now verifies history accumulation, hostile eligibility, path/hit-radius intersection, cooldown consumption and real damage.

An early regression assertion also captured target health too early, before concurrent enemy-scaling refresh could change the target's current health. The final test records health immediately before the retrace strike so it measures the damage event rather than a stale spawn-time baseline.

## Lightning repair

The passive Lightning proc had a live logic/readability defect:

- reaching the hit threshold reset the counter before a valid secondary chain target was confirmed;
- with no second target nearby, the proc was silently consumed and appeared broken;
- successful chains used only small electric particles rather than a readable vanilla lightning strike.

New behavior:

- if the threshold is reached with no valid chain target, Lightning stays **primed** instead of consuming the proc;
- the next eligible direct hit can discharge immediately once a secondary hostile target is available;
- a successful discharge resets the counter, reuses the existing derived-chain damage pipeline and creates a visual-only vanilla lightning bolt on each chained target;
- presentation lightning adds no separate vanilla lightning damage or fire.

## Divine Punishment presentation

Gameplay damage remains a single percent-health application per affected target. Presentation is now separated clearly from ordinary Lightning:

- three visual-only vanilla lightning strikes are staged at `0 / +4 / +8` ticks;
- additional crimson and dark-crimson particle rings surround the impact;
- six jagged branching crimson particle rails expand from the strike core;
- electric sparks and layered impact sound reinforce the strike.

Minecraft's native lightning bolt itself remains the vanilla render; the red/branching identity is built around it with dedicated particles. This avoids inventing a fragile custom lightning renderer while making Divine Punishment visibly stronger than the ordinary passive Lightning proc.

## Validation

Final Bundle I verified checkpoint: `b30d0a7e4a2bbeb4d5038bbe6861b18f1d80d6c0`.

Workflow **#544** completed successfully:

- clean build / pack dependency checks: **PASS**
- required GameTests: **144 / 144 PASS**
- World Bosses localization capture: **PASS**
- external Korean localization audit: **PASS**
- dedicated-server boot smoke: **PASS**
- client boot smoke: **PASS**
- verified MRPack generation: **PASS**
- production JAR structure/checksum: **PASS**

Artifacts:

- production JAR SHA-256: `23fd6820f05f48405fab863ba50eafb876f0e78192d38daa6748e8cd7ed42dc1`
- MRPack SHA-256: `ef83d91403d48f8cc25e251d707e1c0a22697d9b0eccbb09ac6b5d0ba694848d`

Validation boundary:

- CODE REVIEWED: YES
- TESTED: YES — 144 required automated GameTests
- BUILD VERIFIED: YES — workflow #544
- JAR PRODUCED: YES
- MRPACK PRODUCED / VERIFIED: YES
- DEDICATED SERVER SMOKE: YES
- CLIENT BOOT SMOKE: YES
- PLAYTESTED AFTER FIX: NO
- MULTIPLAYER TESTED: NO

## Separate deferred work

- allied-player Revive remains a dedicated multiplayer death/respawn-authority pass;
- `aerial_combo` air-jump strength 1.5x remains a separate requested change and was not mixed into Bundle I.
