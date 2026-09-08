# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.9`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.9.jar`
- Existing-world compatibility: before first playable alpha, save schema may change deliberately; from first playable alpha onward registry IDs, save roots, module IDs and migration rules become compatibility contracts.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` (`NOT RUN` at current gate)
- Server lifecycle: P0-G dedicated-server two-boot restore verified in run `34188840459`; not repeated on unrelated pushes.
- Client smoke: `NOT RUN`
- Live multiplayer: `NOT TESTED`

## Project identity

EARTH TO STARS is a large-scale science-fiction Minecraft survival/expansion game built around one connected fantasy:

> **Start on the vanilla Overworld as Earth, build industry and a small craft, cross the atmosphere under player control, expand into orbit and other celestial regions, recover resources and technology, and grow one modular ship from a fragile vehicle into a mobile home, factory and warship.**

The project is not a generic tech mod, a planet menu, a collection of colored ores, or a Space Engineers clone. Minecraft survival remains the foundation; the playable world expands upward and outward.

## Locked product decisions

1. `minecraft:overworld` is Earth and remains useful throughout the game.
2. The primary progression route must be completable without mandatory Nether or End visits.
3. Nether and End are optional side routes, shortcuts, specialist-material sources and late-game variants, never mandatory gates.
4. Surface-to-space travel must feel continuous. Internal dimension/layer transitions may protect performance, but presentation must not feel like menu teleportation.
5. Player ships use the **B-type modular ship architecture**: one authoritative ship object with modules, hardpoints, systems and linked interior; not an arbitrary moving-block contraption.
6. Ship progression changes capabilities and play patterns, not only numbers.
7. Manual and automatic weapon control share one server-owned weapon/system truth.
8. Multiplayer is first-class from the beginning. Important state is server-authoritative.
9. Solo play stays viable through automation/assistance; multiplayer roles are opportunities, not jobs.
10. Ship power, ammo and sensors use centralized/batched simulation; modules do not each broad-scan the world every tick.
11. Repeat-adjusted values are data-driven. Code owns rules; data owns content.
12. Final UI/ship/module/weapon/planet/VFX/sound design is reference-gated; generic AI sci-fi styling is not production art.
13. External references/assets are actively used where licensing permits and recorded in `THIRD_PARTY_ASSETS.md`.
14. Technical success is not product completion. Real Minecraft play, visual review, performance and multiplayer verification are separately tracked.

## Core loop

```text
Earth survival/resource acquisition
→ practical industry and launch capability
→ atmosphere/orbit breakthrough
→ orbital salvage/science/combat
→ ship/module upgrade
→ Moon/asteroid/planetary expedition
→ new environmental constraints/resources
→ stronger mobility/automation/weapons/production
→ deeper space
→ discoveries feed back into Earth, bases and ship growth
```

Every major feature must attach to this loop.

## Multiplayer authority

Server authority includes at minimum:

- ship ownership/membership
- ship transform accepted by movement backend
- installed modules/hardpoints
- module damage/repair
- energy/fuel/oxygen/ammunition
- weapon cooldown/target/hit/damage
- pilot/turret control leases
- mining/resource transfer
- recipes/production results
- progression/celestial access
- interior assignment
- save data/migrations

Clients provide input/rendering/animation/UI/safe prediction only. A client never reports that damage, resources, crafting or travel already succeeded.

---

# Current implementation baseline

## 0.1.0-alpha.9 — M1-A/B Earth Preparation + First Launch Craft

Latest verified implementation/CI commit: `bc8e51ba30d2e3eec07dfd868b0f79dc9460e73f`

GitHub Actions `Build earth-to-stars` run `34191142069` verified:

- P0-H progression validator self-tests
- canonical main progression graph
- M1 launch recipe dependency closure
- M1 launch recipe Nether/End independence
- starter craft blueprint JUnit
- P0-A through P0-G regression JUnit
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile
- `clean test build`
- production JAR structure
- M1 recipes/client item definitions packaged

Verified JAR SHA-256:

`c2f033c73de080c90cff7ed77ae0b3d2d07d6ec5d14aa22cc0f173766e223264`

The P0-G dedicated two-boot lifecycle was not rerun because alpha.9 did not change persistence/custom-dimension lifecycle. The retained verified run is `34188840459`.

## M1 Earth launch crafting contract

Alpha.9 introduces the first survival-facing production chain without adding new Earth ores.

Vanilla resources are reinterpreted as early spaceflight materials:

- Iron → structure/pressure vessel
- Copper → conduction/plumbing/control hardware
- Redstone → control/power electronics
- Gold → precision electronics
- Amethyst → early navigation/sensing component
- Gunpowder + Paper → solid propellant abstraction
- Water → oxygen production input
- Leather → life-support sealing/packing

Custom items:

```text
reinforced_frame
avionics_unit
propellant_cell
oxygen_cartridge
life_support_unit
launch_craft_kit
```

Detailed recipes and rationale are canonical in `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md`.

`tools/validate_m1_launch.py` recursively follows the actual `launch_craft_kit` recipe closure. This prevents the practical crafting chain from drifting away from the P0-H abstract progression graph and becoming Nether/End mandatory by accident.

## Launch craft construction contract

`launch_craft_kit` is a construction package, not a portable fully simulated ship.

Server deployment rules:

1. deployment occurs in `minecraft:overworld`
2. 3×3×3 clearance exists
3. player does not already own a registered ship
4. authoritative starter `ShipState` can be created
5. exterior proxy can be placed
6. `ShipSavedData` is updated
7. ship systems runtime is initialized
8. server pilot control lease is granted
9. survival package is consumed only on success

Existing ownership is never silently overwritten.

## Starter craft canonical loadout

Slots:

- `core`
- `engine`
- `power`
- `cargo`
- `life_support`
- `turret`

Installed at construction:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

The `turret` hardpoint is intentionally empty. First orbital salvage/combat should create a meaningful capability upgrade instead of giving the starter craft every system immediately.

---

# Retained P0 architecture contracts

## Progression independence

Canonical graph:
`src/main/resources/data/earth_to_stars/progression/main_path.json`

Validator:
`tools/validate_progression.py`

Each main milestone must retain at least one complete prerequisite derivation that does not require `minecraft:the_nether` or `minecraft:the_end`. Optional Nether/End shortcuts and sidegrades remain legal.

## Linked interior

One stable `earth_to_stars:ship_interiors` server space is partitioned into persistent isolated cells by `ShipId`. Interior crew remain in stable coordinates while the exterior ship moves or changes Earth/orbit layer.

## Central ship systems

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ ShipPowerGrid
     ├─ ShipAmmoPool
     └─ ShipSensorGrid
          ↑
   propulsion / sensors / weapon(s)
```

