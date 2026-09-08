# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.11`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.11.jar`
- Existing-world compatibility: save roots / registry IDs / ShipId / module IDs are compatibility contracts. Current ShipState schema is 2; schema 1 migrates by adding the missing sensor slot without resetting the ship.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` (`NOT RUN` at current gate)
- Server lifecycle: latest two-boot disk restore verification run `34197931566`; ShipState schema 2 and Power/Ammo/Propellant/Oxygen runtime restoration loaded successfully.
- Client smoke: `NOT RUN`
- Live multiplayer: `NOT TESTED`

## Project identity

EARTH TO STARS is a large-scale science-fiction Minecraft survival/expansion game built around one connected fantasy:

> **Start on the vanilla Overworld as Earth, build industry and a small craft, cross the atmosphere under player control, expand into orbit and other celestial regions, recover resources and technology, and grow one modular ship from a fragile vehicle into a mobile home, factory and warship.**

It is not a generic tech mod, a planet-selection menu, a collection of recolored ores, or a Space Engineers clone. Minecraft survival remains the foundation; the playable world expands upward and outward.

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
10. Ship power, ammo, sensors, propellant and oxygen use centralized/batched simulation; modules do not each broad-scan or independently duplicate resources.
11. Repeatedly tuned values should become data-driven. Code owns rules; data owns content.
12. Final UI/ship/module/weapon/planet/VFX/sound design is reference-gated; generic AI sci-fi styling is not production art.
13. External references/assets are actively used where licensing permits and recorded in `THIRD_PARTY_ASSETS.md`.
14. Technical success is not product completion. Real Minecraft play, visual review, performance and multiplayer verification are separately tracked.

## Core loop

```text
Earth survival/resource acquisition
→ practical industry and launch capability
→ fuel/oxygen readiness
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
- accepted ship transform
- installed modules/hardpoints
- module damage/repair
- power / propellant / oxygen / ammunition
- supply transactions
- atmosphere/launch readiness
- weapon cooldown/target/hit/damage
- pilot/turret control leases
- salvage/reward resolution
- mining/resource transfer
- recipes/production results
- progression/celestial access
- interior assignment
- save data/migrations

Clients provide input/rendering/animation/UI/safe prediction only. A client never reports that damage, resources, crafting, refueling, salvage or travel already succeeded.

---

# Current implementation baseline

## 0.1.0-alpha.11 — M1-D Orbital Recovery + First Contact Backend

Latest verified implementation/CI commit:

`afe0d181667582877e911ef279a869c7e31d0c23`

GitHub Actions `Build earth-to-stars` run `34197931566`: **PASS**

Verified JAR SHA-256:

`f818686c7dde57e7a33e27967069c7e2074ce6b97febd0b13721e1165ac37fb0`

Verified in this gate:

- P0-H progression validator
- actual M1 launch recipe dependency closure
- actual launch recipe Nether/End independence
- starter empty weapon/sensor-slot contract
- recovered autocannon/scanner capability progression
- sensor range `64 → 96`
- ShipState schema 1→2 sensor-slot migration
- existing P0/M1 regression JUnit
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile
- `clean test build`
- production JAR verify
- orbital mission / recovered sensor resources packaged
- dedicated server first boot/save/shutdown
- same-world second boot/restore

Because alpha.11 changes ShipState schema, the expensive two-boot lifecycle was deliberately rerun once. After success, ordinary pushes returned to the cheaper gate; lifecycle is explicit `workflow_dispatch` unless a future persistence risk justifies another rerun.

Not verified by this gate:

- live survival crafting/use flow
- actual atmosphere ascent/transition feel
- live Earth→Orbit→salvage→combat→Earth return cycle
- camera/interpolation
- salvage readability and approach feel
- actual turret combat feel
- reward pickup/install UX
- client visual quality
- live multiplayer pilot/gunner/interior session

---

# M1 survival launch contract

The first Earth progression does not add a large new ore layer. Existing Minecraft resources are reinterpreted as early spaceflight materials:

- Iron → structure/pressure vessel
- Copper → conduction/plumbing/control hardware
- Redstone → control/power electronics
- Gold → precision electronics
- Amethyst → early navigation/sensing component
- Gunpowder + Paper → solid propellant abstraction
- Water → oxygen production input
- Leather → life-support sealing/packing

Player-facing items:

```text
reinforced_frame
avionics_unit
propellant_cell
oxygen_cartridge
life_support_unit
launch_craft_kit
recovered_sensor_core
```

`tools/validate_m1_launch.py` recursively follows the actual launch-craft recipe closure so practical recipes cannot silently drift into mandatory Nether/End progression.

## Launch craft construction

`launch_craft_kit` is a construction package, not a portable fully simulated ship.

Server deployment verifies:

