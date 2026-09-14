# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS THREAT-SHAPE TELEGRAPH BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Settled direction — do not redo without new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless technical hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence already exist. Do not reopen these boundaries without a demonstrated regression.
- Player combat already has two server-authoritative weapon families (`mobile_pressure`, `reach_commitment`), one `recovery_pivot` technique path, authenticated move-id input, ACTIVE-only hit authority and per-execution target dedupe.
- Region 01 boss geometry/rig remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this working path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Provenance/license registry remains `docs/THIRD_PARTY_ASSETS.md`. The original Dragon Evolved Atlas is source provenance/reference only and is **not** approved final Region 01 art.
- Production renderer must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not weaken exact-coverage/review gates or fabricate `presentation_assets`.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone-textured boss preview, actionbar and current hit cues are temporary readability/field-review aids, not final Riftfrontier UI/art/VFX language.
- Do not auto-tune provisional damage, hit geometry, charge travel, radial impulse, particle density or key layout without human field evidence.

## Verified connected expedition loop

Fresh-world login can bootstrap the technical hub when there is no active run and no expedition history. Hub lodestone delegates to existing authoritative deployment; smithing table delegates to existing provisioning. Existing expedition history prevents bootstrap reset. `/riftfrontier expedition start` remains diagnostic/fallback only.

Minecraft-native actionbar feedback reads existing authoritative state for deployment objective, salvage count, live patrol threats, pressure, stored salvage, supply and next deployment cost. It introduces no client-owned expedition state or custom final HUD.

Relevant human loop procedure: `docs/M2B_IN_WORLD_HUB_LOOP.md`.

Known verified checkpoints include:

- fresh bootstrap/login: `5f9d6b6bb889a5eab84135437df4150ec80c6494`, `19a54bdf9ac5c620099688cbdeac4e1722d3b21d`, workflow `34808226019` SUCCESS;
- actionbar API correction: `fded37cbe91668af213af477ae0b0238ac455693`, workflow `34821369460` successful rerun;
- player hit readability: `24a15b2a0c8db3ade4c6ae730da1373c93e59e00`, workflow `34825929915` SUCCESS.

Human expedition/player-combat status remains `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO` unless actual field evidence is later supplied.

## Region 01 boss field harness

Development-only commands:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production Region 01 encounter composition.

Current server-authoritative field behavior:

- production semantic profile `riftfrontier:boss/region_01_first_apex`;
- phase 1 committed strike + line displacement;
- phase 2 arena pressure;
- authoritative `TELEGRAPH -> ACTIVE -> RECOVERY`;
- diagnostic `1.0F` damage, ACTIVE-only hit admission and once-per-execution target dedupe;
- attack-start facing commitment, no mid-swing homing;
- provisional role-specific hit geometry;
- ACTIVE-only line-charge travel;
- one-shot arena-pressure radial impulse plus native diagnostic explosion cue;
- health-backed Minecraft boss bar.

These values/cues are field calibration, not final balance/art.

## Dragon Evolved presentation state

The temporary field-review renderer exposes accepted sanitized Dragon geometry with Minecraft stone only while complete production presentation cannot publish. Production rendering always has priority; duplicate preview + production meshes are a failure.

Evidence-backed source motion currently used for field review:

- committed strike -> reviewed `Punch` windows;
- line displacement -> reviewed `Headbutt` windows;
- non-attacking damage -> reviewed `HitReact`;
- terminal death -> reviewed non-looping `Death`;
- unresolved cases -> reviewed neutral `Flying_Idle` fallback.

Presentation priority remains `Death > reviewed authoritative attack sample > HitReact > Flying_Idle`.

Relevant docs:

- `docs/M3_REGION01_BOSS_VISIBLE_FIELD_PREVIEW.md`
- `docs/M3_REGION01_ARENA_PRESSURE_MOTION_REVIEW.md`
- `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`

### Arena-pressure motion candidate

The authored field-only compression/burst/recovery candidate is automated-build verified but **not human accepted**. It does not close production animation coverage and must not be promoted automatically.

Checkpoint `bca0816381c09b28d613bc12d2e395eb62368761`, workflow `34812925676` SUCCESS, deliverable digest `sha256:9ab57cfec3a13f69c37b6d6732f126b9371eaf0b05d5e5fb32f7e39a5bf772f0`.

Human review is required before source-motion authoring/promotion.

