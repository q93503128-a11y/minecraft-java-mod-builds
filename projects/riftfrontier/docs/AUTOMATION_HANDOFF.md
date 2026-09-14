# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT + REGION 01 COMBAT-SPACE/STAGING BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified gameplay checkpoint — Region 01 threat staging

Implementation checkpoint: `f49357355899d3efb0e21f0101c837c5e28d0353` (`riftfrontier: stage Region 01 threats into combat space`).

The preceding combat-space checkpoint created staggered cover across the combat-owned z=-5..2 field, but the encounter runtime still spawned Hunters at z=3 inside the extraction-relay-owned approach and Scouts immediately beside the arrival side at z=-3. This batch connects encounter staging to the reviewed combat-space plan instead of adding more decoration or backend state.

- `Region01FieldArenaPlan` now owns three reviewed technical spawn cells for Hunter and Scout roles plus one Elite anchor cell.
- Hunters stage on the west flank at `(-4,-1)`, `(-4,1)`, `(-4,2)`.
- Scouts stage on the east flank at `(4,2)`, `(4,0)`, `(4,-2)`.
- Elite anchor remains on the open back-center lane at `(0,2)`.
- Every technical spawn is inside the combat-owned z=-5..2 field, outside the relay-owned z=3..5 approach, and does not overlap arrival, salvage objectives, extraction relay, or cover pillars.
- `Region01EncounterRuntime.begin(...)` consumes this pure plan while retaining the existing pressure-derived Hunter/Scout counts and single Elite.
- Encounter composition, mob types, AI/stats, role labels, salvage hazard, rewards, extraction gate, run ownership, save/network contracts and player/boss combat authority are unchanged.
- The plan has explicit capacity for the current maximum three Hunters and three Scouts; if future pressure data exceeds that reviewed capacity, runtime fails rather than silently placing additional mobs into unreviewed cells.
- Pure regression coverage verifies capacity, uniqueness, west/east role separation, combat-space bounds, and non-overlap with objectives/cover.

Human-review record: `docs/M2B_REGION01_THREAT_STAGING_REVIEW.md`.

Verification: `Build Riftfrontier` workflow `34890699183` completed **SUCCESS** on `f49357355899d3efb0e21f0101c837c5e28d0353`.

Passed in that workflow:

- toolchain verification
- asset-intake tests
- `clean test build`
- required native GameTest gate, including `region_01_encounter_runtime`
- dedicated-server smoke
- Xvfb client smoke
- executable-JAR inspection
- build report and artifact upload

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10366927173`
- archive digest: `sha256:37e476566e8fe5e6096a2cd60d89503a3c2f69a9d3fbee48f5112ed2c13a3c0b`
- executable JAR SHA-256: `9cd41f6b080113e5fa79b4e461e917b73cc70e212d726a27fc10554d54968686`

Verification vocabulary:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN COMBAT-STAGING ACCEPTANCE`: NO

### Exact human field procedure

1. Put the verified JAR in a disposable Minecraft 26.2 / NeoForge 26.2.0.38-beta instance.
2. Enter a fresh/disposable world and use the normal commandless hub -> lodestone deployment path into Region 01.
3. Equip `/riftfrontier weapon mobile` or `/riftfrontier weapon reach` only if needed for combat convenience.
4. On a low-pressure run, confirm the initial Hunter begins on the west combat flank, Scout on the east combat flank, and Elite on the open back-center lane; no patrol actor should begin in the far extraction-relay approach.
5. Confirm the player can leave the `(0,-4)` arrival lane without immediately being body-pinned by a spawn and that the Scout's first useful sightline can be broken with the reviewed staggered cover.
6. Pull Hunter and Elite around the cover. They must retain practical paths toward the player instead of idling behind a pillar or becoming trapped.
7. Recover central/corner salvage and reach the existing relay. Spawn staging must not obstruct any objective or change the authoritative 3-recovery extraction requirement.
8. Repeat after enough successful runs to increase Region 01 pressure. Extra Hunters/Scouts should occupy the additional west/east staging cells without overlapping each other or producing an unavoidable opening surround.
9. Reject/revise if arrival becomes an unavoidable crossfire, a role repeatedly stalls behind cover, the Elite spawn pins the center objective, the relay approach is occupied at run start, or higher-pressure staging creates an unwinnable body-block.
10. Do not mark multiplayer tested until two human clients exercise the same run.

