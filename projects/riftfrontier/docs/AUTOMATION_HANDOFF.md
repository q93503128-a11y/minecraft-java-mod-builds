# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE TECHNICAL EXPEDITION LOOP + PLAYER COMBAT BUILD VERIFIED / REGION 01 FIELD READABILITY BUILD VERIFIED / CONNECTED-LOOP LOCALIZATION BUILD VERIFIED / HUMAN FIELD PLAY + FINAL BOSS MATERIAL + ARENA-PRESSURE MOTION ACCEPTANCE + PRODUCTION CREATURE/VFX/SOUND STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Settled direction — do not redo without new evidence

- Core slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless technical hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled. Do not reopen without a demonstrated regression.
- Player combat already has two server-authoritative weapon families (`mobile_pressure`, `reach_commitment`), one `recovery_pivot` technique path, authenticated move-id input, ACTIVE-only hit authority and per-execution target dedupe.
- Region 01 Zombie/Skeleton/Ravager actors remain **behaviour proxies**, not production creature art. Vanilla silhouettes, equipment and role names are field-review scaffolding only.
- Region 01 boss geometry/rig remains Quaternius CC0 `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or add GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Provenance/license registry remains `docs/THIRD_PARTY_ASSETS.md`. The source Dragon Evolved Atlas is provenance/reference only and is **not** approved final Region 01 art.
- Production boss rendering must continue to fail closed until reviewed final material plus complete reviewed animation semantic coverage exist. Do not weaken exact-coverage/review gates or fabricate `presentation_assets`.
- Diagnostic Minecraft particles/sounds, technical blocks, bossbar, stone-textured boss preview, actionbar, extraction-relay dressing/readiness markers, proxy role names, player hit cues, salvage-hazard cues and the vanilla player swing described below are temporary readability/field-review aids, not final Riftfrontier UI/art/VFX/audio/animation language.
- Do not auto-tune provisional damage, hit geometry, charge travel, radial impulse, particle density, salvage-hazard cue intensity or key layout without human field evidence.

## Latest verified gameplay checkpoint — accepted weapon move-start readability

Implementation checkpoint: `22a39b59671ad88cfbf76b830234f222ea9520c2` (`riftfrontier: animate accepted weapon move starts`).

The server now broadcasts Minecraft's native main-hand swing only **after** `MinecraftPlayerWeaponCombatAdapter.beginMove(...)` accepts the authenticated move intent. This is a presentation cue only:

- rejected/stale/invalid move intents do not swing;
- the initiating player and nearby observers receive the same vanilla swing (`updateSelf=true`);
- hit confirmation remains the separate existing damage-indicator/strong-attack cue and is still emitted only after a real server-authoritative hit;
- no client hit authority, second attack clock, damage/shape/timing change, save/network schema change or new gameplay state was introduced;
- this native swing is a field-review baseline, not approved final weapon animation language.

Verification: `Build Riftfrontier` workflow `34878394712` completed SUCCESS on `22a39b59671ad88cfbf76b830234f222ea9520c2`. Toolchain verification, asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads all passed.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10361793292`
- archive digest: `sha256:f8369b29d7c5fe294c0d4cca78ef4cf1ea90c144e28e6b84bb0b53edd85114eb`
- executable JAR SHA-256: `e97dbacbdc7f7f14d6b45067e814ef7da031f7b108051f0be90dae776189a69c`

Verification vocabulary for this checkpoint:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `HUMAN COMBAT-PRESENTATION ACCEPTANCE`: NO

Human field procedure:

1. Use the verified JAR in a disposable world and assign the two Riftfrontier combat actions in Minecraft Controls.
2. Enter Region 01 through the normal hub path and equip `/riftfrontier weapon mobile`.
3. Trigger both authored actions while intentionally missing. Each server-accepted move should visibly swing the main hand even though no hit-confirm particle/sound occurs.
4. Repeat with `/riftfrontier weapon reach`. The move-start swing should remain immediate and the successful-hit cue should remain separate and occur only on actual admitted damage.
5. Exercise `/riftfrontier weapon mobile pivot` and `/riftfrontier weapon reach pivot`. Recovery-pivot use must not manufacture an extra hit-confirm cue or alter ACTIVE-only damage authority.
6. Try an invalid/stale/non-Riftfrontier main-hand state. A rejected intent must not create the server-broadcast move-start swing.
7. If possible, observe from a second human client. Both clients should see the same accepted move-start swing; do not mark multiplayer tested until this has actually happened.
8. Reject this presentation if swing timing materially misrepresents the authored attack startup, causes duplicate-looking hits, hides family differences, or produces a cue for rejected inputs.