Power/ammo are server-owned and persisted. Sensors are reconstructed after restart. Per-turret broad scans remain forbidden.

## Weapon control

Manual and AUTO_DEFENSE use the same authoritative cooldown/arc/power/ammo state. Pilot/turret control leases use server-issued session IDs and replay/stale-input rejection.

## Persistence

Persisted:

- ShipId/owner/modules/slots
- ShipId→interior assignment
- central power/ammo quantities

Not persisted:

- sensor contacts
- control leases
- logical projectiles
- temporary exterior entity IDs

P0-G actual save→shutdown→same-world restart→restore was verified in run `34188840459`.

---

# Technical proxy boundary

The following are temporary technical/client registration proxies, not production design:

- ArmorStand exterior
- vanilla-texture M1 item icons
- generated technical interior room
- empty orbital space
- command-driven technical controls
- logical projectile presentation

They must not become final art by inertia. Production ship/item/cockpit/interior/turret/VFX/sound/space presentation follows `docs/03_UI_ART_REFERENCE_GATE.md` and `THIRD_PARTY_ASSETS.md`.

---

# Verification boundary

`TESTED / BUILD VERIFIED` for alpha.9:

- pure/game-rule JUnit including starter blueprint
- P0 progression and M1 recipe dependency validators
- source/API compilation
- build/JAR packaging

Retained earlier dedicated lifecycle verification:

- two server boots on same world
- custom dimensions loaded
- ship/interior/power/ammo disk restore

Still `NOT RUN / NOT TESTED`:

- live client crafting/recipe book
- in-world launch package deployment
- actual item model appearance
- fuel/oxygen runtime consumption
- atmosphere gameplay
- live Earth→orbit→Earth flight
- camera/interpolation/control feel
- orbital salvage/hostile encounter
- first return/upgrade loop
- live interior multi-crew
- actual manual/auto weapon feel
- live two-player pilot+gunner
- live multiplayer session
- production visuals/audio

No automated result is treated as proof of these live-play items.

---

# Current phase

`P0 AUTOMATED TECHNICAL GATES COMPLETE / M1-A/B EARTH PREPARATION + FIRST LAUNCH CRAFT BACKEND BUILD VERIFIED / LIVE ACCEPTANCE DEFERRED / LIVE MULTIPLAYER NOT TESTED / M1-C LAUNCH READINESS + ATMOSPHERE NEXT`

## Next production unit — M1-C

Connect the alpha.9 preparation items to actual gameplay:

```text
propellant_cell
→ authoritative fuel/launch reserve

oxygen_cartridge + life_support_mk1
→ authoritative oxygen reserve

readiness state
→ atmosphere ascent
→ Earth Orbit transition permission
```

The goal is not to add many gauges. The player should understand whether the craft is ready and how long it can survive without becoming a maintenance worker.

After M1-C, M1-D connects first orbital salvage/contact → Earth return → first ship upgrade. M1 is only complete when that whole cycle is actually playable in Minecraft.
