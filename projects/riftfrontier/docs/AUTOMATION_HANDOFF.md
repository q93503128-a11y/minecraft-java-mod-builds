# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT + REGION 01 COMBAT-SPACE/STAGING BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified gameplay checkpoint — commandless hub feedback regression fix

Implementation checkpoint: `5c5601a1a26c17b3305f38872ed684cf2da816fb` (`riftfrontier: localize commandless hub station feedback`).

A demonstrated player-facing regression remained after the earlier expedition localization checkpoint: the fresh-world hub bootstrap message and both commandless hub station rejection paths still exposed hardcoded English, and the rejection paths appended raw internal `IllegalStateException` text. This batch closes only that proven gap; it does not reopen ordinary localization/UI work.

- `ExpeditionHubTerminal.bootstrapFreshWorld(...)` now uses a translatable EN/KO bootstrap message.
- Provision-station rejection now reports authoritative stored salvage, current supply and next deployment cost through a localized message instead of exposing the internal exception string.
- Deployment rejection now reports authoritative current supply and required cost through a localized message instead of exposing the internal exception string or command-oriented implementation guidance.
- Success paths remain the already-localized expedition feedback paths.
- No save/network schema, expedition state transition, supply consumption, pressure, reward, encounter, combat or extraction contract changed.
- No new UI/art language or external asset was introduced; the existing technical lodestone/smithing-table affordances remain validation scaffolding.

Verification: `Build Riftfrontier` workflow `34896538570` completed **SUCCESS** on `5c5601a1a26c17b3305f38872ed684cf2da816fb`.

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
- artifact id: `10368609320`
- archive digest: `sha256:6fc93f3311d33da7e3670274f9fe2abad757ca7fe32ccd11ac2887d2aece8267`
- executable JAR SHA-256: `232841bcf1aad00d02ccf4a2fd8781a086cffc99a35046fd1d63d9654e259490`

Verification vocabulary:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN LOCALIZATION ACCEPTANCE`: NO

### Exact human field procedure

1. Put the verified JAR in a disposable Minecraft 26.2 / NeoForge 26.2.0.38-beta instance.
2. Enter a fresh/disposable world. On the first commandless bootstrap into the technical hub, confirm the bootstrap instruction is localized for the selected client language and no raw translation key appears.
3. Before the world has enough valid resources/state for provisioning, right-click the smithing table. The rejection must be localized and show stored salvage, current supply and next deployment cost; it must not expose a Java/internal exception string.
4. When deployment is invalid or supply is insufficient, right-click the lodestone. The rejection must be localized and show current supply and required cost; it must not expose the old internal English exception or `/riftfrontier expedition provision` implementation hint.
5. Complete the normal commandless `deploy -> recover 3 salvage -> extract -> provision -> redeploy` path and confirm the existing success messages and authoritative state changes still behave normally.
6. Repeat the bootstrap/rejection observations with English and Korean client language if practical. Reject if `riftfrontier.expedition.detail.*` raw keys appear or values disagree with `/riftfrontier expedition status` diagnostics.
7. Do not mark `PLAYTESTED` or `MULTIPLAYER TESTED` until a human actually performs the relevant run(s).

Ordinary localization/actionbar/live-status work is closed again after this demonstrated regression fix. Do not keep polishing copy without new player evidence.

## Settled direction — do not redo without demonstrated regression or new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Accepted move-start native swing checkpoint remains `22a39b59671ad88cfbf76b830234f222ea9520c2`, workflow `34878394712` SUCCESS. Do not add more generic weapon cues without human evidence.
- Connected expedition localization baseline remains `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`; the commandless-hub gap is now fixed by `5c5601a1...`. Do not reopen ordinary localization without a demonstrated regression.
- Earlier field-readability checkpoints remain valid: hit readability `24a15b2a...`, live status `54f070f1...`, extraction relay `eee2a34b...`, proxy role labels `e7b7421...`, salvage hazard cue `54d03af...`.
- Combat-space baseline remains `40fe05c08634b80f0fc87b3f0df2b1e5cdc02c82`, workflow `34885087681` SUCCESS. Do not grow more tuff decoration; current material/layout is a technical field-review baseline, not final environment art.
- Threat staging baseline remains `f49357355899d3efb0e21f0101c837c5e28d0353`, workflow `34890699183` SUCCESS. Hunters west, Scouts east, Elite back-center; do not auto-tune coordinates/cover without human field evidence.
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

Do **not** respond to this small proven regression fix by continuing message-copy cleanup, growing hub/relay scaffolding, adding generic combat cues, or creating another authority/lifecycle fence.

Unless new human field evidence arrives, prioritize:

1. a legally redistributable, actually decodable, provenance-recorded final Dragon Evolved material/texture direction grounded in strong reference work; or
2. strong-reference / legally usable production creature or Region 01 environment presentation that can replace proxy/technical presentation without bypassing boss material/animation gates; or
3. another connected visible combat/environment improvement that materially advances the vertical slice and is independent of the still-open human gates.

Production creature silhouettes must not be invented from scratch by automation. Final VFX/sound/UI/environment language requires the same reference/provenance discipline and actual Minecraft human review. Remove temporary scaffolding only after a verified replacement exists.
