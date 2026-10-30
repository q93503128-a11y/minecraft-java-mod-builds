# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-30

This is the running engineering ledger for the current multi-bundle live-play repair pass. It is not a player-facing document. Keep resolved items here so the final user checklist can be produced without reopening already closed work.

## Bundle A — combat/death/traversal repair — IMPLEMENTED + BUILD VERIFIED

1. **Delayed Damage feedback**
   - Root cause: delayed installments still inherited normal hurt-direction / hurt-animation state at outer damage boundaries.
   - Fix: preserve real HP loss and dedicated sound while restoring knockback direction, player hurt tilt, `hurtTime`, and `hurtDuration` after the final server damage path.
   - Automated boundary: delayed damage remains real damage but does not create ordinary impact feedback.

2. **Last Stand + Delayed Damage**
   - Root cause: lethal protection could leave delayed debt capable of applying after the save.
   - Fix: clear queued delayed debt when Last Stand actually saves the player and suppress new delayed reservations during its 3-second protection window.

3. **Return to Sender / Shield Compression**
   - Root cause: compression accumulation depended on a synthetic blocked-damage boundary rather than the amount the shield actually blocked.
   - Fix: bind accumulation to actual shield-blocked damage; preserve threshold, cone, damage fraction, carry-over, and ally exclusion rules.

4. **Death Return**
   - Root cause: the stored death destination / one-use ownership could be lost during death-to-respawn state reconciliation.
   - Fix: preserve the target through copy-on-death and arm the return after respawn reconciliation.

5. **Phase Shift**
   - Root cause: vanilla movement validation treated intended wall overlap as newly illegal collision and rubber-banded the player.
   - Fix: while Phase Shift is active, exempt only its intentional overlap from `isEntityCollidingWithAnythingNew`; action locks remain unchanged.

Verification checkpoint: CI #505, 120/120 required GameTests, clean build, dedicated-server boot, client boot, localization, JAR and MRPack verification PASS. Manual feel/visual verification was not performed by CI.

## Bundle B — Legendary HUD/feedback repair — IMPLEMENTED + BUILD VERIFIED

1. **Double Charge HUD**
   - Root cause: runtime charges live in `ability#charge0` / `ability#charge1`, while the old loadout packet only sent the base cooldown key.
   - Fix: server now sends max charges and both recharge timers. The client derives ready charges locally as server-provided timers expire, displays `2/2 -> 1/2 -> 0/2`, and shows the next sequential recharge countdown without hiding a still-usable charge.
   - Time Acceleration now forces a loadout resync when its focus channel commits the real charge.

2. **Area Acceleration visibility/runtime confidence**
   - Existing runtime route and local tick hooks were retained: crop/random-tick growth, furnaces, ageable mobs/chickens and brewing use the area multiplier.
   - Fix: active Area Acceleration is promoted to a persistent corner status with remaining time instead of relying on a launch pulse / center entry alone.
   - New runtime regression uses an actual furnace and requires the level-1 x3 aura to complete smelting inside the accelerated window.

3. **Invulnerability shield visual center**
   - Root cause: the shell used a fixed `CENTER_Y=-1.02`, placing most of the ellipsoid below the visible player model.
   - Fix: recentered the shell to the avatar model midpoint and tightened the vertical radius while preserving the existing gold translucent presentation.
   - Manual visual check remains required after all bundles are complete.

4. **Threat Suppression HUD**
   - Fix: persistent toggle state is rendered as a compact corner status/icon, not a center banner. Server sync rebuilds the status after reconnect/respawn when the toggle is still enabled.

5. **Time Acceleration HUD**
   - Fix: sustained Time Acceleration state is rendered as a persistent corner status while active; its center banner/entry is suppressed. Server temporal transitions also resync active-charge state.

Verification checkpoint: CI #506 completed SUCCESS: clean build/GameTests, localization, dedicated-server boot, client boot, MRPack generation and production-JAR verification all passed. Manual visual verification remains deferred until all repair bundles are complete.

