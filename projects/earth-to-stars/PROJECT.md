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

# Current implementation — Fabric starter-craft foundation

The current Fabric artifact preserves the loader-neutral parts of the previous 26.2 codebase that are worth keeping:

- ship identity / ownership / crew permissions
- authored module catalog, slots and module instances
- ship power / ammo / propellant / oxygen simulation
- sensor and turret domain logic
- deterministic flight/control math used by existing unit tests
- interior assignment math
- launch-readiness and first-orbit progression rules
- binary ship-state codec and migration tests

The Fabric integration now includes both the server-authoritative bridge and the first real physical Launch Craft boundary:

- Fabric C2S control-input payload and S2C control-session payload contracts
- server-owned pilot session binding over the existing `ShipFlightRuntime` permission, lease, sequence and expiry rules
- `ShipRepository` as the authoritative in-memory logical ship registry
- Fabric `SavedData` persistence backed by the existing `ShipStateCodec`
- server lifecycle load/reset so integrated or dedicated server worlds do not leak static ship/runtime state into one another
- construction components: `reinforced_frame`, `avionics_unit`, and `life_support_unit`
- `earth_to_stars:launch_craft_kit` real Overworld deployment transaction
- real Minecraft 26.2 `VehicleEntity` Launch Craft bound to stable `ShipId`
- one real pilot seat with range / ownership / `PILOT` permission validation before server control grant
- W/S throttle, A/D yaw, jump lift-up and sprint lift-down client input tied to server-issued sessions
- collision-resolved movement reconciled back into the existing loader-neutral `ShipFlightRuntime`
- approved Kenney Space Kit CC0 starter-craft source mesh loaded through a Fabric/Minecraft 26.2 submit/render-state client path

The client cannot create a control session or mutate authoritative ship state by packet. The physical entity does not own a duplicate economy, module inventory, fuel store, oxygen store or permission model.

The current initial craft is a **one-pilot implementation** of the planned small 1–2 person Launch Craft. A second seat is not claimed yet and should be added only when crew/passenger play has a concrete gameplay role.

The new Fabric entrypoint lives under `src/fabric/java`. Existing loader-neutral kernel code is reused in-place from `src/main/java`.

The following older NeoForge integration areas remain in Git history/source for migration reference but are not automatically part of the Fabric artifact:

- NeoForge registry/content glue
- NeoForge networking payload registration
- NeoForge SavedData bindings
- NeoForge client renderer/input glue
- old Minecraft-specific ship runtime managers

They are not deleted or replaced with no-op APIs. Each area is ported or redesigned as a real Fabric feature unit.

The 1.20.1 Forge `src/reboot` VS/Genesis/ZPS/ZPL integration is also retained only as technical history. It is not compiled and those mods are not Fabric runtime dependencies.

## Current external assets

The repository contains Kenney Space Kit CC0 source meshes for the starter craft, salvage craft and interceptor with provenance recorded in `THIRD_PARTY_ASSETS.md`.

The Fabric Launch Craft now uses the approved starter-craft mesh through a new 26.2-compatible renderer path. The old NeoForge OBJ adapter was not restored. Current client smoke confirms that the Fabric client resource path parses the starter mesh (`280` triangles), but in-world visual scale/orientation/material/camera acceptance remains a live-playtest task.

No unverified current-version vehicle-mod source code was copied into the physical craft implementation. External vehicle projects remain research references unless exact source revision/license/obligations are independently verified and recorded.

---

# Verification state

The current physical Launch Craft integration gate is closed at source commit `6ec6e8c860e3f1f52403bd8be3e721667ebaabe2` after initial implementation commit `69bde96fc6fec2ad7c93048e0de853ecbcf13e10`.

`Build earth-to-stars Fabric 26.2` run `35054576328` / run number `#42`: **PASS**

- standalone Fabric contract validator: PASS
- retained loader-neutral ship kernel tests: PASS
- clean test/build: PASS
- production JAR structure verification: PASS
- dedicated Fabric server boot: PASS
- physical Launch Craft registration + ship authority bridge initialization: PASS
- production JAR: `earth_to_stars-0.3.0-alpha.1.jar`
- SHA-256: `23f652d32f5053a83c2ecfb468d5fdcbaa6b98ae51a421aaa89eee3404d88122`

`Smoke earth-to-stars Fabric client` run `35054576366` / run number `#17`: **PASS**

- Fabric client preparation: PASS
- Xvfb `runClient` smoke: PASS
- ETS client initialization: PASS
- resource reload: PASS
- Kenney Launch Craft OBJ parser: PASS (`280` triangles)
- no fatal ETS client initialization failure in the smoke gate

The first physical-craft build run `35054175541` passed the standalone contract validator and then correctly failed during Java compilation on Minecraft 26.2 API differences at the new renderer/entity/message/riding/removal boundaries. Commit `6ec6e8c...` aligned those APIs and also removed pre-spawn world mutation from deployment and made physical-entity unbinding safely own runtime teardown. No craft feature was hidden or deleted to obtain a green build.

The headless CI client reports missing narrator `flite` and unavailable OpenAL/audio device. Minecraft continued to run and the ETS client smoke passed; these are runner-environment limitations rather than an ETS entrypoint failure.

Current verification labels:

- `CODE REVIEWED`: YES for the Fabric construction + authority + physical Launch Craft source
- `TESTED`: YES for automated kernel/contract/build/server/client initialization gates
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `DEDICATED FABRIC BOOT`: YES
- `CLIENT FABRIC SMOKE`: YES
- `KENNEY MESH PARSE`: YES
- `PHYSICAL STARTER CRAFT PLAYTESTED`: **NO**
- `LIVE FLIGHT FEEL`: **NOT TESTED**
- `CRAFT VISUAL ACCEPTANCE IN WORLD`: **NOT TESTED**
- `POPULATED CRAFT SAVE/RELOAD`: **NOT TESTED**
- `POWER / PROPELLANT / OXYGEN FLIGHT CONSUMPTION`: **NOT CONNECTED**
- `EARTH→SPACE CONTINUITY`: **NOT TESTED**
- `MULTIPLAYER TESTED`: **NO**

A successful build/server/client smoke proves integration and initialization. It does not prove flight feel, collision fairness, visible model quality, populated-world persistence or multiplayer correctness.

Detailed implementation/acceptance notes are canonical in `docs/10_FABRIC_LAUNCH_CRAFT.md`.

---

# Immediate migration sequence

Do not expand Moon/asteroid content yet. Close the first complete starter-craft vertical slice in this order:

```text
Fabric 26.2 build + kernel regression tests              [PASS]
→ Fabric construction content bridge                     [PASS]
→ Fabric server authority / network / save bridge        [PASS]
→ real launch / deploy transaction                       [PASS - automated]
→ real VehicleEntity starter craft                       [PASS - automated]
→ Kenney starter-craft Fabric render resource path       [PASS - initialization / mesh parse]
→ physical seat validation + server pilot session        [PASS - code/build; live play pending]
→ collision + movement reconciliation                    [PASS - code/build; live feel pending]
→ actual client craft playtest                           [NEXT]
→ power / propellant / oxygen hooked to real flight
→ continuous-feeling atmosphere → space transition
→ return to Earth
```

The immediate product gate is not more content. It is a live acceptance test of the Launch Craft's scale, orientation, seat/camera, acceleration, yaw, lift, collision, jitter, chunk/save behavior and material presentation.

The old Display-entity-style 26.2 implementation is not restored. Its failed cockpit/collision/flight assumptions stay retired.

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
