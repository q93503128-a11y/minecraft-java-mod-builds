# Third-party notices

## Skill Proficiencies
Source project: `balovich-matje/skill-proficiencies`  
Copyright (c) 2026 balovich-matje  
License: MIT License

Survival Ascension adapts permissively licensed patterns for generic per-skill XP storage, NeoForge sync, recent-skill XP HUD behavior, crop classification, and skill/help-screen information architecture. Full notice: `META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt`.

## Veinminer++
Source project: `kestalkayden/veinminer-plus-plus`  
Copyright (c) 2026 Kestalkayden  
License: MIT License

Survival Ascension adapts ore-family equivalence matching, bounded connected traversal, normal player destroy-controller handling, smart-tree leaf-safety and tick-drained large-tree work. Full notice: `META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt`.

## MineMenu
Source project: `GirafiStudios/MineMenu`  
Copyright (c) 2013 Dylan Miller  
License: MIT License

Survival Ascension adapts MineMenu's radial interaction/presentation model, including ring proportions, mouse-angle segment selection, selected-segment expansion, icon placement, live-world overlay and translucent palette. 0.28+ reuses Survival Ascension's existing adapted radial implementation for the nested Industrial Works menu. Full notice: `META-INF/third-party/MINEMENU_MIT.txt`.

## Building Gadgets 2
Source project: `Direwolf20-MC/BuildingGadgets2`  
Copyright (c) 2023 Direwolf20-MC  
License: MIT License

Survival Ascension independently implements Construction after studying Building Gadgets 2's permissively licensed material-backed building, interaction/protection checks, NeoForge placement hooks and tick-distributed work. 0.29+ linked-Barrel/outpost material resolution is new Survival Ascension code; no Building Gadgets storage/network implementation is copied. Full notice: `META-INF/third-party/BUILDING_GADGETS_2_MIT.txt`.

## Mob Champions
Source project: `wendall911/MobChampions`  
Copyright (c) 2024 Wendall Cada  
License: MIT License

Survival Ascension adapts Mob Champions' permissively licensed patterns for rank-driven permanent entity attribute modifiers and rank-aware hostile-mob construction. Full notice: `META-INF/third-party/MOB_CHAMPIONS_MIT.txt`.

## Apotheosis
Source project: `Shadows-of-Fire/Apotheosis`  
Copyright (c) 2018-2025 Stormraven Studios, LLC  
Official GitHub `26.1` code license: MIT License

Survival Ascension adapts the high-level separation of loot rarity, item category and affix generation from the MIT-licensed official GitHub source. Distribution-page/assets rights are treated separately. No Apotheosis textures, models, sounds, data files, GUI assets or datapacks are bundled. Full code notice: `META-INF/third-party/APOTHEOSIS_MIT.txt`.

## Mekanism
Source project: `mekanism/Mekanism`  
Copyright (c) 2017-2025 Aidan C. Brady  
License: MIT License

Survival Ascension adapts the Digital Miner's high-level target-filtered bounded-search idea for Mining Lv.90 Extract. No Mekanism assets, machines, energy systems, filters, GUIs, data files or namespaces are bundled. Full notice: `META-INF/third-party/MEKANISM_MIT.txt`.

## Warband
Source project: `Renasca-Studios/Warband`  
Copyright (c) 2026 Divesh Gupta  
License: MIT License

Survival Ascension adapts Warband's lightweight tactical-squad concepts: persistent squad membership, shared target focus, explicit combat roles, and a temporary rout window after the leader falls. Full notice: `META-INF/third-party/WARBAND_MIT.txt`.

## Hostiles Are Too Easy
Source project: `MinecraftIsTooEasy/HostilesAreTooEasy`  
License: CC0 1.0 Universal

Survival Ascension adapts boss/progression-driven world difficulty and the Withered/Phase/Plague vocabulary with its own modern spawn events, persistent NBT, reaction cooldowns, rewards and stage gates. Runtime notice: `META-INF/third-party/HOSTILES_ARE_TOO_EASY_CC0.txt`.

## Gateways to Eternity
Source project: `Shadows-of-Fire/GatewaysToEternity`  
Copyright (c) 2020 Brennan Ward  
License: MIT License

Survival Ascension adapts timed sequential-wave encounter lifecycle, boss-bar status and wave-changing modifier concepts into its own Ascension Trial doctrines, costs, vanilla compositions and failure rules. Full notice: `META-INF/third-party/GATEWAYS_TO_ETERNITY_MIT.txt`.

## Create — design reference for 0.28–0.30
Source project: `Creators-of-Create/Create`  
Current repository `LICENSE.md`: code MIT; all files under `./src/main/resources/assets/` All Rights Reserved.

Survival Ascension 0.28 studies only the high-level product philosophy of turning large throughput into multi-input processing stages and logistics value. 0.29 additionally studies stock-backed request/local-restocking ideas. 0.30 keeps the same server-authoritative logistics boundary: all depot/outpost actions are revalidated against actual blocks, loaded chunks and `mayInteract`, rather than trusting client coordinates or UI state.

The Survival Ascension implementation is independent: `field_depots_v1`, `outpost_v1`, vanilla Barrel coordinates, physical camp-block detection, same-dimension/radius checks, loaded-chunk-only container access and player-first exact-item consumption. No Create logistics source implementation, Packager/Stock Link/Stock Ticker/Requester code, package formats, blocks, assets, textures, models, sounds, recipes, processing data, machines, GUI/data, namespaces or Ponder content are copied or bundled.

