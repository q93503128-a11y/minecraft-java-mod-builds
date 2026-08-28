# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.1`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62 stable artifact
- Java 25
- Gradle 9.2.1
- ModDevGradle 2.0.143
- UI Lib 21.1.1 (client-only UI framework)
- GeckoLib 5.5.3 (animation runtime, character phase)

## P0 acceptance
- Turn threshold 1000.
- Gauge overflow survives an action.
- Natural consecutive actions are allowed without an arbitrary count cap.
- Decision time does not advance logical combat time.
- Cooldowns tick on the owner’s later regular turns.
- Basic actions may be non-damaging.
- 1–4 allies / 1–5 enemies.
- Damage, heal, barrier, gauge manipulation, death, revive.
- P01–P04 core kits represented.
- Deterministic timeline preview does not mutate battle state.
- `/turnbound p0` can run the deterministic diagnostic simulation.

## UI/asset rule
Do not ship an improvised Minecraft-grey-box UI as the final interface. Use the external UI framework and licensed/open assets registered in `EXTERNAL_ASSETS.md`. Proprietary game UIs are reference-only.
