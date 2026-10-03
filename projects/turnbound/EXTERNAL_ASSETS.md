# TURNBOUND External Assets / References

이 문서는 현재 TURNBOUND가 조사했거나 사용할 수 있는 외부 디자인/자산의 상태표다.
실제 import 규칙은 `ASSET_PIPELINE_v1.md`를 따른다.

## Directly usable candidates


### Cavehorn Ravager production base
- Type: direct_asset / editable_base
- Model + texture + idle/walk base: Tolkien Tweaks - Mobs Edition goat
- License: MIT
- Intended use: Capital Valley Warning Cave optional Elite
- Design rule: preserve the upstream horned quadruped identity; remove mount-only gear rather than rebuilding a temporary beast
- Status: adopted production base
- Tracking: `THIRD_PARTY/elite_cv_cavehorn_ravager/`

### CV-B / CV-C Capital Valley humanoid production family
- Type: direct_asset / editable_base
- Model, texture, weapon and motion base: FableCraft
- License: Apache-2.0
- Intended use: Road Cutthroat (CV-B), Hill Marksman (CV-C)
- Design rule: body + weapon share one upstream visual family; no temporary vanilla mob shell
- Status: adopted production base
- Tracking: `THIRD_PARTY/fablecraft_capital_valley/`

### New Drabyel service NPC production family
- Type: direct_asset / editable_base
- Model + texture bases: FableCraft guard_bowerstone / villager_farmer / trader / villager_blacksmith / guildmaster / summoner
- License: Apache-2.0
- Intended use: New Drabyel entrance greeter, stables/travel, equipment merchant, blacksmith, central story NPC, summon keeper
- Design rule: one coherent upstream visual family across the compact first hub; role silhouette must be readable without floating long-range nameplates
- Status: adopted production bases; exact physical placement remains 26.2 survey-gated
- Tracking: `THIRD_PARTY/drabyel_fablecraft_npcs/`

### CV-A Mossback Boar production base
- Type: direct_asset / editable_base
- Geometry + texture: Herbiary boar / wild swine
- Geometry/texture license: MIT (Avetharun)
- Locomotion/attack motion base: Photosynthesis boar animation source
- Animation-source license: MIT (Martin Floden)
- Intended use: Capital Valley first mandatory visible encounter
- Status: adopted production base
- Tracking: `THIRD_PARTY/cv_a_mossback_boar/`


### Foozle RPG UI Set 1
- Type: direct_asset / editable_base
- License: CC0
- Source: https://foozlecc.itch.io/rpg-ui-set-1
- Upstream license text: Creative Commons Zero 1.0; commercial use/modification allowed; attribution not required
- Intended use: production management UI, map chrome, character portrait/menu controls
- Status: adopted primary production skin
- Imported pieces: Panel_1, Button, Main_Button_BG, Main_Button_Overlay (+ light/dark states)
- Rule: visible management/map chrome comes from the pack; TURNBOUND code only supplies layout, text, state and live 3D content

### Kenney Fantasy UI Borders
- Type: direct_asset / editable_base
- License: CC0
- Source: https://kenney.nl/assets/fantasy-ui-borders
- Intended use: 9-slice frame / compact controls 후보
- Status: candidate

### Kenney UI Pack / RPG Expansion
- Type: direct_asset / editable_base
- License: CC0
- Source: https://kenney.nl/assets/ui-pack
- Source: https://kenney.nl/assets/ui-pack-rpg-expansion
- Intended use: button/utility component 후보
- Status: 일부 기존 조각 존재. 새 v1 UI에서 자동 채택하지 않음.

### Kenney UI Pack — Pixel Adventure
- Type: direct_asset / editable_base
- License: CC0
- Source: https://kenney.nl/assets/ui-pack-pixel-adventure
- Intended use: TURNBOUND management/map panel frame, focus/disabled/success/warning state chrome
- Status: adopted for the 26.2 client UI readability pass; external frame pixels only, navigation/layout remains TURNBOUND-specific

### Pretendard
- Type: direct_asset candidate
- License: SIL Open Font License 1.1
- Source: https://github.com/orioncactus/pretendard
- Intended use: Korean UI typography
- Status: Minecraft 26.2 font-provider/runtime 검증 전



### Quaternius Ultimate Monsters
- Type: direct_asset / editable_base candidate
- License: CC0 (official pack page verified 2026-09-30)
- Source: https://quaternius.com/packs/ultimatemonsters.html
- Contents: 50 fully animated textured monsters; FBX / OBJ / Blend / glTF
- Intended use: Av'Sal and later-region non-humanoid common/Elite/Midboss visual pool
- Status: verified candidate; no files imported yet

### Quaternius Animated Robot Pack
- Type: direct_asset / editable_base candidate
- License: CC0 (official pack page verified 2026-09-30)
- Source: https://quaternius.com/packs/animatedrobot.html
- Contents: one animated textured robot; FBX / OBJ / Blend
- Intended use: construct NPC/enemy/playable candidate where a mechanical silhouette fits the mechanic
- Status: verified candidate; no files imported yet

