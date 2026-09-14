# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 FIELD READABILITY BUILD VERIFIED / CONNECTED-LOOP LOCALIZATION BUILD VERIFIED / BOSS THREAT-SHAPE TELEGRAPH BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Settled direction — do not redo without new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless technical hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled. Do not reopen without a demonstrated regression.
- Player combat already has two server-authoritative weapon families (`mobile_pressure`, `reach_commitment`), one `recovery_pivot` technique path, authenticated move-id input, ACTIVE-only hit authority and per-execution target dedupe.
- Region 01 technical Zombie/Skeleton/Ravager actors remain **behaviour proxies**, not production creature art. Their vanilla silhouettes, equipment and role names are field-review scaffolding only.
- Region 01 boss geometry/rig remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Provenance/license registry remains `docs/THIRD_PARTY_ASSETS.md`. The source Dragon Evolved Atlas is provenance/reference only and is **not** approved final Region 01 art.
- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not weaken exact-coverage/review gates or fabricate `presentation_assets`.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone-textured boss preview, actionbar, extraction-relay dressing/readiness markers, proxy role names, player hit cues and salvage-hazard cues are temporary readability/field-review aids, not final Riftfrontier UI/art/VFX/audio language.
- Do not auto-tune provisional damage, hit geometry, charge travel, radial impulse, particle density, salvage-hazard cue intensity or key layout without human field evidence.

## Connected expedition loop — current verified checkpoint

Fresh-world login can bootstrap the technical hub when there is no active run and no expedition history. Hub lodestone delegates to authoritative deployment; smithing table delegates to authoritative provisioning. Existing expedition history prevents bootstrap reset. `/riftfrontier expedition start` remains diagnostic/fallback only.

Latest implementation checkpoint: `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9` (`riftfrontier: localize expedition loop feedback`).

This batch removes hard-coded English from the normal `ExpeditionGameplayService` player-facing detail path without changing gameplay authority:

- deployment, salvage recovery, extraction and provisioning detail messages now use `Component.translatable(...)`;
- matching `en_us` and `ko_kr` resources are present for those detail messages;
- expedition failure and post-failure re-entry now localize the existing `ExpeditionRun.EndReason` values instead of exposing English-only reason text;
- `/riftfrontier expedition status` remains a developer/diagnostic string intentionally and was not turned into player UI;
- no save/network schema, expedition lifecycle transition, supply/storage/pressure mutation, encounter ownership, reward math, extraction gate, hit authority or combat timing changed;
- temporary hub/relay supplemental diagnostics were deliberately not expanded in this batch. Do not turn localization into another round of technical fixture/UI churn without actual field evidence.

