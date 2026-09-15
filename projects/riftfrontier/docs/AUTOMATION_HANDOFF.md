# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT + REGION 01 COMBAT-SPACE/STAGING + FIRST-SLICE FIELD GUIDE BUILD VERIFIED / BOSS DARK-ROCK FIELD-REVIEW BUILD VERIFIED / HUMAN FIELD PLAY + BOSS MATERIAL ACCEPTANCE + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest boss-material field-review checkpoint

Code checkpoint: `863717398a1ed2e952d5f1c76f00ea8fee1c6765` (`riftfrontier: render boss material candidate in field review`). `Build Riftfrontier` workflow `34955777908` completed **SUCCESS**.

The explicit development-only `/riftfrontier boss fieldtest spawn` presentation now renders the accepted Dragon Evolved geometry with the repository's provenance-tracked derived Dark Rock material candidate instead of Minecraft stone. This is intentionally a human field-review path only. Production rendering remains fail-closed behind the existing reviewed-material and semantic-animation coverage gates; seeing this candidate in the fieldtest does not approve or publish it.

Passed in workflow `34955777908`:

- toolchain verification
- asset-intake tests
- `clean test build`
- required native GameTest gate
- dedicated-server smoke
- Xvfb client initialization smoke
- executable-JAR inspection
- build report and artifact upload

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10391665418`
- archive digest: `sha256:644bf5b3bd37863ee3fcf8b8deb87cec68c9f49c8b5f99dad86ae61693e00992`
- executable JAR SHA-256: `1ba220ab318d7285299faf3f02d42cd5e77747867759648012ba0d33e7471a2a`

Verification vocabulary for this checkpoint:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN MATERIAL ACCEPTANCE`: NO

### Exact human boss-material review

Install the executable JAR from artifact `10391665418` into a Minecraft 26.2 / NeoForge 26.2.0.38-beta test instance. In a test world run `/riftfrontier boss fieldtest spawn`. Review the Dragon at near, combat and medium distances while idle, then exercise the existing `phase1` / `phase2` fieldtest controls and ordinary damage/death presentation.

Accept only if the actual Minecraft render has coherent Dark Rock scale and orientation across face/head, neck, torso, limbs and wing membranes; no severe UV seam, mirrored discontinuity, stretching, texture swimming or bright-vein concentration that destroys silhouette/readability; and the candidate remains readable during Flying_Idle, Punch, Headbutt, HitReact and Death. Record screenshots/observations before any production-material receipt is created. Reject or request a deterministic re-derivation if the UV result is visibly poor. Do not silently tune the material from automation without that evidence.

## Latest verified gameplay checkpoint — Region 01 first-slice field guide

Verified gameplay/client checkpoint: `041fb1731a5b27decb78660902525659d35ffaee` (`riftfrontier: isolate field guide test contract`), following the functional field-guide implementation `f6c7a2876ce8b5c7b048887af565c68337f875d2`.

Fresh-world commandless bootstrap gives the player one vanilla `WRITTEN_BOOK` field guide. This intentionally uses Minecraft's existing written-book UX instead of inventing a custom Riftfrontier Screen before final UI language is reviewed. The four EN/KO-localized pages only explain rules that already exist in authoritative gameplay: hub prepare -> lodestone deploy -> recover 3 salvage -> field relay extraction; technical threat roles and optional patrol-clear +1; salvage-triggered rift drag / 3-of-3 relay activation; and extraction storage/pressure plus smithing-table conversion of 1 stored salvage into 2 expedition supply.

`Region01FieldGuide` owns no progression state and cannot mutate supply, salvage, pressure, encounter ownership, extraction or combat. No persistent guide-received flag was added; the guide is supplied only by the existing one-time fresh-world bootstrap.

Verification: `Build Riftfrontier` workflow `34912099164` completed SUCCESS. Deliverable artifact id `10374836675`; executable JAR SHA-256 `fc8bc27465be67f47db7ee63ba3a9e423debc1169f6b64285c2f55917373c81a`. Human guide acceptance remains open.

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
- Diagnostic particles/sounds, technical blocks, bossbar, actionbar, relay dressing, proxy labels, native swing and tuff arena are field-review aids, not final Riftfrontier presentation language.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, control layout, cover geometry or threat staging without human field evidence.

## Region 01 boss presentation gates — still open

- Selected boss rig/geometry remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer.
- Accepted sanitized runtime resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`; SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Source Atlas art remains provenance/reference only and must not be shipped unchanged as final art.
- Provenance-tracked Poly Haven Dark Rock source and deterministic derived candidate are now available for field review. Do not restart material/model search merely because human acceptance is pending.
- Production boss rendering remains fail-closed until reviewed final material and complete reviewed animation semantic coverage exist. Do not fabricate `presentation_assets` or weaken coverage/review gates.
- Evidence-backed source motion: committed strike -> `Punch`, line displacement -> `Headbutt`, damage -> `HitReact`, death -> non-looping `Death`, unresolved -> `Flying_Idle`.
- Authored arena-pressure compression/burst/recovery candidate `bca0816381c09b28d613bc12d2e395eb62368761` is automated-build verified but **not human accepted**.
- Boss TELEGRAPH threat-shape rendering remains build verified (`fc5e841...`). Do not add telegraph layers without evidence.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; the boss is still excluded from natural/production Region 01 encounter insertion.

## Next useful development boundary

The Dark Rock Dragon material is now actually visible in the build-verified Minecraft field-review path. Do not keep changing that material automatically while human acceptance is pending. Parallel automation should prioritize production-visible work independent of this gate: strong-reference / legally usable production creature or Region 01 environment presentation that replaces proxy/technical presentation, or another connected visible combat/environment improvement that materially advances the vertical slice. Preserve provenance and use actual Minecraft review before declaring final art.

Production creature silhouettes must not be invented from scratch by automation. Final VFX/sound/UI/environment language requires the same reference/provenance discipline and actual Minecraft human review. Remove temporary scaffolding only after a verified replacement exists.