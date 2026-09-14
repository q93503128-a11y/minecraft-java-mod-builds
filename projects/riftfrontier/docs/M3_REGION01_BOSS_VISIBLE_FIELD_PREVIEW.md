# M3 — Region 01 Boss Visible Field-Review Preview

This checklist verifies the temporary **field-review visibility path** for the Region 01 boss. It does **not** approve final boss material, color language, attack animation coverage, VFX, balance, or production encounter composition.

## Why this exists

The accepted Dragon Evolved geometry/rig is already bundled and integrity-gated, while production renderer publication still correctly requires all nine logical attack-animation keys plus a reviewed final material. Before those gates close, a human field test must not be forced to evaluate an invisible boss.

The field-review renderer therefore uses only:

- the accepted sanitized Dragon Evolved geometry/skin;
- the already reviewed cyclic `Flying_Idle` source motion as a neutral silhouette preview;
- Minecraft's own `minecraft:textures/block/stone.png` as an intentionally non-final review material;
- local render time only for this neutral preview loop.

It does **not** map `Flying_Idle` to any gameplay attack role and does not weaken the production 9/9 semantic-coverage or reviewed-material gates.

## Build / launch

Use the JAR produced by the successful `Build Riftfrontier` workflow for the checkpoint commit under test.

Launch a normal client/server development or packaged-JAR environment, then run:

```text
/riftfrontier boss fieldtest spawn
```

Optional phase commands:

```text
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

## Required observations

1. **Visible accepted silhouette**
   - The spawned field boss body is visible, not only its particles/boss bar.
   - The mesh should read as the accepted Dragon Evolved silhouette and should not collapse into a rigid cube proxy.

2. **Neutral hover motion only**
   - The body visibly cycles through the reviewed `Flying_Idle` motion.
   - Do not interpret this preview loop as committed-strike, line-displacement, or arena-pressure animation approval.

3. **Temporary material is unmistakably temporary**
   - The body uses Minecraft stone texture as a neutral review surface.
   - Any visual ugliness caused by source UVs against the stone texture is not a final-art defect; record only whether silhouette, deformation and scale are readable enough for field review.

4. **Combat authority remains independent**
   - Telegraph boundary particles, hit/miss results, line-charge travel, arena-pressure impulse, and boss health remain authoritative exactly as before.
   - The neutral preview loop must not alter hit timing, movement, damage, target selection or phase transitions.

5. **Production publication has priority**
   - When a future complete reviewed production presentation publishes, the field-review preview must not draw on top of it.
   - If duplicate overlapping dragon meshes ever appear, treat that as a failure.

6. **Reload behavior**
   - Reload client resources once while the field boss exists.
   - The preview should either continue from freshly prepared accepted geometry or disappear fail-closed during invalidation; it must not crash the client or keep stale geometry indefinitely.

## Evidence to capture

Capture at least:

- one front/three-quarter screenshot showing scale and silhouette;
- one short clip showing the neutral hover deformation;
- one attack sequence showing that gameplay telegraph/hit behavior remains separate from the preview motion;
- any duplicate-render, stale-render or deformation defect with reproduction steps.

## Status vocabulary

Automated CI may establish `CODE REVIEWED`, `TESTED`, `BUILD VERIFIED`, and `JAR PRODUCED`.

Only actual human observation from the steps above can establish `PLAYTESTED`. A real two-client/server session is required for `MULTIPLAYER TESTED`.
