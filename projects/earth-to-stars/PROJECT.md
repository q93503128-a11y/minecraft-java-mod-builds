# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.2`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.2.jar`
- Existing-world compatibility: before first playable alpha, save schema may change deliberately; from first playable alpha onward registry IDs, save roots, module IDs and migration rules become compatibility contracts.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` (`NOT RUN` at current gate)
- GameTest task: not yet registered; first Minecraft ship persistence integration gate must add it
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

Latest verified implementation commit: `cc89f0c1693fa063de5bb091ca355a35a5413b8f`

GitHub Actions `Build earth-to-stars` run `34176522000` verified:

- M0 Gradle 9.2.1 / Java 25 / NeoForge 26.2.0.38-beta bootstrap regression
- P0-A authoritative pure-Java ship kernel regression
- P0-B pure movement transform and orientation math
- throttle / yaw / pitch / acceleration / braking rules
- server-issued control lease with session UUID, monotonically increasing input sequence and expiry
- owner/crew pilot permission enforcement
- client payload contains control input only; authoritative transform remains server-owned
- logout/dimension-change lease revocation path
- Minecraft-side temporary exterior proxy and control command/network adapter compile
- `clean test build`
- P0-A/P0-B JUnit
- production JAR verifier

Verified JAR SHA-256: `a834a372008b1604d4591b595b88b10ba06446e1c149ce044a6842db0f3a2413`

The P0-B exterior is currently a temporary vanilla `ArmorStand` proxy used only to prove the backend boundary. It is **not** a production ship model, visual target or content decision.

Not yet verified:

- Minecraft SavedData persistence adapter
- GameTest create/save/reload/restore integration
- dedicated server smoke
- client smoke
- live multiplayer session
- real in-game P0-B control feel / camera / interpolation acceptance
- reconnect lease behavior in a live Minecraft session
- production ship exterior model/rendering
- Earth → orbital-space transition
- linked interior
- manual/automatic turret gameplay

## Current phase

`M0 VERIFIED / P0-A PURE KERNEL VERIFIED / P0-B BACKEND BUILD VERIFIED / MINECRAFT PLAY VALIDATION DEFERRED / P0-C NEXT`

P0-B's movement and multiplayer-authority backend is now real code and passes the project build/JUnit/JAR gate. It is **not** called fully gameplay-verified until an actual Minecraft client/server session checks control feel, interpolation, reconnect and lifecycle behavior. To avoid repetitive testing, that live validation is deliberately batched with the next meaningful integration slice instead of being requested after every code unit.

The next production unit is **P0-C Earth → Orbital Space Transition**, while also connecting ShipState to Minecraft persistence so the next larger integration gate can cover P0-A persistence + P0-B movement lifecycle + P0-C transition together.