## MineColonies — reference only for 0.30
Source project: MineColonies  
License: GNU General Public License version 3 (GPLv3)

Survival Ascension studies only the high-level product lesson that a forward settlement becomes meaningful when physical facilities, supply and local defense are combined. The 0.30 outpost is independent Survival Ascension code using a registered vanilla Barrel plus nearby Bed/Campfire/Crafting/Furnace blocks, owner-nearby activation and a NATURAL-hostile-only spawn filter. No MineColonies source code, citizens/workers, builders/couriers, blueprints, structures, claims, research tree, raids, quests, GUI, assets, data files or namespaces are copied or bundled.

## Waystones — reference only for 0.31
Source project: `TwelveIterations/Waystones`  
Current official `26.2` branch license: All Rights Reserved

Survival Ascension studies only the product-level tradeoff between travel convenience and preserving the value of world traversal. 0.31 deliberately does not implement general waystone/outpost fast travel: the independent `field_recovery_v1` system only returns a player after a qualifying death and only with a prepaid one-use contract. No Waystones source code, Waystone/Warp Stone/Return Scroll blocks or items, teleport network, menus, data, assets, icons, models, recipes, configuration or namespace are copied or bundled.

## Corpse — reference only for 0.31
Source project: `denmeh/Corpse`  
License: GNU Lesser General Public License version 3 (LGPL-3.0)

Survival Ascension studies only the high-level product goal of reducing repetitive travel after death. 0.31 does not create or copy a corpse container/entity and does not preserve, move or expose dropped inventory through a Corpse-style system. Field recovery is independent Survival Ascension SavedData plus server-side death qualification and safe post-respawn teleport. No Corpse source code, inventory-storage/transfer implementation, entity, GUI, data, assets, configuration or namespace are copied or bundled.

## Heracles — design reference only for 0.32
Source project: `terrarium-earth/Heracles`  
Copyright (c) 2023 Terrarium Earth  
License: MIT License

Survival Ascension studies only the product-level idea that multi-step repeatable tasks should keep explicit objective and completion state. The 0.32 operation system is independent Survival Ascension code built around owned physical outposts, stage-specific outbound range gates, existing validated ExpeditionAction hooks, exact-origin return, per-player persistence and its own rewards. No Heracles quest data, quest trees, editor, UI, source structures, assets, configuration, import formats or namespace are copied or bundled.

## Silent Gear — design reference only for 0.27
Source project: Silent Gear  
Current project license: MIT

Survival Ascension studies only the product philosophy that long-lived equipment progression should preserve item identity while continuing to create reasons to spend gathered resources. Apex Hunts do not copy Silent Gear's material system, parts, blueprints, grading, traits, recipes, data formats, source code, assets or namespace.

## Lootr — design reference only for 0.23+
Source project: Lootr  
License: MIT

Survival Ascension studies player-fair exploration progression. `expedition_v1` is independent Survival Ascension SavedData and vanilla-item reward code. No Lootr source, custom chests, assets, namespace or loot implementation is bundled.

## Bountiful — reference only for 0.24+
Source project: `ejektaflex/Bountiful`  
License: GPL-3.0

Only high-level variable objective-to-reward contract philosophy is studied. No Bountiful source, data, GUI, assets, item code, namespace or dependency is bundled.

## FTB Quests — reference only for 0.24+
Source project: `FTBTeam/FTB-Quests`  
License: All Rights Reserved

Only product-level task/completion/reward separation and multi-task quest structure are studied. No FTB Quests source, assets, quest files, UI, namespace or dependency is bundled.

## Enhanced Celestials Tweaks — design reference only for 0.26
Source project: `SxilverKat/Enhanced-Celestials-Tweaks`  
License: MIT

Only the product-level temporary event lifecycle idea is studied. Regional incidents use independent Survival Ascension code, vanilla triggered spawns, boss bars, persistence and rewards.

## Repurposed Structures — reference only for 0.23+
License: LGPL-3.0-only

Reference only for making existing vanilla world regions worth revisiting. No source, templates, processors, worldgen data, assets or namespace is bundled.

## Explorer's Compass / Nature's Compass — reference only for 0.23+
License: CC-BY-NC-SA-4.0

Reference only for destination-driven exploration. Survival Ascension uses direct vanilla biome presence and its own per-player expedition record; no source/UI/assets are copied.

## MIT License text
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Project MMO 2.0
Project MMO 2.0 is **reference-only**. No source, assets, textures, data, namespaces or implementation markers are copied or bundled.

## ParCool
Source project: `alRex-U/ParCool`  
License: LGPL-3.0

ParCool is reference-only for movement vocabulary. Survival Ascension Mobility is independent vanilla/NeoForge 26.2 attribute and server-authoritative dash code.

## Majrusz's Progressive Difficulty — reference only for 0.26
Source project: `Majrusz/MajruszsProgressiveDifficultyMod`  
License status: no reusable source license was confirmed during the 0.26 review

Only high-level rare forced-encounter pacing is studied. No source code, event data, assets, namespaced content or implementation is copied or bundled.

## Other restricted/reference-only mods
Handwerk, NeoEnchant+ and other custom-license/ARR projects are used only for behavior and UX study unless their exact license explicitly permits reuse. No restricted source or assets are bundled.
