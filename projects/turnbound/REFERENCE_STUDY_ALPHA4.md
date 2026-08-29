# TURNBOUND Reference Study — alpha.4

This document records what was actually learned from external references before the next P0 client test. It is a design/engineering audit, not a license to copy proprietary assets.

## 1. Roblox R_PG / R_PG X — primary identity reference

Verified public Roblox pages:
- R_PG place 10145990490, 갓 스튜디오
- R_PG X place 15205405381, same developer

The public descriptions establish the high-level identity that matters to TURNBOUND:
- authored town/overworld adventure rather than a sandbox-survival progression loop
- collectable spirits/characters as the player's combat roster
- overworld battles as the normal encounter layer
- powerful boss battles as major progression tests
- a repeatable space-time rift / infinite mode
- quests and multiple event/mode layers around the combat core

The public pages currently expose no running experience and do not provide enough trustworthy detail to reconstruct exact battle UI, frame timings, camera cuts, or internal formulas. Those details are therefore NOT invented or copied into TURNBOUND. If direct gameplay footage/screenshots become available later, presentation can be compared again.

Applied now:
- keep P0 focused on a clean party-battle loop rather than vanilla Minecraft survival
- keep combat actions readable and quick to select
- preserve room for overworld encounter -> battle -> result -> field flow and later boss/rift layers

Not copied:
- Roblox assets, UI textures, icons, source/code, names, proprietary art or exact layouts

## 2. TurnBasedMC — battle lifecycle/input robustness

Reference:
- MIT-licensed open-source Minecraft turn-based combat mod with Forge/NeoForge support.
- Its changelog documents battle cooldowns, frozen battle entities, target-list fixes, network refactors, and a turn timer intended to stop players from hanging a battle forever.

Applied now:
- explicit deterministic local target navigation
- invalid/downed targets are skipped by keyboard cycling
- UI click and keyboard selection feed the same existing server-authoritative ACT command
- live battle screen can no longer be accidentally dismissed with ESC

Deliberately NOT copied:
- TURNBOUND does not add a decision timer merely because TurnBasedMC has one. TURNBOUND's canonical rule says logical battle time stops while the player chooses an action; thinking time is not combat time.

## 3. Soulbound: Turnbattle — 3D party presentation reference

Reference:
- MIT-licensed Fabric mod.
- Recent versions render party members on one side, enemies on the other, and place skill VFX plus damage/heal numbers over the 3D combatants.

Applied now:
- keep the center of the battlefield visually available
- strengthen current-actor/selected-target readability without replacing the 3D scene with a menu
- preserve the existing 4v5 stand-in formation as a transition point toward authored models

Deferred to the next presentation phase:
- actual character meshes/rigs
- skill VFX
- floating damage/heal numbers
- authored animation clips

## 4. Cobblemon — server battle registry and cleanup lessons

Reference:
- MPL-2.0 open source.
- Current battle code separates battle instances/actors/party stores and has a central BattleRegistry; its history includes fixes for battles not being cleaned up after ending.
- Public issue history also shows how stale client battle UI can survive a server transition when lifecycle cleanup is incomplete.

Applied now:
- server remains authoritative for whether a battle exists
- ESC only cancels a local skill/target selection; it cannot tear down the screen while leaving a live server session behind
- the post-result return action is explicit
- emergency developer cleanup remains `/turnbound leave`

## 5. Craftics — camera/client lifecycle reference

Reference:
- All Rights Reserved; behavior can be studied, source/assets are not a reuse target.
- Its changelog documents fixes for camera/combat state leaking across disconnects and moved client state mutations onto the client/render thread.
- Later releases defaulted enemy/ally camera following and cinematic enemy turns off to preserve pacing.

Applied now:
- do not add aggressive per-action camera following to P0
- keep a stable battle overview while input/targeting is proven
- retain queued client payload handling and explicit server close snapshot
- do not use Craftics code or assets

## 6. alpha.4 P0 UX decisions

Keyboard and mouse now converge on one command path:
- `1`–`5`: choose the visible skill slot
- `Tab`: next valid target
- `Shift+Tab`: previous valid target
- `Enter`: confirm highlighted target
- `Esc`: cancel skill/target selection only
- `A`: AUTO
- `X`: x1/x2 presentation speed
- `R`: return only after battle result
- Arrow keys: temporary P0 camera yaw/pitch adjustment

The current P0 deliberately has no normal mid-battle flee button. `/turnbound leave` remains an emergency/test cleanup command.

## 7. Legal/asset boundary

R_PG and Craftics are reference-only for behavior, information hierarchy, pacing, and presentation principles.
TurnBasedMC, Soulbound and Cobblemon have open-source licenses, but TURNBOUND still does not wholesale transplant their code. Minecraft/loader/API generations and game rules differ, so only architecture/UX lessons are adopted unless a future change explicitly records compatible licensed reuse.
