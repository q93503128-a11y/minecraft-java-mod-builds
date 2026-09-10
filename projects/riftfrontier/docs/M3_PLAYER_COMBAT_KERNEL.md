# M3 Player Combat Kernel

Status: semantic/data foundation only. This document does **not** approve final weapon art, damage, range, cooldown, hitbox, VFX, sound, or balance.

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

## Production gate

This kernel is **not** permission to fabricate the two final weapon families. Before production definitions are added:

1. write a bounded player-weapon reference/design comparison;
2. lock two genuinely different combat roles and their counterplay/commitment trade-offs;
3. author their real `attack_pattern` moves from that evidence;
4. then author `weapon_family` / `weapon_module` data;
5. connect those definitions to server-authoritative Minecraft execution;
6. tune damage/range/cadence only from real field play.

Do not duplicate boss presentation plumbing, infer combat values from third-party animation clips, or promote fixture IDs to production content.
