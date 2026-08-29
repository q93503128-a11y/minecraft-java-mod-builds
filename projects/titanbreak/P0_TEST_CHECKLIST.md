# TITANBREAK alpha.8 T0 hunting test checklist

## Environment
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Test without other gameplay mods first.
- Multiplayer validation remains deferred.

## 1. Baseline combat display
Enter a world at full health.

PASS:
- TITANBREAK health HUD reads approximately `100 / 100` for an unmodified player;
- vanilla hearts, hunger and armor HUD remain hidden;
- existing Reflex Drive HUD behavior is unchanged.

## 2. Ripper hunt
Spawn one Ripper on open ground:

```mcfunction
/summon titanbreak:ripper ~ ~ ~6
```

PASS:
- the entity is named Ripper/리퍼;
- it pursues faster than an ordinary slow zombie and periodically tries to approach from a lateral angle at medium range;
- one successful melee sequence contains an initial strike and one delayed follow-up rather than one giant single-hit value;
- killing it drops High-Density Muscle Fiber x1-2;
- High-Density Neural Fiber appears occasionally (20% target chance);
- the first player kill of the species shows `Research Data +10` once;
- immediately summoning and killing a second Ripper does not grant another first-kill +10 message.

For a quick repeat-kill check:

```mcfunction
/summon titanbreak:ripper ~ ~ ~6
/summon titanbreak:ripper ~2 ~ ~6
```

## 3. Skitter hunt
Spawn one Skitter:

```mcfunction
/summon titanbreak:skitter ~ ~ ~6
```

PASS:
- the entity is named Skitter/스키터;
- it uses spider-like wall climbing rather than behaving as a grounded humanoid;
- a successful melee sequence can produce three separated strikes;
- killing it drops Servo Bundle x1 and Synthetic Tendon x1-2;
- the first player kill of the species shows `Research Data +10` once;
- repeat kills keep dropping physical materials but do not repeat the first-kill Research Data reward.

A wall-climb setup can be made by spawning it beside a simple wall:

```mcfunction
/summon titanbreak:skitter ~ ~ ~4
```

## 4. Reflex Drive regression with production enemies
Obtain the drive if needed:

```mcfunction
/give @s titanbreak:reflex_drive_i 1
```

Hold it in either hand and press `R` to engage. For a controlled comparison, spawn both enemies:

```mcfunction
/summon titanbreak:ripper ~-3 ~ ~8
/summon titanbreak:skitter ~3 ~ ~8
```

PASS:
- the player keeps normal movement and melee timing;
- Ripper and Skitter movement is slowed by the local temporal field;
- their multi-strike cadence slows with their AI clock instead of continuing at full real-time speed;
- neither enemy repeatedly hard-freezes due to whole-entity tick cancellation;
- `/tick query` remains at normal server tick rate.

Tick-rate verification command:

```mcfunction
/tick query
```

## 5. Existing giant regression
Spawn the multipart target:

```mcfunction
/summon titanbreak:hollow_colossus ~ ~ ~10
```

Then press `F3+B`.

PASS:
- six cyan part hitboxes remain aligned to the giant;
- part damage and leg-break movement reduction still work;
- Reflex Drive can slow the giant without breaking part alignment.

## What to report
For enemies: say whether the issue is movement, attack cadence, wall climbing/flanking, damage, or drops.
For Reflex Drive: distinguish player timing from enemy timing.
For giant hitboxes: send one front/side `F3+B` screenshot.
For crashes: attach `latest.log` or the crash report.