1. Overworld/Earth
2. 3×3×3 clearance
3. no existing registered owned ship
4. authoritative starter ShipState creation
5. exterior proxy placement
6. ShipSavedData persistence
7. ShipSystemsRuntime initialization/persistence
8. server-issued pilot control lease
9. item consumption only on success

Existing ownership is never silently overwritten.

## Starter craft canonical loadout

Slots:

- `core`
- `engine`
- `power`
- `cargo`
- `life_support`
- `turret`
- `sensor`

Installed at construction:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

Intentionally empty:

- `turret`
- `sensor`

Initial central resources:

- Power `80 / 100`
- Propellant `80 / 240`
- Oxygen `80 / 240`

---

# Launch readiness / atmosphere

Current Earth bands:

```text
Dense Atmosphere : Y < 256
Thin Atmosphere  : 256 ≤ Y < 384
Upper Atmosphere : 384 ≤ Y < 512
Earth→Orbit      : Y = 512
Earth re-entry   : Y = 504
```

Propellant draw scales with control activity and atmosphere band. Oxygen draw scales with active crew: pilot lease holder + logged-in players in the linked interior, deduplicated by UUID.

Earth→Orbit requires:

- `life_support_mk1`
- Propellant ≥ `8`
- Oxygen ≥ `20`

If insufficient, transition is denied, the craft is held below the boundary, upward velocity is removed, and the pilot receives throttled readiness feedback.

Current supply:

- propellant cell → up to `+40` Propellant
- oxygen cartridge → up to `+40` Oxygen
- full/no accessible ship does not consume item
- successful supply persists immediately

The current use-on-block interaction is temporary M1 UX, not final refueling design.

---

# M1-D progression contract

The first Orbit trip fills actual missing ship capabilities rather than just raising a stat.

```text
starter: no turret / no scanner
→ first orbital salvage
→ autocannon_mk1 installs into turret slot
→ AUTO_DEFENSE immediately available
→ first unmanned interceptor encounter
→ server-owned contact/movement/health/power attack
→ central SensorGrid + turret engage the encounter
→ recovered_sensor_core reward
→ Earth return
→ orbital_scanner_mk1 installs into sensor slot
→ sensor range 64 → 96
```

Important rules:

- no installed autocannon means no usable TurretRuntime, even through technical commands.
- MANUAL and AUTO_DEFENSE are modes of the same installed turret state.
- encounter target data enters the ship-level central SensorGrid; turrets do not each broad-scan the world every tick.
- hostile attacks drain authoritative ship PowerGrid.
- projectile hits and hostile health are server-resolved.
- first hostile clear prevents immediate same-session reward farming.
- if the player loses/misses the core and returns to Earth without installing the scanner, session-only clear resets so a later Orbit trip can recover the progression item again.
- scanner install is Earth-only and consumes the core only on success.

Current ArmorStand salvage/interceptor and technical cockpit tether are functional proxies only.

---

# Persistence / migration

Persisted:

- ShipId / owner / crew / slots / modules
- ShipId→interior assignment
- central Power / Ammo / Propellant / Oxygen quantities

Not persisted:

- SensorGrid contact cache
- pilot/turret leases
- logical projectiles
- temporary exterior IDs
- M1 encounter proxy state

ShipState schema 2 adds the `sensor` slot. Schema 1 decode preserves existing identity, ownership, crew, slots and modules and adds only a missing `sensor` UTILITY size-1 slot. Unsupported future schema is rejected rather than silently resetting the ship.

---

# Current visual boundary

The following are NOT production quality:

- ArmorStand ship exterior
- ArmorStand salvage/interceptor proxies
- vanilla texture item proxies
- technical cockpit tether
- technical linked-interior room
- current orbital-space presentation
- command-based technical controls
- logical projectile visual
- temporary use-on-block supply/upgrade UX

Production ship/cockpit/interior/turret/hostile/salvage/UI/VFX/sound/space visuals must pass `docs/03_UI_ART_REFERENCE_GATE.md` and use the license ledger where external assets are involved.

---

# Current status / next work

`M1-D ORBITAL RECOVERY + FIRST CONTACT BACKEND VERIFIED / SHIPSTATE SCHEMA 1→2 LIFECYCLE VERIFIED / FULL EARTH→ORBIT→RETURN LIVE ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`

Do **not** expand to Moon/asteroid content merely because the backend builds.

The next meaningful gate is the complete live M1 loop:

```text
Earth resources
→ craft/deploy launch craft
→ fuel/oxygen
→ controlled ascent
→ Earth Orbit
→ salvage approach
→ autocannon recovery
→ first interceptor
→ sensor core pickup
→ re-entry
→ scanner install
→ sensor-range improvement
```

Acceptance focuses on progression blockers, control/camera feel, readability, combat duration, reward recovery, re-entry and whether the first trip actually feels worth doing.

Only after this loop is playable and feels coherent should M2 Moon content become the next expansion target.
