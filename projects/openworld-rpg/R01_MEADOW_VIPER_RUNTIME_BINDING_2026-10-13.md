# Openworld RPG — R01 Meadow Viper Runtime Binding Evidence — 2026-10-13

> Scope: Meadow Viper project-owned entity/runtime binding only. This record does not promote unreviewed presentation or ecology placement to production acceptance.

## Bound source

- source asset: `external-assets/quaternius-easy-enemies/Snake.glb`
- runtime asset: `src/main/resources/assets/openworld_rpg/models/entity/meadow_viper/snake.glb`
- source/runtime bytes: exact match
- size: 216696 bytes
- Git blob SHA-1: `fdb107f06725f487fed66c7386b31c56c236e8ad`
- SHA-256: `70b4af1088dec939e167b07c0a5e69992a4851beac8bd24a9da1189b65ae101c`
- license evidence: Quaternius source-specific CC0 1.0 / Public Domain evidence already recorded in the pinned intake source.

The runtime does not substitute a vanilla entity model and does not reduce the source to a static mesh. `MeadowViperGltfModel` loads the exact GLB, validates its hash/structure/animation contract, samples the skinned animation channels and submits the skinned geometry through the Minecraft 26.2 renderer path.

Preserved source clips:

- `SnakeArmature|Snake_Idle` — 2.083333 s
- `SnakeArmature|Snake_Walk` — 0.833333 s
- `SnakeArmature|Snake_Attack` — 0.666667 s
- `SnakeArmature|Snake_Jump` — 0.958333 s

The authored Coil Bite combat sequence remains server-owned and is not replaced by the raw source clip duration. The Attack clip is time-mapped across warning/wind-up, active/lunge and recovery so the visible source motion can follow the canonical 0.45 s + ~0.15 s + 0.55 s combat contract.

## Server-authoritative runtime

Implemented project entity: `openworld_rpg:meadow_viper`.

Implemented state cycle:

`NEUTRAL -> WARNING -> BITE_WINDUP/BITE_ACTIVE -> RECOVERY -> SPACING`

Canonical runtime contracts implemented:

- Lv2 common profile: HP 75 / Defense 7 / MR 4 / Poise 18
- defensive warning radius: 3.5 blocks
- minimum warning/wind-up: 0.45 s
- Coil Bite legal range: 2.2 blocks
- active/lunge: ~0.15 s
- recovery: 0.55 s
- post-bite spacing target: ~2 blocks
- direct player attack provokes immediate engagement
- leaving warning space before an unprovoked attack de-escalates the encounter
- physical bite damage uses the canonical 9% same-Lv benchmark post-mitigation authoring path
- guard pressure: light
- guardable: yes
- perfect-guardable: yes
- player poise pressure: light-contact baseline 16
- Poison buildup: 40 before guard/perfect-guard handling

Target selection, warning state, attack commitment, legal contact, damage, Poison buildup, cooldown/state transition and spacing are server-owned. The client owns model/animation presentation only.

## Viper Poison

Implemented player-side Viper Poison:

- threshold derives from the authoritative player WIL build and fails closed if no build exists
- buildup decay follows the canonical player buildup timing
- duration: 8 s
- total fixed source-Lv budget: 9% Lv2 benchmark HP
- damage uses project timed magic/MR application rather than target-MaxHP runtime damage
- healing received: -20%
- status is registered as `minor_dispellable`
- ordinary guard reduces buildup by the same physical guard absorption percentage
- perfect guard / dodge applies no Poison buildup
- Poison-reduced healing is not converted into Saint overheal Barrier

## Rewards / progression boundary

Meadow Viper is project-owned, not an external-dependency registry target. The external pinned registry surface therefore remains 13 targets / 7 external combat profiles, while the project combat-profile list contains 8 profiles including Meadow Viper.

Ordinary Meadow Viper rewards are bound to the existing ordinary-enemy authority:

- Combat EXP: ~1% current next-Lv requirement
- Class XP: ~0.8% current Class Rank requirement
- Gold: 2 at 60%
- no baseline material/consumable/equipment drop
- donor/vanilla XP and death loot are suppressed through project ownership

Natural R01 ecology spawning remains intentionally gated. A command-spawned review fixture is not promoted into an authored production spawn merely because the entity is registered.

## Verification checkpoint

Code checkpoint: `a6268aede5087d9d335af020c0d5fe7c97a2a7e6`

Build Openworld RPG #358 / run `38348777599`: SUCCESS.

Completed by CI:

- pinned gameplay dependency verification: PASS
- pinned creature dependency verification: PASS
- clean unit test + build: PASS
- R01 creature registry inspection: PASS
- Earthloong dependency inspection: PASS
- bootstrap JAR verification: PASS
- core-profile dedicated server smoke: PASS
- gameplay-profile dependency server smoke: PASS
- gameplay-profile client startup/render smoke: PASS
- M0 playtest JAR: PRODUCED
- R01 integration playtest JAR: PRODUCED
- Modrinth playtest pack: PRODUCED
- output artifact upload: PASS

## Acceptance state after this checkpoint

- SOURCE ASSET ACQUIRED: YES
- SOURCE HASH VERIFIED: YES
- LICENSE EVIDENCE RECORDED: YES
- ANIMATION CONTRACT VERIFIED: YES
- SOURCE FILE PINNED IN REPO: YES
- RUNTIME ENTITY BOUND: YES
- SERVER COMBAT RUNTIME BOUND: YES
- SOURCE ANIMATION RUNTIME BOUND: YES
- POISON RUNTIME BOUND: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT STARTUP: YES
- CLIENT WORLD JOIN: NOT RUN
- MINECRAFT SCALE ACCEPTED: NO
- GROUND CONTACT ACCEPTED: NO
- ATTACK VISUAL/RANGE ACCEPTED: NO
- NATURAL R01 SPAWN ACCEPTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Manual client review path

Use a disposable gameplay world with cheats enabled:

```mcfunction
/gamemode survival
/summon openworld_rpg:meadow_viper ~ ~ ~
```

Review before promoting presentation/spawn acceptance:

1. Idle and Walk retain the source snake motion language without mesh collapse or skinning artifacts.
2. The body is not visibly floating or buried at the current 0.35 review scale.
3. Entering 3.5 blocks produces a readable coil/warning before an unprovoked bite.
4. Moving out of warning space before commit de-escalates the Viper.
5. A direct hit against the Viper engages it immediately.
6. Coil Bite contact visually agrees with the server hit near the source attack's lunge/contact peak and does not rely on an invisible extension beyond the legal 2.2-block range.
7. The Viper enters recovery and then creates roughly 2 blocks of space rather than staying glued to the player's feet.
8. Jump animation is visible when airborne outside the active bite.
9. Three poorly handled bites on a baseline-WIL build are sufficient to demonstrate the intended Poison buildup lesson; guard/perfect guard must reduce/nullify buildup according to the canonical defense result.
10. Poison persists for 8 s, applies its fixed source-Lv damage budget and reduces received healing by 20%.

Do not mark scale, ground contact, visual attack reach, natural spawn or final presentation complete until this client-world review is actually performed.
