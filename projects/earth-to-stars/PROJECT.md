# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.5`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.5.jar`
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
15. `docs/QUALITY_STANDARD.md`, `docs/QUALITY_STANDARD_GAME_DESIGN.md`, the repository `AGENTS.md`, and the project development playbook are production constraints, not optional inspiration.
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
- turret control leases and mode transitions
- mining/resource transfer
- recipes and production results
- progression and celestial access
- ship interior assignment
- save data and migrations

Clients provide input, rendering, animation, UI and safe prediction only. A client never tells the server that damage, resources, travel completion or crafting already succeeded.

## Current implementation baseline

Latest verified implementation/CI commit: `85e4003223839dd3fe24e87e8fd8382a1931e693`

GitHub Actions `Build earth-to-stars` run `34186350799` verified:

- M0 bootstrap regression
- P0-A authoritative ship kernel / persistence-codec regression
- P0-B movement transform / control lease regression
- P0-C Earth↔orbit transition-policy regression
- P0-D stable linked-interior allocation/layout regression
- P0-E representative turret state-machine JUnit
- `OFF / MANUAL / AUTO_DEFENSE`
- exclusive manual turret lease
- session/sequence replay rejection
- ammo consumption / fire cooldown / legal firing arc
- shared `ShipSensorGrid` hostile selection and neutral rejection
- Minecraft 26.2 turret adapter compile
- server-side P0 logical projectile path compile
- version-aware CI report/artifact naming
- `clean test build`
- production JAR verifier

Verified JAR SHA-256: `53499a1cf6f14bd21cfefc8cdd095b2e2999c322c20c1be8c660d005efd48835`

The P0-B/P0-C exterior remains a temporary vanilla `ArmorStand` proxy. `orbital_space`, `ship_interiors`, the generated interior room, command-driven turret control surface, and logical projectile are technical P0 environments only. None is a final ship model, interior layout, cockpit/UI, weapon model, VFX, sound, projectile presentation, space presentation, or world-design decision.

## Linked interior architecture

P0-D uses one stable `earth_to_stars:ship_interiors` server space rather than creating a dynamic dimension per ship. Each authoritative `ShipId` receives a persistent isolated interior cell. Current P0 allocation uses 2048-block spacing in an 8192×8192 grid and rejects persisted slot collisions instead of silently relinking ships.

A player inside a ship interior remains in that stable interior coordinate space while the exterior ship moves or crosses Earth↔orbit. The interior is linked by `ShipId`; interior crew therefore do not need to inherit every exterior translation/rotation or be teleported during every exterior layer transition.

## P0-E weapon architecture

The representative turret is one server-owned `TurretRuntime` per current P0 ship weapon path. It proves that manual and automatic control share the same ammo/cooldown/arc state instead of becoming two unrelated weapon implementations.

### Manual

```text
WEAPON_CONTROL permission
→ mode MANUAL
→ exclusive turret lease
→ server reads authoritative player aim request
→ session/sequence validation
→ legal arc / ammo / cooldown validation
→ server creates logical shot
```

The current command adapter is only a P0 control surface. Production manual control will use a real gunner station/key/UI/camera after the UI/art reference gate.

### Automatic

```text
shared ShipSensorGrid
→ interval contact acquisition
→ hostile filter
→ firing-arc/range eligibility
→ AUTO_DEFENSE fire decision
→ same authoritative ammo/cooldown state
```

The Minecraft P0 adapter refreshes one ship contact cache every 10 ticks with a per-ShipId phase offset. It does **not** make each turret perform a broad world query every tick. P0 hostile classification currently recognizes Minecraft `Enemy` entities only; faction/ship/friendly-fire policy is a later production system.

### Projectile boundary

P0 shots are server-side logical moving points with lifetime, velocity, collision envelope and authoritative damage. They intentionally do not yet provide production projectile entity rendering, tracer VFX, muzzle flash, impact effects, animation, sound or camera feedback.

Ammo is currently local to the P0 turret runtime. P0-F must move it into the central ship ammo/logistics authority so multiple weapons cannot create independent ammunition economies.

## Verification boundary

Verified by the alpha.5 automated gate:

- source/API compilation against Minecraft 26.2 / NeoForge 26.2.0.38-beta
- P0-A/B/C/D regression JUnit
- pure P0-E turret state/lease/ammo/cooldown/arc/target-selection tests
- production JAR structure
- Minecraft turret adapter compilation
- version-aware P0-E build report/artifact generation

Still **NOT RUN / NOT TESTED**:

- actual disk save → dedicated-server restart → same ship/interior restore
- GameTest create/save/reload/restore integration
- dedicated server custom-dimension boot/smoke
- client smoke
- real in-game Earth→orbit→Earth flight transition
- real ship control feel/camera/interpolation/reconnect lifecycle
- actual exterior↔interior entry/exit in Minecraft
- two or more players coexisting in the same ship interior
- one player piloting while another remains inside during exterior movement/layer transition
- real manual turret aiming/control feel
- real automatic turret combat/hit feedback
- actual projectile visual/impact correctness
- pilot + gunner two-player lease conflict/disconnect session
- live multiplayer session
- production ship/interior/turret rendering and audio

No item in the second list is called complete merely because its adapter compiles.

## Current phase

`M0 VERIFIED / P0-A SAVEDDATA ADAPTER BUILD VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D LINKED INTERIOR BACKEND BUILD VERIFIED / P0-E TURRET BACKEND BUILD VERIFIED / LIVE INTEGRATION DEFERRED / P0-F NEXT`

The next production unit is **P0-F Central Ship Systems**. It promotes power, ammunition and sensors into ship-level authoritative services, removes P0-E's local-ammo limitation, ensures many modules/turrets share calculations rather than each simulating the world independently, and adds a synthetic scale test before the larger P0-G live Minecraft/multiplayer lifecycle gate.
