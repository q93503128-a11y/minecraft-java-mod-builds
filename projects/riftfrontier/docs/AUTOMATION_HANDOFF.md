# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 FIELD READABILITY BUILD VERIFIED / BOSS THREAT-SHAPE TELEGRAPH BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Settled direction — do not redo without new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless technical hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled. Do not reopen without a demonstrated regression.
- Player combat already has two server-authoritative weapon families (`mobile_pressure`, `reach_commitment`), one `recovery_pivot` technique path, authenticated move-id input, ACTIVE-only hit authority and per-execution target dedupe.
- Region 01 technical Zombie/Skeleton/Ravager actors remain **behaviour proxies**, not production creature art. Do not mistake temporary names, vanilla silhouettes or equipment for final design.
- Region 01 boss geometry/rig remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Provenance/license registry remains `docs/THIRD_PARTY_ASSETS.md`. The source Dragon Evolved Atlas is provenance/reference only and is **not** approved final Region 01 art.
- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not weaken exact-coverage/review gates or fabricate `presentation_assets`.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone-textured boss preview, actionbar, extraction-relay dressing/readiness markers, proxy role names and current hit cues are temporary readability/field-review aids, not final Riftfrontier UI/art/VFX language.
- Do not auto-tune provisional damage, hit geometry, charge travel, radial impulse, particle density or key layout without human field evidence.

## Connected expedition loop

Fresh-world login can bootstrap the technical hub when there is no active run and no expedition history. Hub lodestone delegates to existing authoritative deployment; smithing table delegates to existing provisioning. Existing expedition history prevents bootstrap reset. `/riftfrontier expedition start` remains diagnostic/fallback only.

Key verified checkpoints:

- fresh bootstrap/login: `5f9d6b6bb889a5eab84135437df4150ec80c6494`, `19a54bdf9ac5c620099688cbdeac4e1722d3b21d`, workflow `34808226019` SUCCESS;
- actionbar API correction: `fded37cbe91668af213af477ae0b0238ac455693`, workflow `34821369460` successful rerun;
- player hit readability: `24a15b2a0c8db3ade4c6ae730da1373c93e59e00`, workflow `34825929915` SUCCESS;
- live field-status readability: `54f070f1937e861f4aabc896b8803fa81aa1aae1`, workflow `34849271056` SUCCESS, deliverable digest `sha256:19b8defdaf00f3f9b2b6c1a2fc40f18cc35ca20ce76ff67abc4897abac41d65c`;
- extraction-relay readability: `eee2a34b28ecaddcb0597ac8f42ddda80a318700`, workflow `34856309722` SUCCESS, deliverable digest `sha256:035d1116a9e9f718888b73868466ba49a3ff9cc9d3099ec61b429989bdfe9fc2`.

Do not keep elaborating the temporary hub/relay palette without new human field evidence.

## Latest Region 01 proxy-role readability checkpoint

Implementation batch ending at `e7b7421ae809711d394189595bbc90c47bc4dd2b` improves the current technical patrol's combat readability without changing its gameplay authority, stats, AI, pressure scaling, reward logic, ownership or persistence.

Implementation:

- existing `hunter`, `scout` and `elite_anchor` role tags remain the authoritative technical role identity;
- `Region01EncounterRuntime.spawn(...)` now assigns a localized, always-visible custom name to each technical proxy using that already-selected role;
- English field-review labels are `Hunter`, `Scout`, `Elite Anchor`; Korean labels are `헌터`, `스카우트`, `엘리트 앵커`;
- labels are deliberately presentation-only and do not create client-owned state, a new network payload, targeting rules, hit authority, reward state or persistence state;
- existing Zombie pursuit, Skeleton bow pressure, Ravager elite/shield-stun counterplay, run-sequence ownership and lure-resistant patrol-clear behavior are unchanged;
- required `region_01_encounter_runtime` GameTest now verifies that every spawned technical threat exposes a visible role name and that each stable role tag maps to the expected translatable field-review label before running the existing lure/cleanup assertions.

