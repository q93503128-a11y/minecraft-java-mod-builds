# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID: `earth_to_stars`
- Namespace: `earth_to_stars`
- Mod version: `0.1.0-alpha.10`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `earth_to_stars-0.1.0-alpha.10.jar`
- Existing-world compatibility: before first playable alpha, save schema may change deliberately; from first playable alpha onward registry IDs, save roots, module IDs and migration rules become compatibility contracts.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: none approved as a hard runtime dependency. Any addition requires current 26.2 compatibility, maintenance, license, multiplayer and performance review.
- Forbidden bundled dependencies: Minecraft original files, NeoForge distribution files, external mod JARs, and models/textures/audio/UI assets without redistribution permission.
- Datagen task: `runData` (`NOT RUN` at current gate)
- Server lifecycle: latest two-boot disk restore verification run `34192830690`, including Power/Ammo/Propellant/Oxygen.
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
10. Ship power, ammo, sensors, propellant and oxygen use centralized/batched simulation; modules do not each broad-scan or independently duplicate resources.
11. Repeat-adjusted values are data-driven. Code owns rules; data owns content.
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
- ship transform accepted by movement backend
- installed modules/hardpoints
- module damage/repair
- power / propellant / oxygen / ammunition
- fuel/oxygen supply transactions
- atmosphere/launch readiness
- weapon cooldown/target/hit/damage
- pilot/turret control leases
- mining/resource transfer
- recipes/production results
- progression/celestial access
- interior assignment
- save data/migrations

Clients provide input/rendering/animation/UI/safe prediction only. A client never reports that damage, resources, crafting, refueling or travel already succeeded.

---

# Current implementation baseline

## 0.1.0-alpha.10 — M1-C Launch Readiness + Atmosphere Backend

Latest verified implementation/CI commit: `34da5747f400d5815e751085afae1fd2fb7a066e`

GitHub Actions `Build earth-to-stars` run `34192830690` verified:

- P0-H progression validator
- M1 actual launch recipe dependency closure
- M1 launch recipe Nether/End independence
- M1 starter blueprint regression
- LaunchReadinessPolicy atmosphere bands/readiness rules
- Power + Propellant atomic propulsion transaction
- Oxygen continuous drain
- fuel/oxygen supply tank/capacity logic
- Fuel/Oxygen snapshot restore and capacity rejection
- P0-A through P0-G regression JUnit
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile
- `clean test build`
- production JAR structure
- dedicated server first boot/save/shutdown
- same-world second boot
- Propellant `51.25` disk restore
- Oxygen `66.5` disk restore

Verified JAR SHA-256:

`aa3c01597544dae55ec1e2309c3c4538b61185bb6022a266cb93374d5db5a8f3`

Because alpha.10 changed the persisted ship-systems schema, the expensive dedicated two-boot lifecycle was intentionally rerun once. After it passed, ordinary pushes returned to the cheaper gate; lifecycle is explicitly rerun only when risk justifies it.

## M1 Earth launch crafting contract

The first survival-facing production chain does not add new Earth ores.

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

`tools/validate_m1_launch.py` recursively follows the actual launch-craft recipe closure so practical recipes cannot silently drift into mandatory Nether/End progression.

## Launch craft construction contract

`launch_craft_kit` is a construction package, not a portable fully simulated ship.

Server deployment rules:

1. deployment occurs in `minecraft:overworld`
2. 3×3×3 clearance exists
3. player does not already own a registered ship
4. authoritative starter `ShipState` can be created
5. exterior proxy can be placed
6. `ShipSavedData` is updated
7. `ShipSystemsRuntime` is initialized and persisted
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

The `turret` hardpoint is intentionally empty so the first orbital trip can unlock a real new capability.

Initial central resources:

- Power `80 / 100`
- Propellant `80 / 240`
- Oxygen `80 / 240`

## Launch readiness / atmosphere contract

Current Earth gameplay bands:

```text
Dense Atmosphere : Y < 256
Thin Atmosphere  : 256 ≤ Y < 384
Upper Atmosphere : 384 ≤ Y < 512
Earth→Orbit      : Y = 512
Earth re-entry   : Y = 504
```

Propellant draw scales with control activity and current band:

