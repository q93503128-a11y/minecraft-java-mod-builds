# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.4`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.4.jar`
- Existing-world compatibility: before first playable alpha, save schema may change deliberately; from first playable alpha onward registry IDs, save roots, module IDs and migration rules become compatibility contracts.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` (`NOT RUN` at current gate)
- GameTest task: not yet registered; save/reload disk integration remains a later batched gate
- Server smoke-test task: `runServer` (`NOT RUN` at current gate)
- Client smoke-test task: `runClient` (`NOT RUN` at current gate)

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

## Current implementation baseline

Latest verified implementation commit: `492d8fa0536b23881591ad9a31b0501c7048b6e3`

GitHub Actions `Build earth-to-stars` run `34183711601` verified:

- M0 bootstrap regression
- P0-A authoritative ship kernel / persistence-codec regression
- P0-B movement transform / control lease regression
- P0-C Earth↔orbit transition-policy regression
- P0-D stable `ShipId → interior slot` allocation/layout JUnit
- duplicate/corrupt interior slot collision rejection
- server-global `InteriorSavedData` adapter compile
- custom `earth_to_stars:ship_interiors` technical dimension packaged in production JAR
- permission-gated exterior→interior entry adapter compile
- interior→current live exterior return adapter compile
- unlinked interior login recovery adapter compile
- Minecraft 26.2 respawn recovery API alignment
- `clean test build`
- production JAR verifier

Verified JAR SHA-256: `a5f3d8ffb24869c6085079af40106a3830b12ce7ea53e57775930b372fc03284`

The P0-B/P0-C exterior remains a temporary vanilla `ArmorStand` proxy. `orbital_space`, `ship_interiors`, and the small generated interior room are technical P0 environments only. None is a final ship model, interior layout, UI, space presentation, or world-design decision.

## P0-D interior architecture

P0-D uses one stable `earth_to_stars:ship_interiors` server space rather than creating a dynamic dimension per ship. Each authoritative `ShipId` receives a persistent isolated interior cell. Current P0 allocation uses 2048-block spacing in an 8192×8192 grid and rejects persisted slot collisions instead of silently relinking ships.

A player inside a ship interior remains in that stable interior coordinate space while the exterior ship moves or crosses Earth↔orbit. The interior is linked by `ShipId`; interior crew therefore do not need to inherit every exterior translation/rotation or be teleported during every exterior layer transition. This is the intended multiplayer-safe foundation for later crew, control-station, power/alarm/damage, cargo, and external-view projection.

The interior assignment is currently stored in dedicated versioned-world data alongside the ship SavedData boundary, not yet embedded into the `ShipState` binary schema. The project is still before the first playable-alpha compatibility freeze, so this may be migrated into a unified save schema before that freeze.

## Verification boundary

Verified by the alpha.4 automated gate:

- source/API compilation against Minecraft 26.2 / NeoForge 26.2.0.38-beta
- P0-A/B/C regression JUnit
- pure interior slot/layout/allocation tests
- production JAR structure
- packaged `orbital_space` and `ship_interiors` dimension data
- SavedData / TeleportTransition / interior entry-exit-recovery adapters compile as part of the mod

Still **NOT RUN / NOT TESTED**:

- actual disk save → dedicated-server restart → same ship/interior restore
- GameTest create/save/reload/restore integration
- dedicated server custom-dimension data-pack boot/smoke
- client smoke
- real in-game Earth→orbit→Earth flight transition
- real P0-B control feel/camera/interpolation/reconnect lifecycle
- actual exterior↔interior entry/exit in Minecraft
- two or more players coexisting in the same ship interior
- one player piloting while another remains inside during exterior movement/layer transition
- destroyed/unavailable exterior recovery in live gameplay
- live multiplayer session
- production ship/interior rendering
- manual/automatic turret gameplay

No item in the second list is called complete merely because its adapter compiles.

## Current phase

`M0 VERIFIED / P0-A SAVEDDATA ADAPTER BUILD VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D LINKED INTERIOR BACKEND BUILD VERIFIED / LIVE INTEGRATION DEFERRED / P0-E NEXT`

P0-D now provides the stable linked-interior backend required by the B-type ship architecture. The next production unit is **P0-E Representative Turret**: one authoritative autocannon path that proves manual and `AUTO_DEFENSE` control can share the same weapon state safely, while beginning the centralized SensorGrid/ammo boundary instead of building per-turret world scans.
