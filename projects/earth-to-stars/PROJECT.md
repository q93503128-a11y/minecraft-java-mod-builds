# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID / namespace: `earth_to_stars`
- Current migration version: `0.3.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `Fabric`
- Fabric Loader: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Gradle: `9.5.1`
- Runtime dependency policy: Fabric API only for the current base; whole external ship/space mods are not part of the product contract.
- Live multiplayer: `NOT TESTED`
- 1.20.1 Forge/VS reboot saves and 0.1.x NeoForge saves: compatibility with the Fabric rebase is **not claimed or verified**.

The loader/reuse migration contract is canonical in `docs/07_FABRIC_26_2_STANDALONE_REBASE.md`.

## Project identity

EARTH TO STARS is a large-scale science-fiction Minecraft survival/expansion game built around one connected fantasy:

> Start on the Overworld as Earth, build practical industry and a small spacecraft, leave the atmosphere under direct control, expand into orbit and other celestial regions, recover resources and technology, and grow one authored modular ship from a fragile vehicle into a mobile home, factory and warship.

It is not a generic tech mod, a planet-selection menu, a collection of recolored ores, or a freeform Space Engineers clone. Minecraft survival remains the foundation; the playable world expands upward and outward.

## Locked product decisions

1. `minecraft:overworld` is Earth and remains useful throughout progression.
2. The primary route must be completable without mandatory Nether or End visits.
3. Surface → atmosphere → space should feel like one continuous trip from the player's perspective. Internal dimension/layer transfer is allowed only when position, direction, velocity, craft identity and control continuity are preserved convincingly.
4. The game remains **B-type authored modular ship progression**. The ship is not arbitrary moving blocks. Hull stages, module slots, hardpoints, permissions, progression and resources are authoritative ETS game state.
5. ETS is developed as a standalone game mod. External projects are researched and selectively reused where their licenses permit, but the player should not be told to install an unrelated space/ship mod just to make ETS work.
6. External reuse means exact code/resources/algorithms may be ported or adapted only after source, license, modification and redistribution terms are verified and recorded in `THIRD_PARTY_ASSETS.md`.
7. Important game state is server-authoritative: ownership, module changes, power/fuel/ammo, progression, damage, rewards, travel success and saves.
8. Manual and automatic weapons use the same authoritative weapon-system state.
9. Solo remains viable through assistance/automation; multiplayer roles are opportunities rather than mandatory jobs.
10. Repeatedly tuned content belongs in data where practical. Code owns rules; data owns content.
11. Final ship/UI/module/weapon/planet/VFX/sound art is reference-gated. Generic AI sci-fi styling is not production art.
12. Technical success is not product completion. Build, runtime boot, live play feel, visual acceptance, performance and multiplayer are tracked separately.

## Why Fabric 26.2

The project returned to Minecraft 26.2 because the 1.20.1 Forge reboot solved technical problems by requiring an entire external ship/space stack, which does not match the intended product. Fabric is selected for the current rebase because 26.2 is actively supported, its API/tooling are current, the project can remain small in runtime dependencies, and its ecosystem is convenient for the intended multiplayer/client workflow.

This is not a claim that Fabric is universally superior. Loader choice is project-specific and can be revisited only if a materially better technical route appears.

## External-code/resource policy

Use external work aggressively as development material, not as installation homework for the player.

Preferred order:

```text
permissively licensed exact code/resource reuse
→ adapted/ported implementation
→ architecture/algorithm reference
→ original implementation only where no suitable reusable solution exists
```

Examples:

- Kenney CC0 spacecraft assets may be vendored and adapted directly.
- Apache/MIT/BSD/CC0 code may be selectively ported when exact files and obligations are recorded.
- GPL projects are valuable research references, but code incorporation requires a deliberate project-license decision and is not casually pasted into this ARR repository.
- proprietary commercial game assets/code are reference-only.

## Core loop

```text
Earth survival / resource acquisition
→ practical industry and launch capability
→ starter craft
→ controlled atmosphere ascent
→ orbit / space
→ salvage, science and combat
→ ship capability upgrades
→ Moon / asteroid / planetary expeditions
→ new environmental constraints and resources
→ stronger mobility / automation / weapons / production
→ deeper space
→ discoveries feed back into Earth, bases and ship growth
```

Every major feature must attach to this loop and create a new choice, capability or risk response rather than only another currency or menu.

## Authority model

Server authority includes at minimum:

- ship ownership / crew permissions
- accepted ship identity and ETS logical state
- installed modules / hardpoints
- module damage / repair
- power / propellant / oxygen / ammunition
- supply transactions
- launch / travel readiness
- weapon cooldown / target / hit / damage
- pilot / turret control rights
- salvage / reward resolution
- mining / resource transfer
- recipes / production results
- progression / celestial access
- save data / migrations

Clients provide input, rendering, animation, UI, sound, VFX and safe prediction only.

---

# Current implementation — Fabric rebase foundation

The current compiled Fabric artifact deliberately starts with the parts of the previous 26.2 codebase that are loader-neutral and worth preserving:

- ship identity / ownership / crew permissions
- authored module catalog, slots and module instances
- ship power / ammo / propellant / oxygen simulation
- sensor and turret domain logic
- deterministic flight/control math used by existing unit tests
- interior assignment math
- launch-readiness and first-orbit progression rules
- binary ship-state codec and migration tests

The new Fabric entrypoint lives under `src/fabric/java`. Existing loader-neutral kernel code is reused in-place from `src/main/java`.

The following older NeoForge integration areas remain in Git history/source for migration reference but are not part of the Fabric artifact yet:

- NeoForge registry/content glue
- NeoForge networking payload registration
- NeoForge SavedData bindings
- NeoForge client renderer/input glue
- old Minecraft-specific ship runtime managers

They are not deleted or replaced with no-op APIs. Each area is ported or redesigned as a real Fabric feature unit.

The 1.20.1 Forge `src/reboot` VS/Genesis/ZPS/ZPL integration is also retained only as technical history. It is not compiled and those mods are not Fabric runtime dependencies.

## Current external assets

The repository already contains Kenney Space Kit CC0 source meshes for the starter craft, salvage craft and interceptor. Their provenance remains recorded in `THIRD_PARTY_ASSETS.md`. They are valid bases for the Fabric visual pipeline, but the old NeoForge OBJ adapter is not assumed to work on Fabric and must be replaced with a Fabric-compatible production rendering path.

---

# Verification state

At the start of this rebase, the required gates are tracked independently:

- `CODE REVIEWED`: Fabric build boundary, loader-neutral kernel reuse boundary and external-dependency policy reviewed.
- `TESTED`: pending first Fabric CI run for the retained pure-Java kernel tests.
- `BUILD VERIFIED`: pending first Fabric CI run.
- `JAR PRODUCED`: pending first Fabric CI run.
- `DEDICATED FABRIC BOOT`: pending first Fabric CI run.
- `CLIENT FABRIC SMOKE`: pending first Fabric client workflow.
- `PLAYTESTED`: **NO** for the Fabric rebase.
- `EARTH→SPACE CONTINUITY`: **NOT TESTED**.
- `MULTIPLAYER TESTED`: **NO**.

Compile/build success must never be described as successful flight feel or successful multiplayer.

---

# Immediate migration sequence

Do not expand Moon/asteroid content yet. Rebuild one complete starter-craft vertical slice on Fabric in this order:

```text
Fabric 26.2 build + kernel regression tests
→ Fabric item/entity/network/save authority bridge
→ production starter-craft visual pipeline using approved external assets
→ seat / pilot control session
→ collision + movement solution informed by permissively licensed external code research
→ power/propellant/oxygen hooked to real flight
→ continuous-feeling atmosphere→space transition
→ return to Earth
→ actual client playtest and feel tuning
```

The old Display-entity-style 26.2 implementation is not automatically restored just because the loader returns to 26.2. Its failed cockpit/collision/flight assumptions must be replaced, preferably using proven external implementation ideas or permissively reusable code rather than repeating the same invention.

---

# Progression contract after physical-flight acceptance

Once the starter craft passes live acceptance, reconnect the first closed loop:

```text
Earth resources
→ starter craft construction
→ fuel / oxygen / power readiness
→ first real flight and orbit transition
→ orbital salvage
→ first weapon capability
→ first hostile encounter
→ recovered sensor capability
→ Earth return
→ scanner / exploration improvement
```

Important retained design principles:

- early spaceflight should reinterpret useful vanilla Earth resources before adding a wall of new ores,
- first-space rewards should unlock capabilities rather than only larger numbers,
- weapons and sensors use central ship-level simulation instead of every module broad-scanning each tick,
- progression must have anti-soft-lock recovery,
- Nether/End remain optional for the main route,
- production UI/art/VFX/sound only proceed through the project's reference/asset gate.

Detailed game design remains canonical in `docs/00_MASTER_GAME_DESIGN.md` and related design docs.
