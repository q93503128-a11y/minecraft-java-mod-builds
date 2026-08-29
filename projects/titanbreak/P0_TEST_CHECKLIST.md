# TITANBREAK P0 alpha.7 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test without other gameplay mods first.
- Multiplayer validation remains deferred.

## 1. Reflex Drive continuous local temporal field
- Obtain `반응가속기 I` from the Combat creative tab and hold it in either hand.
- Press `R` once to engage and again to disengage.
- Run `/tick query` while active if cheats are enabled.

PASS:
- `/tick query` remains at the ordinary 20 TPS while the drive is active and after it disengages;
- the drive user keeps ordinary wall-clock walking, sprinting, jumping, falling, melee swing/recovery, mining and item-use timing;
- nearby ordinary mobs move at roughly 40% pacing without the obvious move-freeze-move cadence caused by complete entity tick cancellation;
- mob attack/goal cadence also slows instead of only their ground speed changing;
- entities outside the 64-block field are unaffected.

## 2. Smoothness comparison
Spawn or find at least one continuously moving mob and watch it from the side with the drive enabled.

PASS:
- walking should remain visually continuous rather than holding the exact same world position for multiple complete server ticks;
- turning/path changes may be slower because AI itself is time-dilated, but the body should not repeatedly hard-freeze between allowed entity ticks;
- crossing the field boundary should change speed without teleporting the mob.

If it still visibly stutters, send a short description of whether the problem is position stepping, rotation stepping, animation stepping, or all three.

## 3. Projectile split
- Let a skeleton or another ordinary hostile mob fire a projectile through the field.
- Fire a bow yourself while your Reflex Drive is active.
- If practical, let a hostile projectile cross the field boundary.

PASS:
- hostile projectiles are visibly slower inside the field and continue moving every frame/tick rather than freezing on skipped entity ticks;
- your own projectile does not get slowed by your own field;
- entering/leaving the field does not cause a large position teleport or sudden reversal.

Projectile gravity/drag parity is not final in alpha.7; report obvious trajectory distortion separately from stutter.

## 4. Player timer regression sweep
While the field is active, compare each action once with the drive disabled:
- receive knockback and land;
- take two closely spaced hits and observe hurt/invulnerability timing;
- use a shield and an item with a cooldown;
- drink/eat or fully charge a bow/crossbow;
- jump from a small ledge and compare gravity/fall pacing.

The drive user should remain on the normal 20 TPS time axis for all of these.

## 5. Safety restore
- Test overheat shutdown, death/respawn and world exit/reload.
- The drive field must disappear immediately when the drive is no longer active.
- No movement/attack/mining compensation modifier may remain after deactivation.
- `/tick query` must never be changed by Reflex Drive.

## 6. Multipart giant target
1. Run `/summon titanbreak:hollow_colossus ~ ~ ~10` on open ground.
2. Press `F3+B`.
3. Confirm the six cyan TITANBREAK part boxes remain aligned to head, torso, arms and legs.
4. Engage Reflex Drive and watch the giant walk/turn.
5. Confirm the parent and all six parts travel together while the giant is time-dilated.
6. Hit the separate regions again and confirm part damage still works.

## Known alpha.7 P0 boundary
The field currently splits entity movement, mob AI and projectile motion onto local temporal paths. Range-local block ticks/redstone/weather/time-of-day, every custom entity timer, exact projectile gravity/drag parity and dedicated-server presentation synchronization remain later P0 work.

## Bug report
Send the symptom in plain language. For hitbox problems, send one F3+B screenshot from the front or side. For crashes, attach `latest.log` or the crash report.
