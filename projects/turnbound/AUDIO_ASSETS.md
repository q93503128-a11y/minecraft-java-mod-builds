# TURNBOUND audio asset manifest

This file records the production audio mapped by `src/main/resources/assets/turnbound/sounds.json`.
Only assets with explicit CC0/public-domain evidence are used in this batch.

## Music

| Target | Source asset | Creator | License | Evidence |
|---|---|---|---|---|
| `sounds/music/hub.ogg` | `GabrielGLevine/wandering-inn-rpg/wandering_inn_game/assets/audio/music/junkala_home_town.ogg` | Juhani Junkala | CC0 | Upstream `ATTRIBUTION.md`: JRPG music packs by Juhani Junkala — CC0 |
| `sounds/music/region_explore.ogg` | `GabrielGLevine/wandering-inn-rpg/wandering_inn_game/assets/audio/music/junkala_sunshine_coast.ogg` | Juhani Junkala | CC0 | Same upstream attribution |
| `sounds/music/battle_normal.ogg` | `GabrielGLevine/wandering-inn-rpg/wandering_inn_game/assets/audio/music/junkala_preparing_battle.ogg` | Juhani Junkala | CC0 | Same upstream attribution |
| `sounds/music/battle_elite.ogg` | `GabrielGLevine/wandering-inn-rpg/wandering_inn_game/assets/audio/music/junkala_encounter_witches.ogg` | Juhani Junkala | CC0 | Same upstream attribution |
| `sounds/music/battle_boss.ogg` | `GabrielGLevine/wandering-inn-rpg/wandering_inn_game/assets/audio/music/junkala_army_approaching.ogg` | Juhani Junkala | CC0 | Same upstream attribution |
| `sounds/music/battle_final.ogg` | `jarlah/dungeon-haskell/assets/music/boss.ogg` | Juhani Junkala | CC0 | Upstream `assets/CREDITS.md`: “Epic Boss Battle” — CC0, seamless dark orchestral boss theme |

Upstream license references:
- `https://github.com/GabrielGLevine/wandering-inn-rpg/blob/main/ATTRIBUTION.md`
- `https://github.com/jarlah/dungeon-haskell/blob/master/assets/CREDITS.md`

## Sound effects

All eleven SFX in this batch are copied from `lavenderdotpet/CC0-Public-Domain-Sounds`, whose repository license is CC0-1.0 and whose RPG SFX folder is explicitly distributed as `80-CC0-RPG-SFX`.

| Target | Source asset |
|---|---|
| `sounds/sfx/skill.ogg` | `80-CC0-RPG-SFX/spell_01.ogg` |
| `sounds/sfx/hit_light.ogg` | `80-CC0-RPG-SFX/blade_01.ogg` |
| `sounds/sfx/hit_heavy.ogg` | `80-CC0-RPG-SFX/metal_01.ogg` |
| `sounds/sfx/reaction_hit.ogg` | `80-CC0-RPG-SFX/creature_hurt_01.ogg` |
| `sounds/sfx/dot_tick.ogg` | `80-CC0-RPG-SFX/spell_fire_07.ogg` |
| `sounds/sfx/heal.ogg` | `80-CC0-RPG-SFX/item_gem_01.ogg` |
| `sounds/sfx/barrier.ogg` | `80-CC0-RPG-SFX/item_gem_04.ogg` |
| `sounds/sfx/revive.ogg` | `80-CC0-RPG-SFX/spell_02.ogg` |
| `sounds/sfx/down.ogg` | `80-CC0-RPG-SFX/creature_die_01.ogg` |
| `sounds/sfx/boss_phase.ogg` | `80-CC0-RPG-SFX/creature_roar_02.ogg` |
| `sounds/sfx/spawn.ogg` | `80-CC0-RPG-SFX/spell_fire_03.ogg` |

Upstream license reference:
- `https://github.com/lavenderdotpet/CC0-Public-Domain-Sounds`

## Runtime contract

The filenames above are canonical. They must stay synchronized with:
- `src/main/resources/assets/turnbound/sounds.json`
- `TurnboundSounds`
- `ClientAudioDirector`
- `ClientAudioPlayback`

The BGM files are streamed by Minecraft. The SFX files are non-streaming one-shots.