## Bundle C — input/range readability repair — IMPLEMENTED + BUILD VERIFIED

1. **K-screen double-click quick equip / unequip**
   - Root cause: the fast-path mixin used `hoveredId` cached by a previous render frame, while another UI mixin owned the actual click geometry. A valid double click could therefore arrive with no/stale target.
   - Fix: double-click resolution now recomputes the exact quick-slot or owned-active tile from the click coordinates using the same responsive geometry as the live screen.
   - Double-click equipped slot/tile -> immediate clear; unequipped owned tile -> first empty slot; full bar -> preserve deliberate replacement selection rather than silently evicting a slot.

2. **Persistent sustained-field range outline**
   - Added a server-owned sparse range telegraph runtime. Sustained or channelled states with a real radius keep a world-space boundary visible at a restrained cadence.
   - Area Acceleration and Revive use this shared persistent boundary; Time Stop keeps its dedicated gameplay-aligned 3D sphere instead of drawing a duplicate generic ring.

3. **Instant radial range ring**
   - Existing successful-use rings were confirmed to already use the authoritative presentation radius for radial instant abilities; they remain one-shot rather than being incorrectly persisted.
   - The persistent service deliberately ignores `INSTANT` phases so burst indicators do not turn into fake sustained zones.

4. **Cone range boundary**
   - Shield Compression / Return to Sender already used the exact gameplay range and half-angle for its cone VFX, but the old points were sparse.
   - Fix: the cone now draws centerline, four side rails and the circular far cross-section using `tan(halfAngle) * range`, matching the same finite 3D cone used by the damage dot-product check.

5. **Indicator/gameplay boundary synchronization**
   - Persistent range values come from authoritative gameplay tuning rather than client-authored estimates.
   - Area Acceleration republishes the sustained state whenever Overtime extends the aura, keeping corner timer and world boundary aligned with the real extended end tick.
   - Dedicated regressions cover Area Acceleration radius/duration, Revive channel cancellation cleanup, Overtime extension synchronization, and Time Stop's dedicated sphere radius without duplicate generic telegraphs.

Verification checkpoint: CI #509 on `fff62aae14e6e6f78b1a1312e0d8154b26d17c28` completed SUCCESS: clean build/GameTests, localization, dedicated-server boot, client boot, MRPack generation and production-JAR verification all passed.

## Bundle D — second live-play UX/combat repair — IMPLEMENTED + BUILD VERIFIED

1. **Active HUD readability**
   - Symptom: ten 16px active slots simultaneously overlaid hotkey digits, cooldown values and double-charge fractions, producing severe visual noise over the hotbar.
   - Fix: keep the ten-icon strip, but reduce each slot to one small hotkey chip. Double Charge now uses two tiny charge pips instead of `1/2` / `0/2` text; a centered cooldown number appears only when the ability is actually unavailable.
   - Manual check: verify the strip is readable at the user's actual GUI scale and that the pips remain legible without obscuring icons.

2. **K-screen quick equip / unequip and intentional blank slots**
   - Symptom: repeated double-click quick actions were unreliable, and clearing an active could be undone by `resolvedSlots()` automatically inserting the owned active into another blank slot.
   - Root cause: the old router had no distinction between untouched auto-layout and a player-authored layout, so every missing owned active was treated as something that should be auto-filled.
   - Fix: left-click keeps the existing deliberate selection/replacement flow. Right-click now directly equips an owned active into the first blank slot or unequips an equipped active. The first deliberate assign/swap/clear marks the loadout as manual; after that, blank slots remain genuinely blank and owned but unequipped actives do not silently return.
   - Regression: a manually cleared active remains unequipped even after another active is newly acquired.