### Quaternius Ultimate Animated Animal Pack
- Type: direct_asset / editable_base candidate
- License: CC0 (official pack page verified 2026-09-30)
- Source: https://quaternius.com/packs/ultimateanimatedanimals.html
- Contents: 12 animated animals with attack/death/locomotion animation coverage
- Intended use: animal allies/enemies and future mount investigation
- Status: verified candidate; no files imported yet

## Reference projects — asset workflow

### MCUI
- Type: reference / code architecture
- License: GPL-3.0
- Source: https://github.com/Bluexin/mcui
- Lesson: resource-pack driven GUI/HUD theming. Visual layer can change without rewriting gameplay logic.

### CustomGUI
- Type: reference / code architecture
- License: CC0-1.0
- Source: https://github.com/omoflop/CustomGUI
- Lesson: resource pack can own GUI texture customization rather than hardcoding artwork.

### Modern UI for Minecraft
- Type: reference / optional library investigation
- Source: https://github.com/BloCamLimb/ModernUI-MC
- Lesson: third-party fonts are credited separately from code and original license/copyright notices are retained.

### Wheel of Creation
- Type: reference
- Source: https://github.com/HormigaDev/wheel-of-creation-addon
- Lesson: reused visual resources are kept under a distinct folder with the upstream LICENSE next to them.

### RealisticCraft Reimagined
- Type: reference
- Source: https://github.com/RishonDev/RealisticCraft-Reimagined
- Lesson: an unlicensed/uncleared bundled visual pack was replaced with an explicitly licensed alternative instead of assuming redistribution rights.

## Design references only

- BetterQuesting: compact nested quest surfaces and clear selected-state hierarchy. MIT source project; no pixels copied by default.
- Roughly Enough Items (REI): dense framed controls, predictable grid rhythm, tooltip hierarchy.
- user-supplied RPG screenshots: spatial hierarchy only; no proprietary pixels/fonts/icons copied.

## Runtime / model dependency

### GeckoLib
- Type: code_library
- Version currently used: 5.5.3
- Purpose: authored character/enemy model animation runtime

## External authored world

### Drehmal: APOTHEOSIS v2.2.2f
- Type: separate-install external world
- Original world/resource pack is not vendored by TURNBOUND.
- TURNBOUND stores only its own binding/profile/semantic gameplay metadata.
- map/wiki visuals are not copied into TURNBOUND assets unless their use permission is independently confirmed.

## Rule

Unknown-license files are reference-only until verified.
A file appearing on GitHub, Modrinth, CurseForge, Planet Minecraft, itch.io or a wiki does not itself prove redistribution permission.


## Creature / mount pool recheck — 2026-10-01

Official Quaternius pages were rechecked before expanding the roaming/non-humanoid direction.

- Ultimate Animated Animal Pack — CC0, 12 textured animated animals, 12+ animations each, FBX / OBJ / Blend / glTF. Candidate for wildlife, playable beasts and mounts.
- Ultimate Monsters — CC0, 50 fully animated textured monsters, FBX / OBJ / Blend / glTF. Candidate for later-region non-humanoid common/Elite/Midboss/playable silhouettes.
- Animated Robot / Sci-Fi creature pools remain candidate sources for construct enemies or allies where the world/kit supports them.

Adoption rule:
- current Capital Valley production uses already integrated licensed creature bases first
- do not import a new pack merely to increase file count
- when a new silhouette is needed, choose the external model/animation first, record license/source, then bind gameplay identity around it


### Roadhorn Mount production base
- Type: direct_asset / editable_base
- Model + texture + idle/walk base: Tolkien Tweaks - Mobs Edition goat
- Immutable upstream commit: `2e3b65a4cbe6cdd5ececdfdbf9afb658ccb805d7`
- License: MIT
- Intended use: physical-waystation rental mount for Capital Valley / Av'Sal traversal
- Design rule: this is a dedicated mount entity, not the hostile Cavehorn Elite reused as a vehicle
- Original chest and Saddle/Saddle2..Saddle7 geometry is retained
- Upstream idle/walk is retained; TURNBOUND adds dedicated gallop and jump motion
- Texture reuses the already imported unchanged upstream goat texture
- Runtime speed/jump deliberately exceed ordinary vanilla horse travel performance
- Status: adopted production base
- Tracking: `THIRD_PARTY/roadhorn_mount/`


## JourneyMap — production cartography dependency (2026-10-03)

- role: client terrain/minimap/full-map renderer for TURNBOUND exploration
- project: JourneyMap
- Minecraft / loader target: 26.2 / NeoForge
- pinned pack version: 6.0.8
- Modrinth project: lfHFW1mp
- Modrinth version id: BvreZ8yd
- API compile dependency: `info.journeymap:journeymap-api-neoforge:26.2-2.0.0`
- usage type: external runtime dependency + public plugin API
- license: All Rights Reserved; TURNBOUND does not copy JourneyMap source/classes into its JAR
- pack handling: MRPACK references the official JourneyMap download remotely with SHA-1/SHA-512 rather than copying the third-party JAR into TURNBOUND overrides
- integration boundary: JourneyMap owns cartography, zoom/pan and minimap presentation; TURNBOUND owns quest/service/NPC meaning and synchronizes those as transient waypoints
