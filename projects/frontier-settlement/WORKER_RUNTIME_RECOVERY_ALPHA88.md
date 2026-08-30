# Frontier Settlement Alpha.88 — worker runtime recovery

Version: `0.1.0-alpha.88`

Alpha.88 is a save-compatible runtime correction over Alpha.87 and adds no required settlement SavedData field.

## Production movement
- Frontier workers remain `PathfinderMob` entities with no villager profession, trade, POI, Brain, bed/jobsite, or schedule authority.
- Work services no longer send ground navigation to the solid log, quarry block, barrel, or fenced work-center itself.
- Each physical move order searches only loaded nearby cells around the interaction target and uses a standable feet/head position with solid dry footing.
- There is no worker teleport and no resource chunk force-load.
- Ordinary resource workers are normalized to `NoAI=false` and `invulnerable=false` on their work tick, repairing stale runtime flags from earlier playtest saves.
- Resource-worker lookup uses the same 56-block local route envelope as Alpha.87 search, so a lumber worker walking toward a tree up to 48 blocks from the camp does not fall out of assignment lookup halfway through the trip.

## 100% completion recovery
Alpha.87 removed the builder-return gate, but finalization still asked for a usable site crate and successful temporary-scaffold cleanup before committing the building record. Those cleanup details could still leave a physically finished structure active at 100%.

Alpha.88 makes the validated physical blueprint the actual commit boundary. The settlement building record commits and the active builder becomes damageable before scaffold cleanup, empty-barrel removal, or return-home navigation. Cleanup then runs best-effort on loaded blocks only. Real leftover cargo remains in the site barrel or builder hand.

A pre-existing Alpha.87 save stuck at 100% should complete after the finished site is loaded and validates.

## Update boundary
Same-world update: fully close Minecraft, back up the world, replace/install Alpha.88, and reopen the same save. This is not a JVM hot reload.