This is **not final creature/UI design**. The labels exist because the current actors are intentionally vanilla technical proxies whose production silhouettes are unresolved. Once production creature presentation communicates role reliably, remove the labels only after that replacement is field-verified.

Verification: `Build Riftfrontier` workflow `34860050735` completed SUCCESS on `e7b7421ae809711d394189595bbc90c47bc4dd2b`. Toolchain verification, asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10354468538`
- archive digest: `sha256:32f57f58623f5c180ee1f5e58459125bb0a8ccdce3393b3dc198775cc67c14eb`

Verification vocabulary for `e7b7421ae809711d394189595bbc90c47bc4dd2b`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN VISUAL ACCEPTANCE`: NO

Human field procedure:

1. Use a disposable/fresh world and enter Region 01 through the normal hub lodestone path; do not use a synthetic mob summon as the primary check.
2. Verify the active patrol exposes three immediately distinguishable labels matching role: Hunter / Scout / Elite Anchor (or the Korean localized equivalents).
3. Fight normally and verify the labels stay attached while the hunter closes distance, scout uses bow pressure and elite performs its existing behaviour. A label must not change aggression, damage, hit registration, threat counting or the patrol-clear bonus.
4. Lure one living patrol actor away from the technical cell. Its role label may travel with it, but the existing run-owned threat count must still treat it as alive; location must not clear the patrol.
5. Extract or abort, then redeploy. No stale/duplicate proxy or stale role label may remain from the previous run.
6. At higher pressure/density, reject the presentation if stacked labels materially obscure attacks, targets or salvage interaction. Do not auto-adjust label strategy from automation alone; capture actual field evidence first.
7. Treat successful readability as evidence for the temporary proxy harness only, not approval of final Region 01 creature silhouettes/UI.

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

The boss TELEGRAPH outline remains build verified. `Region01BossFieldTelegraphGeometry` derives sparse boundary samples from existing `Region01BossFieldImpactProfile`; `Region01BossFieldTelegraphEmitter` displays them with Minecraft-native particles only during TELEGRAPH and owns no gameplay timing/hit authority. Corrected implementation SHA `fc5e8411714d2dcf87df56141cf92156cb7de809`, workflow `34841785748` SUCCESS, deliverable digest `sha256:261c1f4f6d24f3e671ace52fc57f1ec6694b511db727773e873fe33765654e97`. Human procedure: `docs/M3_REGION01_BOSS_THREAT_TELEGRAPH_REVIEW.md`.

## Exact next development boundary

Do not spend the next run adding more backend/telegraph fences, more actionbar state, more technical proxy labels or more hub/relay decoration just because automation is stable.

1. If actual human field evidence is supplied, use it to accept/reject/calibrate proxy-role label readability, live field status, extraction relay, boss threat outline and/or arena-pressure motion. Do not invent observations.
2. Otherwise prioritize independent **production-visible** Region 01 work: a legally usable, decodable, provenance-recorded final Dragon material/texture direction based on strong references, or other combat/environment presentation that does not bypass the material/animation gates.
3. Production creature silhouettes for hunter/scout/elite are also a valid future visible-quality target, but do not improvise them. Establish a reference/asset direction first; use legal assets where appropriate and preserve provenance.
4. Once arena-pressure motion is human accepted and all nine logical animation keys have evidence, assemble complete production animation binding without weakening exact coverage.
5. Select/author final VFX and sound under the same reference/provenance/readability rules. Presentation may emphasize authoritative windows but must never create a second hit clock.
6. Author the first real `presentation_assets` manifest only after referenced physical resources exist and are reviewed.
7. Publish through the existing reload/render pipeline and verify Minecraft scale, hit-volume readability and resource reload.
8. Remove stone-material/field-preview/proxy-label scaffolding only after verified production presentation supersedes it.
9. Only after presentation plus human field evidence clear the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Never claim `PLAYTESTED` or `MULTIPLAYER TESTED` without actual human sessions.
