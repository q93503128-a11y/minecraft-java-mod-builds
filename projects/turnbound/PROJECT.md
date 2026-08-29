# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.5`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62 stable artifact
- Java 25
- Gradle 9.2.1
- ModDevGradle 2.0.143
- UI Lib 21.1.1 retained as optional client-side UI dependency.
- GeckoLib 5.5.3 retained for authored hero/enemy model and animation phase.

## Design sources
- MasterDocs v0.4 remains the game direction / number rules / character canon.
- `DESIGN_DELTA_ALPHA5.md` is the current implementation canon for battle HUD, camera and P0 flee where it intentionally narrows or corrects the older presentation wording after real client testing.
- `REFERENCE_STUDY_ALPHA4.md` retains external reference research history.

## First playable P0 acceptance
- Turn threshold 1000.
- Gauge overflow survives an action.
- Natural consecutive actions are allowed without an arbitrary count cap.
- Decision time does not advance logical combat time.
- Cooldowns tick on the owner’s later regular turns.
- Basic actions may be non-damaging.
- 1–4 allies / 1–5 enemies are supported; playable test scenario is 4 allies vs 5 enemies.
- Damage, heal, barrier, gauge manipulation, death, revive, redirect, counter and simple status effects are represented.
- P01–P04 core kits are playable.
- Enemy turns are server-authoritative; AUTO uses deterministic role-aware priorities.
- Battle speed 1.0x/2.0x changes presentation delay only, not logical outcomes.
- Battle movement and ordinary world interactions are locked while a session exists, including result state until explicit return.
- General P0 field encounter allows deterministic mid-battle flee; `/turnbound leave` remains an emergency/test cleanup command.
- Deterministic timeline preview does not mutate battle state.
- `/turnbound p0` runs deterministic diagnostic simulation.
- `/turnbound battle` starts the playable P0 battle session.
- Java 25 clean test/build must be green before a test JAR is handed off.

## alpha.5 battle HUD acceptance
- 3D world remains visually dominant; no full-screen battle veil.
- No permanent giant ally/enemy button columns.
- Ally status is a compact bottom party strip; enemy summary is compact at the upper-right.
- Action buttons appear contextually on the right only when a player-controlled ally can act.
- Target selection is tied to a world-space marker as well as HUD highlight.
- Internal combat IDs/events are not player-facing text.
- P0 ArmorStand custom names are hidden.
- Battle UI keeps positive in-viewport bounds at high GUI scale / low logical resolution.
- LMB drag on empty scene orbits; mouse wheel zooms; RMB cancels selection; Esc opens battle settings.
- A toggles AUTO, X toggles speed, R flees/returns, 1–5 chooses actions, Tab/Shift+Tab cycles valid targets.

## P0 camera acceptance
- Battle entry switches to third-person-back presentation camera.
- Default detached distance 11 blocks.
- Zoom clamp 6–18.
- Pitch clamp -10°–58°.
- Camera state restores when battle snapshot closes.
- P0 commander body is hidden server-side for the temporary third-person battle view and restored on cleanup.

## Presentation boundary
- ArmorStand actors are temporary placement/motion stand-ins, not final-quality characters.
- alpha.5 widens their formation and adds basic role silhouettes only to make battle spacing testable.
- Final character meshes, rigs, clips, VFX, damage/heal numbers, model outline and ground targeting rings belong to the GeckoLib presentation phase.

## UI/asset rule
Do not ship improvised Minecraft-grey-box UI as the final interface. The current HUD uses TURNBOUND `Dark Glass + Ivory` color rules and reference-derived information hierarchy. Proprietary R_PG UI art/code is reference-only and is not copied.
