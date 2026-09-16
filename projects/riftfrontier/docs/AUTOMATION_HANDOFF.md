# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `/AGENTS.md`, `/docs/BUILD_STANDARD.md`, `/docs/QUALITY_STANDARD.md`, `PROJECT.md`, `docs/CANONICAL.md`, relevant project docs/data and implementation before acting. This file records the latest useful checkpoint and repeated-work boundaries; it never overrides code/data contracts.

## Current stage

`M3 — PLAYABLE REGION 01 TECHNICAL VERTICAL SLICE / PLAYER COMBAT + CONNECTED EXPEDITION LOOP BUILD VERIFIED / BOSS + HUNTER FIELD-REVIEW PATHS READY / HUMAN PRESENTATION ACCEPTANCE STILL OPEN`

Priority remains a genuinely playable, polished Region 01 vertical slice. Do not grow speculative authority/lifecycle infrastructure or expand region count while visible gameplay/presentation remains incomplete.

## Latest verified checkpoint

Current code checkpoint before this handoff refresh: `a6a81d01b06e2ad06b79687533d84d1240952cc1` (`riftfrontier: add Korean boss field-review localization`). `Build Riftfrontier` workflow `35038504898` completed **SUCCESS** on 2026-09-16.

The boss field-review command messages are now EN/KO-localized instead of exposing hard-coded English literals. The preceding extraction-relay player-facing messages are also localized. These changes do not alter boss authority, natural encounter insertion, expedition state, combat balance or persistence.

Verification vocabulary for `a6a81d01...`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES by CI
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Do not infer human visual acceptance from CI client smoke.

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

## Connected Region 01 baseline — settled unless evidence regresses it

- Core loop: `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy`.
- Fresh-world commandless hub/bootstrap, extraction, failure/restart reconciliation, supply/storage/pressure and authoritative expedition persistence are settled.
- Player combat has `mobile_pressure` / `reach_commitment`, `recovery_pivot`, authenticated move-id input, server-owned attack timing, ACTIVE-only damage and per-execution target dedupe.
- Field provisioning remains `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, `/riftfrontier weapon reach pivot`.
- Zombie/Skeleton/Ravager remain behaviour/runtime proxies, not production creature art.
- Do not reopen M0/M1/M2 authority, lifecycle, restart, ownership or persistence fences without a demonstrated regression.
- Do not auto-tune provisional damage, hit geometry, boss attack timing/travel/impulse, particle density, salvage cue intensity, final control layout, cover geometry or threat staging without human field evidence.
- Existing diagnostic particles/sounds, technical blocks, bossbar/actionbar, relay dressing, proxy labels, native swing and tuff arena are review aids, not final Riftfrontier presentation language.

## Region 01 boss presentation — human gates remain open

- Selected rig/geometry: Quaternius CC0 `Dragon Evolved`.
- Accepted sanitized runtime geometry/skin/animation resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`.
- Source Atlas art is provenance/reference only and must not ship unchanged as final art.
- Provenance-tracked Poly Haven Dark Rock derived material candidate is available in the explicit field-review path but is **not human accepted**.
- Production boss rendering remains fail-closed until reviewed final material and complete reviewed animation semantic coverage exist.
- Development harness remains `/riftfrontier boss fieldtest spawn`, `phase1`, `phase2`; boss remains excluded from natural/production Region 01 encounter insertion.
- Boss field-review command output is now localized EN/KO.

Do not restart boss model/material search merely because human acceptance is pending. Do not silently retune Dark Rock UV/material, arena-pressure motion, telegraph density or boss timings without field evidence.

## First-slice guide and presentation baseline

Fresh-world bootstrap provides a vanilla written-book field guide explaining only existing authoritative rules: hub preparation, lodestone deployment, three salvage recovery, relay extraction, technical threat roles, optional patrol-clear reward, salvage-triggered rift drag and hub storage/pressure conversion. It owns no progression state. Continue using vanilla written-book UX until a final Riftfrontier UI language has reference approval; do not invent a replacement custom Screen from scratch.

## Next useful development boundary

Human field play is required to approve or reject the Alien Hunter render and the boss Dark Rock presentation, but development is not globally blocked on those two gates.

While those reviews are pending, prefer independent visible-gameplay work that advances Region 01 completion: connected expedition readability, combat feel that does not require unapproved numeric tuning, legally sourced/provenance-tracked production presentation, or another clearly missing vertical-slice requirement. Use already approved external/reference direction. Do not collect broad speculative asset lists.

For production creatures, do not start another open-ended search. Scout `Armabee` and Elite Anchor `Goleling Evolved` remain recorded Quaternius CC0 role candidates, but only advance one when doing so produces a concrete exact-source inspection or Minecraft field-review result. Preserve Scout ranged-pressure semantics and Elite observable counterplay; do not add flight merely because Armabee is winged, and do not delete Ravager shield-stun counterplay until a verified replacement deliberately preserves or replaces it.

When human field play is the actual next dependency, provide the exact verified JAR, commands, reproduction sequence and expected observations. Never report `PLAYTESTED` or `MULTIPLAYER TESTED` from GameTest, dedicated-server smoke or Xvfb client initialization alone.
