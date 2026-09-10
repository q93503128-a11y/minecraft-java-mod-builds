# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at run start: `d91acbdf0ba111f69946c7bedfa1e5fcc3b39dbc`.
- Previous fully validated implementation/test HEAD: `c89eec410711d312c862804ec2e64c3f1ce56f8b` (`Build Riftfrontier` run `34439967460`, FULL SUCCESS).
- Current batch implementation/docs HEAD before this handoff commit: `2a7de569612c063b03a107401926c2e599e73bb4`.
- Production Region 01 still has no approved final boss material treatment and no legitimate production numeric `attack_pattern` / final `boss_profile` discovered in the recovered canonical/handoff state.

## Completed in this batch

M3 player weapon reference/role lock:

- Re-read remote `main`, project canon, roadmap, M3 combat reference dossier, player combat kernel, and this handoff before changing anything.
- Performed bounded external reference research using official Capcom Monster Hunter manuals, official Fatshark Darktide weapon/update notes, and official Supergiant Hades II combat/update notes.
- Added `docs/M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md`.
- Locked exactly two first-slice semantic role contracts, without final item names or stable production IDs:
  - `mobile_pressure`: short-reach close pressure, repeated repositioning, lower ordinary-entry commitment, punishable committed reward/finisher.
  - `reach_commitment`: deliberate spacing, longer practical melee reach, constrained decisive commitment, readable anticipation and punishable recovery.
- Locked one first-slice module-composition direction:
  - socket semantic `technique`;
  - behaviour semantic `recovery_pivot`;
  - changes an eligible recovery transition only; it may not create a parallel hit clock, skip required recovery, grant generic invulnerability, or silently alter authoritative hit windows.
- Updated `docs/M3_PLAYER_COMBAT_KERNEL.md` so the reference gate is marked complete and the next engineering order is explicit.
- No production `attack_pattern`, `weapon_family`, or `weapon_module` JSON was authored yet. No damage/range/cooldown/hitbox/stamina/cadence/art/animation/VFX/sound value was invented.

## Changed systems/files

- `docs/M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md` — new bounded reference/design lock.
- `docs/M3_PLAYER_COMBAT_KERNEL.md` — production gate advanced from reference comparison to authored data/runtime boundary.
- `docs/AUTOMATION_HANDOFF.md` — this recovery record.

## Verification

- Source/reference inspection: PASS. Dossier sources are official Capcom/Fatshark/Supergiant pages and are recorded as URLs in the dossier.
- Remote-main conflict check before writes: PASS; `main` remained `d91acbdf0ba111f69946c7bedfa1e5fcc3b39dbc` immediately before the first write.
- `Build Riftfrontier` run `34443381052` for HEAD `2a7de569612c063b03a107401926c2e599e73bb4`: QUEUED at last check. Do **not** report this run as successful until all gates finish.
- Local clone/build: NOT RUN in this automation environment.
- Production player weapon execution: NOT AUTHORED / NOT TESTED.
- Player damage/range/cadence/hitboxes, resource costs, final art, animation/VFX/sound, and human field-play: NOT AUTHORED / NOT TESTED.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED.
- Production Region 01 numeric boss `attack_pattern`/final `boss_profile` and actual boss encounter attachment: NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified presentation/network ordering/lifecycle/cache/render work are DONE. Do not recreate or weaken those gates.
- `weapon_family` / `weapon_module` schema, strict decoder, graph validation, and regression tests are DONE. Do not replace them with weapon-specific subclass duplication or a second timing system.
- The player-weapon reference comparison and role-lock stage is now DONE. Do not reopen the first-slice family count or add a third family unless canonical direction is explicitly changed.
- `AttackPattern` remains the only authoritative `telegraph -> ACTIVE -> recovery` cadence primitive for player and boss authored moves.
- The first two player role contracts are `mobile_pressure` and `reach_commitment`; the first module direction is `technique` / `recovery_pivot`.
- Never promote fixture IDs/timings, copy third-party moves/assets, infer balance from animation clips, or invent final player/boss art or field-balance values without evidence.

## Exact next start point

1. Re-check current remote `main`, canon, roadmap, `M3_PLAYER_COMBAT_KERNEL.md`, `M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md`, and this handoff.
2. Check `Build Riftfrontier` run `34443381052`; if it failed, fix the first real failure without weakening tests or requirements. If still pending, do not call it successful.
3. Re-check whether approved final Region 01 boss material or legitimate production boss data appeared. If so, use the existing completed boss gates rather than replacing them.
4. If boss inputs remain absent, author the **smallest production `attack_pattern` set** needed to express exactly the locked `mobile_pressure` and `reach_commitment` roles. Timing fields must be defensible semantic placeholders only if the existing schema requires positive windows; do not claim them as final balance and keep them explicitly tuneable/data-driven.
5. Author exactly two production `weapon_family` definitions with the shared `technique` socket and exactly one production `weapon_module` line for `recovery_pivot`; extend validation/tests only for real ambiguities discovered by those definitions.
6. Then connect the authored definitions to a server-authoritative Minecraft player-combat execution boundary. Do not tune final damage/range/cadence or approve art before executable field-play/presentation evidence exists.
