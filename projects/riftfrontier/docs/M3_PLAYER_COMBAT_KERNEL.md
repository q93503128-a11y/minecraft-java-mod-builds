# M3 Player Combat Kernel

Status: semantic/data foundation, reference role lock, API-free server execution capability, Minecraft-facing server-authority adapter, first production move/family/module pack, server-owned ItemStack loadout decoding, and authenticated serverbound move-intent handling are complete. Concrete client action/key emission, approved production hit volumes/damage, and final presentation/field balance are still pending. This document does **not** approve final weapon art, damage, range, cooldown, hitbox, VFX, sound, or field balance.

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

`AttackPattern` remains the authoritative telegraph/ACTIVE/recovery clock. A weapon family does not create a second timing system. `WeaponModule` expresses a behaviour change contract; exact numeric modifiers are deliberately excluded until real field-play evidence exists.

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

## Production role lock — completed 2026-09-10

`M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md` is the bounded evidence gate for the first vertical-slice families. It locks exactly:

- `mobile_pressure`: short-reach close pressure, repeated repositioning, lower ordinary-entry commitment and a punishable committed reward/finisher;
- `reach_commitment`: deliberate spacing, longer practical melee engagement reach, constrained decisive commitment and a real punishable recovery;
- shared module socket semantic `technique`;
- first module behaviour direction `recovery_pivot`, which changes an eligible recovery transition without altering the authoritative hit clock or skipping required recovery.

These are role contracts, not an approval of final presentation or balance values.

## Runtime capability — completed 2026-09-10

The API-free server execution boundary is:

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

Runtime rules remain fail-closed:

- the assembled move set must exactly equal the family-authored move IDs;
- every move must resolve to the same `AttackPattern` ID requested by the family;
- a module must include the family in `compatibleFamilies` and use a socket declared by that family;
- the controller cannot start a move outside its family and cannot overlap an executing move;
- `recovery_pivot` is an authorization only and becomes true exclusively while the same authoritative attack clock samples `RECOVERY`;
- `recovery_pivot` does not shorten recovery, open a second hit window, grant invulnerability, or implement movement by itself.

## Minecraft authority adapter — completed 2026-09-10

`MinecraftPlayerWeaponCombatAdapter` consumes that deterministic capability without introducing balance values or a second attack clock:

```text
server equipment resolver
  -> Loadout(familyId, optional moduleId)
  -> CombatRuntimeCatalog.playerWeaponController(...)
  -> input move intent
  -> authoritative AttackStateMachine
  -> ACTIVE-only HitVolume candidate resolution
```

The adapter re-resolves server equipment while ticking. A family/module swap, unequip, death/logout/despawn lifecycle clear, or other loadout invalidation cancels and discards the old per-UUID session. Hit candidates are deduplicated per execution and are never exposed during TELEGRAPH or RECOVERY. `recovery_pivot` delegates to the same controller and therefore cannot survive an equipment change or open outside RECOVERY.

The adapter intentionally emits candidates rather than applying a guessed damage value. Shape-specific production hit volumes, damage policy, and visual presentation remain later production bindings.

Native `player_weapon_authority` GameTest coverage exercises TELEGRAPH/ACTIVE/RECOVERY authority, per-execution target deduplication, recovery-only module authorization, loadout-swap invalidation, session replacement, actor isolation and lifecycle cleanup using fixture-only definitions.

## First production weapon data — completed 2026-09-10

`data/riftfrontier/riftfrontier/content/player_combat_01.json` is the first production player-combat pack. It intentionally stays small and contains exactly:

- `riftfrontier:weapon_family/mobile_pressure`
  - `riftfrontier:attack/player/mobile_pressure_entry`
  - `riftfrontier:attack/player/mobile_pressure_finisher`
- `riftfrontier:weapon_family/reach_commitment`
  - `riftfrontier:attack/player/reach_commitment_strike`
- shared `technique` module `riftfrontier:weapon_module/recovery_pivot`.

