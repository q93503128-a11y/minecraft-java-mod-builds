# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1` target for M0 bootstrap
- Build plugin: `ModDevGradle 2.0.143` target for M0 bootstrap
- Final JAR: `earth_to_stars-0.1.0-alpha.1.jar`
- Existing-world compatibility: before first playable alpha, save schema may change deliberately; from first playable alpha onward registry IDs, save roots, module IDs and migration rules become compatibility contracts.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency at project registration. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` after M0 bootstrap
- GameTest task: register during M0/P0 for ship/runtime/network contracts
- Server smoke-test task: `runServer` after M0 bootstrap
- Client smoke-test task: `runClient` after M0 bootstrap

## Project identity

EARTH TO STARS is a large-scale science-fiction Minecraft survival/expansion game built around one connected fantasy:

> **Start on the vanilla Overworld as Earth, build industry and a small craft, cross the atmosphere under player control, expand into orbit and other celestial regions, recover resources and technology, and grow one modular ship from a fragile vehicle into a mobile home, factory and warship.**

The project is not a generic tech mod, a planet menu, a collection of colored ores, or a Space Engineers clone. Minecraft survival remains the foundation; the playable world expands upward and outward.

## Locked product decisions

1. `minecraft:overworld` is Earth and remains useful throughout the game.
2. The primary progression route must be completable without mandatory Nether or End visits.
3. Nether and End are optional side routes, shortcuts, specialist-material sources and late-game variants, never mandatory gates for the main space progression.
4. Surface-to-space travel must feel continuous to the player. Internally, dimension/layer transitions may be used to protect performance and compatibility, but the presentation must hide abrupt menu-teleport behavior.
5. Player ships use the **B-type modular ship architecture**. A ship is an authoritative ship object with modules, hardpoints, systems and an optional linked interior instance. It is not an arbitrary moving block contraption.
6. Ship progression must change capabilities and play patterns, not only increase numbers.
7. The same weapon hardpoint may support manual and automatic control when appropriate. Manual control should reward skill; automation should make solo play viable and reduce multiplayer role pressure.
8. Multiplayer is a first-class requirement. Important game state is server-authoritative from the beginning.
9. Solo play must remain viable. Missing crew roles are covered by automation, AI assistance or reduced management load rather than forced multi-boxing.
10. Multiplayer roles are opportunities, not jobs. No player should be trapped in repetitive gauge-watching or maintenance clicks.
11. Ship power, ammo, sensors and target acquisition use centralized/batched simulation where possible; individual modules must not independently perform expensive broad world scans every tick.
12. Repeat-adjusted content values are data-driven. Code owns rules; data owns content.
13. Visual direction is design-gated. Final UI, ship forms, modules, weapons, planets, VFX and sound are not invented from generic AI sci-fi styling.
14. External references and assets are actively used when licensing permits; source, author, license and modifications are recorded in `THIRD_PARTY_ASSETS.md`.
15. `docs/QUALITY_STANDARD.md`, `docs/QUALITY_STANDARD_GAME_DESIGN.md`, the repository `AGENTS.md`, and the uploaded Moon Seungjun High-Quality Development Playbook are production constraints, not optional inspiration.
16. Technical success is not product completion. Real Minecraft play, multiplayer validation where available, visual review, performance measurement and build/JAR validation are required according to risk.

## Core loop

```text
Earth survival / resource acquisition
→ practical industry and launch capability
→ atmosphere / orbit breakthrough
→ orbital salvage, science and combat
→ ship/module upgrade
→ Moon / asteroid / planetary expedition
→ unique resources and environmental constraints
→ stronger mobility, automation, weapons and production
→ larger-range expeditions
→ deeper space
→ new discoveries feed back into Earth, bases and ship growth
```

Every major feature must attach to this loop. Independent feature creep is rejected by default.

## Multiplayer authority

Server authority includes at minimum:

- ship ownership and membership
- ship transform accepted by the current movement backend
- installed modules and hardpoints
- module damage and repair state
- energy production/storage/consumption
- fuel and propellant
- ammunition and weapon cooldowns
- target eligibility and hit results
- mining/resource transfer
- recipes and production results
- progression and celestial access
- ship interior assignment
- save data and migrations

Clients provide input, rendering, animation, UI and safe prediction only. A client never tells the server that damage, resources, travel completion or crafting already succeeded.

## Current phase

`M0 — CANON LOCKED / BUILD BOOTSTRAP NEXT`

This project is ready for technical production bootstrap. No claim is made yet that a runnable JAR, seamless space transition, multiplayer session, modular ship or turret is implemented or tested.
