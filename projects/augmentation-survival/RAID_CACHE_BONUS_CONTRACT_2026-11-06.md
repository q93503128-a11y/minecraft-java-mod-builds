# Augmentation Survival — World Bosses raid cache integrated gacha

Date: 2026-11-06
State: USER-DIRECTED DESIGN ACCEPTED / PINNED EXTERNAL LOOT AUDIT PENDING / RUNTIME INTEGRATION NOT IMPLEMENTED

## Decision
World Bosses 2.0.2 Fabric 26.2 has its own raid-cache **opening UI with the visible list of possible rewards and chances**. Add augmentation cores as actual entries in **that same reward table and UI**, not an invisible second bonus roll after the claim. The displayed chance must equal the effective server-side chance for each specific cache and loot context.

### Target final outcome chances per successfully claimed raid cache
- Mythic augmentation core: **0.2%** unconditional per cache (average 1 per 500 eligible claims, not a guarantee).
- Epic augmentation core: initial provisional target **2.0%**.
- Legendary augmentation core: initial provisional target **0.8%**.
- Total core entries: provisional 3.0%; adapt the non-mythic proportions after the real 2.0.2 loot/config pool is inspected.
- Mythic is one tier above World Bosses' existing direct boss-kill core ceiling (Epic 80% / Legendary 20%).
- The 0.2% is **not** 0.2% conditional on a bonus-core hit, and **not** 0.2% of a rarity submenu.

### Safeguards
- Keep original boss-shard, boss element, Worldbreaker/Shadowbreaker materials and other World Bosses-exclusive progression currencies/items **at their original final effective draw probabilities**, not just nominal weights. Do not delete, redirect, or replace their reward paths.
- Reduce only low-value, common, ordinary/non-exclusive filler outcomes to make room. Do not assume an item is filler without inspecting its crafting dependencies.
- Avoid large global rarity rebalance: select the narrowest supported loot-pool edits and verify final per-item rates via normalization/simulation.
- Keep all boss-kill player core rewards unchanged. No additional guaranteed core per cache.
- Preserve exact opening UI, effects, item consumption and inventory behavior; add core entries in the visible chance list and winning result as proper item icons.
- Keep reward roll exactly once on confirmed server-side successful claim. Previewing, reopening, disconnect/retry, empty inventory, and other client interactions must not produce extra rolls.
- Do not modify or redistribute the All Rights Reserved World Bosses JAR. Use a supported config/data API if it truly drives both probability and GUI; otherwise use narrowly version-pinned, tested integration adapters.

## Exact external integration gate
The official 2.0.2 release documents commented `worldbosses.json` and raid-cache config files; the repository currently lacks verified default config contents and source-level claim callback. The modpack pinned artifact is Curse Maven `world-bosses-1545677:9254111` / version `2.0.2`.

1. Inspect the **exact pinned external JAR** and its default raid-cache files, reward category/tier schema, claim transaction, and client UI data flow.
2. Enumerate each distinct raid cache's current reward entries/weight normalization. Separate ordinary fillers from required boss-exclusive shards and crafting materials.
3. Add core registry IDs to the actual pool at true 0.2% Mythic per draw and approximate 2.0% Epic/0.8% Legendary without altering the protected outcomes' effective probabilities.
4. Prove UI chance preview and reward roll are backed by the same canonical server-authored pool; no visual-only entries or phantom bonus awards.
5. Run deterministic repeat sampling, retry/concurrency/duplicate prevention, all 11 cache variants, dedicated-server and client smoke, then in-game review.
6. Deliver rebuilt JAR + verified .mrpack only after the above succeeds.

This document **supersedes** the prior draft's 10% invisible bonus-core concept and 0.05% Mythic chance. This is a design and implementation gate, not a claim that the feature is already playable.
