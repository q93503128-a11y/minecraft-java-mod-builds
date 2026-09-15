# 06 — Valkyrien Skies / Genesis Reboot

Status: canonical reboot architecture for `0.2.0-alpha.2`; physical starter-craft build/server assembly verified, live flight acceptance still required.

## Why the 26.2 vehicle stack was abandoned

The previous 26.2 implementation rendered the starter craft through a `Display.ItemDisplay`-based runtime entity and implemented flight, riding, collision, camera and Earth/orbit transfer in EARTH TO STARS itself. Live playtest showed that this was the wrong foundation: cockpit placement was visibly wrong, input semantics were inconsistent, block collision was absent, motion felt artificial, and `orbital_space` was only a technical void dimension.

This project will not continue extending that custom vehicle/physics implementation. The old source remains in Git history and temporarily remains in the tree for forensic reference, but the reboot Gradle `sourceSets` do not compile or package it.

## Reboot stack

| Layer | Pinned baseline | Role | License / source use |
| --- | --- | --- | --- |
| Minecraft | 1.20.1 | ecosystem target | Mojang runtime |
| Forge | 47.4.0 | loader | build/runtime dependency |
| Valkyrien Skies | 2.4.10 | moving block ships, physics and world/entity collision | LGPL-3.0-only; runtime dependency/API reference |
| Genesis | 1.20.1-0.7.3 | physical space dimension and VS ship Earth/space transfer | Apache-2.0; runtime dependency and reference implementation |
| VLib | 1.20.1-0.1.0+forge | VS addon utility required by Genesis | Apache-2.0; runtime dependency |
| Zero Point Systems | 1.20.1-2.5.1 | cockpit/controller and finite starter Power Cell | MIT; runtime dependency/reference |
| Zero Point Labs | 1.20.1-1.5.0 | VS spacecraft thrusters and gyroscope hardware | Apache-2.0; runtime dependency/reference |
| Lodestone | 1.20.1-1.6.4.1 | dependency used by the space stack | LGPL-3.0-only; runtime dependency |
| Fusion | 1.2.12 Forge 1.20.1 | ZPS client model dependency when required by the resolved runtime graph | third-party client runtime dependency |

The first integration baseline deliberately follows Genesis' current 1.20.1 source contract: Forge 47.4.0, Java 17, Valkyrien Skies 2.4.x. We start with VS 2.4.10 because Genesis 0.7.3 itself is built against it. A Forge/VS upgrade is evaluated only after the known-good stack and live starter-craft loop are stable; newer version availability alone is not a reason to move the baseline.

## Non-negotiable architecture rules

1. **No custom replacement for Valkyrien Skies physics.** EARTH TO STARS may tune gameplay and integrate systems, but it does not recreate ship collision, transform physics or player-on-ship physics.
2. **No Display Entity spacecraft as the gameplay vehicle.** Starter and later ships must be actual VS block ships.
3. **Cockpit controls use proven external hardware first.** The starter craft baseline is a real VS ship containing a ZPS rideable controller, ZPL propulsion and ZPL gyroscopes. Custom controls are added only where these systems cannot satisfy the game design.
4. **Genesis owns the first real space transition.** We do not rebuild a fake black void dimension or teleport only the player while leaving the ship behind.
5. **Ship modules remain server-authoritative game state.** EARTH TO STARS progression, ownership, economy, unlocks and combat rules wrap the external physical ship rather than replacing its physics. The physical representation may be a VS block ship while the game's module/progression truth remains one authoritative logical ship state.
6. **Creative-test access remains mandatory.** Any new EARTH TO STARS player-facing test item must be available through its creative tab; routine `/give` commands are not the primary test workflow.
7. **External source and license provenance stays recorded.** Runtime dependency does not mean copying assets/code without tracking terms.
8. **External optional compatibility failures must not be hidden with fake dependencies.** Normal ETS client/server smoke runs disable Forge GameTest discovery because released VS carries optional GameTest holders that may mention unrelated soft dependencies such as Create. This is separate from real ETS GameTests and live runtime verification.

## 0.2.0-alpha.2 starter-craft implementation

The first physical starter craft is now assembled from real blocks and then converted through the released VS 2.4.10 `ShipAssembler` API. Its current technical loadout is:

- real ZPS Octo Controller seat/input source,
- finite ZPS Power Cell,
- ZPL Ion Modulator + Thruster Exhaust propulsion pairs,
- opposed ZPL gyroscopes for yaw,
- ETS flight core and redstone control nodes,
- ETS power bus that transfers Forge Energy from the Power Cell to ZPL ion modulators,
- W/A/S/D plus arrow-channel mapping sourced from the occupied ZPS controller.

VS remains responsible for physical ship relocation/collision/physics. ZPL remains responsible for actual thrust/gyro behavior. ETS only supplies the game-specific assembly, input routing, power distribution and later progression rules.

Current automated gates have verified:

- reboot source-set isolation from the retired 26.2 custom vehicle stack,
- clean test/build and production JAR packaging,
- dedicated Forge server boot with VS + Genesis + VLib + ZPS + ZPL,
- real VS starter-craft assembly on the server,
- presence of Octo controller, finite battery, ZPL thrusters and gyroscopes in that assembly,
- headless graphical client startup far enough to initialize Forge 47.4.0 / Minecraft 1.20.1, the external stack and EARTH TO STARS `0.2.0-alpha.2`.

The released VS 2.4.10 line can emit optional-compat missing-class warnings when mods such as Create/Mekanism/ComputerCraft are absent. It also documents that its Forge 1.20.1 sculk vibration interface mixin may fail to apply without crashing. These messages are tracked as external-stack warnings rather than being hidden by installing unrelated mods solely for CI.

Automated boot is **not** equivalent to play acceptance. The following remain `NOT PLAYTESTED` until a real client session verifies them:

- actual seat position and cockpit usability,
- forward/reverse/strafe/lift/yaw direction and strength,
- terrain/entity collision feel,
- player movement on the physical ship while unseated,
- camera readability and hull silhouette,
- full occupied-craft Genesis Earth→space transition,
- continued control after the transition,
- live multiplayer behavior.

## First vertical slice after stack boot

The next implementation unit is not a generic vehicle prototype. It is one playable starter craft:

- deploy/spawn a pre-authored real VS block ship,
- cockpit contains the proven ZPS controller,
- ZPL thrusters + gyroscope produce the motion,
- player sits at the actual cockpit seat and can move around the physical ship when not seated,
- hull collides with terrain/world/entities through VS,
- controls are mapped and playtested before balance work,
- the complete ship crosses the Genesis Earth/space boundary,
- space presentation comes from Genesis first, then EARTH TO STARS art/gameplay layers are added on top.

Acceptance is a live playable sequence, not a compile result: enter craft → sit in cockpit → take off → collide correctly → fly naturally → cross to space with the craft intact → continue controlling it in space.

Do not expand Moon/asteroid content merely because the automated backend gates pass.

## Alternatives evaluated

- **Starlance:** useful VS spacecraft addon, but its current documented space integrations prioritize Cosmic Horizons / Ad Astra. It is not needed for the first Genesis baseline and can be evaluated later.
- **Ad Astra Adapted on 26.2:** preserves the newer Minecraft version but does not provide the same mature VS block-ship physics + Genesis physical-space foundation. The project prioritizes game quality over staying on 26.2.
- **Continue custom 26.2 physics:** rejected after live acceptance failures. It duplicates solved vehicle/physics work and violates the project's reuse-first rule.
