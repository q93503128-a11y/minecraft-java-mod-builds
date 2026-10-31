# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-31 — Bundle F

Bundle F follows the post-Bundle-E live play screenshots and focuses only on newly observed regressions or verification gaps.

## 1. Return to Sender player-facing wording — FIXED

- Gameplay implementation remains durability-decrement-counted internally so only a real shield block is counted.
- That implementation detail is no longer exposed to the player.
- Player-facing contract: blocking attacks with a shield builds block count; Lv.1 / Lv.2 / Lv.3 trigger after 5 / 4 / 3 blocks and emit the existing 7-block, full-45-degree cone for 60% / 80% / 100% of accumulated blocked attack damage.
- The presentation regression now explicitly rejects the word `내구도` from Return to Sender player text.

## 2. Time Acceleration canon/runtime verification — FIXED + RUNTIME TESTED

- Canon restored: 3-second focus -> 20-second sustained state; movement / attack speed / mining speed +60%; item-use speed and owned-projectile speed +40%; cooldown 45 seconds.
- The previous Bundle-E test only inspected tuning constants and therefore could pass without proving the live player runtime.
- Bundle F replaces that check with an actual connected Survival player: start Time Acceleration, wait through focus, require the sustained state, then verify live movement and attack attributes, final `Player#getDestroySpeed`, item-use duration, and owned-projectile speed.
- CI #521 exposed the old test boundary as insufficient. CI #522 then proved the final mining path still was not receiving +60%.
- Root cause: the Time Acceleration mining addition was implemented through a MixinExtras return-value adapter on the shared mining service, but it was not affecting the final live result.
- Fix: move that addition to a standard cancellable RETURN injection on the same authoritative server mining-speed boundary, preserving additive same-stat behavior.

## 3. Recurring Invulnerability shield vertical direction — FIXED, MANUAL VISUAL CHECK REQUIRED

- Live screenshot after Bundle E showed the gold shell higher than before.
- Root cause: render-space Y direction had been interpreted backwards. Moving `CENTER_Y` from `-0.62` to `-0.82` moved the rendered shell upward, not downward.
- Fix: move the center in the opposite direction to `-0.20` while keeping the current radius and translucent gold presentation.
- This is a visual fix; CI/client boot cannot establish final on-screen placement.

## 4. Area Acceleration crop observation — INVESTIGATED, NO BALANCE CHANGE IN BUNDLE F

- Legendary Lv.2 remains x7, 30 seconds, radius 24 blocks.
- The growth sampler contains 57,777 positions in the radius-24 sphere and distributes the additional random-tick opportunities over that volume while preserving the long-run x3/x7 random-tick-rate contract.
- A single wheat plant receiving zero visible growth stages during one 30-second window does not by itself prove the aura is inactive: random-tick selection and the crop's own random growth roll are separate.
- The correct manual check is a multi-crop inside-field patch against an equivalent outside control patch, plus the existing deterministic furnace production regression.
- No multiplier/duration rebalance is made from a one-plant sample. If the multi-crop test still shows no meaningful separation, treat that as a gameplay-feel/design failure rather than hiding it behind the mathematical x7 contract.

## 5. Revive canon/runtime discrepancy — DISCOVERED, NOT PART OF BUNDLE F

- Canon: Legendary Lv.1 active; an allied player or tamed/dominated permanent companion dying within 20 blocks leaves a marker for 20 seconds; 3-second focus revives at 50% max HP; successful cooldown 120 seconds; self and temporary clones excluded.
- Current runtime explicitly excludes `ServerPlayer` deaths and implements only permanent-companion revival.
- Allied-player revival therefore remains an implementation gap and requires a dedicated multiplayer death/respawn authority pass.

## 6. Auto Refill / unexpected hotbar insertion — WAITING FOR VIDEO

- No further change in Bundle F. The reported spawn-egg/hotbar behavior will be investigated from the promised reproduction video before changing Auto Refill.

## Verification

CI #523 on runtime commit `b4709126c02c86a5b40aad285cc54f34c46c656d` completed SUCCESS:

- 132 / 132 required GameTests PASS
- clean build / dependency checks PASS
- World Bosses localization capture PASS
- external Korean localization audit PASS
- dedicated-server boot smoke PASS
- client boot smoke PASS
- MRPack generation / verification PASS
- production JAR structure / checksum PASS

Manual post-Bundle-F visual/feel testing: NOT YET RUN.
Multiplayer testing: NOT RUN.
