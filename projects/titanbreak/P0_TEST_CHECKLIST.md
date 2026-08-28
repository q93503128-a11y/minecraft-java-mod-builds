# TITANBREAK P0 alpha.5 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test without other gameplay mods first.
- Multiplayer validation remains deferred.

## 1. Reflex Drive temporal separation
- Obtain `반응가속기 I` from the Combat creative tab and hold it in either hand.
- Press `R` once to engage and again to disengage.
- Run `/tick query` while active if cheats are enabled.

PASS:
- the world remains near 8 TPS while active and returns to 20 TPS after disengaging;
- mobs/projectiles/world simulation have the same smooth slow-motion cadence as ordinary `/tick rate 8`;
- the local player no longer feels slowed to the same degree as the world;
- walking, sprinting, melee recovery, mining and ordinary item use stay close to normal real-time pacing.

## 2. Safety restore
- Test overheat shutdown, death/respawn and world exit/reload.
- The world must always recover to 20 TPS when the drive is no longer active.
- Movement/attack/mining modifiers must disappear immediately after deactivation.

## 3. Multipart giant target
1. Run `/summon titanbreak:hollow_colossus ~ ~ ~10` on open ground.
2. Press `F3+B`.
3. The small parent anchor may still be visible, but six cyan TITANBREAK part boxes should cover the rendered humanoid: head, core/torso, left/right arms, left/right legs.
4. The part boxes must rotate and travel with the giant.
5. Hit head/core/arms/legs separately. The parent anchor itself must not be a valid attack target.
6. Destroy one leg and confirm movement drops sharply; destroy both and confirm near-immobilization.
7. Engage Reflex Drive near the giant and confirm the part boxes remain aligned during slow motion.

The six regions are intentionally coarse. Bone/OBB refinement comes after this alignment test passes.

## Bug report
Send the symptom in plain language. For hitbox problems, send one F3+B screenshot from the front or side. For crashes, attach `latest.log` or the crash report.
