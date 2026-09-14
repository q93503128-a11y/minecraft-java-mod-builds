# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 FIELD READABILITY BUILD VERIFIED / BOSS THREAT-SHAPE TELEGRAPH BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

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

## Connected expedition loop

Fresh-world login can bootstrap the technical hub when there is no active run and no expedition history. Hub lodestone delegates to existing authoritative deployment; smithing table delegates to existing provisioning. Existing expedition history prevents bootstrap reset. `/riftfrontier expedition start` remains diagnostic/fallback only.

Key verified checkpoints:

- fresh bootstrap/login: `5f9d6b6bb889a5eab84135437df4150ec80c6494`, `19a54bdf9ac5c620099688cbdeac4e1722d3b21d`, workflow `34808226019` SUCCESS;
- actionbar API correction: `fded37cbe91668af213af477ae0b0238ac455693`, workflow `34821369460` successful rerun;
- player hit readability: `24a15b2a0c8db3ade4c6ae730da1373c93e59e00`, workflow `34825929915` SUCCESS;
- live field-status readability: `54f070f1937e861f4aabc896b8803fa81aa1aae1`, workflow `34849271056` SUCCESS;
- extraction-relay readability: `eee2a34b28ecaddcb0597ac8f42ddda80a318700`, workflow `34856309722` SUCCESS;
- proxy-role readability: `e7b7421ae809711d394189595bbc90c47bc4dd2b`, workflow `34860050735` SUCCESS.

Do not keep elaborating temporary hub/relay palette, actionbar state or proxy labels without new human field evidence.

## Latest Region 01 salvage-hazard readability checkpoint

Implementation batch ending at `54d03af79ec33471d3263f79d3bd2ba60529727b` makes the already-existing salvage `rift drag` penalty perceptible at the moment a salvage node is successfully recovered. It does **not** alter the penalty, reward, pressure or extraction contract.

Implementation:

- `Region01EncounterRuntime.applySalvageHazard(...)` still derives duration/amplifier from the same authoritative `EncounterPlan` and still applies the same Slowness effect;
- after that authoritative effect succeeds, the server emits a small Minecraft-native `SCULK_SOUL` burst around the player and a restrained `SCULK_SHRIEKER_SHRIEK` cue;
- the previous hard-coded English diagnostic line was replaced with localized `riftfrontier.expedition.feedback.rift_drag` feedback in `en_us` and `ko_kr`, carrying the same authoritative duration and intensity values;
- no new saved state, network payload, damage, knockback, teleport, hit clock, reward path, threat-count rule or extraction rule was added;
- the reference rationale is Minecraft's established Deep Dark/sculk warning language, especially Mojang's sculk-shrieker warning/alarm presentation (`https://www.minecraft.net/en-us/article/sculk-shrieker`). No external asset/code was copied or bundled;
- this is field-review presentation only. It is **not** final Riftfrontier VFX/audio language and must not become a reason to expand sculk theming across Region 01 without a separate art-direction decision.

Verification: `Build Riftfrontier` workflow `34866121039` completed SUCCESS on `54d03af79ec33471d3263f79d3bd2ba60529727b`. Toolchain verification, asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10357890112`
- archive digest: `sha256:249c96795080ad428461a6bf40009ec29a19222c356bcdfff42da0f1a9c581fa`
- executable JAR SHA-256: `9d0e57219340f3dba5d284da123e533b09a9384889e2f3a30c3350f3bf436e3b`

Verification vocabulary for `54d03af79ec33471d3263f79d3bd2ba60529727b`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN AUDIO/VISUAL ACCEPTANCE`: NO

Human field procedure:

1. Use a disposable/fresh world and deploy through the normal hub lodestone path into Region 01.
2. Recover an actual amethyst salvage node through normal interaction. On each successful recovery, verify that the existing Slowness penalty occurs and the brief sculk-soul + restrained shrieker cue occurs at the same moment.
3. Verify the system feedback reports the same duration/intensity as the applied effect in both `en_us` and `ko_kr`.
4. Click invalid/non-salvage blocks and confirm no rift-drag cue appears. A cue without a successful salvage recovery is a failure.
5. Confirm the cue itself causes no damage, knockback, teleport, threat-count change, salvage duplication/loss, pressure change or extraction-state mutation.
6. At a higher persisted Region 01 pressure, confirm only the already-defined Slowness duration/intensity scaling changes; the audiovisual cue must not create a second gameplay scaling system.
7. Reject the presentation if the shriek is too loud/long, the particle burst materially obscures combat, or the sculk language reads as an unintended final biome identity. Do not auto-tune it from automation alone; record actual field evidence first.

