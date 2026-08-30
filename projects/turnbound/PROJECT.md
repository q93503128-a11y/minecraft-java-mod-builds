# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.9`

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

## Battle UX acceptance — alpha.9
- 3D battlefield remains dominant; no full-screen battle veil.
- Camera pivot is the average position of actual combatant anchors, not an arbitrary player viewpoint.
- Detached third-person camera position and rendered yaw/pitch use the same smoothed player-shell rotation.
- Default camera follows the v0.4 recommended orbit range; player can freely yaw 360° and zoom within the safe range.
- All skills require explicit final confirmation.
- Single-target skills do not silently preselect the first valid target.
- Single targets can be selected by clicking the projected 3D combatant body; HUD/Tab remain secondary inputs.
- 3D click picking uses a feet-to-head body capsule rather than one projected center point.
- Selected target updates both world marker and HUD emphasis.
- Skill actions use a two-column contextual dock at the right edge.
- Standard viewport skill buttons retain a practical click footprint close to the v0.4 56–72 logical-px recommendation.
- Hovering any visible skill shows skill name, target type, base cooldown, remaining cooldown and player-facing effect description.
- Party/enemy state remains edge HUD and must not obscure the center battle scene.

## Southgate Meadow A01 acceptance
- First field cell: X `-32..31`, Z `128..191`, base Y `64`, 64×64.
- Visible encounter party uses real `E001 + E002 + E005` data.
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

## Development target
Follow `01_세부기획서_v0.4.md` phase order rather than adding disconnected features.
- P2: fixed world, visible enemies, encounter, NPC, quest, reward, region travel.
- P3: level/star progression, ownership, gacha, duplicate conversion, three normal equipment slots, gold enhancement, party UI, CP.
- P4: ★6 awakening, signature equipment, advanced AUTO, expanded bosses/story/regions/characters, high-difficulty repeatable content.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- Final JAR metadata/classes/resources verified before handoff.
