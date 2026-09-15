# EARTH TO STARS

- Slug: `earth-to-stars`
- Mod ID / namespace: `earth_to_stars`
- Current reboot version: `0.2.0-alpha.2`
- Minecraft: `1.20.1`
- Java: `17`
- Loader: `Forge`
- Forge: `47.4.0`
- Gradle: `8.14`
- Build plugin: ModDevGradle legacy Forge line
- Final JAR: `earth_to_stars-0.2.0-alpha.2.jar`
- Physical ship foundation: Valkyrien Skies `2.4.10`
- Space transition foundation: Genesis `1.20.1-0.7.3`
- Cockpit / control / power hardware: Zero Point Systems `1.20.1-2.5.1`
- Thruster / gyroscope hardware: Zero Point Labs `1.20.1-1.5.0`
- Live multiplayer: `NOT TESTED`
- Pre-reboot `0.1.x` / Minecraft 26.2 saves: compatibility with the `0.2.x` reboot is **not claimed or verified**.

The detailed external stack and reboot rationale are canonical in `docs/06_VS_GENESIS_REBOOT.md`.

## Project identity

EARTH TO STARS is a large-scale science-fiction Minecraft survival/expansion game built around one connected fantasy:

> Start on the Overworld as Earth, build practical industry and a small spacecraft, leave the atmosphere under player control, expand into orbit and other celestial regions, recover resources and technology, and grow one authored modular ship from a fragile vehicle into a mobile home, factory and warship.

It is not a generic tech mod, a planet-selection menu, a collection of recolored ores, or a freeform Space Engineers clone. Minecraft survival remains the foundation; the playable world expands upward and outward.

## Locked product decisions

1. `minecraft:overworld` is Earth and remains useful throughout progression.
2. The primary route must be completable without mandatory Nether or End visits. Nether/End may provide side routes, shortcuts or specialist rewards.
3. Surface → atmosphere → space should feel like one continuous trip from the player's perspective. Internal dimension transfer is allowed when the transition itself preserves the craft and control fantasy.
4. The physical vehicle is a real Valkyrien Skies block ship. EARTH TO STARS does not recreate ship physics, collision or player-on-ship movement.
5. The game remains **B-type authored modular ship progression**. Using VS as the physical substrate does not turn the game into unrestricted freeform ship construction. Hull stages, module slots, hardpoints, permissions, progression and resources remain authoritative ETS game state.
6. Genesis owns the baseline Earth↔space ship transition. ETS does not return to the retired fake-void/player-only teleport implementation.
7. Proven external hardware is used before custom replacement: ZPS cockpit/control/power and ZPL propulsion/gyro are the current starter baseline.
8. Important game state is server-authoritative: ownership, module changes, power/fuel/ammo, progression, damage, rewards, travel success and saves.
9. Manual and automatic weapons use the same authoritative weapon-system state.
10. Solo remains viable through assistance/automation; multiplayer roles are opportunities rather than mandatory jobs.
11. Repeatedly tuned content belongs in data where practical. Code owns rules; data owns content.
12. Final ship/UI/module/weapon/planet/VFX/sound art is reference-gated. Generic AI sci-fi styling is not production art.
13. External code/assets/references are actively used when appropriate and provenance/license terms are recorded.
14. Technical success is not product completion. Build, runtime boot, live play feel, visual acceptance, performance and multiplayer are tracked separately.

## Core loop

```text
Earth survival / resource acquisition
→ practical industry and launch capability
→ physical starter craft
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

# 0.2 reboot boundary

The old Minecraft 26.2 vehicle stack is retired. It used a custom Display-entity-style spacecraft, custom movement/collision approximations and a technical void-space implementation. Live testing showed unacceptable cockpit placement, collision, control and flight feel. The reboot intentionally keeps those sources outside the compiled source set rather than continuing to patch the wrong foundation.

Current compiled source roots are:

```text
src/reboot/java
src/reboot/resources
```

The previous implementation remains in Git history for forensic reference only.

## External runtime stack

```text
Minecraft 1.20.1 / Forge 47.4.0
        ↓
Valkyrien Skies 2.4.10
        ↓
Genesis 0.7.3 + VLib
        ↓
ZPS 2.5.1 cockpit / controller / finite Power Cell
ZPL 1.5.0 ion propulsion / gyroscopes
        ↓
