# TITANBREAK Character Design Pipeline

This is the required production path for new or revised enemies, elites, bosses and large multipart entities.

## Gate 0 — Canonical intake
Read the current content bible first. Extract only presentation-relevant facts: role, scale, behavior, movement, environmental trace, destructible parts, phases, signature skills and strategy. Do not invent progression/rewards to solve an art problem.

## Gate 1 — Silhouette brief
Write one sentence that still identifies the creature when texture detail is removed. Define primary mass, movement axis, secondary masses, negative space, weakpoint placement and what prevents confusion with nearby roster entries.

If the sentence is “large humanoid with X attached,” redesign before modeling unless humanoid anatomy is itself canonical.

## Gate 2 — Runtime contract audit
Before touching `.geo.json`, inspect renderer bone visibility/phase logic, entity part constants, animation bone references, renderer scale and culling behavior. Freeze every runtime-referenced bone name. Decorative bones can be added freely; required bones cannot be silently renamed or nested under visibility-breaking parents.

## Gate 3 — Geometry blockout
Block major silhouette first, then add gameplay organs. Do not start by decorating the old model.

Required checks:
- front/side silhouette differs from neighboring threats,
- weakpoints remain spatially separated,
- locomotion bones have credible pivots,
- scale reads correctly next to the player,
- phase shell/core transitions have independent geometry.

For alpha.53-style remasters, prefer adding identity architecture around frozen runtime bones rather than renaming contract bones merely to make the file look cleaner.

## Gate 4 — Animation compatibility
Parse every animation and verify every referenced bone exists. Walk/idle/attack must still move a sensible structure after remapping anatomy. A legacy name retained for compatibility is an implementation contract, not a visual mandate.

## Gate 5 — Weakpoint and phase audit
For every destructible part:
1. identify its renderer/part bone or explicit visual proxy,
2. confirm it is visible in the attackable phase,
3. destroy it and confirm only the intended mass disappears,
4. confirm shell/core transitions do not hide unrelated organs.

## Gate 6 — Asset provenance
Update `ASSET_REGISTRY.md` whenever runtime art/dependencies change source or ownership. Project-owned remasters remain traceable by implementation notes and version control.

## Gate 7 — Automated visual contract
`tools/verify_visual_assets.py` is the reusable structural gate. CI must reject:
- malformed geometry JSON,
- duplicate bone names,
- invalid parent references or cycles,
- malformed cube vectors,
- animation references to missing bones,
- missing ordinary model/texture stem mappings,
- missing persistent signature bones/cube-density floors for protected remasters.

Do not hard-code a historical exact alpha number into the reusable regression workflow. Version-specific completion belongs in the implementation note/status document.

## Gate 8 — Playable presentation review
At the first integrated test build, verify spawn/culling distance, scale/ground contact, weakpoint alignment, traversal/clipping, phase visibility, animation pivots and severe texture stretching.

Only then mark presentation “play-tested.” A clean build means structurally shippable, not visually approved.
