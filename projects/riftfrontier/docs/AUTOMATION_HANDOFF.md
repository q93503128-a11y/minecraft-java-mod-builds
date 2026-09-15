# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT + REGION 01 COMBAT-SPACE/STAGING + FIRST-SLICE FIELD GUIDE BUILD VERIFIED / BOSS MATERIAL DECODE GATE BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified gameplay checkpoint — Region 01 first-slice field guide

Verified gameplay/client checkpoint: `041fb1731a5b27decb78660902525659d35ffaee` (`riftfrontier: isolate field guide test contract`), following the functional field-guide implementation `f6c7a2876ce8b5c7b048887af565c68337f875d2`.

Fresh-world commandless bootstrap now gives the player one vanilla `WRITTEN_BOOK` field guide. This intentionally uses Minecraft's existing written-book UX instead of inventing a custom Riftfrontier Screen before final UI language is reviewed. The four EN/KO-localized pages only explain rules that already exist in authoritative gameplay:

1. hub prepare -> lodestone deploy -> recover 3 salvage -> field relay extraction;
2. Hunter / Scout / Elite Anchor technical roles and optional patrol-clear +1 extraction salvage;
3. salvage-triggered rift drag, 3/3 relay activation, patrol clear as optional bonus;
4. extraction storage/pressure and smithing-table conversion of 1 stored salvage into 2 expedition supply.

`Region01FieldGuide` owns no progression state and cannot mutate supply, salvage, pressure, encounter ownership, extraction or combat. `Region01FieldGuideSpec` is a pure-data contract so the normal JUnit source set can verify the compact/stable localization-key surface without importing Minecraft classes. The actual Minecraft item/component construction is compiled and then exercised through the normal full client/server build path.

No persistent "guide received" flag was added. The guide is supplied only by the already-authoritative one-time fresh-world bootstrap; if the inventory is full the item drops at the player instead of being silently lost. Existing worlds with expedition history are intentionally not retroactively mutated by this first-slice onboarding checkpoint.

Verification: `Build Riftfrontier` workflow `34912099164` completed **SUCCESS** on `041fb1731a5b27decb78660902525659d35ffaee`.

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
- artifact id: `10374836675`
- archive digest: `sha256:81f6771f6b23a6aa7ae8d01c585e0f1fc06a54ff1e4e736255f464f8ae8ed4ed`
- executable JAR SHA-256: `fc8bc27465be67f47db7ee63ba3a9e423debc1169f6b64285c2f55917373c81a`

Verification vocabulary:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN GUIDE ACCEPTANCE`: NO

### Human field review procedure

Use a fresh world with the verified JAR. The first commandless bootstrap into the technical hub must place exactly one `Region 01 Field Guide` in the player's inventory, or drop that one book at the player if the inventory is deliberately full. Open all four pages in English, then Korean. Reject if a raw `riftfrontier.guide...` key appears, if the book describes a rule different from actual `/riftfrontier expedition status`/play behavior, if station use or extraction duplicates the guide, or if merely possessing/opening the book changes supply, salvage, pressure, threats or extraction state.

Then run the normal loop once: lodestone deploy -> recover three salvage -> optionally clear patrol -> relay extraction -> smithing-table provision -> redeploy. The guide must remain explanatory only. Do not mark this checkpoint `PLAYTESTED` or `HUMAN GUIDE ACCEPTANCE` until a person actually performs that review.

This is the minimal first-slice guide, not final UI/art language and not a complete bestiary system. Do not respond by building speculative custom guide screens, persistence flags, quest journals or menu infrastructure without new evidence.

## Latest boss-material checkpoint — real-decode gate

Verified checkpoint: `a961099624fdbf44aa81cd5d224b976b6ca37902` (`riftfrontier: reject undecodable boss materials`), workflow `34902795221` SUCCESS.

`Region01BossMaterialPreparation` now authenticates exact resource bytes/SHA-256 and performs a real image decode before a material can become prepared. Missing, hash-mismatched, empty, malformed or undecodable texture bytes fail closed. This does not approve art or weaken human review.

Material direction remains settled:

- selected boss rig/geometry: Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer;
- accepted sanitized runtime resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`;
- accepted sanitized model SHA-256: `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`;
- source Atlas remains provenance/reference only and must not be shipped unchanged as final art;
- older broken dark-rock file remains rejected;
- Poly Haven `Dark Rock` remains an eligible CC0 material-source direction, but page/license confirmation is not asset admission. Exact downloaded bytes, checksum, decode, transformation/UV provenance, final checksum, Minecraft render and human review are still required.

