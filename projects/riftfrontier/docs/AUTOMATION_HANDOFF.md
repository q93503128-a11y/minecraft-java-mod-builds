# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE REGION 01 TECHNICAL VERTICAL SLICE / CONNECTED EXPEDITION + CONTRACT + FIRST EVENT BUILD VERIFIED / PRODUCTION PRESENTATION + HUMAN PLAY ACCEPTANCE STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified checkpoint

Latest build-verified Riftfrontier gameplay checkpoint is `408ec6d1bebf759083bef97d210593e6817cdf5e` (`riftfrontier: keep blackout pressure legible in field HUD`). `Build Riftfrontier` workflow run `35155622241` completed **SUCCESS** on 2026-09-16.

This checkpoint includes the build-verified first Region 01 dynamic event `riftfrontier:encounter/region_01_rift_blackout` and improves its field readability without creating a parallel event lifecycle or custom HUD. While the authoritative run has exactly two recovered salvage and the player actually has Minecraft Darkness, the existing technical actionbar keeps the blackout state visible together with Salvage Recovery progress. When Darkness ends, normal authoritative salvage/threat/extraction projection resumes. The sculk/Darkness presentation and actionbar remain review aids rather than final Riftfrontier art/UI language.

Verified CI deliverables artifact: `10470689522` (`riftfrontier-0.1.0-alpha.1-deliverables`), archive digest `sha256:e54ec2c7153b45197498ff8c94db89c04c25d8a330e07a87cfcf83efb9c1fd92`. Logs artifact: `10470664697`, digest `sha256:b20c5fe8a678ed15982dc0575a35fb572718a14755d46673d2585ae73acaed37`.

Verification vocabulary for `408ec6d1...`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES by CI
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Do not infer human play or visual acceptance from GameTest, dedicated-server smoke or Xvfb client initialization.

## Connected Region 01 baseline — settled unless evidence regresses it

- Core loop: `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- The production `riftfrontier:contract/region_01_salvage_recovery` is legible through the connected player loop. EN/KO field-guide overview, deployment status, salvage/patrol actionbar states and extraction completion identify the contract and its return-to-next-preparation consequence. Do not invent a second Region 01 contract merely to satisfy the first-slice checklist.
- The first dynamic-event checklist item is represented by the build-verified Rift Blackout encounter. Do not add another event merely to satisfy the same checkbox. Improve or replace its temporary presentation only when doing visible-gameplay/production-presentation work with approved/reference-backed direction.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Field provisioning commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, `/riftfrontier weapon reach pivot`. In normal first-slice play, the same four loadouts are reachable from the hub without commands: grindstone = Mobile Pressure, fletching table = Reach Commitment, crouch-use = Recovery Pivot variant. These issue distinct localized custom names while retaining the same authoritative loadout data. Do not redo this identification/localization/station-routing work unless a regression is demonstrated.
- Zombie/Skeleton/Ravager remain behaviour/runtime proxies, not production creature art.
- Live actionbar readability distinguishes `salvage incomplete`, `relay ready but patrol alive`, and `patrol cleared / +1 secured` from authoritative run + tracked threat state. During the one-shot blackout, the same technical projection now retains explicit Darkness visibility until the effect ends.
- Between expeditions, the technical-hub actionbar continuously exposes authoritative supplies, stored salvage, next deployment cost and the already-established station guidance. Do not rebuild this or the expedition actionbar as a new custom HUD before final UI direction is approved.
- The first-slice written guide is five pages and covers the contract, hub preparation, combat rigs, patrol/salvage/extraction and logistics. It owns no progression state.
- Do not reopen M0/M1/M2 authority, lifecycle, restart, ownership or persistence fences without a demonstrated regression.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, blackout duration/intensity, final control layout, cover geometry or threat staging without human field evidence.
- Existing diagnostic particles/sounds, technical blocks, bossbar/actionbar, relay dressing, proxy labels, native swing, sculk blackout cues and tuff arena are review aids, not final Riftfrontier presentation language.

## Hunter production-creature checkpoint — Alien is no longer an uninspected candidate

Quaternius CC0 `Ultimate Monsters` `Big/glTF/Alien.gltf` has passed exact-source intake and structural inspection. Canonical machine-readable evidence is in `docs/provenance/region01_hunter_alien_source_inspection.json`, `docs/provenance/region01_hunter_alien_field_review_build.json`, and `docs/provenance/region01_hunter_alien_localized_field_review_build.json`.

A review-only Minecraft actor exists at `/summon riftfrontier:region_01_hunter_field_review`. It renders the exact inspected Alien source through the project skinned-mesh path and cycles the review motions. It remains deliberately isolated from natural Region 01 spawning and authoritative Hunter encounter semantics.

Do not redo Alien candidate search, exact-source intake, source hash inspection, animation inventory or field-review actor plumbing. Human Minecraft review is the next Alien gate. Until that review exists: `selected_for_production = false`, `human_visual_acceptance = false`, keep the server-authoritative Hunter behaviour proxy underneath, and do not wire Alien into natural Region 01 encounters.

Human review must inspect player-relative scale, facing, ground alignment, UV/texture integrity, near/combat/medium-distance silhouette and Idle/Walk/Run/Punch/HitReact/Death deformation/readability.

## Region 01 boss presentation — human gates remain open

- Selected rig/geometry: Quaternius CC0 `Dragon Evolved`.
- Accepted sanitized runtime geometry/skin/animation resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`.
- Source Atlas art is provenance/reference only and must not ship unchanged as final art.
- Provenance-tracked Poly Haven Dark Rock derived material candidate is available in the explicit field-review path but is **not human accepted**.
- Production boss rendering remains fail-closed until reviewed final material and complete reviewed animation semantic coverage exist.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; boss remains excluded from natural/production Region 01 encounter insertion.
- Boss field-review command output is localized EN/KO.

Do not restart boss model/material search merely because human acceptance is pending. Do not silently retune Dark Rock UV/material, arena-pressure motion, telegraph density or boss timings without field evidence.

## Next useful development boundary

Contract connection, the first dynamic event, and the event's technical field readability are now build-verified. Do not churn these paths or add speculative siblings just to increase feature count.

Human field play is required to approve or reject the Alien Hunter render and boss Dark Rock presentation, but development is not globally blocked on those gates. While those reviews are pending, prioritize remaining visible production gaps that do not require unapproved numeric tuning. For production creatures, do not start another open-ended search: Scout `Armabee` and Elite Anchor `Goleling Evolved` are already recorded Quaternius CC0 role candidates in `THIRD_PARTY_ASSETS.md`. Advance only one when the work produces a concrete exact-source inspection and/or isolated Minecraft field-review result. Preserve Scout ranged-pressure semantics; do not add flight merely because Armabee is winged. Preserve Elite observable counterplay; do not delete Ravager shield-stun behaviour until a verified replacement deliberately preserves or replaces it.

The next human field-play package should use the verified `408ec6d1...` CI deliverable and cover the full commandless loop plus blackout readability: obtain a rig at the hub, deploy by lodestone, recover salvage 1 then salvage 2, verify the one-shot visibility-collapse cue occurs and the technical field projection remains legible during Darkness, complete salvage 3/patrol decision, extract, and verify returned resources feed the next preparation cycle. Separately use `/summon riftfrontier:region_01_hunter_field_review` and `/riftfrontier boss fieldtest spawn` for the pending presentation gates. Record actual observations; never report `PLAYTESTED` or `MULTIPLAYER TESTED` from automated gates alone.
