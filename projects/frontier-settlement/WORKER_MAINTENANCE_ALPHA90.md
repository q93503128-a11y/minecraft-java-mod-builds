# Frontier Settlement Alpha.90 — worker / construction maintenance recovery

Version: `0.1.0-alpha.90`

## `/frontier normalize`
- Works on the currently loaded Overworld only; it never force-loads a chunk.
- Repairs a 100%-step building only when the remaining mismatch is an air gap or known self-changing farm/path drift.
- Unexpected solid blocks and block entities are reported with coordinates instead of being overwritten.
- Removes loaded local-production duplicates only when the loaded count alone exceeds the number of completed jobs.
- Broadly scans loaded town space for historical shared-builder duplicates and preserves exact MAINHAND cargo before removal.
- Clears stale navigation, NoAI and invulnerability state on surviving loaded production workers.
- One shared `건설 주민` remains by design even with no active project.

## Farm 100% recovery
A farm can hold `FARMLAND` for many build steps before final commit. Vanilla block updates may turn it back into dirt.
Alpha.89 treated any wrong non-air block as a permanent finalization veto, leaving `농장 공사 100% · 마감 확인`.
Alpha.90 recognizes dirt/grass/coarse-dirt drift where the blueprint expects farmland (and dirt/grass where it expects a path),
repairs that Frontier-owned drift, and can finish the construction. The maintenance command performs the same safe repair immediately.

## Lumber worker
- Tree discovery no longer returns a high canopy log merely because that log is close to leaves.
- Leaf evidence is descended to the lowest contiguous same-species trunk base.
- The base must sit on natural ground and have a loaded walkable interaction approach.
- Empty-handed workers choose the visible species with the greatest physical trunk-log supply, not simply the nearest rare tree.
- The same species is kept until the one-stack cargo limit is reached when enough matching logs remain.
- A partial trip is still possible if no more matching physical logs exist or town storage cannot accept a full stack.
- Approach cells are tried nearest-first; no teleport or direct path-to-solid-block shortcut is used.
- Quarry target selection now also rejects targets with no walkable interaction approach.

## Save / authority boundaries
- No new required SavedData field.
- No virtual resource ledger, cargo minting, chunk force-load, or teleport.
- Natural Minecraft villagers are untouched.
- Exact carried ItemStacks are materialized before an excess Frontier worker is discarded.
- Existing Alpha.89 companion binary pins remain unchanged.
