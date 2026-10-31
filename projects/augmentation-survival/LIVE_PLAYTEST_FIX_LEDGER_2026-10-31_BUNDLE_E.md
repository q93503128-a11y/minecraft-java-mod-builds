# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-31 — Bundle E

This document supersedes the Bundle A–D closure note in `LIVE_PLAYTEST_FIX_LEDGER_2026-10-30.md` for issues newly reported in the 2026-10-31 live-play pass. It records the production changes and the exact verification checkpoint; it does not claim manual playtest completion.

## Bundle E — combat reaction / sustained ability / movement safety — IMPLEMENTED + BUILD VERIFIED

### 1. Return to Sender / Shield Compression

Live symptom: the augment behaved as though taking damage was the trigger instead of a successful shield block.

Fix:

- The trigger is now tied to the vanilla shield durability boundary, not to generic received damage.
- A block counts only when the blocking item actually loses durability.
- Required durability-consuming blocks are level 1 / 2 / 3 = **5 / 4 / 3**.
- At the threshold, the existing server-authoritative 7-block, 45-degree forward shockwave returns the accumulated blocked attack damage at **60% / 80% / 100%**.
- The player-facing description now states the shield-block requirement and 5 / 4 / 3 cadence.
- Existing hurt-i-frame bypass/restoration for the derived shockwave remains intact.

Regression coverage uses the real 26.2 `BlocksAttacks` durability boundary and verifies the three thresholds, damage fractions, cone hit/exclusion, and pre-existing target hurt-i-frame behavior.

### 2. Hotbar insertion versus Auto Refill

Investigation result: unrelated spawn eggs/items appearing in a free hotbar slot are **not** caused by the augment Auto Refill runtime.

Auto Refill only restores the currently selected empty hotbar slot from the main inventory when it finds the exact same item/component stack that previously occupied that slot. Vanilla `/give` and Creative inventory insertion may prefer free hotbar slots independently. No augment behavior was changed for this symptom.

### 3. Persistent augment HUD placement

Sustained/persistent augment status presentation was moved to the **left side** so it no longer competes with vanilla status-effect icons in the upper-right. This includes sustained Area Acceleration / Time Acceleration presentation and the recurring invulnerability-shield status presentation routed through the same persistent HUD layout.

### 4. Area Acceleration growth feel

Root cause: crop/random-tick acceleration previously multiplied only the relatively rare vanilla random-tick event that happened to select a crop. The long-run multiplier could be mathematically correct while feeling bursty and weak in short live-play observation.

Fix:

- Crop growth now uses a smoothed spatial sampler distributed across the active spherical field instead of waiting for a selected crop and replaying that event in a burst.
- The long-run level contracts remain **x3 / x7**.
- `randomTickSpeed=0` remains respected.
- Overlapping fields use the strongest applicable multiplier instead of stacking duplicate acceleration.
- Machine/other established Area Acceleration consumers remain on their existing authoritative runtime paths.

Required regression verifies nonzero sampling and the exact x3/x7 long-run budget relationship.

### 5. Time Acceleration consistency

The live feel was inconsistent because movement/attack/mining used a 60% acceleration contract while item use and owned projectile speed were still at x1.40.

Fix: all intended speed axes now use the same 60% contract:

- movement bonus: +60%
- attack-speed bonus: +60%
- mining bonus: +60%
- item-use speed: x1.60
- owned projectile launch speed: x1.60

This is an axis-consistency repair, not an additional balance buff beyond the stated 60% contract.

### 6. Recurring invulnerability shield presentation

The live screenshot still showed the translucent gold shell concentrated too high around the avatar. The shell center was lowered from `CENTER_Y=-0.62` to **`-0.82`**, preserving the existing size and translucent gold presentation. This remains a manual visual-check item because CI can verify loading/state boundaries but not whether the effect looks correctly centered at the user's GUI/camera conditions.

### 7. Phase Shift in-wall damage

While Phase Shift is active, vanilla `IN_WALL` suffocation damage is suppressed. Other damage remains valid; Phase Shift does not become generic invulnerability.

Required regression verifies both sides of that contract.

### 8. Rewind after Phase Shift

A rewind history point can legitimately have been recorded while the player was phasing through solid terrain. Rewinding directly to that historical coordinate after Phase Shift ends could therefore strand the player inside blocks.

Fix: Rewind validates the historical destination before teleport. If it is colliding, it resolves to nearby safe free space instead of restoring the player into a solid block.

Required regression constructs a solid historical destination and requires the adjusted destination to be both different and collision-safe.

### 9. Monster-room chest core observation

The current monster-room chest profile retains an **8% normal-core roll per chest**. Four independent chests therefore have only about a **28.4%** chance to contain at least one core. Finding one normal core from four chests is not evidence of a loot failure, so no drop-rate change was made in Bundle E.

## Verification checkpoint

Code checkpoint validated by CI:

- Commit: `0857a86cbabcfe2f55c22e3cef0bbdce3bc79171`
- Workflow: **Build Augmentation Survival Pack #520**
- Required GameTests: **132 / 132 PASS**
- Clean build / dependency inspection: **PASS**
- World Bosses localization capture: **PASS**
- External Korean localization audit: **PASS**
- Dedicated-server boot smoke: **PASS**
- Client boot smoke: **PASS**
- Verified MRPack generation: **PASS**
- Production JAR structure/checksum: **PASS**

Verified MRPack:

- `augmentation-survival-pack-26.2.mrpack`
- SHA-256: `832a447dabdfd708cb81537435e512dee34367b031af2fd62d294d56d1030847`

Verified production JAR:

- `augmentation-survival-0.1.0-alpha.1.jar`
- size: `661779` bytes
- SHA-256: `763ccba23459f24d2ab9a3c5b4fe81216fedc3edb964291eae56b15f5d25654d`

## Verification state

- CODE REVIEWED: YES
- TESTED: YES — 132/132 required automated GameTests
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- DEDICATED SERVER BOOT SMOKE: YES
- CLIENT BOOT SMOKE: YES
- PLAYTESTED AFTER BUNDLE E: NO
- MULTIPLAYER TESTED AFTER BUNDLE E: NO

The next action is a focused manual retest of the changed player-facing behaviors, not another code redesign or another CI run without a newly observed failure.