Do not auto-tune the spawn coordinates or cover geometry from automation alone after this checkpoint. Human field evidence should decide whether staging stays, moves, or is replaced by later production creature/environment work.

## Settled direction — do not redo without demonstrated regression or new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Accepted move-start native swing checkpoint remains `22a39b59671ad88cfbf76b830234f222ea9520c2`, workflow `34878394712` SUCCESS. Do not add more generic weapon cues without human evidence.
- Connected expedition localization remains `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`, workflow `34873147707` SUCCESS. Do not reopen ordinary localization/actionbar/live-status work without evidence.
- Earlier field-readability checkpoints remain valid: hit readability `24a15b2a...`, live status `54f070f1...`, extraction relay `eee2a34b...`, proxy role labels `e7b7421...`, salvage hazard cue `54d03af...`.
- Combat-space baseline remains `40fe05c08634b80f0fc87b3f0df2b1e5cdc02c82`, workflow `34885087681` SUCCESS. Do not grow more tuff decoration; current material/layout is a technical field-review baseline, not final environment art.
- Zombie/Skeleton/Ravager remain behaviour proxies only. Their vanilla silhouettes, equipment and role names are not production creature art.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone boss preview, actionbar, relay dressing, proxy labels, player hit cues, salvage cues, native swing and current tuff combat-space treatment are field-review aids, not final Riftfrontier UI/art/VFX/audio/environment language.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, control layout, cover geometry or threat spawn staging without human field evidence.

## Region 01 boss presentation gates — still open

- Selected boss rig/geometry remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized runtime resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Source/provenance registry remains `docs/THIRD_PARTY_ASSETS.md`. Original source Atlas is reference/provenance only and is not approved final art.
- The prior dark-rock material candidate contained a corrupt PNG and is rejected. Keep the verified stone fallback. Any future material candidate needs redistributable provenance/license evidence and successful real decode/format validation before renderer hookup. See `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`.
- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not fabricate `presentation_assets` or weaken exact-coverage/review gates.
- Evidence-backed source motion remains committed strike -> `Punch`, line displacement -> `Headbutt`, damage -> `HitReact`, death -> non-looping `Death`, unresolved -> `Flying_Idle`.
- Authored arena-pressure compression/burst/recovery candidate `bca0816381c09b28d613bc12d2e395eb62368761` is automated-build verified but **not human accepted** and does not close production animation coverage.
- Boss TELEGRAPH threat-shape rendering remains build verified (`fc5e841...`). Do not add more telegraph layers without evidence.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; the boss is still excluded from natural/production Region 01 encounter insertion.

## Next useful development boundary

Do **not** respond to this staging checkpoint by growing more tuff decoration, spawn labels, actionbar text, generic combat cues or another authority/lifecycle fence.

Unless new human field evidence arrives, prioritize:

1. a legally redistributable, actually decodable, provenance-recorded final Dragon Evolved material/texture direction grounded in strong reference work; or
2. strong-reference / legally usable production creature or Region 01 environment presentation that can replace proxy/technical presentation without bypassing boss material/animation gates; or
3. another connected visible combat/environment improvement that materially advances the vertical slice and is independent of the still-open human gates.

Production creature silhouettes must not be invented from scratch by automation. Final VFX/sound/UI/environment language requires the same reference/provenance discipline and actual Minecraft human review. Remove temporary scaffolding only after a verified replacement exists.
