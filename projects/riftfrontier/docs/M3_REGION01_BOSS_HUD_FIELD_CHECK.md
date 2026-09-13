# M3 Region 01 Boss HUD Field Check

Status: **HUMAN FIELD PLAY READY / NOT YET PLAYTESTED**

This check covers only the development-only Region 01 boss health-readability layer. It does not approve final boss naming, health balance, encounter tuning, custom HUD art, material, animation, VFX or sound.

## Reference boundary

The field actor deliberately uses Minecraft's native `ServerBossEvent` boss bar rather than inventing a Riftfrontier-specific final HUD before that visual language is reviewed. The intent is the same baseline readability already established by Minecraft's major boss encounters: when a tracked boss is present, players can read its current authoritative health without opening debug tools or relying on particles.

The neutral localization `Region 01 Boss` / `01구역 보스` is a technical field-test label, not final lore naming.

## Preconditions

Use a JAR built from the checkpoint containing the `Region01BossEntity` native boss-event integration. Run a real client; CI and Xvfb do not count as human play.

Spawn the actor:

```text
/riftfrontier boss fieldtest spawn
```

Cleanup:

```text
/kill @e[type=riftfrontier:region_01_boss]
```

## Test A — tracking visibility

1. Spawn the field boss six blocks ahead.
2. Confirm the native red progress boss bar appears while the client tracks the actor.
3. Move far enough away that the entity is no longer tracked, then return if practical.
4. Kill/remove the actor.

Expected:

- the bar is visible for the enabled field-test actor while it is tracked;
- tracking another ordinary entity does not create a Riftfrontier boss bar;
- leaving tracking/removing the actor removes the bar rather than leaving a stale HUD entry;
- the label is the neutral localized Region 01 boss name, not an invented final boss title.

## Test B — authoritative health progress

1. Spawn the field boss.
2. Use either supported player weapon loadout, for example `/riftfrontier weapon mobile`.
3. Hit the boss with the existing authoritative player-combat path.
4. Compare visible health loss with the boss-bar progress after several hits.
5. Finish or kill the actor.

Expected:

- boss-bar progress decreases from the entity's real server-owned `getHealth()/getMaxHealth()` state;
- the HUD does not maintain a second combat-health variable;
- damage rejected by the existing combat authority does not independently move the bar;
- the bar reaches/approaches zero consistently with entity death/removal.

Do not use this check to approve the current boss max health, weapon DPS, phase threshold or encounter duration. Those require human combat-tuning evidence.

## Test C — multiplayer tracking

Only mark this test attempted when two real clients (or an equivalent genuine multiplayer session) are present.

1. Put two players near the same field boss.
2. Confirm both tracked clients receive the same boss identity/progress.
3. Move one client out of entity-tracking range while the other remains near the boss.
4. Deal damage from the remaining tracked client.

Expected:

- both clients read one server-owned health value;
- the distant/untracking client loses the bar without affecting the other client;
- there is no client-local boss-health divergence.

Do not mark `MULTIPLAYER TESTED` unless this was actually performed.

## Evidence to record

Record exact JAR SHA-256, Minecraft/NeoForge version, singleplayer vs dedicated multiplayer, PASS/FAIL for A/B/C, and screenshot/video for stale bars, incorrect progress, duplicate bars or tracking inconsistencies.
