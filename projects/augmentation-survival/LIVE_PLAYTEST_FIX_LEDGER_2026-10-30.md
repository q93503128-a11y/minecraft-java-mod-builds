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

## Bundle B — Legendary HUD/feedback repair — IMPLEMENTED, VERIFICATION PENDING

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

## Deferred to following bundles

- Double-click quick equip / quick activation reliability.
- Ability range telegraphs: persistent sustained-field outlines, instant burst rings, and cone boundaries where applicable.
- Any additional live-play failures already reported after these items; do not reopen confirmed lower-tier mechanics without related code changes.
- `aerial_combo`: requested later change to 1.5x own air-jump strength remains deferred until its dedicated change pass.
