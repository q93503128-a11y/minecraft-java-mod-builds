# TITANBREAK P0 alpha.6 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test without other gameplay mods first.
- Multiplayer validation remains deferred.

## 1. Reflex Drive local temporal field
- Obtain `반응가속기 I` from the Combat creative tab and hold it in either hand.
- Press `R` once to engage and again to disengage.
- Run `/tick query` while active if cheats are enabled.

PASS:
- the server/world tick rate remains at the ordinary 20 TPS while the drive is active and after it disengages;
- ordinary nearby mobs and their projectiles advance at roughly 40% of normal P0 pacing inside the 64-block field;
- the drive user keeps ordinary wall-clock walking, sprinting, jumping, falling and camera control without global-tick compensation;
- melee hand-swing animation and attack-strength recovery both remain close to ordinary wall-clock pacing;
- mining, block interaction, bow/crossbow charging, food/item use and item cooldowns remain close to ordinary wall-clock pacing;
- projectiles fired by the drive user inherit the user's time axis while they remain inside the field;
- entities outside the field are not intentionally slowed by the drive.

## 2. Player timer regression sweep
While the field is active, compare each action once with the drive disabled:
- receive knockback and land;
- take two closely spaced hits and observe hurt/invulnerability timing;
- use a shield and an item with a cooldown;
- drink/eat or fully charge a bow/crossbow;
- jump from a small ledge and compare gravity/fall pacing.

Report any action that still takes materially longer or shorter in real time with the drive active.

## 3. Safety restore
- Test overheat shutdown, death/respawn and world exit/reload.
- The drive field must disappear immediately when the drive is no longer active.
- No movement/attack/mining compensation modifier may remain after deactivation.
- `/tick query` must not be changed by activating or deactivating Reflex Drive.

## 4. Multipart giant target
1. Run `/summon titanbreak:hollow_colossus ~ ~ ~10` on open ground.
2. Press `F3+B`.
3. The small parent anchor may still be visible, but six cyan TITANBREAK part boxes should cover the rendered humanoid: head, core/torso, left/right arms, left/right legs.
4. The part boxes must rotate and travel with the giant.
5. Hit head/core/arms/legs separately. The parent anchor itself must not be a valid attack target.
6. Destroy one leg and confirm movement drops sharply; destroy both and confirm near-immobilization.
7. Engage Reflex Drive near the giant and confirm the giant slows while the six part boxes stay aligned.

The six regions are intentionally coarse. Bone/OBB refinement comes after this alignment test passes.

## Known alpha.6 P0 boundary
The local field currently throttles entity simulation. Range-local block ticks/redstone/weather/time-of-day simulation and dedicated-server client-side field synchronization are not yet part of this singleplayer P0 pass.

## Bug report
Send the symptom in plain language. For hitbox problems, send one F3+B screenshot from the front or side. For crashes, attach `latest.log` or the crash report.
