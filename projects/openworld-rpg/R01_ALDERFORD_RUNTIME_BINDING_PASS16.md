# Open-World RPG — R01 Alderford Runtime Binding Pass 16

> Status: **STRUCTURE-SHELL / LIVE-SERVICE / LIVE-PROPERTY GEOMETRY SEPARATED / PRODUCTION GATE IMPLEMENTED**
>
> Date: 2026-09-28

## Result

Alderford's authored structure family binding already separated terrain candidates from exact visual compositions.

This pass adds the missing runtime geometry boundary:

### Services

A service is not live merely because its building shell is accepted.

Production access now requires:

1. the exact structure composition is accepted and production;
2. the service binding itself is production;
3. the composition-owned service interaction socket exists as a production spatial anchor.

The terrain-center anchor cannot substitute for the service socket.

This covers all nine R01 service identities:

- Gate Shrine
- Copper Kettle
- Wayfarers' Hall
- Holt Forge
- Alderford Vault
- Greenwater Remedies
- Fordside Stables
- Market
- Route Board

### Properties

A residence is not purchasable merely because its exterior shell is accepted.

Production purchase now also requires:

- production protected property volume;
- production interior volume;
- production furnishing volume;
- authored doorway/critical-interaction clearance volumes.

The five current Alderford properties are all represented by stable runtime target ids, but no dimensions/coordinates are invented before the exact house composition is accepted.

That means the housing purchase backend from Pass 14 cannot accidentally open on a visually reviewed shell that still lacks a safe usable interior.

## Current gate

All current Alderford structure/service/property/runtime records remain candidate.

Therefore:

```text
ALDERFORD STRUCTURE FAMILY BINDING: IMPLEMENTED
SERVICE SOCKET CONTRACT: IMPLEMENTED
PROPERTY RUNTIME VOLUME CONTRACT: IMPLEMENTED
SHELL-ONLY LIVE SERVICE LEAK: BLOCKED
SHELL-ONLY PROPERTY PURCHASE LEAK: BLOCKED

EXACT PREFAB COMPOSITION: NOT ACCEPTED
PRODUCTION SERVICE SOCKET COORDINATES: NOT AUTHORED
PRODUCTION PROPERTY INTERIOR VOLUMES: NOT AUTHORED
PRODUCTION FURNISHING VOLUMES: NOT AUTHORED
DOORWAY/CLEARANCE VOLUMES: NOT AUTHORED

ALDERFORD RUNTIME PRODUCTION READY: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

The next visual/spatial step still depends on the actual accepted Medieval Village / Fantasy Props archive contents and Minecraft composition review. No temporary vanilla settlement shell is introduced.
