# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.12`

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

## Battle UX acceptance
- 3D battlefield remains dominant; no full-screen battle veil.
- Camera pivot is the average position of actual combatant anchors.
- Detached third-person camera position and rendered yaw/pitch stay synchronized.
- Default camera: pitch 22°, distance 11; yaw 360°, pitch -10°~58°, distance 6~18.
- All skills require explicit final confirmation.
- Single-target skills do not silently preselect the first valid target.
- Single targets can be selected by clicking the projected 3D combatant body; HUD/Tab remain secondary inputs.
- Skill actions use a two-column contextual dock and hover detail.
- Party/enemy state remains edge HUD and must not obscure the center battle scene.

## Southgate Meadow Chapter 1 acceptance
- A01: X `-32..31`, Z `128..191`, base Y `64`, 64×64.
- Five visible normal encounters: `ENC_M01~M05`.
- E001/E002/E005 retain alpha.8 canonical data.
- E003/E004 remain distinct speed-pressure and defense-anchor roles.
- All visible encounter compositions use the same definitions as their battle sessions.
- Field phases: PATROL → ALERT/chase → ENGAGE.
- Engagement transitions to the same server-authoritative battle session.
- Battle cleanup returns to exact pre-battle field position/yaw/pitch.
- Victory removes only that encounter for the current field session.
- Flee/defeat restores only that encounter after grace.
- First-clear rewards are idempotent.
- Five normal clears unlock B01 Graul at the south blockade.
- B01 victory marks Chapter 1 cleared.

## alpha.12 field UX / travel acceptance
- `남문 정찰관` opens a world-first quest panel, not a full-screen opaque menu.
- Quest UI shows current objective, five normal encounter states, B01 lock/clear state, cumulative XP/Gold.
- Field UI state is server-authoritative and synchronized with a dedicated payload.
- Returning from a victorious encounter opens a reward result panel with first-clear XP/Gold and next objective.
- A01 relay requires physical interaction before it becomes a Fast Travel destination.
- A02 relay cannot activate before Chapter 1 clear.
- B01 clear removes the A02 north lock and allows the player into South Road A02.
- A02 is contiguous with A01: X `-32..31`, Z `192..255`, base Y `64`.
- Physically reaching and activating A02 relay enables two-way Fast Travel between the two activated relays.
- Locked relay destinations are visible but cannot be selected.
- `/turnbound status` opens the same server-backed quest panel.
- Field block/item/vanilla combat interactions remain suppressed.

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
Follow the established P2→P3→P4 phase order rather than adding disconnected features.
- P2: fixed world, visible enemies, encounter, NPC, quest, reward, region travel.
- P3: level/star progression, ownership, gacha, duplicate conversion, three normal equipment slots, gold enhancement, party UI, CP.
- P4: ★6 awakening, signature equipment, advanced AUTO, expanded bosses/story/regions/characters, high-difficulty repeatable content.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- Final JAR metadata/classes/resources verified before handoff.
