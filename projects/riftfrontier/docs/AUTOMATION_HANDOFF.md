# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT + REGION 01 COMBAT-SPACE/STAGING BUILD VERIFIED / BOSS MATERIAL DECODE GATE BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified checkpoint — boss material real-decode gate

Implementation checkpoint: `a961099624fdbf44aa81cd5d224b976b6ca37902` (`riftfrontier: reject undecodable boss materials`).

This closes a demonstrated intake hole from the previously rejected dark-rock candidate. `Region01BossMaterialPreparation` formerly authenticated only the exact resource bytes and SHA-256. That meant a corrupt/non-image payload with the recorded hash could still become a `PreparedMaterial` even though Minecraft could never use it as a texture. The material preparation path now performs an actual image decode after hash verification and before publication; empty, malformed or undecodable image bytes fail closed with `MaterialDecodeException`.

The order remains deliberate: current reload ownership -> exact resource read -> current-generation check -> SHA-256 integrity -> current-generation check -> real image decode -> current-generation check -> immutable prepared material. This does not approve art, alter the selected Dragon geometry/rig, weaken human review, or introduce a new final visual language.

Verification: `Build Riftfrontier` workflow `34902795221` completed **SUCCESS** on `a961099624fdbf44aa81cd5d224b976b6ca37902`.

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
- artifact id: `10371478692`
- archive digest: `sha256:9330c8089715286906598a85ea340dcda0cf89c549447706a4c6bdbd2d08dc76`
- executable JAR SHA-256: `a0dfffe0125894d9a9c7c4c93bc631551b46a6e48a2b5d8bed6a1f5494cc03e0`

Verification vocabulary:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN MATERIAL ACCEPTANCE`: NO

No human field play is required merely to validate this decode guard. Human review remains required only after a real final-material candidate is legally ingested, transformed for the Dragon Evolved UVs and rendered in Minecraft.

### Material intake boundary after this checkpoint

- Selected boss rig/geometry remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized runtime resource remains `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- The stripped/source Atlas is reference/provenance only and must not be promoted unchanged as final art.
- The older dark-rock file documented in `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md` remains rejected; successful HTTP/build packaging is not texture validation.
- A current Poly Haven `Dark Rock` asset page has been identified as a strong CC0 candidate direction (author Amal Kumar; diffuse/normal/roughness maps are offered), but **candidate-page/license confirmation is not asset admission**. Do not record it as shipped or approved until exact downloaded bytes, checksum, decode, transformation/UV provenance and rendered human review are complete.
- Any admitted final candidate must have redistributable provenance/license evidence, exact source URL/version or retrieval evidence, source checksum, transformation record, final checksum, real decode/format validation and Minecraft render review.
- `Region01BossMaterialPreparation` must continue to fail closed on missing, hash-mismatched or undecodable texture bytes. Do not weaken this gate to make an asset load.

## Latest connected gameplay baseline — commandless hub regression fix

Previous verified implementation: `5c5601a1a26c17b3305f38872ed684cf2da816fb`, workflow `34896538570` SUCCESS.

Fresh-world hub bootstrap and commandless station rejection paths are localized EN/KO and no longer expose raw internal exception text. No save/network schema, expedition transition, supply consumption, pressure, reward, encounter, combat or extraction contract changed. Ordinary localization/actionbar work is closed again unless a demonstrated regression appears.

## Settled direction — do not redo without demonstrated regression or new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Accepted move-start native swing checkpoint remains `22a39b59671ad88cfbf76b830234f222ea9520c2`, workflow `34878394712` SUCCESS. Do not add more generic weapon cues without human evidence.
- Connected expedition localization baseline remains `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`; commandless-hub gap is fixed by `5c5601a1...`. Do not reopen ordinary localization without a demonstrated regression.
- Earlier field-readability checkpoints remain valid: hit readability `24a15b2a...`, live status `54f070f1...`, extraction relay `eee2a34b...`, proxy role labels `e7b7421...`, salvage hazard cue `54d03af...`.
- Combat-space baseline remains `40fe05c08634b80f0fc87b3f0df2b1e5cdc02c82`, workflow `34885087681` SUCCESS. Do not grow more tuff decoration; current material/layout is a technical field-review baseline, not final environment art.
- Threat staging baseline remains `f49357355899d3efb0e21f0101c837c5e28d0353`, workflow `34890699183` SUCCESS. Hunters west, Scouts east, Elite back-center; do not auto-tune coordinates/cover without human field evidence.
- Zombie/Skeleton/Ravager remain behaviour proxies only. Their vanilla silhouettes, equipment and role names are not production creature art.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone boss preview, actionbar, relay dressing, proxy labels, player hit cues, salvage cues, native swing and current tuff combat-space treatment are field-review aids, not final Riftfrontier UI/art/VFX/audio/environment language.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, control layout, cover geometry or threat spawn staging without human field evidence.

## Region 01 boss presentation gates — still open

- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not fabricate `presentation_assets` or weaken exact-coverage/review gates.
- Evidence-backed source motion remains committed strike -> `Punch`, line displacement -> `Headbutt`, damage -> `HitReact`, death -> non-looping `Death`, unresolved -> `Flying_Idle`.
- Authored arena-pressure compression/burst/recovery candidate `bca0816381c09b28d613bc12d2e395eb62368761` is automated-build verified but **not human accepted** and does not close production animation coverage.
- Boss TELEGRAPH threat-shape rendering remains build verified (`fc5e841...`). Do not add more telegraph layers without evidence.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; the boss is still excluded from natural/production Region 01 encounter insertion.

## Next useful development boundary

Do not respond to the material decode fix by growing more intake framework. The proven corrupt-image hole is closed.

Unless new human field evidence arrives, prioritize one of these concrete completion paths:

1. legally obtain an exact, redistributable final-material source candidate (current Poly Haven Dark Rock direction is eligible for intake evaluation), preserve exact provenance/checksums, actually decode it, adapt it coherently to Dragon Evolved UVs without shipping the source Atlas unchanged, render it in Minecraft, and prepare a human material review build; or
2. strong-reference / legally usable production creature or Region 01 environment presentation that can replace proxy/technical presentation without bypassing boss material/animation gates; or
3. another connected visible combat/environment improvement that materially advances the vertical slice and is independent of still-open human gates.

Production creature silhouettes must not be invented from scratch by automation. Final VFX/sound/UI/environment language requires the same reference/provenance discipline and actual Minecraft human review. Remove temporary scaffolding only after a verified replacement exists.
