# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE REGION 01 TECHNICAL VERTICAL SLICE / CONNECTED EXPEDITION + CONTRACT + FIRST EVENT BUILD VERIFIED / PRODUCTION PRESENTATION + HUMAN PLAY ACCEPTANCE STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified checkpoint

Latest build-verified Riftfrontier gameplay checkpoint is `9d7636c09a3e7fd2c2099554dbd187153b90e12f` (`test(riftfrontier): isolate blackout trigger rule from runtime`). `Build Riftfrontier` workflow run `35143895535` completed **SUCCESS** on 2026-09-16.

This verifies the first Region 01 dynamic event added in its parent: `riftfrontier:encounter/region_01_rift_blackout`. The event is tied to the existing authoritative Salvage Recovery contract and fires when the second salvage recovery makes authoritative recovered-salvage progress equal to 2. It applies a short Darkness window plus temporary sculk-based technical warning VFX/audio. The sculk presentation is explicitly a review aid, not final Riftfrontier art language. The trigger rule is isolated in the Minecraft-free `Region01SalvageEventRules` so JVM contract tests do not load runtime Minecraft classes; runtime still derives the event from server-owned expedition state and owns no parallel persistence/lifecycle flag.

Verified CI deliverables artifact: `10466705285` (`riftfrontier-0.1.0-alpha.1-deliverables`), archive digest `sha256:ffdeee090fe3f934eb17cbc4d4d1ee136605f1667a9a41c2a99b3dda8455347c`. Logs artifact: `10466855043`, digest `sha256:66f53a3a64a6b421b9a70f48be47bfe1b0e989996494358a2aba874e4b0097ad`.

Verification vocabulary for `9d7636c0...`:

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
- The production `riftfrontier:contract/region_01_salvage_recovery` is now legible through the connected player loop rather than existing only as backend content. EN/KO field-guide overview, deployment status, salvage/patrol actionbar states and extraction completion identify the Salvage Recovery / 회수 작전 contract and its return-to-next-preparation consequence. Do not invent a second Region 01 contract merely to satisfy the first-slice checklist.
- The first dynamic-event checklist item is now represented by the build-verified Rift Blackout encounter. Do not add another event merely to satisfy the same checkbox. Improve or replace its temporary presentation only when doing visible-gameplay/production-presentation work with approved/reference-backed direction.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Field provisioning commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, `/riftfrontier weapon reach pivot`. In normal first-slice play, the same four loadouts are reachable from the hub without commands: grindstone = Mobile Pressure, fletching table = Reach Commitment, crouch-use = Recovery Pivot variant. These issue distinct localized custom names while retaining the same authoritative loadout data. Do not redo this identification/localization/station-routing work unless a regression is demonstrated.
- Zombie/Skeleton/Ravager remain behaviour/runtime proxies, not production creature art.
- Live actionbar readability distinguishes `salvage incomplete`, `relay ready but patrol alive`, and `patrol cleared / +1 secured` from authoritative run + tracked threat state. Salvage/relay guidance states the actual 3-salvage unlock and field-lodestone right-click interaction.
- Between expeditions, the technical-hub actionbar continuously exposes authoritative supplies, stored salvage, next deployment cost and the already-established station guidance. Do not rebuild this or the expedition actionbar as a new custom HUD before final UI direction is approved.
- The first-slice written guide is five pages and covers the contract, hub preparation, combat rigs, patrol/salvage/extraction and logistics. It owns no progression state.
- Do not reopen M0/M1/M2 authority, lifecycle, restart, ownership or persistence fences without a demonstrated regression.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, blackout duration/intensity, final control layout, cover geometry or threat staging without human field evidence.
- Existing diagnostic particles/sounds, technical blocks, bossbar/actionbar, relay dressing, proxy labels, native swing, sculk blackout cues and tuff arena are review aids, not final Riftfrontier presentation language.

## Hunter production-creature checkpoint — Alien is no longer an uninspected candidate

The old handoff text saying Alien still needs exact-source intake is obsolete and must not be followed.

Quaternius CC0 `Ultimate Monsters` `Big/glTF/Alien.gltf` has already passed exact-source intake and structural inspection. Canonical machine-readable evidence is in:

- `docs/provenance/region01_hunter_alien_source_inspection.json`
- `docs/provenance/region01_hunter_alien_field_review_build.json`
- `docs/provenance/region01_hunter_alien_localized_field_review_build.json`

Verified source facts include source SHA-256 `e6fec42f9d4db3c3177da9027c5cdb2c4abd71cb934ea4f4788aea155f267124`, one skinned mesh, embedded texture and usable Hunter-relevant source clips including Idle, Walk, Run, Punch, HitReact and Death.

A review-only Minecraft actor already exists:

`/summon riftfrontier:region_01_hunter_field_review`

It renders the exact inspected Alien source through the project skinned-mesh path and cycles the review motions. It is localized EN/KO. It remains deliberately isolated from natural Region 01 spawning and from authoritative Hunter encounter semantics.

**Do not redo Alien candidate search, exact-source intake, source hash inspection, animation inventory or field-review actor plumbing.** Human Minecraft review is the next Alien gate. Until that review exists:

- `selected_for_production = false`
- `human_visual_acceptance = false`
- keep the server-authoritative Hunter behaviour proxy underneath
- do not wire Alien into natural Region 01 encounters

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

The contract connection and first dynamic-event checklist item are now implemented and build-verified. Do not churn those paths or add speculative siblings just to increase feature count.

Human field play is required to approve or reject the Alien Hunter render and the boss Dark Rock presentation, but development is not globally blocked on those two gates. While those reviews are pending, prioritize the remaining visible production gap: Region 01 presentation and combat feel that can be improved without unapproved numeric tuning, or a concrete provenance-tracked production creature/presentation advancement using the already selected direction.

For production creatures, do not start another open-ended search. Scout `Armabee` and Elite Anchor `Goleling Evolved` remain recorded Quaternius CC0 role candidates, but only advance one when doing so produces a concrete exact-source inspection or Minecraft field-review result. Preserve Scout ranged-pressure semantics and Elite observable counterplay; do not add flight merely because Armabee is winged, and do not delete Ravager shield-stun counterplay until a verified replacement deliberately preserves or replaces it.

The next human field-play package should use the verified `9d7636c0...` CI deliverable and cover the full commandless loop plus the new blackout: obtain a rig at the hub, deploy by lodestone, recover salvage 1 then salvage 2, verify the one-shot visibility-collapse cue occurs on salvage 2, complete salvage 3/patrol decision, extract, and verify returned resources feed the next preparation cycle. Separately use `/summon riftfrontier:region_01_hunter_field_review` and `/riftfrontier boss fieldtest spawn` for the pending presentation gates. Record actual observations; never report `PLAYTESTED` or `MULTIPLAYER TESTED` from automated gates alone.
