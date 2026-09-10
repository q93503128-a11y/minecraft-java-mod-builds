# Changelog

## 0.1.0-alpha.5

- Replaced the obvious circular hooked-fish motion with a curved final approach and irregular fight motion.
- Added species-size scaling for temporary fish proxies so small freshwater fish and large catches no longer read at the same physical size.
- Added short fish pull bursts during reeling. Bursts visibly extend the fish away from the hook and apply a small server-authoritative tension impulse so visuals and mechanics agree.
- Added bubble trails on the final approach plus stronger bite and successful-catch splash/audio feedback.
- Reworked temporary proxy selection to avoid inappropriate random pufferfish shapes for ordinary species.
- Added deterministic presentation math tests for fish scale, hook approach and burst behavior.
- Kept the existing user-test gate: build success still does not substitute for actual Minecraft visual review.

## 0.1.0-alpha.4

- Made the reel fight readable instead of hiding its rules: the HUD now shows the actual safe-tension band used by server-side catch progress.
- Added live reel guidance that changes between reel in, release and maintain rhythm based on current tension.
- Replaced the old fill-only tension display with a safe-band plus moving tension marker so line state is easier to judge at a glance.
- Added a compact catch-result card after a successful catch with species, rarity, weight, length and value.
- Kept the result card client-presentation-only; catch ownership, size/value, coins and progression remain server authoritative.
- Kept the playtest gate unchanged: the lake composition and fish motion still require actual Minecraft visual review before this is declared ready for the user's playtest.

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