- Dense `0.020/tick`
- Thin `0.040/tick`
- Upper `0.070/tick`
- Orbit `0.015/tick`
- idle `0`

Oxygen draw scales with actual active crew:

- Dense `0`
- Thin `0.0025/tick/crew`
- Upper `0.010/tick/crew`
- Orbit `0.015/tick/crew`

Active crew currently means the pilot lease holder plus logged-in players in the linked interior, deduplicated by UUID.

Earth→Orbit requires all of:

- `life_support_mk1`
- Propellant ≥ `8`
- Oxygen ≥ `20`

If readiness is insufficient, the transition is denied, the craft is held below the boundary, upward velocity is removed, and the pilot receives throttled readiness feedback.

## Supply contract

Player-facing M1 supply behavior:

- propellant cell → up to `+40` Propellant
- oxygen cartridge → up to `+40` Oxygen
- supply is applied to the authoritative accessible `ShipId`
- full tank / no accessible ship does not consume the item
- successful supply persists immediately

The current use-on-block supply interaction is temporary M1 UX. Production refueling/oxygen ports and feedback require the visual/UX reference gate.

## Persistence / migration

Persisted:

- ShipId/owner/modules/slots
- ShipId→interior assignment
- central Power / Ammo / Propellant / Oxygen quantities

Not persisted:

- sensor contacts
- control leases
- logical projectiles
- temporary exterior entity IDs

Alpha.10 adds optional `propellant_stored` and `oxygen_stored` fields. Saves created before these fields existed receive starter defaults `80 / 80`, preventing old craft from loading stranded at zero resources.

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
     ├─ ShipSensorGrid
     ├─ PropellantTank
     └─ OxygenTank
```

Sensors are reconstructed after restart. Per-turret broad scans remain forbidden.

## Weapon control

Manual and AUTO_DEFENSE use the same authoritative cooldown/arc/power/ammo state. Pilot/turret control leases use server-issued session IDs and replay/stale-input rejection.

---

# Technical proxy boundary

The following are temporary technical/client registration proxies, not production design:

- ArmorStand exterior
- vanilla-texture item icons
- generated technical interior room
- empty orbital space
- command-driven technical controls
- use-on-block supply UX
- logical projectile presentation

They must not become final art by inertia. Production ship/item/cockpit/interior/turret/fuel-port/VFX/sound/space presentation follows `docs/03_UI_ART_REFERENCE_GATE.md` and `THIRD_PARTY_ASSETS.md`.

---

# Verification boundary

`TESTED / BUILD VERIFIED` for alpha.10:

- pure/game-rule JUnit including fuel/O2/readiness rules
- P0 progression and M1 recipe dependency validators
- source/API compilation
- build/JAR packaging
- actual dedicated server save→shutdown→restart→restore for Power/Ammo/Propellant/Oxygen

Still `NOT RUN / NOT TESTED`:

- live client crafting/recipe book
- in-world launch package deployment
- fuel/oxygen supply feel
- actual atmosphere ascent/readiness boundary
- live Earth→orbit→Earth flight
- camera/interpolation/control feel
- orbital salvage/hostile encounter
- first return/upgrade loop
- live interior multi-crew oxygen behavior
- actual manual/auto weapon feel
- live two-player pilot+gunner / pilot+crew
- live multiplayer session
- production visuals/audio

No automated result is treated as proof of these live-play items.

---

# Current phase

`P0 AUTOMATED TECHNICAL GATES COMPLETE / M1-C LAUNCH READINESS + ATMOSPHERE BACKEND VERIFIED / FUEL-OXYGEN DISK LIFECYCLE VERIFIED / LIVE ACCEPTANCE DEFERRED / LIVE MULTIPLAYER NOT TESTED / M1-D ORBITAL SALVAGE + CONTACT NEXT`

## Next production unit — M1-D

Connect the first actual orbit gameplay:

```text
Earth preparation
→ direct atmosphere ascent
→ Earth Orbit
→ salvage contact
→ first hostile contact
→ authoritative recovery reward
→ Earth return
→ first meaningful ship upgrade
```

The goal is not random loot boxes in a black dimension. The first orbit trip must unlock a new action/capability and prove why going to space changes the game.

M1 is only complete when the entire Earth→Orbit→recovery→return→upgrade cycle is actually playable in Minecraft.
