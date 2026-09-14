# Region 01 Combat-Space Field Review

Status: **AUTOMATED IMPLEMENTATION CANDIDATE — HUMAN FIELD ACCEPTANCE OPEN**

This checkpoint replaces only the featureless combat portion of the Region 01 technical floor with a navigation-bearing field-review baseline. It does **not** approve final Riftfrontier environment art.

## Reference boundary

Primary reference: Mojang/Minecraft Trial Chambers.

- https://www.minecraft.net/en-us/article/minecraft-preview-1-20-60-20
- https://feedback.minecraft.net/hc/en-us/articles/27451789924237-Minecraft-Bedrock-Edition-1-21-Tricky-Trials

The reference is used for two concrete ideas only: combat rooms should support mixed melee/ranged pressure, and tuff-family materials can make combat-space structure readable inside Minecraft. Riftfrontier does not copy a Trial Chamber room layout.

No external asset bytes are bundled. The implementation uses Minecraft runtime blocks (`TUFF_BRICKS`, `POLISHED_TUFF`) only, so `docs/THIRD_PARTY_ASSETS.md` requires no new redistribution entry.

## Implemented field geometry

- Existing authoritative Region 01 size, run state, salvage positions, spawn counts, pressure math and extraction gate are unchanged.
- The combat floor from local z=-5 through z=2 is rematerialized into a tuff-family field-review surface.
- The central/arrival lanes remain visually open with polished tuff.
- Four two-block tuff-brick cover pillars form two staggered left/right pairs.
- Arrival `(0,-4)`, central salvage `(0,0)`, the four corner salvage locations, and extraction relay `(0,5)` are intentionally not occupied by cover.
- z=3..5 remains owned by the existing extraction-relay presentation, preventing two temporary presentation systems from overwriting one another.
- The cover is materialized when the authoritative Region 01 encounter begins, after the technical cell reset and before mobs are spawned.

## What this must improve in human play

1. A player should be able to break a skeleton/scout sightline without leaving the field cell.
2. Hunters and the elite should still have obvious paths around the cover; no actor should become permanently trapped.
3. The player should still be able to move directly from arrival toward central salvage and later toward the extraction relay.
4. The four corner salvage nodes must remain reachable while under pressure.
5. Higher-pressure extra hunters/scouts must not create an accidental doorway body-block that makes the run unwinnable.
6. Relay readiness markers and the reviewed basalt/calcite/deepslate relay approach must remain visually and mechanically intact.

## Reject conditions

Reject or revise this baseline if human field play shows any of the following:

- a proxy repeatedly fails to path around the cover;
- the Ravager/elite becomes stuck or pins the player in an unavoidable spawn trap;
- the cover trivializes the scout role by allowing permanent zero-risk ranged denial;
- salvage or the extraction relay is obscured or unreachable;
- the tuff field reads as a claimed final Riftfrontier biome/art language rather than temporary combat-space structure;
- the field materially conflicts with the existing boss arena/readability work.

Do not call this `PLAYTESTED` or `MULTIPLAYER TESTED` until a human session actually exercises it.
