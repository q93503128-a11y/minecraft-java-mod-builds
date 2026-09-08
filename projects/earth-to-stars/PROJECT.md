# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.8`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.8.jar`
- Existing-world compatibility: before first playable alpha, save schema may change deliberately; from first playable alpha onward registry IDs, save roots, module IDs and migration rules become compatibility contracts.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` (`NOT RUN` at current gate)
- GameTest task: not yet registered; persistence restart was verified by the P0-G dedicated-server two-boot lifecycle gate.
- Server smoke-test task: `runServer` (`P0-G TWO-BOOT LIFECYCLE VERIFIED`, not rerun on ordinary alpha.8 push)
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

### Latest P0-H verification

Latest verified implementation/CI commit: `8b3b4edda64505d476418e0b08fbe85baea6b0ba`

GitHub Actions `Build earth-to-stars` run `34189697283` verified:

- P0-H progression validator self-tests
- canonical main progression graph validation
- Nether-only required main route rejection
- End-only required main route rejection
- clean alternative route acceptance
- optional Nether/End side-route acceptance
- dependency cycle rejection
- unknown dependency rejection
- P0-A through P0-G JUnit regression
- `clean test build`
- production JAR verifier
- production JAR contains `data/earth_to_stars/progression/main_path.json`

Verified alpha.8 JAR SHA-256: `3af179b7cdb16236722507434a000f38dcc82fc59079aab584e1f79771f2e688`

### P0-G lifecycle verification retained

The expensive two-boot dedicated lifecycle gate was **not rerun** for alpha.8 because P0-H does not modify persistence or custom-dimension lifecycle behavior. The last dedicated lifecycle verification remains GitHub Actions run `34188840459`, commit `557d273ecfa78c1ba9cc62956cd78f6eb7c55153`.

That run verified:

- dedicated server first boot on a clean run directory
- `earth_to_stars:orbital_space` and `earth_to_stars:ship_interiors` registration
- deterministic ship/interior/system seed into real SavedData
- clean server shutdown with all dimensions saved
- second dedicated-server boot on the same world directory
- disk restore of ShipId / owner / module slots
- disk restore of ShipId → interior slot
- disk restore of central power / ammo quantities
- restored central systems runtime initialization
- sensor cache rebuilt instead of persisted

This test is now run on explicit lifecycle verification rather than every ordinary code/data push, to avoid validation spam.

## P0-H progression independence contract

Canonical graph:

`src/main/resources/data/earth_to_stars/progression/main_path.json`

Validator:

`tools/validate_progression.py`

Self-tests:

`tools/test_progression_validator.py`

The graph uses `requires_any` alternatives. A main milestone passes only when at least one complete prerequisite derivation exists that never requires a node located in:

- `minecraft:the_nether`
- `minecraft:the_end`

This means optional Nether/End shortcuts remain legal. A node merely mentioning or living in Nether/End is not automatically rejected. The failure condition is that a **main milestone loses every Nether/End-independent derivation**.

Current main milestones:

```text
Earth Industry
→ Launch Craft
→ Earth Orbit Access
→ Orbital Salvage
→ Moon Access
→ Near-Earth Asteroid Access
→ Mars Access
→ Main Belt Access
→ Outer System Access
→ Deep Space Access
```

The canonical graph deliberately contains optional `nether_heat_shortcut`, `nether_propellant_variant`, and `end_navigation_sidegrade` nodes so the validator itself proves that optional side routes remain allowed.

## Linked interior architecture

P0-D uses one stable `earth_to_stars:ship_interiors` server space rather than creating a dynamic dimension per ship. Each authoritative `ShipId` receives a persistent isolated interior cell. Current P0 allocation uses 2048-block spacing in an 8192×8192 grid and rejects persisted slot collisions instead of silently relinking ships.

A player inside a ship interior remains in that stable interior coordinate space while the exterior ship moves or crosses Earth↔orbit. The interior is linked by `ShipId`; interior crew therefore do not need to inherit every exterior translation/rotation or be teleported during every exterior layer transition.

## Weapon architecture

The representative turret uses the same server-owned mode/cooldown/arc logic for manual and automatic operation.

### Manual

```text
WEAPON_CONTROL permission
→ mode MANUAL
→ exclusive turret lease
→ server reads authoritative player aim request
→ session/sequence validation
→ legal arc / shared power / shared ammo / cooldown validation
→ server creates logical shot
```

The current command adapter is only a P0 control surface. Production manual control will use a real gunner station/key/UI/camera after the UI/art reference gate.

### Automatic

```text
central ShipSensorGrid
→ interval contact acquisition
→ hostile filter
→ firing-arc/range eligibility
→ AUTO_DEFENSE fire decision
→ same authoritative PowerGrid / AmmoPool / cooldown state
```

P0 hostile classification currently recognizes Minecraft `Enemy` entities only; faction/ship/friendly-fire policy is a later production system.

### Projectile boundary

P0 shots are server-side logical moving points with lifetime, velocity, collision envelope and authoritative damage. They intentionally do not yet provide production projectile entity rendering, tracer VFX, muzzle flash, impact effects, animation, sound or camera feedback.

## Central ship systems architecture

The ship's operational truth is centralized per `ShipId`.

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ ShipPowerGrid
     ├─ ShipAmmoPool
     └─ ShipSensorGrid
          ↑
   propulsion / sensors / turret(s)
```

