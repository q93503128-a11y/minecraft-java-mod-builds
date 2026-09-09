# Fishing Game

A Minecraft Java 26.2 Fabric fishing progression game focused on a very simple loop:

`catch -> collect -> sell -> improve rod -> hunt bigger/rarer fish`

The project intentionally starts with fishing feel and collection instead of pets, gacha, many currencies or stacked upgrade menus.

## Current alpha scope

Alpha 0.1.0 starts with the server-authoritative fishing session and a small test species catalog. The vanilla fishing hook is temporarily used as the cast anchor while catch logic is owned by this mod.

### Current interaction

1. Hold a normal Minecraft fishing rod.
2. Right click to cast into water.
3. The bite timer starts only after the hook actually reaches water.
4. When a fish bites, hold right click to increase line tension and release it to let tension fall.
5. Keeping tension in the safe range advances catch progress.
6. Excess tension breaks the line; too little tension loses progress.

The held-use input is sent to the server as state, while the server owns tension, progress, fish selection and the final catch result. The current action-bar telemetry is still temporary; a proper fishing HUD and physical hooked-fish presentation are core follow-up work.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.5
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1
- Essential: optional external convenience mod, not a dependency

## Development rules

Repository root `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`, `docs/QUALITY_STANDARD_GAME_DESIGN.md` and this project's `PROJECT.md`/`docs/GAME_DESIGN.md` are the working references.

Do not turn temporary action-bar telemetry or the vanilla hook anchor into the final presentation. Fishing feel, real fish presentation, sound and HUD are core gameplay work, not polish to postpone until the end.