The three `AttackPattern` documents provide the structurally required authoritative clock and preserve the dossier contrast: the ordinary mobile entry has lower commitment, the mobile finisher has a larger punishable recovery, and the reach strike exposes the clearest anticipation/recovery commitment. Their current tick counts are **provisional engineering values**, remain data-driven, and are not approved field balance or final cadence. They exist so the production graph can execute and be measured; later changes require actual field-play evidence rather than inference from animation clips or third-party balance numbers.

The production test `ProductionPlayerWeaponContentTest` loads the packaged JSON through `ContentPackLoader`, requires graph validity, locks exactly two families/three moves/one module, verifies the shared socket/module semantics and role contrast, and prevents fixture IDs from leaking into the production pack.

## Server equipment + move-intent authority — completed 2026-09-10

The first production equipment/input authority boundary is now:

```text
server-owned main-hand ItemStack
  -> riftfrontier:player_weapon_loadout data component
  -> PlayerWeaponItemStackLoadoutResolver
       -> only mobile_pressure / reach_commitment
       -> optional recovery_pivot only
       -> current CombatRuntimeCatalog revalidation

client intent
  -> PlayerWeaponMoveIntentPayload(moveId only)
  -> authenticated ServerPlayer from payload context
  -> current server-owned main-hand loadout
  -> family-authored move validation
  -> PlayerWeaponServerRuntime
  -> MinecraftPlayerWeaponCombatAdapter.beginMove(..., serverGameTime)
```

The ItemStack component is stable content identity metadata, not a second weapon registry. It carries no damage, reach, timing, hit result or target authority. The resolver accepts only the two locked production families and the one current module, then reconstructs the profile from the current published content graph; malformed, stale, unknown or incompatible component data fails closed.

The serverbound payload likewise carries only `moveId`. The sender identity comes from NeoForge's authenticated payload context, while loadout and server game time are resolved on the server. Client-provided damage, target, hit confirmation, attack phase, duration, cooldown or clock are not part of the wire contract.

`PlayerWeaponServerRuntime` is generation-aware. A published content-generation change rebuilds the capability and discards stale executions. Player ticks advance only already-active sessions; logout/clone lifecycle paths clear per-UUID state rather than scanning all entities.

Required native `player_weapon_input_authority` GameTest coverage proves wrong-family and spoofed move intents fail closed, a correct move can begin from the server-owned ItemStack component, equipment swap invalidates the old session before further execution, the new family can establish a fresh session, and unequip removes authority. Together with `player_weapon_authority`, the required GameTest suite also covers actor isolation and lifecycle cleanup.

This does **not** mean a finished player weapon is playable yet. No approved production ItemStack/model provisioning path or concrete client key/action sender has been committed, and the current server runtime deliberately exposes no production hit-volume candidates until a separate evidence-backed geometry/damage gate is approved.

## Production gate

The schema, bounded reference comparison, reusable server execution capability, Minecraft-facing authority adapter, first production move/family/module graph, typed server ItemStack identity boundary, and serverbound move-intent authority are complete. The remaining order is:

1. do not invent a production weapon item/model or input UX: first check whether an approved ItemStack identity/provisioning and client action/control mapping now exists;
2. if an approved client control mapping exists, bind that action to emit only `PlayerWeaponMoveIntentPayload(moveId)` and add an executable client/server transport test where feasible;
3. otherwise keep the wire contract ready and move to the next objective M3 server-authority boundary rather than fabricating UX;
4. bind approved shape-specific hit volumes and eventual damage policy to the adapter only after field/presentation evidence exists;
5. tune the provisional attack timings, damage and range only from real field play;
6. approve final item/model/animation/VFX/sound only through the separate presentation evidence gate.

Do not duplicate boss presentation plumbing, infer combat values from third-party animation clips, promote fixture IDs to production content, add a third family to evade the two-role contrast requirement, create a second attack timing system, or treat the provisional production tick counts as final balance.
