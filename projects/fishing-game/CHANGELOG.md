# Changelog

## 0.1.0-alpha.3

- Added the first dedicated fishing dimension, `fishinggame:lakeside`, instead of relying on ordinary survival terrain.
- Added an authored Cheongram Lakeside environment with a central fishing lake, multiple piers, pavilion, tackle shelter, paths, trees, shoreline and rock dressing.
- Added boundary recovery and fixed daytime maintenance for the dedicated fishing location.
- Added physical fish presentation: a server-controlled fish becomes visible underwater before the bite, approaches the bobber and visibly fights around the hook during reeling.
- Kept catch species hidden from the HUD until the bite while allowing the server to preselect the visual proxy consistently.
- Added cleanup for temporary hooked-fish visuals on cancel, catch, line loss and disconnect.
- Updated the fishing build workflow to verify the alpha.3 JAR, world dimension resource and fishing world class.
- Kept the playtest gate unchanged: build success is not enough until the actual Minecraft screen/play pass is acceptable.

## 0.1.0-alpha.2

- Converted project direction from survival-adjacent mod to standalone fishing game.
- Added persistent catch bag, coins and three-tier rod progression.
- Expanded catalog to freshwater, coast and deep-sea species pools.
- Added server-authoritative sell-all and rod purchase actions.
- Added player-safe Adventure-mode rules and removed survival HUD layers.
- Added dedicated fishing HUD and catch-bag/rod screen using Kenney CC0 UI assets.
- Kept Essential optional and external to game-state authority.
- User-test gate raised: no more tiny technical JARs before a meaningful fishing-game slice.
