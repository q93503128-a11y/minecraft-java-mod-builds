# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 COMBAT-SPACE BASELINE BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified gameplay checkpoint — Region 01 combat space

Implementation checkpoint: `40fe05c08634b80f0fc87b3f0df2b1e5cdc02c82` (`riftfrontier: shape Region 01 combat space`).

The previous Region 01 technical field was still functionally an 11×11 smooth-stone plane. This batch turns the combat portion into a small navigation-bearing field-review arena without changing authoritative expedition state:

- `Region01FieldArenaPlan` is a pure layout contract with an 11-wide field, explicit open lanes and four two-block staggered cover pillars.
- `Region01FieldArena` materializes that plan with Minecraft runtime `POLISHED_TUFF` / `TUFF_BRICKS` only.
- `Region01EncounterRuntime.begin(...)` materializes the arena after the technical-cell reset and before threat spawn.
- Arrival `(0,-4)`, central salvage `(0,0)`, the four corner salvage nodes and extraction relay `(0,5)` remain unobstructed by cover.
- z=3..5 is intentionally untouched so the existing reviewed extraction-relay basalt/calcite/deepslate approach keeps ownership of that space.
- Encounter counts, pressure math, hazard values, rewards, extraction gate, run ownership, save/network contracts and player/boss hit authority are unchanged.
- A pure JUnit regression test checks field bounds, open objective lanes, cover height and non-overlap with arrival/salvage/relay positions.

Reference boundary: Mojang/Minecraft Trial Chambers are used only for the design principle that mixed melee/ranged combat benefits from varied combat spaces and readable tuff-family structure. No Trial Chamber room is copied. No external asset bytes were bundled, so `docs/THIRD_PARTY_ASSETS.md` needs no new redistribution entry. Human-review record: `docs/M2B_REGION01_COMBAT_SPACE_REVIEW.md`.

Verification: `Build Riftfrontier` workflow `34885087681` completed **SUCCESS** on `40fe05c08634b80f0fc87b3f0df2b1e5cdc02c82`.

Passed in that workflow:

- toolchain verification
- asset-intake tests
- `clean test build`
- required native GameTest gate
- dedicated-server smoke
- Xvfb client smoke
- executable-JAR inspection
- build report and artifact upload

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10364073552`
- archive digest: `sha256:fdd41129d7490ce21098353e40f625407d8b146de6b5cc095f8d2517b8d5de51`
- executable JAR SHA-256: `025055ce7acf54742b36438dd142dcf0e2c1712aca3314728364adb0a2ebc6cf`

Verification vocabulary:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN COMBAT-SPACE ACCEPTANCE`: NO

### Exact human field procedure

1. Put the verified JAR in a disposable Minecraft 26.2 / NeoForge 26.2.0.38-beta instance.
2. Enter a fresh/disposable world and use the normal commandless hub -> lodestone deployment path into Region 01.
3. Equip a field weapon (`/riftfrontier weapon mobile` or `/riftfrontier weapon reach`) only if needed for combat convenience; the arena itself must not depend on the command.
4. Confirm the combat portion of the field is tuff-family rather than a featureless smooth-stone plane and that the existing extraction-relay approach at the far z edge still retains its basalt/calcite/deepslate presentation.
5. Use the staggered tuff-brick cover to break the Skeleton/Scout line of sight, then re-engage. The scout must remain threatening when the player leaves cover; cover must not permit permanent zero-risk ranged denial.
6. Pull Zombie/Hunter and Ravager/Elite around both left and right cover pairs. They must find obvious paths and must not become permanently stuck or create an unavoidable spawn pin.
7. Recover central and corner salvage under pressure. All five technical resource positions must remain reachable; the objective still requires the same authoritative 3 recoveries.
8. Reach 3/3 and use the existing extraction relay. Relay readiness markers and extraction must work unchanged.
9. Repeat on a higher-pressure run if practical. Extra hunters/scouts must not body-block the staggered passages into an unwinnable state.
10. Reject this field baseline if pathing repeatedly fails, salvage/relay is obscured, cover trivializes scouts, the elite traps itself, or the tuff treatment reads as an approved final Riftfrontier biome/art direction.
11. Do not mark multiplayer tested until two human clients actually exercise the same run.

Do not auto-tune cover positions or expand temporary field decoration based only on automation observation. Human field evidence should decide whether this layout is retained, revised or replaced by later production environment work.

## Settled direction — do not redo without demonstrated regression or new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- Player combat has `mobile_pressure` / `reach_commitment`, a `recovery_pivot` option, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Accepted move-start native swing checkpoint remains `22a39b59671ad88cfbf76b830234f222ea9520c2`, workflow `34878394712` SUCCESS. Do not add more generic weapon cues without human evidence.
- Connected expedition localization remains `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`, workflow `34873147707` SUCCESS. Do not reopen ordinary localization/actionbar/live-status work without evidence.
- Earlier verified field-readability checkpoints remain valid: hit readability `24a15b2a...`, live status `54f070f1...`, extraction relay `eee2a34b...`, proxy role labels `e7b7421...`, salvage hazard cue `54d03af...`.
- Zombie/Skeleton/Ravager remain behaviour proxies only. Their vanilla silhouettes, equipment and role names are not production creature art.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone boss preview, actionbar, relay dressing, proxy labels, player hit cues, salvage cues, native swing and the current tuff combat-space treatment are field-review aids, not final Riftfrontier UI/art/VFX/audio/environment language.
- Do not auto-tune provisional damage, hit geometry, attack timing, charge travel, radial impulse, particle density, salvage cue intensity, control layout or the new cover geometry without human field evidence.

## Region 01 boss presentation gates — still open

- Selected boss rig/geometry remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized runtime resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Source/provenance registry remains `docs/THIRD_PARTY_ASSETS.md`. The original source Atlas is reference/provenance only and is not approved final art.
- A previous dark-rock material candidate contained a corrupt PNG and is rejected. Keep the verified stone fallback. Any future material candidate must have redistributable provenance/license evidence and successful real decode/format validation before renderer hookup. See `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`.
- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not fabricate `presentation_assets` or weaken exact-coverage/review gates.
- Evidence-backed source motion remains committed strike -> `Punch`, line displacement -> `Headbutt`, damage -> `HitReact`, death -> non-looping `Death`, unresolved -> `Flying_Idle`.
- The authored arena-pressure compression/burst/recovery candidate (`bca0816381c09b28d613bc12d2e395eb62368761`, workflow `34812925676`) is automated-build verified but **not human accepted** and does not close production animation coverage.
- Boss TELEGRAPH threat-shape rendering remains build verified (`fc5e841...`). Do not add more telegraph layers without evidence.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; the boss is still excluded from natural/production Region 01 encounter insertion.

## Next useful development boundary

Do **not** respond to the new combat-space checkpoint by growing more tuff decoration, more relay/hub scaffolding, more proxy labels, more actionbar text, more generic weapon cues or another authority/lifecycle fence.

Unless new human field evidence arrives, prioritize one of these production-visible paths:

1. a legally redistributable, actually decodable, provenance-recorded final Dragon Evolved material/texture direction grounded in strong reference work; or
2. strong-reference / legally usable production creature or Region 01 environment presentation that can replace existing proxy/technical presentation without bypassing the boss material/animation gates; or
3. another connected visible combat/environment improvement that materially advances the vertical slice and does not reopen settled backend work.

Production creature silhouettes should not be invented from scratch by automation. Final VFX/sound/UI/environment language requires the same reference/provenance discipline and actual Minecraft human review. Remove temporary scaffolding only after a verified replacement exists.