Do not auto-tune attack timings or replace the native swing with invented final animation based only on automation observation. Human field evidence should decide whether this baseline is retained, replaced or family-differentiated later.

## Connected expedition loop — retained checkpoint

The commandless hub/bootstrap -> deploy -> salvage/fight -> extract -> provision -> redeploy loop remains build verified. The normal `ExpeditionGameplayService` detail path is localized through `Component.translatable(...)` with `en_us`/`ko_kr`; `/riftfrontier expedition status` remains diagnostic. Latest localization implementation `2bee9f88d2e978a742a107e2e5d96c02ed1df1e9`, workflow `34873147707` SUCCESS. Do not reopen localization, actionbar, live field status, relay dressing, proxy names or salvage warnings without field evidence.

Earlier connected-loop checkpoints remain valid and should not be reimplemented merely to create activity: fresh bootstrap/login (`5f9d6b6...`, `19a54bdf...`, workflow `34808226019`), actionbar API correction (`fded37cb...`, workflow `34821369460`), player hit readability (`24a15b2a...`, workflow `34825929915`), live field status (`54f070f1...`, workflow `34849271056`), extraction relay (`eee2a34b...`, workflow `34856309722`), proxy-role readability (`e7b7421a...`, workflow `34860050735`), salvage-hazard readability (`54d03af7...`, workflow `34866121039`) and connected-loop localization (`2bee9f88...`, workflow `34873147707`).

## Region 01 boss field harness and production gates

Development-only commands remain:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production Region 01 encounter composition. Current authoritative behavior remains phase 1 committed strike + line displacement, phase 2 arena pressure, server-owned `TELEGRAPH -> ACTIVE -> RECOVERY`, diagnostic `1.0F` ACTIVE-only damage, per-execution dedupe, attack-start facing commitment, role-specific hit geometry, ACTIVE-only line travel, one-shot radial impulse and health-backed bossbar.

The temporary field-review renderer exposes accepted sanitized Dragon geometry with Minecraft stone only while production presentation cannot publish. Evidence-backed source motion remains committed strike -> `Punch`, line displacement -> `Headbutt`, damage -> `HitReact`, death -> non-looping `Death`, unresolved -> `Flying_Idle`; priority remains `Death > reviewed authoritative attack sample > HitReact > Flying_Idle`.

The authored arena-pressure compression/burst/recovery candidate is automated-build verified but **not human accepted**. It does not close production animation coverage. Checkpoint `bca0816381c09b28d613bc12d2e395eb62368761`, workflow `34812925676` SUCCESS.

A previous dark-rock material candidate contained a corrupt PNG and is rejected. Keep the verified stone fallback. Future material candidates must be physical redistributable resources with provenance/license evidence and successful decode/format validation before renderer hookup. Canonical rejection record: `docs/M3_REGION01_MATERIAL_INTAKE_REJECTION.md`.

The boss TELEGRAPH outline remains build verified at corrected implementation `fc5e8411714d2dcf87df56141cf92156cb7de809`, workflow `34841785748` SUCCESS. Do not keep hardening it absent regression or field evidence.

## Exact next development boundary

Do not spend the next run adding more backend/telegraph fences, actionbar state, technical proxy labels, hub/relay decoration, salvage warning layers, localization churn or additional generic weapon-start cues simply because automation is stable.

1. If actual human field evidence is supplied, use it to accept/reject/calibrate the relevant temporary readability layers. Never invent observations.
2. Otherwise prioritize independent **production-visible** Region 01 work: a legally usable, decodable, provenance-recorded final Dragon material/texture direction based on strong references, or other combat/environment presentation that does not bypass the material/animation gates.
3. Production creature silhouettes for hunter/scout/elite are valid only after establishing a reference/asset direction. Do not improvise them from scratch merely to replace vanilla proxies.
4. Once arena-pressure motion is human accepted and all nine logical animation keys have evidence, assemble complete production animation binding without weakening exact coverage.
5. Select/author final VFX and sound under the same reference/provenance/readability rules. Presentation may emphasize authoritative windows but must never create a second hit clock.
6. Author the first real `presentation_assets` manifest only after referenced physical resources exist and are reviewed.
7. Publish through the existing reload/render pipeline and verify Minecraft scale, hit-volume readability and resource reload.
8. Remove stone-material/field-preview/proxy-label/native-cue scaffolding only after verified production presentation supersedes it.
9. Only after presentation plus human field evidence clear the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Never claim `PLAYTESTED` or `MULTIPLAYER TESTED` without actual human sessions.
