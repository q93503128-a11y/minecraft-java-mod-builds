# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.10`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62
- Java 25
- Gradle 9.2.1
- ModDevGradle 2.0.143
- GeckoLib 5.5.3 retained for authored model/animation phase

## Combat rules contract
- Turn threshold 1000; action subtracts 1000 rather than resetting Gauge.
- Gauge overflow survives actions and natural consecutive turns have no arbitrary cap.
- Player decision time does not advance logical combat time.
- Cooldowns tick on the owner's later regular actions, not reactions.
- Basic actions may be non-damaging and have cooldown 0.
- 1–4 allies / 1–5 enemies.
- Damage, heal, barrier, gauge manipulation, death, revive, redirect, counter and status effects are represented.
- P01–P04 are playable.
- Enemy turns and AUTO are server-authoritative.
- 1×/2× changes presentation only.
- General field battles retain deterministic flee; boss/event locks are encounter data.

## Battle UX acceptance — alpha.10
- 3D battlefield remains dominant; no full-screen battle veil.
- Camera pivot is the average position of actual combatant anchors.
- Canonical default camera: yaw encounter-facing, pitch 22°, distance 11 blocks.
- Camera clamps: pitch -10°..58°, distance 6..18 blocks; Minecraft third-person collision may shorten closer to 4.5.
- Camera input: horizontal drag 0.18°/px, vertical drag 0.15°/px, wheel 0.75 block/step.
- Detached third-person camera position and rendered yaw/pitch use the same smoothed player-shell rotation.
- All skills require explicit final confirmation.
- Single-target skills do not silently preselect the first valid target.
- Single targets can be selected by clicking the projected 3D combatant body; HUD/Tab remain secondary inputs.
- 3D click picking uses a feet-to-head body capsule rather than one projected center point.
- Selected target updates both world marker and HUD emphasis.
- Skill actions use a two-column contextual dock at the right edge.
- Hovering any visible skill shows name, target type, base cooldown, remaining cooldown and player-facing effect description.
- Party/enemy state remains edge HUD and must not obscure the center battle scene.

## Southgate Meadow A01 acceptance
- First field cell: X `-32..31`, Z `128..191`, base Y `64`, 64×64.
- Visible encounter party currently uses real `E001 + E002 + E005` data for the first integrated field-loop test.
- Field phases: PATROL → ALERT/chase → ENGAGE.
- Random encounter is not the normal flow.
- Engagement transitions to the same server-authoritative battle session.
- Battle cleanup returns to exact pre-battle field position/yaw/pitch.
- Victory removes encounter for the current field session; non-victory restores it after grace.
- Field block breaking/placing and vanilla survival item progression are disabled.
- `/turnbound field` is the normal alpha field entry; `/turnbound battle` remains diagnostic.

## Minecraft player shell rule
TURNBOUND is not a survival game.
- Vanilla player hearts are not gameplay HP.
- Incoming vanilla damage to the shell is ignored.
- Hunger is kept full and is not a resource.
- Survival HUD is hidden.
- Combat HP belongs exclusively to party/enemy CombatantState.

## World contract
- Fixed authored RPG world, not vanilla infinite progression.
- v0.1 target: Aster March 1024×1024.
- World boundaries are disguised by terrain/architecture/story gating.
- External maps/structures may be used only with license review and gameplay re-authoring.

## Production gate
Follow `01_세부기획서_v0.4.md`, `02_수치규칙위키_v0.4.md`, and `03_캐릭터설계위키_v0.4.md` phase order rather than adding disconnected features.
- P2: fixed world, Southgate encounter catalog, visible enemies, NPC, quest, reward, region travel, B01/Chapter 1.
- P3: save schema 4, level/star progression, ownership, gacha, duplicate conversion, three normal equipment slots, gold enhancement, party UI, CP.
- P4: ★6 awakening, signature equipment, P01–P08, advanced AUTO, expanded bosses/story/regions/characters, high-difficulty repeatable content.
- Presentation is developed alongside each feature set; ArmorStand stand-ins do not count as final presentation completion.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- Final JAR metadata/classes/resources verified before handoff.
