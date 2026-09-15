# EARTH TO STARS — Fabric 26.2 Standalone Rebase

Status: current technical architecture contract for the 0.3.x migration line.

This document supersedes `06_VS_GENESIS_REBOOT.md` as the current loader/runtime direction. The 1.20.1 Forge document remains a technical experiment record and source of lessons, not a runtime dependency contract.

## 1. Decision

EARTH TO STARS returns to Minecraft `26.2` and migrates to Fabric.

Pinned foundation for the first rebase gate:

```text
Minecraft: 26.2
Fabric Loader: 0.19.5
Fabric API: 0.160.0+26.2
Java: 25
Gradle: 9.5.1
Loom: 1.17.19
```

The version contract follows the official Fabric 26.2 example line for Minecraft/Loader/API/Java and reuses the repository's already-working 26.2 Fabric project structure where practical.

## 2. Why the 1.20.1 Forge experiment is not the product

The Forge/VS/Genesis/ZPS/ZPL reboot proved several useful things:

- a craft must have real collision and convincing motion,
- cockpit placement and camera quality matter as much as coordinate movement,
- power/propulsion/control should be separated cleanly,
- deployment must be transactional,
- automated boot/build gates are valuable.

But it also turned ETS into a composition of whole external mods that the player would need to install. That is not the requested product.

The correct interpretation of "use external work aggressively" is:

> inspect good public implementations, reuse exact permissively licensed code/resources where allowed, port/adapt them into ETS, and record provenance — not require the player to install every source project.

## 3. Runtime dependency policy

Current base runtime dependencies:

- Fabric Loader
- Fabric API

Whole external space/ship mods are **not** accepted as default runtime dependencies.

A future library dependency may still be approved when it is genuinely library-shaped, current on 26.2, clearly licensed, materially better than local implementation, and does not turn installation into a modpack assembly task. That decision must be explicit in `PROJECT.md` and `THIRD_PARTY_ASSETS.md`.

## 4. External code/resource reuse classes

### Direct code reuse / port

Allowed when the exact source file is under a compatible permissive license and its obligations are recorded. Port loader/API calls as needed; do not drag an entire mod into the runtime merely because one algorithm is useful.

### Editable/direct asset base

Models, textures, UI, audio and other resources may be vendored when redistribution/modification terms permit it. Preserve source/author/license records.

### Code research only

GPL or otherwise license-incompatible projects can still teach architecture, algorithms, edge cases and player-facing solutions. Do not casually paste their source into this ARR repository.

### Reference only

Commercial/proprietary games and unclear-license resources are design reference only.

## 5. Migration boundary

The old repository contains three technical layers:

1. loader-neutral ETS game/domain code,
2. old Minecraft 26.2 NeoForge integration,
3. 1.20.1 Forge external-stack reboot integration.

The Fabric rebase keeps layer 1 active immediately. Layers 2 and 3 remain available for forensic comparison but are not compiled into the Fabric artifact.

Current compiled kernel includes:

- `ship/domain/**`
- `ship/systems/**`
- `ship/combat/**`
- `ship/gameplay/**`
- `ship/interior/**`
- loader-neutral files directly under `ship/runtime/`
- loader-neutral files directly under `ship/persistence/`
- new `src/fabric/java` bridge

Excluded pending real Fabric ports:

- `ship/client/**`
- `ship/networking/**`
- `ship/persistence/minecraft/**`
- `ship/runtime/minecraft/**`
- old NeoForge content/registry entrypoint
- `src/reboot/**`

This is not a deletion/no-op strategy. Existing source remains intact until equivalent Fabric functionality is implemented and accepted.

## 6. What must not return

Returning to 26.2 does **not** authorize simply re-enabling the failed old flight presentation.

Do not ship the old assumptions unchanged:

- fake or weak collision,
- cockpit/player placement that does not match the model,
- coordinate movement presented as convincing spacecraft physics,
- abrupt player-only space teleport,
- generic temporary visual treatment as final art.

The new Fabric vehicle layer must be informed by external proven solutions and tested in the actual client.

## 7. First vertical slice migration order

```text
build + pure kernel regression
→ Fabric registry/content bridge
→ Fabric networking and server-authoritative pilot session
→ save bridge
→ approved starter-craft visual asset path
→ movement/collision implementation informed by permissive external research
→ seat/camera/input
→ power + propellant + oxygen
→ atmosphere transition
→ space continuation
→ Earth return
→ live playtest
```

Do not build Moon/asteroid breadth before this sequence produces one convincing round trip.

## 8. External research priorities

For the next vehicle work, prefer sources that satisfy both technical usefulness and reuse clarity. Search for:

- current Fabric/modern-Minecraft vehicle/entity movement implementations,
- seat/passenger/camera handling,
- collision handling suitable for authored vehicle objects,
- interpolation/network prediction,
- space-layer transition patterns,
- current rendering paths for imported/converted CC0 craft assets.

Record exact repository, file, commit/tag, license and what was actually reused before code enters production.

## 9. Verification language

The rebase deliberately separates:

- `CODE REVIEWED`
- `TESTED`
- `BUILD VERIFIED`
- `JAR PRODUCED`
- `PLAYTESTED`
- `MULTIPLAYER TESTED`

A Fabric build or headless client boot is not proof that flight feels good. A deterministic movement unit test is not proof that Minecraft collision/camera is correct.
