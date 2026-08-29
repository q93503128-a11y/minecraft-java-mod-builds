# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.8`

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
- 1–4 allies / 1–5 enemies supported; diagnostic P0 is 4 vs 5.
- Damage, heal, barrier, gauge manipulation, death, revive, redirect, counter and simple status effects represented.
- P01–P04 core kits playable.
- Enemy turns and AUTO are server-authoritative.
- x1/x2 affects presentation only.
- General field battles currently retain deterministic flee; boss/event locks remain encounter data.

## alpha.7 battle presentation acceptance
- The 3D battlefield remains visually dominant; no full-screen battle veil.
- Party state is a thin bottom strip; enemy state is a compact top-right summary.
- Timeline is a thin top-center strip.
- Skills appear only when the current ally can act.
- All skills require explicit confirmation; no SELF/ALL click-to-fire exception.
- Single targets can be selected by clicking the projected 3D combatant, with HUD/Tab as secondary input.
- Battle camera orbits the arena center and safe-arena search avoids nearby walls/trees.

## alpha.8 Aster March field / encounter acceptance
- World canonical name: `Aster March`; v0.1 world plan remains 1024×1024, X/Z `-512..511`, base surface Y 64.
- The first implemented cell is only `Southgate Meadow A01`: X `-32..31`, Z `128..191`, Y 64, size 64×64.
- It sits inside the canonical Southgate Meadow range and directly beyond Radia's planned south edge.
- The normal game flow does not use random encounters as the default.
- Hostile parties visibly exist in the authored field before combat.
- A01 uses the real v0.4 enemy IDs/data for `E001 + E002 + E005`.
- Field behavior separates patrol, alert/chase and engagement from Combat Rules.
- Entering engagement distance transitions into a server-authoritative battle session.
- The visible field party and battle party match.
- Battle cleanup returns the player to the exact pre-battle field position/yaw/pitch.
- Victory removes the encounter for the current field session; non-victory restores it after a short grace period.
- E001 `끈질김` and E005 `전열 정비` operate in the shared battle rules, not as field-only fake effects.
- A01 contains a Radia-facing entrance, authored road, irrigation stream/bridge, encounter clearing, relay ruin, disguised boundaries and a blocked future-cell continuation.
- Field block breaking/placing and vanilla survival item use are not progression systems.
- `/turnbound field` is the canonical alpha.8 gameplay test entry; `/turnbound battle` remains a combat diagnostic shortcut.

## Minecraft player shell rule
TURNBOUND is not a survival game. The Minecraft Player entity is an exploration/input/camera/session shell, not the party's combat health object.

- Vanilla player hearts are not a gameplay HP system.
- Vanilla incoming damage to the player shell is ignored.
- Vanilla hunger is kept full and is not a resource.
- Player-health and food HUD layers are hidden.
- Combat health is exclusively `CombatantState.hp` on party/enemy combatants.
- Do not create a second survival-health economy beside the party RPG combat model.

## World contract
- The game world is a fixed authored RPG world, not vanilla infinite-world progression.
- v0.1 canonical world is Aster March 1024×1024; alpha.8 deliberately builds only one 64×64 Southgate cell before expansion.
- Later cells may use licensed external maps/structures + WorldEdit/structure integration, with routes and combat areas re-authored for gameplay.
- World boundaries should be disguised by terrain/architecture/story gating rather than a naked visible world border.
- The first field must support visible enemy avoidance as well as engagement.

## Reference boundary
- R_PG/R_PG X: spatial/information/pacing reference only; proprietary assets/code/layout values are not copied.
- TurnBasedMC, Soulbound and Cobblemon: open-source architecture/UX study only unless compatible licensed reuse is explicitly recorded.
- Craftics: behavior reference only.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- Final JAR metadata/classes/resources verified before handoff.