Verification: `Build Riftfrontier` workflow `34873147707` completed SUCCESS on `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`. Toolchain verification, asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10360076506`
- archive digest: `sha256:013f9cd9fdd7f0cb67f90da0a331fe0a32fe47af2708bf1a89e5ee7f88bc52ee`
- executable JAR SHA-256: `315e64da295f326fc19a0cc881c9b3dfd2b7fa283a3dde493668214aa1b27208`

Verification vocabulary for `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN LOCALIZATION ACCEPTANCE`: NO

Human localization field procedure:

1. Use a disposable/fresh world with language set to English (US), log in and enter the commandless technical hub.
2. Deploy through the hub lodestone. Confirm the detailed deployment line and actionbar are English, no raw translation key is visible, and pressure/threat numbers match the actual run.
3. Recover a real amethyst salvage node. Confirm the detailed salvage line and actionbar agree on recovered count and live patrol threats.
4. Reach 3/3 and use the extraction relay. Confirm extraction detail reports retained/base/patrol-bonus/stored salvage, pressure and next supply cost consistently with `/riftfrontier expedition status`.
5. Provision through the smithing table and redeploy; confirm the detailed provision values are current and the normal loop still closes.
6. On a disposable active run, disconnect and reconnect to exercise logout failure/re-entry. Confirm the reason is localized and no active-run state is resurrected.
7. Switch client language to Korean and repeat at minimum deploy -> salvage -> extract -> provision. Confirm Korean glyphs render, placeholders resolve, values remain identical to authoritative state, and no untranslated key appears.
8. Reject the localization if any message appears before its mutation succeeds, any numeric value drifts from authoritative state, a raw key appears, or changing client language changes gameplay state.

Earlier connected-loop checkpoints remain valid and should not be reimplemented merely to create activity: fresh bootstrap/login (`5f9d6b6...`, `19a54bdf...`, workflow `34808226019`), actionbar API correction (`fded37cb...`, workflow `34821369460`), player hit readability (`24a15b2a...`, workflow `34825929915`), live field status (`54f070f1...`, workflow `34849271056`), extraction relay (`eee2a34b...`, workflow `34856309722`), proxy-role readability (`e7b7421a...`, workflow `34860050735`) and salvage-hazard readability (`54d03af7...`, workflow `34866121039`).

## Region 01 salvage hazard

The already-existing salvage `rift drag` penalty remains unchanged: authoritative pressure-derived Slowness first, then a small Minecraft-native `SCULK_SOUL` burst plus restrained `SCULK_SHRIEKER_SHRIEK` cue only after successful salvage recovery. It is field-review presentation, not final Riftfrontier VFX/audio or Region 01 biome identity. Do not add more warning layers or auto-tune cue intensity without human evidence. Detailed human procedure remains recover valid/invalid blocks, compare duration/intensity to state, verify no damage/knockback/reward/threat/extraction side effect, and reject if the cue obscures combat or overstates sculk theming.

## Region 01 boss field harness

Development-only commands:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production Region 01 encounter composition.

Current authoritative field behavior remains: production semantic profile `riftfrontier:boss/region_01_first_apex`; phase 1 committed strike + line displacement; phase 2 arena pressure; authoritative `TELEGRAPH -> ACTIVE -> RECOVERY`; diagnostic `1.0F` ACTIVE-only damage with per-execution dedupe; attack-start facing commitment; provisional role-specific hit geometry; ACTIVE-only line travel; one-shot arena-pressure radial impulse; native diagnostic explosion cue; health-backed bossbar.

### Dragon Evolved presentation gates

The temporary field-review renderer exposes accepted sanitized Dragon geometry with Minecraft stone only while production presentation cannot publish. Evidence-backed field motion remains committed strike -> reviewed `Punch`, line displacement -> reviewed `Headbutt`, damage -> reviewed `HitReact`, death -> reviewed non-looping `Death`, unresolved -> reviewed `Flying_Idle`; priority remains `Death > reviewed authoritative attack sample > HitReact > Flying_Idle`.

The authored arena-pressure compression/burst/recovery candidate is automated-build verified but **not human accepted**. It does not close production animation coverage. Checkpoint `bca0816381c09b28d613bc12d2e395eb62368761`, workflow `34812925676` SUCCESS. Human review is required before promotion.

A previous dark-rock material candidate contained a corrupt PNG and is rejected. Keep the verified stone fallback. Future material candidates must be physical redistributable resources with provenance/license evidence and successful decode/format validation before renderer hookup. Canonical rejection record: `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`.

The boss TELEGRAPH outline remains build verified. `Region01BossFieldTelegraphGeometry` derives sparse boundary samples from existing `Region01BossFieldImpactProfile`; `Region01BossFieldTelegraphEmitter` displays them only during TELEGRAPH and owns no gameplay timing/hit authority. Corrected implementation SHA `fc5e8411714d2dcf87df56141cf92156cb7de809`, workflow `34841785748` SUCCESS. Human procedure remains in `docs/M3_REGION01_BOSS_THREAT_TELEGRAPH_REVIEW.md`.

## Exact next development boundary

Do not spend the next run adding more backend/telegraph fences, more actionbar state, more technical proxy labels, more hub/relay decoration, more salvage warning layers, or more localization churn simply because automation is stable.

1. If actual human field evidence is supplied, use it to accept/reject/calibrate localization, proxy-role labels, live field status, extraction relay, salvage-hazard cue, boss threat outline and/or arena-pressure motion. Do not invent observations.
2. Otherwise prioritize independent **production-visible** Region 01 work: a legally usable, decodable, provenance-recorded final Dragon material/texture direction based on strong references, or other combat/environment presentation that does not bypass the material/animation gates.
3. Production creature silhouettes for hunter/scout/elite are a valid visible-quality target only after establishing a reference/asset direction. Do not improvise them from scratch merely to replace vanilla proxies.
4. Once arena-pressure motion is human accepted and all nine logical animation keys have evidence, assemble complete production animation binding without weakening exact coverage.
5. Select/author final VFX and sound under the same reference/provenance/readability rules. Presentation may emphasize authoritative windows but must never create a second hit clock.
6. Author the first real `presentation_assets` manifest only after referenced physical resources exist and are reviewed.
7. Publish through the existing reload/render pipeline and verify Minecraft scale, hit-volume readability and resource reload.
8. Remove stone-material/field-preview/proxy-label/native-cue scaffolding only after verified production presentation supersedes it.
9. Only after presentation plus human field evidence clear the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Never claim `PLAYTESTED` or `MULTIPLAYER TESTED` without actual human sessions.
