# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.7`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62
- Java 25
- Gradle 9.2.1
- ModDevGradle 2.0.143
- GeckoLib 5.5.3 retained for the authored model/animation phase

## Core combat acceptance
- Turn threshold 1000.
- Gauge overflow survives actions.
- Natural consecutive actions have no arbitrary count cap.
- Decision time does not advance logical combat time.
- Cooldowns tick on the owner's later regular turns.
- Basic actions may be non-damaging.
- 1–4 allies / 1–5 enemies supported; P0 is 4 vs 5.
- Damage, heal, barrier, gauge manipulation, death, revive, redirect, counter and simple status effects represented.
- P01–P04 core kits playable.
- Enemy turns and AUTO are server-authoritative.
- x1/x2 affects presentation only.
- General field battles allow deterministic flee; boss/event locks are encounter data.

## alpha.7 battle UX acceptance
- The battlefield, not the HUD, remains visually dominant.
- Battle creation finds a nearby open arena instead of anchoring the encounter at the exact command/player block.
- During battle the invisible Minecraft player shell is the battlefield-center camera pivot; its original field position/yaw/pitch are restored on cleanup.
- Default camera distance is materially closer than alpha.6, orbit is smooth and responsive, and zoom stays bounded.
- Party state is a thin bottom strip; enemy state is a compact top-right summary; timeline is a thin top-center strip.
- Contextual skills occupy only the right edge and do not overlap Auto/speed/flee or the party strip.
- Vanilla hotbar and crosshair are hidden during battle.
- Skill selection and action execution are separate phases for every skill, including SELF and ALL targets.
- Clicking a skill never emits an ACT command by itself.
- A single-target skill can select a valid combatant by clicking its projected 3D model position; HUD bars and Tab remain fallback selectors.
- Target HUD highlight and the 3D focus marker refer to the same target ID.
- Enter or the explicit confirm control commits the pending action.
- RMB cancels the pending skill/target selection.
- High GUI scale / low logical resolution must never create zero-sized or off-viewport HUD rectangles.

## Minecraft player shell rule
TURNBOUND is not a survival game. The Minecraft Player entity is an exploration/input/camera/session shell, not the party's combat health object.

- Vanilla player hearts are not a gameplay HP system.
- Vanilla incoming damage to the player shell is ignored.
- Vanilla hunger is kept full and is not a resource.
- Player-health, food, armor, air and XP survival HUD layers are hidden.
- Combat health is exclusively `CombatantState.hp` on party/enemy combatants.
- Do not create a second survival-health economy beside the party RPG combat model.
- Future field hazards must use authored RPG rules, encounter triggers or checkpoint consequences instead of vanilla hearts/hunger.

## Reference boundary
- R_PG/R_PG X: spatial/information/pacing reference only; proprietary assets/code/layout values are not copied.
- TurnBasedMC, Soulbound and Cobblemon: open-source architecture/UX study only unless compatible licensed reuse is explicitly recorded.
- Craftics: behavior reference only.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- Final JAR metadata/classes/resources verified before handoff.
