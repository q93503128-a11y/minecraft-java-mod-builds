# M3 — Region 01 Boss Threat-Telegraph Field Review

This checkpoint adds a **field-review-only threat-footprint outline** to the existing Region 01 boss harness. It improves the player's ability to read the already-authored provisional hit shapes before ACTIVE; it does not approve final Riftfrontier VFX, boss material, damage, geometry balance, arena-pressure animation, or production encounter insertion.

## Design boundary

Reference principle: readable action-boss encounters in established Minecraft mods such as Mowzie's Mobs teach players to recognize committed patterns and response windows rather than relying on opaque stat checks. Riftfrontier adapts that problem-solving principle only. No external code, particles, textures, sounds, models, animation, or other redistributable assets were imported for this checkpoint.

The implementation deliberately reuses Minecraft's native `CRIT` particle as a temporary calibration language. Final Region 01 VFX still requires the project's normal reference/provenance/review process.

## What is authoritative

The visual footprint does **not** define a second hit volume. `Region01BossFieldImpactProfile` remains the single provisional field-calibration source used by the server hit resolver:

- `region_01_committed_strike` — broad close `FORWARD_ARC`, reach `3.4`, half-width `2.2`;
- `region_01_line_displacement` — narrow `FORWARD_LANE`, reach `6.0`, half-width `1.15`, plus the existing ACTIVE forward travel;
- `region_01_arena_pressure` — facing-independent `LOCAL_AREA`, radius `4.5`, plus the existing one-shot ACTIVE radial impulse.

`Region01BossFieldTelegraphGeometry` only samples sparse boundary points from those existing profiles. `Region01BossFieldTelegraphEmitter` emits those samples only while the server semantic state is `TELEGRAPH`, at half tick-rate for readability. It owns no attack clock, target admission, damage, movement, hit deduplication, knockback, facing commitment, or phase transition.

## Verification history

Implementation chain:

- `794ca22b6f08f2f0cb213afd22b5c367ecd626f4` — add field threat-shape emitter;
- `4a1ea5d0a4f72b48ad13bed16b85fe21a35b6b67` — wire it to the validated boss tick result;
- `77cfedf953b251a2770105b40fcec1a24c276dec` — initial boundary tests;
- `77c9cea8a88d949ffffaddd5945f372b2c08dcfd` — isolate local boundary sampling from Minecraft runtime classes;
- `1226449db16adf993209ef4e330526ff4ec75363` — make the emitter consume the pure geometry helper;
- `fc5e8411714d2dcf87df56141cf92156cb7de809` — move boundary tests onto the Minecraft-free helper.

The first CI run, `34841557693`, failed in `Tests and clean build` because the pure JUnit test directly loaded `Region01BossFieldTelegraphEmitter`, which links Minecraft's `ParticleOptions`; the test runtime therefore raised `NoClassDefFoundError`. Gameplay code was not hidden or weakened to make the test pass. Geometry sampling was separated into a Minecraft-API-free helper and the tests now target that helper.

Workflow `34841785748` on `fc5e8411714d2dcf87df56141cf92156cb7de809` completed successfully: toolchain, asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, reports and artifact uploads all passed.

Deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10346567359`
- digest: `sha256:261c1f4f6d24f3e671ace52fc57f1ec6694b511db727773e873fe33765654e97`

Verification vocabulary for this code checkpoint:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

## Human field procedure

Use the verified JAR from workflow `34841785748` in a disposable world.

Spawn the development-only actor:

```text
/riftfrontier boss fieldtest spawn
```

### Phase 1 — arc versus lane readability

Run:

```text
/riftfrontier boss fieldtest phase1
```

Observe several complete attacks.

For committed strike:

1. During TELEGRAPH, a **broad, short forward boundary** should be visible on the ground.
2. Strafe across the side edge before ACTIVE. The boss must keep the facing committed at attack start rather than rotating the outline to home onto the player.
3. Stand clearly outside the shown boundary for one execution and confirm the outline does not visually claim obviously safe distant space.
4. Stand deliberately inside for another execution and confirm the later server hit is spatially plausible relative to the outline. Do not infer final damage/balance from the diagnostic hit.

For line displacement:

1. During TELEGRAPH, a **long, narrow lane** with a readable forward cap should appear.
2. Move laterally out of the lane before ACTIVE; the existing line charge must continue along the committed facing rather than following the player.
3. The lane outline must disappear when TELEGRAPH ends. Existing ACTIVE travel is server-owned and must not be driven by particles.

### Phase 2 — local pressure readability

Run:

```text
/riftfrontier boss fieldtest phase2
```

1. During arena-pressure TELEGRAPH, a **facing-independent ring** should appear around the boss.
2. Walk around the boss during TELEGRAPH; the ring should remain radial rather than rotate as a directional cone/lane.
3. At ACTIVE entry, the pre-existing one-shot radial impulse/explosion cue should occur; the telegraph ring must not create additional damage or repeated knockback.
4. This test does **not** accept the separately pending arena-pressure authored motion candidate. Motion acceptance remains governed by `M3_REGION01_ARENA_PRESSURE_MOTION_REVIEW.md`.

## Failure conditions to record

Treat any of the following as a field failure rather than silently tuning automation values:

- the visible boundary appears to rotate/home after the attack has committed;
- the broad strike and narrow lane are difficult to distinguish at normal Minecraft camera distance;
- obvious server hits occur materially outside the shown calibration footprint, or obvious inside positions consistently miss for geometry reasons;
- particles remain through ACTIVE/RECOVERY and falsely imply a second damage window;
- line-charge travel, radial impulse, damage, attack phase, or target selection changes because of the readability layer;
- particle density obscures the Dragon silhouette or other counterplay cues;
- client/resource reload crashes or duplicates presentation;
- multiplayer clients observe meaningfully inconsistent footprints from the same server execution.

Do not change final geometry, particle language, damage, material, animation acceptance, or production encounter composition from automation alone. Human observations should be recorded first, then the smallest evidence-backed calibration change can be made.