3. **Return to Sender / Shield Compression live-combat reliability**
   - Symptom: automated stationary-target tests passed, but live combat still reported the shockwave as doing nothing.
   - Investigation: CI #510 rejected an attempted reinterpretation of the 26.2 `applyItemBlocking` return value; the established Fabric boundary was restored. The meaningful difference between the old test target and live combat was target hurt i-frame state.
   - Root cause: unlike the project's other derived impacts (for example Wall Slam), Shield Compression did not temporarily bypass a target's pre-existing vanilla hurt i-frame. A monster that had just been hit could therefore swallow the independent counter-shockwave.
   - Fix: only for the derived Shield Compression hit, temporarily clear the target hurt i-frame, apply the server-authoritative derived AOE damage, then restore at least the previous i-frame value.
   - Regression: a level-1 10-damage real shield block must still deal 6 damage to an attacker that already has a hurt i-frame, and that previous i-frame must be restored afterward.

4. **Area Acceleration duration/range presentation**
   - Symptom: the live aura could lose its visible duration and range, making a working or non-working field impossible to distinguish.
   - Fix: while the server aura exists, republish its authoritative remaining duration and radius once per second. The persistent world telegraph now draws three orthogonal great circles instead of a horizontal-only ring.
   - Geometry: gameplay continues to use full 3D `distanceToSqr` against the 24-block radius, so the actual field and the new telegraph are spherical/all-directional rather than a sideways disk.
   - Existing deterministic furnace regression remains in the required GameTest suite.

5. **Time Acceleration duration display**
   - Symptom: the player saw the 3-second focus timer but not the real sustained duration, making the ability appear to last only 3 seconds.
   - Root cause: the focus presentation existed, but the transition into the successful temporal state did not publish the separate sustained presentation window.
   - Fix: successful focus completion now publishes the real 20-second sustained timer; END clears it, and Overtime extension republishes the extended remaining time.
   - Canonical gameplay remains 3 seconds focus -> 20 seconds sustained.

6. **Phase Shift visibility**
   - Symptom: intentional movement inside solid terrain was too dark to navigate comfortably.
   - Fix: while the local player is actually in Phase Shift, suppress the camera-inside-block screen overlay only. This gives the intended spectator/x-ray-like navigation readability without making ordinary terrain globally transparent.
   - Manual visual check remains required.

7. **Invulnerability shield vertical position**
   - Symptom: the corrected shield still appeared slightly high in the second live-play pass.
   - Fix: lower the shell center modestly from `-0.50` to `-0.62`; do not return to the old `-1.02` value that buried the effect below the avatar.
   - Manual visual check remains required.

Verification history:
- CI #510: FAILED intentionally-useful regression checkpoint. It disproved the attempted external-version assumption about `applyItemBlocking` and exposed that treating every saved blank as manual broke the existing new-active auto-placement contract. Those two approaches were removed rather than bypassing tests.
- CI #511: SUCCESS after correcting both regressions, but its report still showed 126 tests because the new Bundle D test class had not yet been registered as a `fabric-gametest` entrypoint.
- CI #512 on `1cc7366988fbe26141a345b073c97529c3f164df`: SUCCESS with **128/128 required GameTests**, clean build, external localization checks, dedicated-server smoke, client boot smoke, verified MRPack generation and production-JAR structure/checksum verification.
- Manual second-pass fixes in this bundle are not called PLAYTESTED until the user runs the #512 artifact.

## Current live-play report closure

Bundles A through D now cover every concrete issue reported across the two Legendary live-play passes: Delayed Damage feel, Last Stand debt handling, Return to Sender/Shield Compression, Death Return, Phase Shift movement and visibility, Double Charge HUD state, Area Acceleration operation/readability, Invulnerability shield placement, Threat Suppression HUD, Time Acceleration state/duration, K-screen quick equip/unequip and intentional blanks, persistent 3D range telegraphs, and active-HUD clutter.

Further repair work starts from a newly observed failure in the #512 artifact or an explicitly requested design change; resolved lower-tier augments are not reopened without a new symptom.

## Deferred explicit change request

- `aerial_combo`: requested later change to 1.5x own air-jump strength remains deferred until its dedicated change pass.
