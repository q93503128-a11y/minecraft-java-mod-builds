# M3 — Region 01 Boss Visible Field-Review Preview

This checklist verifies the temporary **field-review visibility path** for the Region 01 boss. It does **not** approve final boss material, color language, unresolved attack animation coverage, VFX, balance, or production encounter composition.

## Why this exists

The accepted Dragon Evolved geometry/rig is already bundled and integrity-gated, while production renderer publication still correctly requires all nine logical attack-animation keys plus a reviewed final material. Before those gates close, a human field test must not be forced to evaluate an invisible boss or ignore source motions whose visual evidence is already reviewed.

The field-review renderer therefore uses only:

- the accepted sanitized Dragon Evolved geometry/skin;
- the reviewed `Punch` source windows for `region_01_committed_strike`;
- the reviewed `Headbutt` source windows for `region_01_line_displacement`;
- the directly reviewed `HitReact` source motion for non-attacking damage readability;
- the directly reviewed terminal `Death` source motion while the entity is dying;
- the already reviewed cyclic `Flying_Idle` source motion only when no reviewed attack/reaction sample exists, including unresolved `region_01_arena_pressure`;
- Minecraft's own `minecraft:textures/block/stone.png` as an intentionally non-final review material.

The `Punch` / `Headbutt` samples are driven by the synced server semantic phase and phase progress. `HitReact` uses Minecraft's client-visible hurt timers only as a presentation sampler, and `Death` uses the entity death timer as a non-looping presentation sampler. None creates a gameplay clock or authorizes server damage, hit geometry, movement, target selection, phase transitions, or death state.

Presentation priority is deliberate: terminal `Death` > reviewed authoritative attack sample > `HitReact` > neutral `Flying_Idle`. Therefore taking damage during an executing reviewed attack must **not** visually cancel the attack merely because `hurtTime` is non-zero.

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

4. **Reviewed hit reaction does not cancel an authoritative attack**
   - Damage the boss while no reviewed attack sample is active and confirm the short reviewed `HitReact` recoil is visible.
   - Damage the boss during a visible committed-strike or line-displacement execution and confirm that `Punch` / `Headbutt` remains visually authoritative rather than being replaced by `HitReact`.
   - Hit reaction must not change attack timing, damage, movement, target commitment, or boss health beyond Minecraft's existing damage result.

5. **Reviewed death presentation is terminal and non-looping**
   - Reduce the field boss to zero health and confirm the reviewed `Death` whole-body transition becomes visible during Minecraft's normal death lifecycle.
   - The preview samples forward and clamps at the end of the source clip; it must not loop back into hover or attack before entity removal.
   - Death presentation must not resurrect the actor, reopen attack state, or create a separate terminal-state authority.

6. **Unresolved arena pressure stays unresolved**
   - In phase 2, `region_01_arena_pressure` should still display the neutral reviewed `Flying_Idle` fallback when no reaction/death presentation is active.
   - It must not reuse `Punch`, `Headbutt`, `Fast_Flying`, `Yes`, `No`, `HitReact`, or `Death` as an arena-pressure attack mapping merely to look complete.
   - The authoritative radial impulse and diagnostic ACTIVE-entry cue remain the gameplay evidence; neutral hover is only a visible-body fallback.

7. **Temporary material is unmistakably temporary**
   - The body uses Minecraft stone texture as a neutral review surface.
   - Any visual ugliness caused by source UVs against the stone texture is not a final-art defect; record only whether silhouette, deformation, attack/reaction motion and scale are readable enough for field review.

8. **Combat authority remains independent**
   - Telegraph boundary particles, hit/miss results, line-charge travel, arena-pressure impulse, boss health and death state remain authoritative exactly as before.
   - The preview animation must not alter hit timing, movement, damage, target selection, phase transitions or terminal state.

9. **Production publication has priority**
   - When a future complete reviewed production presentation publishes, the field-review preview must not draw on top of it.
   - If duplicate overlapping dragon meshes ever appear, treat that as a failure.

10. **Reload behavior**
   - Reload client resources once while the field boss exists.
   - The preview should either continue from freshly prepared accepted geometry or disappear fail-closed during invalidation; it must not crash the client or keep stale geometry indefinitely.

## Evidence to capture

Capture at least:

- one front/three-quarter screenshot showing scale and silhouette;
- one committed-strike clip showing `Punch` through TELEGRAPH -> ACTIVE -> RECOVERY;
- one line-displacement clip showing `Headbutt` plus physical charge travel and a lateral dodge;
- one non-attacking damage clip showing `HitReact`;
- one damage-during-attack clip proving reaction presentation does not cancel the authoritative attack motion;
- one death clip showing the terminal `Death` transition without looping;
- one arena-pressure clip showing that unresolved motion remains neutral while radial gameplay behavior still occurs;
- any duplicate-render, stale-render, phase-desync or deformation defect with reproduction steps.

## Status vocabulary

Automated CI may establish `CODE REVIEWED`, `TESTED`, `BUILD VERIFIED`, and `JAR PRODUCED`.

Only actual human observation from the steps above can establish `PLAYTESTED`. A real two-client/server session is required for `MULTIPLAYER TESTED`.
