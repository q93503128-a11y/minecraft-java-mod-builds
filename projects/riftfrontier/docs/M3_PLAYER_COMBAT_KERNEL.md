# M3 Player Combat Kernel

Status: semantic/data foundation complete. `M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md` now locks the first two production role contracts and one module-composition line. This document still does **not** approve final weapon art, damage, range, cooldown, hitbox, VFX, sound, or balance.

## Purpose

M3 requires at least two meaningfully different weapon families and at least one active/passive or module-composition line. The implementation must not turn that requirement into copied Java subclasses or raw stat tiers.

The first reusable data boundary is therefore:

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

`M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md` is now the bounded evidence gate for the first vertical-slice families. It locks exactly:

- `mobile_pressure`: short-reach close pressure, repeated repositioning, lower ordinary-entry commitment and a punishable committed reward/finisher;
- `reach_commitment`: deliberate spacing, longer practical melee engagement reach, constrained decisive commitment and a real punishable recovery;
- shared module socket semantic `technique`;
- first module behaviour direction `recovery_pivot`, which changes an eligible recovery transition without altering the authoritative hit clock or skipping required recovery.

These are semantic role labels, not final stable content IDs or final weapon names. They do not authorize numeric balance or presentation assets.

## Production gate

The schema kernel and the bounded role comparison are complete. The remaining order is:

1. author the smallest production `attack_pattern` move set that expresses the two locked roles without inventing final balance values;
2. author exactly two production `weapon_family` definitions and one `weapon_module` line using the existing schema;
3. connect those definitions to server-authoritative Minecraft execution;
4. verify move-state transitions and hit authority automatically;
5. tune damage/range/cadence only from real field play;
6. approve final art/animation/VFX/sound only through the separate presentation evidence gate.

Do not duplicate boss presentation plumbing, infer combat values from third-party animation clips, promote fixture IDs to production content, add a third family to evade the two-role contrast requirement, or create a second attack timing system.
