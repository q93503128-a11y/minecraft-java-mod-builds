# M3 Player Combat Kernel

Status: semantic/data foundation and API-free server execution capability complete. `M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md` locks the first two production role contracts and one module-composition line. Production move data and the Minecraft player adapter are still pending. This document does **not** approve final weapon art, damage, range, cooldown, hitbox, VFX, sound, or balance.

## Purpose

M3 requires at least two meaningfully different weapon families and at least one active/passive or module-composition line. The implementation must not turn that requirement into copied Java subclasses or raw stat tiers.

The reusable data boundary is:

```text
WeaponFamily
  -> authored AttackPattern moves
  -> combat role semantics
  -> declared module sockets

WeaponModule
  -> compatible WeaponFamily ids
  -> one declared socket
  -> behaviour-change semantics
```

`AttackPattern` remains the authoritative telegraph/ACTIVE/recovery clock. A weapon family does not create a second timing system. `WeaponModule` expresses a behaviour change contract; exact numeric modifiers are deliberately excluded until a real family is authored and field-play evidence exists.

## Validator contract

A valid `weapon_family` must:

- contain at least one move;
- resolve every move to an `attack_pattern`;
- declare at least one combat-role token;
- declare at least one module socket.

A valid `weapon_module` must:

- reference at least one existing `weapon_family`;
- name a nonblank socket;
- use a socket declared by every compatible family;
- declare at least one behaviour-change token.

These are structural errors, not warnings, because accepting an unresolved move or impossible socket would make runtime composition ambiguous.

## JSON shape

Fixture-only example; names and timings below are not production balance:

```json
{"kind":"weapon_family","id":"riftfrontier:fixture/weapon_family/mobile_blade","moves":["riftfrontier:fixture/move/commit_slash"],"combat_roles":["mobile_commitment"],"module_sockets":["technique"]}
```

```json
{"kind":"weapon_module","id":"riftfrontier:fixture/module/tempo_shift","compatible_families":["riftfrontier:fixture/weapon_family/mobile_blade"],"socket":"technique","behaviour_changes":["reposition_after_commitment"]}
```

## Production role lock — completed 2026-09-10

`M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md` is the bounded evidence gate for the first vertical-slice families. It locks exactly:

- `mobile_pressure`: short-reach close pressure, repeated repositioning, lower ordinary-entry commitment and a punishable committed reward/finisher;
- `reach_commitment`: deliberate spacing, longer practical melee engagement reach, constrained decisive commitment and a real punishable recovery;
- shared module socket semantic `technique`;
- first module behaviour direction `recovery_pivot`, which changes an eligible recovery transition without altering the authoritative hit clock or skipping required recovery.

These are semantic role labels, not final stable content IDs or final weapon names. They do not authorize numeric balance or presentation assets.

## Runtime capability — completed 2026-09-10

The API-free server execution boundary now consists of:

```text
validated/published content
  -> CombatRuntimeCatalog
  -> PlayerWeaponRuntimeProfile
       -> exact WeaponFamily move set
       -> optional compatible WeaponModule/socket
       -> resolved AttackPattern set
  -> PlayerWeaponCombatController
       -> AttackStateMachine
       -> AttackExecution / AttackTimeline
```

Runtime rules are fail-closed even after graph validation:

- the assembled move set must exactly equal the family-authored move IDs;
- every move must resolve to the same `AttackPattern` ID requested by the family;
- a module must include the family in `compatibleFamilies` and use a socket declared by that family;
- the controller cannot start a move outside its family and cannot overlap an executing move;
- `recovery_pivot` is an authorization only and becomes true exclusively while the same authoritative attack clock samples `RECOVERY`;
- `recovery_pivot` does not shorten recovery, open a second hit window, grant invulnerability, or implement movement by itself.

This layer is deliberately Minecraft-API-free so the server gameplay adapter can consume a deterministic capability instead of reconstructing family/module semantics from item state.

## Production gate

The schema, bounded role comparison, and reusable server execution capability are complete. The remaining order is:

1. author the smallest production `attack_pattern` move set that expresses the two locked roles without claiming final field balance;
2. author exactly two production `weapon_family` definitions and one `weapon_module` line using the existing schema;
3. assemble those production definitions through `CombatRuntimeCatalog.playerWeaponController(...)` rather than a second execution path;
4. connect Minecraft server-owned player equipment/input state and authoritative hit-volume handling to that controller;
5. add native GameTest coverage for the real player adapter, including illegal-family moves, ACTIVE-only hit authority, recovery completion, and `recovery_pivot` timing;
6. tune damage/range/cadence only from real field play;
7. approve final art/animation/VFX/sound only through the separate presentation evidence gate.

Do not duplicate boss presentation plumbing, infer combat values from third-party animation clips, promote fixture IDs to production content, add a third family to evade the two-role contrast requirement, or create a second attack timing system.