### PowerGrid

Current P0 power policy:

- finite storage
- deterministic generation
- server-tick monotonicity
- input-scaled propulsion draw
- sensor acquisition draw
- weapon shot draw
- priority reserves:
  - `ESSENTIAL`: may use emergency reserve to zero
  - `PROPULSION`: preserves 10% capacity
  - `WEAPONS`: preserves 25%
  - `UTILITY`: preserves 40%

This policy prevents convenience or weapon systems from draining the last energy required by more important systems. The exact percentages are P0 tuning, not final balance.

### AmmoPool

Ammo is keyed by ammo type and owned once per ship system. Two weapon runtimes firing from one `ShipId` reduce the same authoritative ammo count. A rejected shot does not partially consume ammo or power.

### SensorGrid

One cache is shared by weapons. Contact acquisition is interval-based and staggered by `ShipId`, and stale contacts expire. Per-turret broad world scanning remains forbidden.

### Interior crew access

A player inside a linked interior resolves the authoritative ship through `InteriorSavedData → ShipId → ShipRepository`, so interior stations can later operate the same ship systems without depending on proximity to the exterior entity.

## Persistence and lifecycle architecture

Persistent operational state is separated from volatile world-derived state.

Persisted:

- `ShipId`, owner, module slots and installed modules via `ShipSavedData`
- `ShipId → interior slot` via `InteriorSavedData`
- central current power and ammo quantities via `ShipSystemsSavedData`

Not persisted:

- SensorGrid contacts
- manual/pilot control leases
- logical projectiles
- temporary exterior entity IDs

The non-persisted items are runtime state and must be rebuilt/reacquired after restart. Persisting stale contacts, leases or entity IDs would create ghost targets, unauthorized reconnect authority, or invalid entity references.

The CI lifecycle probe is dormant during normal play and activates only through `EARTH_TO_STARS_LIFECYCLE_PROBE`. It exists to prove actual SavedData disk behavior without exposing development commands to players.

## Technical proxy boundary

The P0-B/P0-C exterior remains a temporary vanilla `ArmorStand` proxy. `orbital_space`, `ship_interiors`, the generated interior room, command-driven turret/system control surfaces, and logical projectile are technical P0 environments only. None is a final ship model, interior layout, cockpit/UI, weapon model, VFX, sound, projectile presentation, space presentation, or world-design decision.

## Verification boundary

Automated technical gates completed:

- Minecraft 26.2 / NeoForge 26.2.0.38-beta compilation
- authoritative ShipState/module/permission persistence contract
- movement/control lease logic
- Earth↔orbit transition backend
- linked interior allocation/persistence
- manual/auto turret server state machine
- central power/ammo/sensor authority
- real dedicated-server save/restart restore
- custom dimension dedicated-server registration
- Nether/End-independent main progression graph guard
- production JAR structure

Still **NOT RUN / NOT TESTED**:

- client smoke
- real in-game Earth→orbit→Earth player flight transition
- real ship control feel/camera/interpolation/reconnect lifecycle with a player client
- actual exterior↔interior entry/exit with players
- two or more players coexisting in the same ship interior
- one player piloting while another remains inside during exterior movement/layer transition
- real manual turret aiming/control feel
- real automatic turret combat/hit feedback
- actual projectile visual/impact correctness
- pilot + gunner two-player lease conflict/disconnect session
- live multiplayer session
- production ship/interior/turret rendering and audio

No item in the second list is called complete merely because the automated P0 technical gates passed.

## Current phase

`P0 AUTOMATED TECHNICAL GATES COMPLETE / P0-G DEDICATED LIFECYCLE VERIFIED / P0-H NETHER-END INDEPENDENCE VERIFIED / LIVE ACCEPTANCE DEFERRED / LIVE MULTIPLAYER NOT TESTED / M1 EARTH-ORBIT GAMEPLAY SLICE NEXT`

The next production unit is **M1 Earth/Orbit Gameplay Slice**. Development stops adding isolated technical proofs and begins connecting the existing systems into one player-facing loop: Earth preparation → launch craft → fuel/oxygen → atmospheric ascent → orbit → salvage/hostile contact → manual/auto weapon use → return → ship upgrade.

The slice is not considered complete until it is actually playable and visually reviewed in Minecraft. User testing should happen once the slice is meaningful enough to evaluate as a whole, not after every small implementation change.
