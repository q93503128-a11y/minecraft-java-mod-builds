# TITANBREAK P0 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test the generated TITANBREAK JAR without other gameplay mods first.
- Multiplayer validation is intentionally deferred until multiplayer testing is available.

## P0.1 — replacement HUD and persistent player state
1. Enter a world in Survival and confirm vanilla hearts, armor icons, and hunger icons are not rendered.
2. Confirm the replacement HP and 정신 bars render without clipping at the normal GUI scale.
3. Change GUI scale once and verify the HUD remains on-screen.
4. Take damage and confirm the HP rail follows actual health immediately.
5. Exit to title and reload the world; confirm the mod loads normally and the player profile remains valid.
6. Die and respawn once; confirm the replacement HUD returns and no save/profile reset or crash occurs.

Report:
- screenshot of the HUD at the normal GUI scale
- any overlap/clipping
- whether death, reload, or dimension change causes a crash or resets state

## P0.1 — Reflex Drive I time field
Setup:
- Obtain `Reflex Drive I` from the Combat creative tab.
- Put it in the off hand.
- Crouch to maintain the field; release crouch to disengage it.

Tests:
1. Put a zombie or similar mob 5–15 blocks away. Crouch and verify its motion/AI becomes dramatically slower while the player remains responsive.
2. Put another mob more than roughly 96 blocks away and verify it is not affected.
3. Fire or observe projectiles inside the field and report whether projectile motion visibly slows. If a projectile type does not slow, record its exact type.
4. Release crouch and verify affected entities return to normal without teleporting, duplicating, or freezing permanently.
5. Hold the field long enough for heat to reach the limit. Confirm the field disengages rather than continuing indefinitely.
6. Release crouch to cool down, then activate again.
7. Spawn roughly 30 mobs in range and repeat. Note any visible FPS or simulation hitching.

Report:
- mob types tested
- projectile types tested
- whether each slowed correctly
- any animation stutter, teleport, permanent AI freeze, sound oddity, or crash
- approximate FPS before/while active if the difference is obvious

## P0.1 — augmented sprint / soft-terrain breach
1. Activate Reflex Drive I and sprint on flat ground. Confirm the player accelerates beyond vanilla sprint speed but remains steerable.
2. Sprint into leaves, glass, dirt, planks, and logs separately.
3. Confirm low-resistance blocks can be broken during the charge.
4. Sprint toward a chest or other block-entity container and confirm it is not automatically destroyed by the breach pass.
5. Try a hard block such as obsidian and confirm it is not breached.
6. Repeat near a wall corner and on uneven ground. Watch for clipping, rubber-banding, suffocation, or being trapped inside blocks.

Report:
- blocks that broke correctly
- blocks that broke but should not have
- any clipping/rubber-band/suffocation cases
- whether the speed feels controllable or unusably fast

## P0 acceptance deferred to later patches
The following are intentionally not player-testable in P0.1 yet and will be added after the base project passes clean CI:
- player-pose afterimages
- large multipart target/boss with independent body-part hitboxes
- body-part destruction changing AI/attack state
- multiplayer relative-time validation between different Reflex Drive ratings

## Bug report format
For each issue provide, when possible:
1. What you were doing.
2. What you expected.
3. What actually happened.
4. Whether it happens every time.
5. Screenshot/video for visual problems.
6. `latest.log` or crash report for crashes.
7. Exact mob/block/projectile involved.
