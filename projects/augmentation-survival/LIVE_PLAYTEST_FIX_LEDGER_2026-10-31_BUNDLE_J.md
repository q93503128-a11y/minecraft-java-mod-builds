# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-31 — Bundle J

## Trigger

The post-Bundle-I live pass reported or requested four concrete changes:

- Time Stop must read as a world-space stopped-time volume rather than a full-screen gray HUD/filter; the field must remain at the cast location and must also stop other players inside it.
- `retrace` / 되밟기 should be retired rather than repaired further.
- Lightning must count real direct melee such as bare-hand and ordinary sword attacks, and its threshold must be visibly readable.
- Echo Strike must retain its existing description but resolve as a real delayed second hit with a cyberpunk-like afterimage presentation.

## Time Stop — fixed world-space gray field

Bundle I's full-screen scene wash was removed from the intended presentation contract. Time Stop is now represented in the world itself:

- the authoritative field captures the caster's bounding-box center when the ability is activated;
- all subsequent freeze checks and sphere presentation use that immutable cast center, so moving the caster does **not** drag the stopped-time zone along;
- the real 32-block sphere is rendered with three great-circle shell traces, latitude rings and distributed gray/ash/smoke volume samples so the affected space itself reads as desaturated/frozen;
- the caster remains exempt from their own field;
- other players inside the sphere are valid Time Stop targets, alongside the other living/projectile/world entities already covered by the Bundle-I broad-stop contract.

Automated regression creates two server players, confirms that the second player is frozen, moves the caster well outside the original sphere, and confirms both the field center and entity-freeze boundary remain at the original cast location.

## Retrace / 되밟기 — retired

The augment is retired from normal play rather than receiving another presentation pass.

- new Epic offers exclude `retrace`;
- legacy owned state is inert: no path damage and no cooldown consumption;
- the internal definition/ID remains only as a compatibility tombstone so existing save/index layouts are not renumbered or corrupted.

## Lightning — real melee direct-hit bridge

The previous generic post-damage boundary did not reliably observe real `Player.attack` melee in the same way as the live player path.

New contract:

- a successful server-side `Player.attack` is explicitly classified as one ROOT_MELEE Lightning hit;
- the generic post-damage route is suppressed only while that wrapped melee call is active, preventing double counting;
- bare-hand and ordinary melee weapons therefore use the same authoritative counter;
- reaching threshold produces a visual-only vanilla lightning strike on the primary target even in a one-target fight so the proc is readable;
- if no additional hostile target exists, the proc remains primed rather than being silently consumed;
- with a valid secondary hostile, derived chain damage is applied through the normal entity damage boundary and the counter is consumed;
- visual lightning itself adds no extra vanilla lightning damage or fire.

The live regression uses an actual bare-hand `Player.attack`, allows normal melee recovery, then uses an actual wooden-sword `Player.attack` and requires the Lv.3 threshold, secondary-target health loss and visual lightning presentation.

## Echo Strike — actual delayed afterimage hit

The player-facing description is unchanged.

Runtime behavior is changed from a hidden same-tick derived damage application into a distinct second strike:

- qualifying melee hits still use the canonical per-level trigger count and damage fraction;
- when the trigger is reached, the second hit is scheduled 10 ticks after the root melee hit;
- the delayed strike clears only the first hit's remaining vanilla hurt-immunity window and then enters the normal public entity-damage boundary with `ECHO` provenance, preventing recursive root-only augment triggers;
- cyan, magenta and pale afterimage rails, electric sparks, sweep impact and layered attack/chime audio make the second hit visually separate from the first;
- a successful echo establishes its own normal vanilla hurt state rather than being folded into the first attack.

Automated regression uses two fully recovered wooden-sword `Player.attack` hits at Lv.3, records health after the second root hit, then requires a further health drop after the delayed afterimage window.

## Validation contract

This ledger is committed **before** the final Bundle-J verification so the production JAR/MRPack and the ledger can share the exact same final Git HEAD.

The final checkpoint for this HEAD must satisfy all of the following before the bundle is called BUILD VERIFIED:

- clean build / pack dependency checks: PASS
- required GameTests: **148 / 148 PASS**
- World Bosses localization capture: PASS
- external Korean localization audit: PASS
- dedicated-server boot smoke: PASS
- client boot smoke: PASS
- verified MRPack generation: PASS
- production JAR structure/checksum: PASS

Validation boundary after CI remains:

- CODE REVIEWED: YES
- TESTED: pending final same-HEAD CI
- BUILD VERIFIED: pending final same-HEAD CI
- JAR PRODUCED: pending final same-HEAD CI
- MRPACK PRODUCED / VERIFIED: pending final same-HEAD CI
- DEDICATED SERVER SMOKE: pending final same-HEAD CI
- CLIENT BOOT SMOKE: pending final same-HEAD CI
- PLAYTESTED AFTER FIX: NO
- MULTIPLAYER TESTED: NO — the two-player GameTest is automated regression coverage, not a real multiplayer playtest

## Separate deferred work

- allied-player Revive remains a dedicated multiplayer death/respawn-authority pass;
- `aerial_combo` air-jump strength 1.5x remains a separate requested change and was not mixed into Bundle J.
