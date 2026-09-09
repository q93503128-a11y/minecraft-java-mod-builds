# Fishing Game

- Slug: `fishing-game`
- Mod ID: `fishinggame`
- Namespace: `fishinggame`
- Display name: `Fishing Game` (working title; technical id remains stable)
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `Fabric`
- Loader version: `0.19.5`
- Fabric API: `0.159.0+26.2`
- Loom: `1.17.19`
- Gradle: `9.5.1`
- Final JAR: `fishing-game-0.1.0-alpha.1.jar`
- Existing-world compatibility: no compatibility promise before first playable save-bearing build
- Required dependencies: Fabric API
- Optional external mods: Essential for friend-hosted multiplayer convenience
- Forbidden bundled dependencies: Essential and third-party assets without redistribution permission
- Datagen task: not configured yet
- GameTest task: not configured yet
- Server smoke-test task: not configured yet
- Client smoke-test task: not configured yet

## Loader decision

Fabric is selected for this project because Minecraft 26.2 is currently supported, Essential 26.2 Fabric is available for convenient friend multiplayer, the current Fabric toolchain is stable enough for Java 25, and useful fishing references exist in the Fabric ecosystem.

This is a project-specific choice, not a repository-wide default. Essential is never a gameplay authority; server-side game logic must work the same on integrated, LAN, Essential-hosted, and dedicated servers.

## Current implementation target

The first implementation unit is the fishing-session foundation:

`cast -> wait -> bite -> reel/tension -> catch or line break`

The server owns the session, fish roll, resistance, tension, progress and result. Client visuals are allowed to predict/present but never finalize a catch.
