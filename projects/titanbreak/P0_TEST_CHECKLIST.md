# TITANBREAK P0 alpha.3 singleplayer test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test without other gameplay mods first.
- Multiplayer validation remains deferred.

## 1. HUD / armor replacement
- Confirm vanilla hearts, armor icons, and hunger icons are hidden.
- Confirm the replacement health and mentality gauges stay on-screen at different GUI scales.
- Take damage and heal once; confirm health follows the real value.
- Try equipping vanilla armor; it must not become part of normal progression or restore the vanilla armor HUD.

## 2. Reflex Drive input
- Obtain `반응가속기 I` from the Combat creative tab.
- Hold it in either main hand or off hand.
- Press `R` once to engage and `R` again to disengage. Crouching is no longer part of activation.
- If R has been rebound in Controls, use the rebound key.

PASS: one press reliably toggles the ability and the HUD changes state.
FAIL: no response, repeated presses required, or the ability activates without the item.

## 3. Global slow-motion proof
- Spawn a zombie, skeleton, passive animal, and several projectiles.
- Engage Reflex Drive.
- The world simulation should slow globally rather than entities moving in stop-start tick skips.
- The local player should remain substantially more responsive/faster relative to the world.
- Disengage and confirm normal speed returns without teleporting or permanent AI freeze.

Report any stutter, input delay, projectile jump, sound oddity, or world remaining slow after disengage.

## 4. Heat / recovery
- Keep Reflex Drive active until it overheats.
- Confirm it shuts off and cannot be immediately re-enabled.
- Let it cool and confirm it can be enabled again.
- Test death, respawn, world exit/reload, and dimension change once. The world must not remain stuck below normal tick rate.

## 5. Breach separation
- While Reflex Drive is active, sprint into leaves, glass, dirt, planks, and logs.
- Reflex Drive alone must NOT break terrain. Breach is reserved for later heavy-frame / reinforced-body augment builds.

## 6. Multipart giant target
1. Run `/summon titanbreak:hollow_colossus ~ ~ ~10` on open ground.
2. Press `F3+B`.
3. The placeholder should be a giant humanoid target, not a pig.
4. Verify the small parent anchor does not cover the body.
5. Verify eight distinct part boxes are visible: head, core, left/right arms, left/right shoulders, left/right legs.
6. Hit head/core/arms/legs separately.
7. Destroy one leg and confirm mobility drops noticeably; destroy both and confirm movement becomes nearly disabled.
8. Move away and return; part boxes must remain attached to the target.
9. Engage Reflex Drive near it and confirm the body and multipart boxes stay aligned.

## Bug report
For each problem, send the symptom in plain language. Add a screenshot/video for visual issues and `latest.log` or crash-report for crashes.
