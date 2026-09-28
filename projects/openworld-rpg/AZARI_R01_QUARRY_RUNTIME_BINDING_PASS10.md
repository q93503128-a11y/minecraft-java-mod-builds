# Open-World RPG — Azari R01 Quarry Runtime Binding Pass 10

> Status: **QUARRY REVIEW-SHELL / RUNTIME-GEOMETRY BOUNDARY IMPLEMENTED / SEMANTIC SPAWN SOCKET CONTRACT FIXED / FINAL AUTHORED GEOMETRY STILL GATED**
>
> Date: 2026-09-28
>
> Canon owners: `R01_VERTICAL_SLICE.md`, `R01_CONTENT_BIBLE.md`, `STATUS_AND_R01_ENCOUNTERS.md`
>
> Predecessors: `AZARI_R01_QUARRY_INTERIOR_PASS3.md`, Quarry encounter authority in source

## 1. Purpose

Pass 3 proved that the real Azari mountain contains suitable rock/seam neighborhoods, but every selected 3D box was explicitly a **review shell**, not a final room or boss arena.

The encounter backend already owns room order, exact authored enemy counts, multiplayer scaling, clear/reset state and reconnect-safe class attribution. Pass 10 connects those rules to the spatial work without turning review coordinates into gameplay authority.

## 2. Runtime binding boundary

New resource:

```text
data/openworld_rpg/world/r01_quarry_runtime_bindings.json
```

New runtime types:

```text
R01QuarrySpatialBindingData
R01QuarrySpatialBindingLoader
R01QuarrySpatialBindingRegistry
```

Each room now distinguishes:

```text
Pass-3 source review shell
!= final runtime room volume
!= concrete encounter spawn sockets
```

Stable semantic IDs are fixed for the final runtime volumes, but no coordinates are invented for them.

## 3. Spawn socket contract

The runtime binding records exactly the semantic sockets already owned by the encounter rules:

```text
Upper Gallery: 7 sockets
Collapsed Hoist: 6 sockets
Root-Breached: 1 socket
```

The loader recomputes the canonical 4-player encounter plans and rejects any mismatch.

Upper Gallery also preserves `openworld_rpg:r01/quarry/upper_gallery/deeper_gallery_threshold` as a future production volume, not a guessed point.

## 4. Production-only gate

A room is exposed by the runtime registry only when:

1. the binding is `production`;
2. its separate final runtime volume exists and is `production`;
3. all required spawn sockets exist as production anchors;
4. Upper Gallery's second-wave trigger exists as a production volume.

Relay Gallery and the Earthloong arena require their own final production volumes.

The Pass-3 Earthloong `32 x 12 x 32` box remains only a solid-rock carve probe. Its final runtime target is separately named:

```text
openworld_rpg:r01/quarry/earthloong/runtime_arena
```

## 5. Current state

```text
QUARRY NON-SPATIAL ENCOUNTER AUTHORITY: IMPLEMENTED
QUARRY REVIEW SHELLS FROM ACTUAL AZARI: PRESENT
QUARRY RUNTIME BINDING CONTRACT: IMPLEMENTED
QUARRY SPAWN SOCKET CONTRACT: IMPLEMENTED
CANDIDATE -> GAMEPLAY LEAK: BLOCKED

FINAL ROOM RUNTIME VOLUMES: NOT AUTHORED
FINAL SPAWN SOCKET COORDINATES: NOT AUTHORED
UPPER SECOND-WAVE TRIGGER VOLUME: NOT AUTHORED
FINAL RELAY VOLUME: NOT AUTHORED
FINAL EARTHLOONG ARENA: NOT AUTHORED

QUARRY SPATIAL PRODUCTION READY: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

No Minecraft test is requested from the user at this stage. The next geometry step remains real-client review/authored excavation, not arbitrary coordinate invention.
