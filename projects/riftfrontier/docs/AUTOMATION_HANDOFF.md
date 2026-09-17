# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE REGION 01 TECHNICAL VERTICAL SLICE / CONNECTED EXPEDITION + CONTRACT + FIRST EVENT + OPTIONAL SALVAGE RISK/REWARD BUILD VERIFIED / PRODUCTION PRESENTATION + HUMAN PLAY ACCEPTANCE STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified checkpoint

Latest build-verified Riftfrontier gameplay checkpoint is `e81ccb44bccc3906d7e03639fe8a5da4de809cfe` (`fix(riftfrontier): resolve same-tick spawned combat targets`). `Build Riftfrontier` workflow run `35172934294` completed **SUCCESS** on 2026-09-17.

This checkpoint contains the Region 01 optional-salvage risk/reward presentation introduced before it and the combat regression fix required to make the full gate reliable. Region 01 has five authoritative salvage nodes while the Salvage Recovery contract requires three: after the third recovery, field feedback exposes the real choice between extracting, clearing the remaining patrol for its established bonus, or risking the remaining optional salvage nodes. The arena plan owns the five positions and remaining-node calculation rather than duplicating those facts in presentation code.

The first CI attempt for that gameplay batch exposed a reproducible `riftfrontier:attack_hit_window` GameTest failure. The cause was not relaxed away: a target created with `addFreshEntity` could already be authoritative while not yet visible through the spatial-section query on the ACTIVE tick. `MinecraftAttackAdapter.AabbHitVolume` now falls back to a bounded `level.getAllEntities()` scan only when the indexed query is empty, applying the same AABB and `MinecraftCombatAuthority.isEligibleTarget` predicates. The successful full workflow verifies the regression fix together with the gameplay batch.

Verified CI deliverables artifact: `10477481933` (`riftfrontier-0.1.0-alpha.1-deliverables`), archive digest `sha256:08a7d961cc68fbe03a7190dddbf91a94204fc1ed80048d81ed31f78a0fdd5695`. Logs artifact: `10477626764`, digest `sha256:a710a65af7621d7499df17bbe89334ada461ccfb1c6aa388e149baa1b31d1488`.

Verification vocabulary for `e81ccb44...`:

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
- Same-tick spawned authoritative targets are intentionally covered by the verified AABB fallback described above. Do not remove it as cleanup unless an equivalent verified query path replaces it; do not broaden it into speculative combat lifecycle hardening.
- Field provisioning commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, `/riftfrontier weapon reach pivot`. In normal first-slice play, the same four loadouts are reachable from the hub without commands: grindstone = Mobile Pressure, fletching table = Reach Commitment, crouch-use = Recovery Pivot variant. These issue distinct localized custom names while retaining the same authoritative loadout data. Do not redo this identification/localization/station-routing work unless a regression is demonstrated.
- Zombie/Skeleton/Ravager remain behaviour/runtime proxies, not production creature art.
- Live actionbar readability distinguishes `salvage incomplete`, `relay ready but patrol alive`, and `patrol cleared / +1 secured` from authoritative run + tracked threat state. After the contract minimum is met it also exposes the remaining optional salvage count/risk choice rather than hiding the already-real five-node arena structure. During the one-shot blackout, the same technical projection retains explicit Darkness visibility until the effect ends.
- Between expeditions, the technical-hub actionbar continuously exposes authoritative supplies, stored salvage, next deployment cost and the already-established station guidance. Do not rebuild this or the expedition actionbar as a new custom HUD before final UI direction is approved.
- The first-slice written guide covers the contract, hub preparation, combat rigs, patrol/salvage/extraction, optional salvage risk/reward and logistics. It owns no progression state.
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

Contract connection, the first dynamic event, blackout technical readability, the real five-node optional-salvage choice, and the same-tick combat regression are build-verified. Do not churn these paths or add speculative siblings just to increase feature count.

Human field play is required to approve or reject the Alien Hunter render and boss Dark Rock presentation, but development is not globally blocked on those gates. While those reviews are pending, prioritize remaining visible production gaps that do not require unapproved numeric tuning. For production creatures, do not start another open-ended search: Scout `Armabee` and Elite Anchor `Goleling Evolved` are already recorded Quaternius CC0 role candidates in `THIRD_PARTY_ASSETS.md`. Advance only one when the work produces a concrete exact-source inspection and/or isolated Minecraft field-review result. Preserve Scout ranged-pressure semantics; do not add flight merely because Armabee is winged. Preserve Elite observable counterplay; do not delete Ravager shield-stun behaviour until a verified replacement deliberately preserves or replaces it.

The next human field-play package should use the verified `e81ccb44...` CI deliverable and cover the full commandless loop: obtain a rig at the hub, deploy by lodestone, recover salvage 1 then salvage 2, verify the one-shot visibility-collapse cue occurs and the technical field projection remains legible during Darkness, reach salvage 3, verify the extract-vs-patrol-vs-optional-salvage choice is understandable, optionally recover salvage 4/5, extract, and verify returned resources feed the next preparation cycle. Separately use `/summon riftfrontier:region_01_hunter_field_review` and `/riftfrontier boss fieldtest spawn` for the pending presentation gates. Record actual observations; never report `PLAYTESTED` or `MULTIPLAYER TESTED` from automated gates alone.
