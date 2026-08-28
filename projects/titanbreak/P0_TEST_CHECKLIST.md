# TITANBREAK P0 alpha.4 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test without other gameplay mods first.
- Multiplayer validation remains deferred.

## 1. Reflex Drive input and real tick-rate slow motion
- Obtain `반응가속기 I` from the Combat creative tab.
- Hold it in either main hand or off hand.
- Press `R` once to engage and `R` again to disengage.
- While active, run `/tick query` if cheats are enabled.

PASS:
- active world tick rate reports about 8 TPS;
- disengaged world tick rate returns to 20 TPS;
- mobs/projectiles/world simulation look like ordinary `/tick rate 8` slow motion instead of stop-start motion.

FAIL:
- client/world snaps between slow and normal speed;
- entities move in visible chunks;
- the world remains below 20 TPS after disengaging, death, or reloading the world.

The client no longer forces its own level tick rate back to 20 while Reflex Drive is active. User speed compensation is separate from world time.

## 2. Player relative-speed compensation
- Walk, sprint, jump, attack, place blocks, open containers, and fire projectiles while Reflex Drive is active.
- Report whether the player feels too slow, too fast, jittery, or correction-heavy relative to the slowed world.
- Terrain must not break from Reflex Drive movement alone.

## 3. HUD / armor replacement
- Confirm vanilla hearts, armor icons, and hunger icons are hidden.
- Confirm replacement health and mentality gauges remain visible at different GUI scales.
- Take damage and heal once.
- Try equipping vanilla armor and confirm it does not return vanilla armor progression/HUD.

## 4. Heat / recovery / safety restore
- Keep Reflex Drive active until overheat shutdown.
- Confirm immediate reactivation is blocked until sufficient cooling.
- Test death/respawn, world exit/reload, and dimension travel.
- World tick rate must always recover to 20 TPS when the drive is no longer active.

## 5. Multipart giant target — alpha.4 rebuild
1. Run `/summon titanbreak:hollow_colossus ~ ~ ~10` on open ground.
2. Press `F3+B`.
3. The giant humanoid's real parent physical box should be approximately the visible Giant size rather than an ant-sized anchor.
4. Verify six distinct attack parts approximately cover the rendered body: head, core/torso, left/right arms, left/right legs.
5. Confirm the boxes rotate and travel with the giant instead of lagging behind or snapping from another location.
6. Hit head/core/arms/legs separately.
7. Destroy one leg and confirm movement drops sharply; destroy both and confirm near-immobilization.
8. Engage Reflex Drive near the giant and confirm body and part boxes remain aligned during slow motion.

This P0 deliberately uses six coarse humanoid regions. More detailed bone/OBB hitboxes come only after this coarse alignment passes.

## Bug report
Send the symptom in plain language. For visual hitbox problems, send one F3+B screenshot from the front or side. For crashes, attach `latest.log` or the crash report.
