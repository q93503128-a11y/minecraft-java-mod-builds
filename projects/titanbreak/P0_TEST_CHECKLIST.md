# TITANBREAK P0 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test the generated TITANBREAK JAR without other gameplay mods first.
- Multiplayer validation is intentionally deferred until multiplayer testing is available.

## P0.1 — replacement HUD and persistent player state
1. Enter a world in Survival and confirm vanilla hearts, armor icons, and hunger icons are not rendered.
2. Confirm the replacement HP and 정신력 bars render without clipping at the normal GUI scale.
3. Change GUI scale once and verify the HUD remains on-screen.
4. Take damage and confirm the HP rail follows actual health immediately.
5. Exit to title and reload the world; confirm the mod loads normally and the player profile remains valid.
6. Die and respawn once; confirm the replacement HUD returns and no save/profile reset or crash occurs.
7. Confirm the HUD never exposes internal implementation labels, debug values, or developer-only terminology.

Report:
- screenshot of the HUD at the normal GUI scale
- any overlap/clipping
- whether death, reload, or dimension change causes a crash or resets state

## P0.1 — Reflex Drive I time field
Setup:
- Obtain `Reflex Drive I` from the Combat creative tab.
- Put it in the off hand.
- Crouch to maintain the field; release crouch to disengage it.
- This off-hand + crouch interaction exists only for the current singleplayer technology test and is not the final augmentation UX.

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

## P0.2 — multipart giant target
Setup:
1. Use a test world with cheats enabled.
2. Run `/summon titanbreak:hollow_colossus ~ ~ ~8` on open flat ground.
3. Press `F3+B` so the separate body-part hitboxes are visible. The current visible creature is only a technical placeholder; it is not a final boss model or art direction.

Tests:
1. Confirm the target appears without a crash and moves normally before any part is destroyed.
2. Confirm separate hitboxes exist around the head, core, both arms, both shoulders, and both legs rather than one oversized box only.
3. Attack the head and core hitboxes separately. Confirm hits register when the crosshair is on those part boxes.
4. Attack only one leg hitbox repeatedly until it stops accepting further hits. Confirm target movement becomes noticeably slower.
5. Repeat on the second leg. Confirm destroying both leg parts reduces mobility much more strongly and the target can no longer wander normally.
6. Move at least one chunk away and return. Confirm the part hitboxes still track the parent rather than remaining at an old position.
7. Activate Reflex Drive I near the target and confirm the parent and all hit regions continue to move together while slowed.
8. Save and reload the world with the target present. Report any crash, duplicate parts, detached hitboxes, or invalid entity state. Part-specific damage persistence is not an acceptance requirement yet.

Report:
- whether all eight body-part boxes were visible and hittable
- whether one-leg and two-leg destruction changed movement as described
- any detached, duplicated, invisible, or incorrectly positioned hitbox
- any crash or severe lag with the target present
- screenshot or short video with `F3+B` enabled if anything looks wrong

## P0 acceptance deferred to later patches
The following are intentionally not player-testable yet:
- player-pose afterimages using the final player-animation integration
- production-scale giant model, animation, and visual body-part feedback
- full boss attack-state transitions beyond the current leg-mobility proof
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
