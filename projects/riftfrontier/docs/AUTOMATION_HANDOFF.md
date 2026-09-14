# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 LIVE FIELD-STATUS + EXTRACTION-RELAY READABILITY BUILD VERIFIED / REGION 01 BOSS THREAT-SHAPE TELEGRAPH BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Settled direction — do not redo without new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless technical hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled. Do not reopen without a demonstrated regression.
- Player combat already has two server-authoritative weapon families (`mobile_pressure`, `reach_commitment`), one `recovery_pivot` technique path, authenticated move-id input, ACTIVE-only hit authority and per-execution target dedupe.
- Region 01 boss geometry/rig remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Provenance/license registry remains `docs/THIRD_PARTY_ASSETS.md`. The source Dragon Evolved Atlas is provenance/reference only and is **not** approved final Region 01 art.
- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not weaken exact-coverage/review gates or fabricate `presentation_assets`.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone-textured boss preview, actionbar, extraction-relay dressing/readiness markers and current hit cues are temporary readability/field-review aids, not final Riftfrontier UI/art/VFX language.
- Do not auto-tune provisional damage, hit geometry, charge travel, radial impulse, particle density or key layout without human field evidence.

## Connected expedition loop

Fresh-world login can bootstrap the technical hub when there is no active run and no expedition history. Hub lodestone delegates to existing authoritative deployment; smithing table delegates to existing provisioning. Existing expedition history prevents bootstrap reset. `/riftfrontier expedition start` remains diagnostic/fallback only.

Key prior verified checkpoints:

- fresh bootstrap/login: `5f9d6b6bb889a5eab84135437df4150ec80c6494`, `19a54bdf9ac5c620099688cbdeac4e1722d3b21d`, workflow `34808226019` SUCCESS;
- actionbar API correction: `fded37cbe91668af213af477ae0b0238ac455693`, workflow `34821369460` successful rerun;
- player hit readability: `24a15b2a0c8db3ade4c6ae730da1373c93e59e00`, workflow `34825929915` SUCCESS.

### Latest field-status readability checkpoint

Commit `54f070f1937e861f4aabc896b8803fa81aa1aae1` keeps the existing Minecraft-native field objective readable during combat without adding a custom HUD or new gameplay state.

Implementation:

- `ExpeditionGameplayEvents.playerTick(...)` refreshes once per 20 server game ticks;
- `ExpeditionPlayerFeedback.fieldStatus(...)` reads the existing active authoritative Region 01 run and `Region01EncounterRuntime`'s already-owned live threat handles;
- the actionbar reuses the existing localized salvage/threat string and therefore updates after patrol deaths without waiting for another salvage interaction;
- `salvageUpdated(...)` delegates to the same projection so interaction and periodic refresh cannot drift into separate display logic;
- no new SavedData, client-owned state, balance values, world scan, reward path, extraction eligibility or combat authority was introduced.

Verification: `Build Riftfrontier` workflow `34849271056` completed SUCCESS on the implementation SHA. Asset intake, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10349801150`
- digest: `sha256:19b8defdaf00f3f9b2b6c1a2fc40f18cc35ca20ce76ff67abc4897abac41d65c`

Verification vocabulary for `54f070f1937e861f4aabc896b8803fa81aa1aae1`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Human procedure: `docs/M2B_REGION01_LIVE_FIELD_STATUS_REVIEW.md`.

### Latest extraction-relay world-space readability checkpoint

Commit `eee2a34b28ecaddcb0597ac8f42ddda80a318700` improves the existing technical field relay without changing expedition authority or inventing final Riftfrontier art language.

Implementation:

- the relay remains the existing far-edge lodestone and still delegates extraction exclusively to `ExpeditionGameplayService.extract(...)` and the existing extraction gate;
- the small technical approach floor now uses only Minecraft runtime blocks in a vanilla-reference hierarchy: smooth basalt -> calcite -> deepslate tiles, with chiseled deepslate directly under the relay;
- `FieldRelayPresentationPolicy` derives readiness only from the existing authoritative recovered Region 01 salvage count; no SavedData field, client-owned state or new network payload exists;
- at the existing `3/3` threshold two vanilla end-rod flank markers appear immediately after the successful third salvage interaction; below threshold they stay absent, and successful extraction clears them;
- the dressing helper replaces only the technical floor material set it owns and does not blindly overwrite unrelated blocks;
- rewards, pressure, encounter ownership, salvage recognition, save/network contracts and extraction eligibility remain unchanged.

Reference basis is recorded in `docs/M2B_REGION01_EXTRACTION_RELAY_PRESENTATION_REVIEW.md`: Mojang's vanilla amethyst-geode smooth-basalt/calcite layering and Ancient City / Deep Dark deepslate contrast. No external redistributable asset was introduced.

Verification: `Build Riftfrontier` workflow `34856309722` completed SUCCESS on `eee2a34b28ecaddcb0597ac8f42ddda80a318700`. Asset intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10353482114`
- archive digest: `sha256:035d1116a9e9f718888b73868466ba49a3ff9cc9d3099ec61b429989bdfe9fc2`
- executable JAR SHA-256: `3616ebbb045ecfbb8722e52727b9a85e5f0c2bf9bebe2939c17cfff845760371`

Verification vocabulary for `eee2a34b28ecaddcb0597ac8f42ddda80a318700`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN VISUAL ACCEPTANCE`: NO

Human procedure: `docs/M2B_REGION01_EXTRACTION_RELAY_PRESENTATION_REVIEW.md`.

Do not keep elaborating the technical relay or hub palette without new human field evidence. It is a temporary wayfinding/readiness aid, not a final Region 01 environment or portal design.

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

The boss TELEGRAPH outline remains build verified. `Region01BossFieldTelegraphGeometry` derives sparse boundary samples from existing `Region01BossFieldImpactProfile`; `Region01BossFieldTelegraphEmitter` displays them with Minecraft-native particles only during TELEGRAPH and owns no gameplay timing/hit authority. The corrected implementation SHA is `fc5e8411714d2dcf87df56141cf92156cb7de809`, workflow `34841785748` SUCCESS, deliverable digest `sha256:261c1f4f6d24f3e671ace52fc57f1ec6694b511db727773e873fe33765654e97`. Human procedure: `docs/M3_REGION01_BOSS_THREAT_TELEGRAPH_REVIEW.md`.

## Exact next development boundary

Do not spend the next run adding more backend/telegraph fences or decorating the temporary hub/relay just because current automation is stable.

1. If actual human field evidence is supplied, use it to accept/reject/calibrate the live field-status readability, extraction-relay readability, boss threat outline and/or arena-pressure motion candidate. Do not invent observations.
2. Otherwise prioritize independent production-visible Region 01 work: a legally usable, decodable, provenance-recorded final Dragon material/texture direction based on strong references, or other combat/environment presentation that does not bypass the material/animation gates.
3. Once arena-pressure motion is human accepted and all nine logical animation keys have evidence, assemble complete production animation binding without weakening exact coverage.
4. Select/author final VFX and sound under the same reference/provenance/readability rules. Presentation may emphasize authoritative windows but must never create a second hit clock.
5. Author the first real `presentation_assets` manifest only after referenced physical resources exist and are reviewed.
6. Publish through the existing reload/render pipeline and verify Minecraft scale, hit-volume readability and resource reload.
7. Remove stone-material/field-preview scaffolding only after verified production presentation supersedes it.
8. Only after presentation plus human field evidence clear the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Never claim `PLAYTESTED` or `MULTIPLAYER TESTED` without actual human sessions.
