# M3 — Region 01 Boss Dragon Evolved Semantic Role Review

## Result

The selected Quaternius Dragon Evolved derivation remains the Region 01 first-boss rig/geometry direction. This review advances only source-animation gameplay-role compatibility; it does not change server combat timing, hit geometry, boss movement, material, VFX, sound, or production encounter attachment.

### Accepted staged mappings

- `region_01_committed_strike` → source clip `Punch`.
  - Evidence: the already reviewed skinned motion is dominated by a raised forelimb committing outward with torso follow-through and a readable retraction.
  - Exact reviewed source windows: `0.0–0.2`, `0.2–0.275`, `0.275–1.0`.
- `region_01_line_displacement` → source clip `Headbutt`.
  - Evidence: the already reviewed skinned motion commits the head and torso in a pronounced forward/downward drive with secondary forelimbs, supporting a directional body-line read.
  - Exact reviewed source windows: `0.0–0.15555555555555556`, `0.15555555555555556–0.2`, `0.2–0.4888888888888889`.

The source `ACTION` windows above are presentation-motion segments only. They do **not** redefine Minecraft `ACTIVE`, damage timing, movement distance, lane shape, or target admission.

## Intentionally unresolved

`region_01_arena_pressure` remains unmapped. None of the currently carried source clips has both an explicitly accepted area-pressure semantic fit and exact reviewed source phase windows. `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` must not be recycled simply to satisfy exact-coverage validation.

Therefore `Region01BossDragonEvolvedStagedAttackBinding` is deliberately incomplete: six logical animation keys are reviewed and encoded, while the three arena-pressure keys remain absent. The production `BossAnimationSemanticBinding` exact-coverage gate must continue to reject this partial set as a complete renderer publication.

## Evidence

Canonical machine-readable review receipt:

`assets/sources/region_01_boss_dragon_evolved.semantic_role_review.json`

It references the existing motion and phase-window receipts and the accepted sanitized derivation SHA-256. Clip names were not used as semantic evidence.

## Next production boundary

1. Acquire/review a legally usable arena-pressure motion compatible with the selected Dragon Evolved rig direction, or author a documented derivative motion without replacing the selected rig merely for convenience.
2. Give that motion the same direct visual review and exact phase-window evidence.
3. Only then assemble the complete production source binding and pass `BossAnimationSemanticBinding.validate(...)`.
4. Material/texture, VFX and sound still require their own reviewed resources and provenance before a `presentation_assets` manifest is authored.
5. Human field play remains required; automated tests and client/server smoke are not PLAYTESTED or MULTIPLAYER TESTED.
