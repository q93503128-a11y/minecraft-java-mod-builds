# 06 — Valkyrien Skies / Genesis Reboot

Status: canonical architecture reset for `0.2.0-alpha.1`.

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
| Zero Point Systems | 1.20.1-2.4.0 | cockpit/controller and ship control circuits | MIT; runtime dependency/reference |
| Zero Point Labs | 1.20.1-1.5.0 | VS spacecraft thrusters and gyroscope hardware | Apache-2.0; runtime dependency/reference |
| Lodestone | 1.20.1-1.6.4.1 | dependency used by the space stack | LGPL-3.0-only; runtime dependency |
| Fusion | 1.2.12 Forge 1.20.1 | ZPS client model dependency | third-party client runtime dependency |

The first integration baseline deliberately follows Genesis' current 1.20.1 source contract: Forge 47.4.0, Java 17, Valkyrien Skies 2.4.x. We start with VS 2.4.10 because Genesis 0.7.3 itself is built against it. A VS 2.4.11 upgrade is evaluated only after the known-good stack boots.

## Non-negotiable architecture rules

1. **No custom replacement for Valkyrien Skies physics.** EARTH TO STARS may tune gameplay and integrate systems, but it does not recreate ship collision, transform physics or player-on-ship physics.
2. **No Display Entity spacecraft as the gameplay vehicle.** Starter and later ships must be actual VS block ships.
3. **Cockpit controls use proven external hardware first.** The starter craft baseline is a real VS ship containing a ZPS rideable controller, ZPL propulsion and a ZPL gyroscope. Custom controls are added only where these systems cannot satisfy the game design.
4. **Genesis owns the first real space transition.** We do not rebuild a fake black void dimension or teleport only the player while leaving the ship behind.
5. **Ship modules remain server-authoritative game state.** EARTH TO STARS progression, ownership, economy, unlocks and combat rules wrap the external physical ship rather than replacing its physics.
6. **Creative-test access remains mandatory.** Any new EARTH TO STARS player-facing test item must be available through its creative tab; routine `/give` commands are not the test workflow.
7. **External source and license provenance stays recorded.** Runtime dependency does not mean copying assets/code without tracking terms.

## First vertical slice after stack boot

The next implementation unit is not a generic vehicle prototype. It is one playable starter craft:

- deploy/spawn a pre-authored real VS block ship,
- cockpit contains a ZPS Octo/Dodeca controller or the smallest proven controller that fits,
- ZPL thrusters + gyroscope produce the motion,
- player sits at the actual cockpit seat and can move around the physical ship when not seated,
- hull collides with terrain/world/entities through VS,
- controls are mapped and playtested before balance work,
- the complete ship crosses the Genesis Earth/space boundary,
- space presentation comes from Genesis first, then EARTH TO STARS art/gameplay layers are added on top.

Acceptance is a live playable sequence, not a compile result: enter craft → sit in cockpit → take off → collide correctly → fly naturally → cross to space with the craft intact → continue controlling it in space.

## Alternatives evaluated

- **Starlance:** useful VS spacecraft addon, but its current documented space integrations prioritize Cosmic Horizons / Ad Astra. It is not needed for the first Genesis baseline and can be evaluated later.
- **Ad Astra Adapted on 26.2:** preserves the newer Minecraft version but does not provide the same mature VS block-ship physics + Genesis physical-space foundation. The project prioritizes game quality over staying on 26.2.
- **Continue custom 26.2 physics:** rejected after live acceptance failures. It duplicates solved vehicle/physics work and violates the project's reuse-first rule.