## Region 01 boss field harness

Development-only commands:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production Region 01 encounter composition.

Current server-authoritative field behavior remains: production semantic profile `riftfrontier:boss/region_01_first_apex`; phase 1 committed strike + line displacement; phase 2 arena pressure; authoritative `TELEGRAPH -> ACTIVE -> RECOVERY`; diagnostic `1.0F` ACTIVE-only damage with per-execution dedupe; attack-start facing commitment; provisional role-specific hit geometry; ACTIVE-only line travel; one-shot arena-pressure radial impulse; native diagnostic explosion cue; health-backed bossbar.

### Dragon Evolved presentation

The temporary field-review renderer exposes the accepted sanitized Dragon geometry with Minecraft stone only while production presentation cannot publish. Production rendering has priority; duplicate preview + production meshes are a failure.

Evidence-backed source motion currently used for field review:

- committed strike -> reviewed `Punch` windows;
- line displacement -> reviewed `Headbutt` windows;
- non-attacking damage -> reviewed `HitReact`;
- terminal death -> reviewed non-looping `Death`;
- unresolved cases -> reviewed neutral `Flying_Idle` fallback.

Presentation priority remains `Death > reviewed authoritative attack sample > HitReact > Flying_Idle`.

The authored arena-pressure compression/burst/recovery candidate is automated-build verified but **not human accepted**. It does not close production animation coverage. Checkpoint `bca0816381c09b28d613bc12d2e395eb62368761`, workflow `34812925676` SUCCESS. Human review is required before promotion.

A previous dark-rock material candidate contained a corrupt PNG and is rejected. Keep the verified stone fallback. Future material candidates must be physical redistributable resources with provenance/license evidence and successful decode/format validation before renderer hookup. Canonical rejection record: `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`.

### Threat-shape telegraph

The boss TELEGRAPH outline remains build verified. `Region01BossFieldTelegraphGeometry` derives sparse boundary samples from existing `Region01BossFieldImpactProfile`; `Region01BossFieldTelegraphEmitter` displays them with Minecraft-native particles only during TELEGRAPH and owns no gameplay timing/hit authority. Corrected implementation SHA `fc5e8411714d2dcf87df56141cf92156cb7de809`, workflow `34841785748` SUCCESS. Human procedure remains in `docs/M3_REGION01_BOSS_THREAT_TELEGRAPH_REVIEW.md`.

## Exact next development boundary

Do not spend the next run adding more backend/telegraph fences, more actionbar state, more technical proxy labels, more hub/relay decoration, or more salvage warning layers simply because automation is stable.

1. If actual human field evidence is supplied, use it to accept/reject/calibrate proxy-role labels, live field status, extraction relay, salvage-hazard cue, boss threat outline and/or arena-pressure motion. Do not invent observations.
2. Otherwise prioritize independent **production-visible** Region 01 work: a legally usable, decodable, provenance-recorded final Dragon material/texture direction based on strong references, or other combat/environment presentation that does not bypass the material/animation gates.
3. Production creature silhouettes for hunter/scout/elite are a valid visible-quality target only after establishing a reference/asset direction. Do not improvise them from scratch merely to replace the vanilla proxies.
4. Once arena-pressure motion is human accepted and all nine logical animation keys have evidence, assemble complete production animation binding without weakening exact coverage.
5. Select/author final VFX and sound under the same reference/provenance/readability rules. Presentation may emphasize authoritative windows but must never create a second hit clock.
6. Author the first real `presentation_assets` manifest only after referenced physical resources exist and are reviewed.
7. Publish through the existing reload/render pipeline and verify Minecraft scale, hit-volume readability and resource reload.
8. Remove stone-material/field-preview/proxy-label/native-cue scaffolding only after verified production presentation supersedes it.
9. Only after presentation plus human field evidence clear the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Never claim `PLAYTESTED` or `MULTIPLAYER TESTED` without actual human sessions.