### Final material remains open

A previous dark-rock material candidate was discovered to contain a corrupt PNG only after decoder-level validation was added. That candidate was rejected and the verified stone field-review fallback restored. Do not resurrect the rejected resource or treat JAR inclusion as proof that an image decodes correctly.

Canonical rejection record: `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`.

Future material candidates must be real physical resources with provenance/license evidence and successful decode/format validation before renderer hookup. Final visual language still requires reference review and actual Minecraft visual inspection.

## Latest checkpoint — server-derived boss threat-shape telegraph

Latest gameplay-facing batch makes the existing provisional boss threat geometry readable during TELEGRAPH without creating new gameplay authority.

Implementation:

- `Region01BossFieldTelegraphGeometry` converts the existing `Region01BossFieldImpactProfile` into sparse local-space boundary samples with no Minecraft API dependency;
- `Region01BossFieldTelegraphEmitter` reads the validated server semantic presentation state and emits Minecraft-native `CRIT` particles only during TELEGRAPH, every other server tick;
- `Region01BossEntity` invokes that emitter from the same validated runtime result already used for presentation/networking;
- committed strike appears as a broad short forward outline, line displacement as a long narrow lane with forward cap, arena pressure as a facing-independent ring;
- the particles do **not** own attack timing, hit admission, damage, target selection, facing, movement, knockback, dedupe or phase transitions.

Implementation chain:

- `794ca22b6f08f2f0cb213afd22b5c367ecd626f4` — initial threat-shape emitter;
- `4a1ea5d0a4f72b48ad13bed16b85fe21a35b6b67` — authoritative boss-tick wiring;
- `77cfedf953b251a2770105b40fcec1a24c276dec` — initial geometry tests;
- `77c9cea8a88d949ffffaddd5945f372b2c08dcfd` / `1226449db16adf993209ef4e330526ff4ec75363` / `fc5e8411714d2dcf87df56141cf92156cb7de809` — separate pure geometry from Minecraft runtime and keep unit tests API-free.

Verification history must be preserved:

- workflow `34841557693` failed `Tests and clean build` because the initial pure JUnit test linked `Region01BossFieldTelegraphEmitter`, causing `NoClassDefFoundError: net/minecraft/core/particles/ParticleOptions` in the non-Minecraft test runtime;
- the implementation was not weakened; boundary sampling was moved into a Minecraft-API-free helper and tests target that helper;
- workflow `34841785748` on `fc5e8411714d2dcf87df56141cf92156cb7de809` completed SUCCESS: toolchain, asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10346567359`
- digest: `sha256:261c1f4f6d24f3e671ace52fc57f1ec6694b511db727773e873fe33765654e97`

Human procedure: `docs/M3_REGION01_BOSS_THREAT_TELEGRAPH_REVIEW.md`.

Verification vocabulary for `fc5e8411714d2dcf87df56141cf92156cb7de809`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Unit tests, GameTest, dedicated-server smoke and Xvfb client smoke never count as human play or multiplayer field testing.

## Exact next development boundary

Do not spend the next run adding more telegraph/backend fences merely because this batch is stable. Prefer the highest-impact visible/connected Region 01 work that does not fake unresolved human acceptance.

Priority order:

1. If actual human field evidence exists, use it to calibrate/reject the new threat-outline readability and/or arena-pressure motion candidate; do not invent results.
2. Otherwise continue independent production-visible work: obtain or author a **legally usable, decodable, provenance-recorded final Dragon Evolved material/texture direction** based on strong commercial-game/major-mod references, or improve other Region 01 combat/environment presentation that does not require that material gate to be bypassed.
3. Once arena-pressure motion is human accepted and all nine logical animation keys have evidence, assemble complete production animation binding without weakening exact coverage.
4. Select/author final VFX and sound under the same reference/provenance/readability rules. Presentation may emphasize authoritative windows but must never create a second gameplay hit clock.
5. Author the first real `presentation_assets` manifest only after referenced physical resources exist and are reviewed.
6. Publish through the existing reload/render pipeline; verify Minecraft scale, hit-volume readability and resource reload.
7. Only after verified production presentation replaces temporary review paths should stone-material/field-preview scaffolding be removed as superseded.
8. Only after presentation and human field evidence clear the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Never claim `PLAYTESTED` or `MULTIPLAYER TESTED` without actual human sessions.
