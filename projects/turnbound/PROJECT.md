# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.6`

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

## alpha.6 presentation acceptance
- alpha.4/5 large side panels, framed target-card walls and legacy battle button classes are not reused.
- The 3D battlefield remains visually dominant; no full-screen battle veil.
- Party state is a thin bottom strip; enemy state is a compact top-right summary.
- Timeline is a thin top-center strip.
- Skills appear only when the current ally can act.
- Auto/speed/flee remain small bottom-right secondary controls.
- Internal instance IDs / TURN_READY / pulse data never appear in player-facing HUD.
- Target HUD and the 3D focus marker must refer to the same target ID.
- High GUI scale / low logical resolution must never create zero-sized or off-viewport HUD rectangles.

## Minecraft player shell rule
TURNBOUND is not a survival game. The Minecraft Player entity is an exploration/input/camera/session shell, not the party's combat health object.

- Vanilla player hearts are not a gameplay HP system.
- Vanilla incoming damage to the player shell is ignored.
- Vanilla hunger is kept full and is not a resource.
- Player-health and food HUD layers are hidden.
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
