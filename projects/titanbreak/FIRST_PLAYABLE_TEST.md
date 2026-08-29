# TITANBREAK first playable test

Test the natural loop first in a fresh survival world on open surface terrain. The encounter director is intentionally disabled for Creative, Spectator and Peaceful.

## Natural progression
1. Stay in Survival on Easy or higher. Within the opening minute the hunt-introduction message should appear and normal TITANBREAK species should begin appearing around the player.
2. Hunt Ripper, Skitter, Bulwark, Needler and Howler. The director prefers species that have not yet been recorded.
3. Collect their materials and observe first-discovery Research Data plus Adaptation progression on the HUD.
4. Build a crafting table. Sneak-right-click it while carrying the following to convert it to Fabricator I: iron ingot 8, copper ingot 8, redstone 6, quartz 2.
5. Use Fabricator I to fabricate at least two augmentation modules from hunt materials.
6. From Fabricator I assemble a Surgical Bay using iron 10, copper 6, redstone 4, glass 4 and any bed 1. Place it and install the modules.
7. Once all five normal species are recorded and at least one augmentation is installed, Chrono Hound and Null Eye can begin appearing. Null Eye should jam the tactical-analysis display rather than exposing live target information while the jam lasts.
8. Record both elites, reach Adaptation Level 4 and keep at least two augmentations installed. A Pursuer warning should occur before the boss arrives.
9. Defeat The Pursuer and confirm its advanced reaction/temporal drops, boss Research Data and bonus Adaptation Points.

## Targeted fallback commands
Use these only to isolate a system if natural progression is unclear.

```mcfunction
/summon titanbreak:ripper ~ ~ ~6
/summon titanbreak:skitter ~ ~ ~6
/summon titanbreak:bulwark ~ ~ ~7
/summon titanbreak:needler ~ ~ ~12
/summon titanbreak:howler ~ ~ ~8
/summon titanbreak:chrono_hound ~ ~ ~8
/summon titanbreak:null_eye ~ ~ ~8
/summon titanbreak:the_pursuer ~ ~ ~40
```

Facility resource setup:

```mcfunction
/give @s minecraft:iron_ingot 18
/give @s minecraft:copper_ingot 14
/give @s minecraft:redstone 10
/give @s minecraft:quartz 2
/give @s minecraft:glass 4
/give @s minecraft:white_bed 1
```

Module/ability isolation:

```mcfunction
/give @s titanbreak:tactical_eye 1
/give @s titanbreak:wire_hook_arm 1
/give @s titanbreak:reflex_drive_i 1
/tick query
```

These module items still need to be installed through the Surgical Bay before their body-slot effects become active. Holding Reflex Drive I is not sufficient.

## Regression checks
- `R`: installed Reflex Drive I toggles its temporal field; `/tick query` remains ordinary 20 TPS.
- `Z` hold: tactical/thermal/ballistic analysis works only for installed analysis augmentations.
- `G`: installed Wire Hook Arm pulls toward the aimed block/entity.
- Pursuer parts remain attached and individually damageable. Use `F3+B` for hitbox inspection if needed.
- Player-facing screens must show translated names rather than raw translation keys or missing purple/black item models.
