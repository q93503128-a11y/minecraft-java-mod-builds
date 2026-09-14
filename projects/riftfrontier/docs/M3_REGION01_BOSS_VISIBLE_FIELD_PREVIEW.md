# M3 — Region 01 Boss Visible Field-Review Preview

This checklist verifies the temporary **field-review visibility path** for the Region 01 boss. It does **not** approve final boss material, color language, unresolved attack animation coverage, VFX, balance, or production encounter composition.

## Why this exists

The accepted Dragon Evolved geometry/rig is already bundled and integrity-gated, while production renderer publication still correctly requires all nine logical attack-animation keys plus a reviewed final material. Before those gates close, a human field test must not be forced to evaluate an invisible boss or ignore the two attack motions whose source-role fit and exact phase windows are already reviewed.

The field-review renderer therefore uses only:

- the accepted sanitized Dragon Evolved geometry/skin;
- the reviewed `Punch` source windows for `region_01_committed_strike`;
- the reviewed `Headbutt` source windows for `region_01_line_displacement`;
- the already reviewed cyclic `Flying_Idle` source motion only when no reviewed attack sample exists, including unresolved `region_01_arena_pressure`;
- Minecraft's own `minecraft:textures/block/stone.png` as an intentionally non-final review material.

The `Punch` / `Headbutt` samples are driven by the synced server semantic phase and phase progress. They do not create a second attack clock and do not authorize server damage, hit geometry, movement, target selection, or phase transitions.

`region_01_arena_pressure` remains explicitly unresolved. Do **not** interpret its neutral `Flying_Idle` fallback as an arena-pressure animation mapping, and do not weaken the production 9/9 semantic-coverage or reviewed-material gates.

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

2. **Reviewed committed-strike motion follows authoritative phase**
   - When `region_01_committed_strike` runs, the body should use the reviewed `Punch` source motion rather than the neutral hover loop.
   - TELEGRAPH, ACTIVE and RECOVERY should advance through the previously reviewed source windows instead of restarting the entire clip at each phase.
   - The visible motion must not move the server hit window or change whether damage lands.

3. **Reviewed line-displacement motion follows authoritative phase**
   - When `region_01_line_displacement` runs, the body should use the reviewed `Headbutt` source motion.
   - Its visual phase progression should stay aligned with the synced semantic phase while authoritative line-charge travel remains server-owned.
   - Strafe out of the committed lane during TELEGRAPH and confirm the animation does not cause mid-attack homing.

4. **Unresolved arena pressure stays unresolved**
   - In phase 2, `region_01_arena_pressure` should still display the neutral reviewed `Flying_Idle` fallback.
   - It must not reuse `Punch`, `Headbutt`, `Fast_Flying`, `Yes`, `No`, `HitReact`, or `Death` merely to look complete.
   - The authoritative radial impulse and diagnostic ACTIVE-entry cue remain the gameplay evidence; neutral hover is only a visible-body fallback.

5. **Temporary material is unmistakably temporary**
   - The body uses Minecraft stone texture as a neutral review surface.
   - Any visual ugliness caused by source UVs against the stone texture is not a final-art defect; record only whether silhouette, deformation, attack motion and scale are readable enough for field review.

6. **Combat authority remains independent**
   - Telegraph boundary particles, hit/miss results, line-charge travel, arena-pressure impulse, and boss health remain authoritative exactly as before.
   - The preview animation must not alter hit timing, movement, damage, target selection or phase transitions.

7. **Production publication has priority**
   - When a future complete reviewed production presentation publishes, the field-review preview must not draw on top of it.
   - If duplicate overlapping dragon meshes ever appear, treat that as a failure.

8. **Reload behavior**
   - Reload client resources once while the field boss exists.
   - The preview should either continue from freshly prepared accepted geometry or disappear fail-closed during invalidation; it must not crash the client or keep stale geometry indefinitely.

## Evidence to capture

Capture at least:

- one front/three-quarter screenshot showing scale and silhouette;
- one committed-strike clip showing `Punch` through TELEGRAPH -> ACTIVE -> RECOVERY;
- one line-displacement clip showing `Headbutt` plus physical charge travel and a lateral dodge;
- one arena-pressure clip showing that unresolved motion remains neutral while radial gameplay behavior still occurs;
- any duplicate-render, stale-render, phase-desync or deformation defect with reproduction steps.

## Status vocabulary

Automated CI may establish `CODE REVIEWED`, `TESTED`, `BUILD VERIFIED`, and `JAR PRODUCED`.

Only actual human observation from the steps above can establish `PLAYTESTED`. A real two-client/server session is required for `MULTIPLAYER TESTED`.