Do not restart model search or add GeckoLib merely to duplicate the selected path. Do not grow more material-gate framework unless a demonstrated intake failure requires it.

## Connected Region 01 baseline — settled unless evidence regresses it

- Core slice: `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Native move-start swing checkpoint: `22a39b59671ad88cfbf76b830234f222ea9520c2`, workflow `34878394712` SUCCESS. Do not add generic weapon cues without human evidence.
- Expedition EN/KO localization baseline: `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`; commandless-hub regression fix: `5c5601a1a26c17b3305f38872ed684cf2da816fb`.
- Patrol-clear objective feedback fix: `f356732b1865ef3fa642262b2acc69cca278bde0`, workflow `34907014664` SUCCESS. Ordinary actionbar/localization work is closed again.
- Other field-review checkpoints remain valid: hit readability `24a15b2a...`, live status `54f070f1...`, extraction relay `eee2a34b...`, proxy role labels `e7b7421...`, salvage hazard cue `54d03af...`.
- Combat-space baseline: `40fe05c08634b80f0fc87b3f0df2b1e5cdc02c82`, workflow `34885087681` SUCCESS. Current tuff treatment is technical only; do not grow more tuff decoration.
- Threat staging baseline: `f49357355899d3efb0e21f0101c837c5e28d0353`, workflow `34890699183` SUCCESS. Hunters west, Scouts east, Elite back-center. Do not auto-tune coordinates/cover without human field evidence.
- Zombie/Skeleton/Ravager are behaviour proxies only; vanilla silhouettes/equipment/role labels are not production creature art.
- Diagnostic particles/sounds, technical blocks, bossbar, stone boss preview, actionbar, relay dressing, proxy labels, native swing and tuff arena are field-review aids, not final Riftfrontier presentation language.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, control layout, cover geometry or threat staging without human field evidence.

## Region 01 boss presentation gates — still open

- Production boss rendering remains fail-closed until reviewed final material and complete reviewed animation semantic coverage exist. Do not fabricate `presentation_assets` or weaken coverage/review gates.
- Evidence-backed source motion: committed strike -> `Punch`, line displacement -> `Headbutt`, damage -> `HitReact`, death -> non-looping `Death`, unresolved -> `Flying_Idle`.
- Authored arena-pressure compression/burst/recovery candidate `bca0816381c09b28d613bc12d2e395eb62368761` is automated-build verified but **not human accepted**.
- Boss TELEGRAPH threat-shape rendering remains build verified (`fc5e841...`). Do not add telegraph layers without evidence.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; the boss is still excluded from natural/production Region 01 encounter insertion.

## Next useful development boundary

The first-slice guide is now present and build verified. Do not keep expanding onboarding/UI automatically. Unless new human field evidence arrives, prioritize production-visible completion work:

1. legally obtain an exact redistributable final-material source candidate in the already-selected direction, preserve provenance/checksums, decode it, adapt it coherently to Dragon Evolved UVs, render it in Minecraft, and prepare a human material-review build; or
2. strong-reference / legally usable production creature or Region 01 environment presentation that can replace proxy/technical presentation without bypassing boss material/animation gates; or
3. another connected visible combat/environment improvement that materially advances the vertical slice and is independent of still-open human gates.

Production creature silhouettes must not be invented from scratch by automation. Final VFX/sound/UI/environment language requires the same reference/provenance discipline and actual Minecraft human review. Remove temporary scaffolding only after a verified replacement exists.
