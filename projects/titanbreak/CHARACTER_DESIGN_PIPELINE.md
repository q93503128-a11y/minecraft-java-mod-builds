# TITANBREAK Character Design Pipeline

This is the required production path for new or revised enemies, elites, bosses and large multipart entities.

## Gate 0 — Canonical intake
Read the current content bible first. Extract only the facts that affect presentation: role, scale, behavior, movement, environmental trace, destructible parts, phases, signature skills and strategy. Do not invent progression/rewards to solve an art problem.

## Gate 1 — Silhouette brief
Write one sentence that still identifies the creature when all texture detail is removed. Define:
- primary mass,
- movement axis,
- two or three secondary masses,
- negative space,
- weakpoint placement,
- what makes it impossible to confuse with nearby roster entries.

If the sentence is “large humanoid with X attached,” redesign before modeling unless humanoid anatomy is itself canonical.

## Gate 2 — Runtime contract audit
Before touching `.geo.json`, inspect:
- renderer `hideBone` / phase visibility logic,
- entity part constants,
- animation bone references,
- renderer scale and culling behavior.

Freeze every runtime-referenced bone name. Decorative bones can be added freely; required bones cannot be silently renamed or nested under a parent whose visibility would break the mechanic.

## Gate 3 — Geometry blockout
Block the major silhouette first. Use a small number of large masses to prove the body plan, then add gameplay organs. Do not start by decorating the old model.

Required checks:
- front/side silhouette differs from existing bosses,
- weakpoints remain spatially separated,
- locomotion bones have credible pivots,
- scale reads correctly next to the player,
- phase shell/core transitions have independent geometry.

## Gate 4 — Animation compatibility
Parse every animation and verify every referenced bone exists. Walk/idle/attack must still move a sensible structure after remapping anatomy. If a legacy animation name such as `left_arm` is retained for compatibility, the geometry attached to it may be a weapon pylon or scythe limb; the name is an implementation contract, not a visual mandate.

## Gate 5 — Weakpoint and phase audit
For every destructible part:
1. identify its renderer bone,
2. confirm it is visible in the phase where it can be attacked,
3. destroy it and confirm only the intended mass disappears,
4. confirm core-shell transitions do not hide unrelated organs.

## Gate 6 — Asset provenance
Update `ASSET_REGISTRY.md` whenever runtime art/dependencies change source or ownership. Project-owned remasters must remain traceable to their boss and purpose.

## Gate 7 — Automated visual contract
CI must parse geometry JSON and fail on:
- missing required boss bones,
- duplicate bone names,
- invalid parent references,
- animation references to missing bones,
- missing current Visual Bible / pipeline / status documents,
- wrong alpha build version.

Static validation is not a substitute for in-game review; it prevents structural regression before manual testing.

## Gate 8 — Playable presentation review
At the first integrated test build, verify:
- spawn/culling at intended distance,
- scale and ground contact,
- weakpoint alignment with damage routing,
- no geometry clipping that blocks intended traversal,
- phase visibility transitions,
- animation pivots,
- texture stretching severe enough to obscure mechanics.

Only then mark presentation “play-tested.” A clean build means structurally shippable, not visually approved.