EARTH TO STARS game rules and progression
```

ETS does not bundle or counterfeit those external projects. They remain runtime dependencies/API owners according to their licenses and are tracked in `THIRD_PARTY_ASSETS.md` / `docs/06_VS_GENESIS_REBOOT.md`.

---

# Current implementation — 0.2.0-alpha.2

## Physical starter craft

The reboot can construct an authored starter shuttle from real blocks and convert the complete structure into a Valkyrien Skies ship through the released VS `ShipAssembler` API.

Current technical loadout:

- tapered iron / waxed copper / glass hull
- ZPS Octo Controller as the real rideable cockpit/input source
- finite ZPS Power Cell
- ZPL Ion Modulator + Thruster Exhaust pairs for forward, reverse, lift/descent and lateral translation
- opposed ZPL gyroscopes for yaw
- ETS `starter_flight_core`
- ETS control nodes used to route controller channels to the external hardware
- ETS battery bus transferring Forge Energy from the Power Cell to the ion modulators

VS owns physical ship relocation, collision and physics. ZPL owns actual thruster/gyro force behavior. ETS owns assembly choice, input routing, power distribution and later progression rules.

## Starter controls

While the player is actually riding the ZPS Octo mounting seat, ETS reads the controller's supplied channels:

```text
W / OCT_A → forward
A / OCT_B → yaw left
S / OCT_C → reverse
D / OCT_D → yaw right
Up / OCT_E → ascend
Left / OCT_F → strafe left
Down / OCT_G → descend
Right / OCT_H → strafe right
```

These mappings are technically connected but are **not yet live-play accepted** for direction, strength or feel.

## Runtime verification structure

Normal ETS client/server smoke runs disable Forge GameTest discovery and restrict the namespace because released VS contains optional compatibility GameTest holders whose method signatures can reference unrelated absent mods such as Create. This avoids turning optional external tests into false ETS runtime failures; it does not replace future ETS-specific GameTests.

The released VS 2.4.10 Forge line may also log optional compatibility missing-class warnings and its documented sculk vibration interface-mixin limitation. These are tracked as external-stack warnings rather than hidden by adding unrelated fake dependencies.

---

# Verification state

Current automated evidence establishes the following categories independently:

- `CODE REVIEWED`: current reboot assembly/control/power integration reviewed against the pinned external APIs.
- `BUILD VERIFIED`: clean test/build and production JAR verification have passed on the reboot stack.
- `JAR PRODUCED`: `earth_to_stars-0.2.0-alpha.2.jar` has been produced and structurally verified.
- `DEDICATED STACK BOOT`: Forge + VS + Genesis + VLib + ZPS + ZPL have reached real dedicated-server startup.
- `PHYSICAL VS STARTER ASSEMBLY`: previously verified on the dedicated-server gate; the gate is being kept regression-safe against terrain-dependent placement.
- `CLIENT RESOURCE/MODEL SMOKE`: headless `runClient` now verifies actual resource reload / block-atlas creation in addition to mod initialization.
- `PLAYTESTED`: **NO** for the reboot starter craft.
- `GENESIS OCCUPIED-CRAFT EARTH→SPACE`: **NOT TESTED**.
- `MULTIPLAYER TESTED`: **NO**.

Automated boot or compile success must never be described as successful flight feel or successful multiplayer.

---

# Immediate acceptance gate

Do not expand Moon/asteroid content yet. The next real milestone is one complete live starter-craft sequence:

```text
open a real client
→ obtain/deploy the starter craft from the ETS creative tab
→ confirm the assembled object is a real VS ship
→ board the actual ZPS cockpit
→ verify W/A/S/D + arrow controls
→ take off
→ collide with terrain/entities correctly
→ leave the seat and move on the physical ship where appropriate
→ inspect hull scale, cockpit position and camera
→ ascend to the Genesis atmosphere boundary
→ transfer the complete occupied VS ship to space
→ keep controlling the same craft after transition
```

Acceptance is based on feel and continuity, not only whether coordinates changed.

If the first craft is too heavy, weak, unstable or awkward, tune hardware/layout using actual play evidence before building more progression around it.

---

# Progression contract after physical-flight acceptance

The previous 0.1.x M1 progression is retained as **design intent**, not as a claim about the current compiled reboot runtime. Once the physical starter craft passes live acceptance, rebuild the first closed loop on top of the new stack:

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

Detailed legacy M1 design remains available in `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md`; implementation details in that document referring to the retired 26.2 vehicle stack must not override the current reboot architecture.

---

# Next expansion after the gate

Only after the starter craft is visibly correct, directly usable, physically convincing and able to cross Earth→space intact should the project reconnect survival crafting/progression and then advance toward Moon/asteroid content.
