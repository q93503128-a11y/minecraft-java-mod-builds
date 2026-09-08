# Alpha.130 — Construction-worker routing recovery

Real play after Alpha.129 showed construction workers repeatedly standing in odd places and appearing to lose routes. This pass stays inside the existing construction system and treats that report as a movement-authority problem rather than adding teleportation or another worker layer.

## Root cause

Alpha.128 made builder spawn/home recovery reject obvious roofs, but ordinary building work still used `safeSurfaceCell`, which accepted the highest walkable collision surface at a coordinate. The Alpha.94 local-work shortcut also reused the 12-block protection margin. A builder on a nearby roof, log top or other artificial perch could therefore be considered locally valid and stop navigating. Separately, every idle builder was routed back to the exact same safe home coordinate, causing avoidable crowding/collision and poor-looking return paths as builder capacity increased.

## Fix

- active ordinary-building work uses a dedicated 4-block local envelope; the wider protection envelope is unchanged;
- work, grading and home candidates share a ground/path-only staging-cell authority instead of arbitrary highest collision surfaces;
- normal natural ground remains a heightmap fast path, while tree/structure cases use the existing bounded terrain scan;
- idle and normalization return routing reserves distinct safe home cells for each builder;
- blocked/elevated recovery avoids already occupied builder cells;
- disconnected elevated legacy workers may still be recovered, while physically connected bridges/balconies remain real navigation;
- no force loading, new save field, virtual material/cargo, repeated teleport loop or extra management UI is introduced.

## Acceptance

Automated source/docs regression and Java 25 compile are required before the patch commits. A produced JAR may then go through the existing canonical build/runtime pipeline. Real graphical play must still verify that several builders visibly approach ordinary construction from ground, spread out when idle, and do not stall on a nearby roof/log structure. Multiplayer remains separately unverified until an actual LAN/server session is run.
