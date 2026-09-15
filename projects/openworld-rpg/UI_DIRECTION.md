# Open-World RPG — UI / UX Direction

> Status: subordinate design reference  
> Master canon: `GAME_DESIGN.md`  
> Loader: Fabric 26.2  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file records the selected external UI language and screen architecture for `openworld-rpg`. It exists so implementation does not drift back into improvised Minecraft-style menus, generic black panels, inconsistent spacing or unrelated UI packs.

---

# 1. Selected visual family

## Primary family — Foozle `Lucifer - RPG UI`

Source: https://foozlecc.itch.io/lucifer-rpg-ui  
License: CC0 1.0 Universal  
Cost: free / name-your-own-price with a `$0` download path  
Assets: pixel-art HUD, panels, buttons, font and 100+ skill icons; editable `.ase` source files are included.

Project role: **primary visual base**.

Why it fits:

- it is a complete RPG family instead of a few unrelated frames;
- crisp pixel art sits naturally over Minecraft without pretending to be a web/app UI;
- it already contains HUD and skill-language material, reducing hand-authored inconsistency;
- `.ase` sources make legal extension and recoloring practical;
- CC0 permits modification and public-repo redistribution of adapted assets;
- the family has matching equipment/effect/pickup packs, so inventory, loot and HUD can share one visual grammar.

Do not import the source pack's infernal/red theme unchanged everywhere. Preserve its shapes, framing, pixel density, ornament logic and control language, then adapt the palette to the game's broader open-world fantasy identity.

## Equipment companion — Foozle `Lucifer - Equipment`

Source: https://foozlecc.itch.io/lucifer-equipment  
License: CC0 1.0 Universal  
Cost: free / name-your-own-price with a `$0` download path  
Assets: 50+ equipment sprites, rarity backgrounds and editable `.ase` source.

Project role: **equipment-slot / rarity / inventory visual companion**.

Use the pack as a visual source for equipment slots, rarity framing and item presentation, but the project keeps its canonical 12 equipment slots and its own equipment progression. Do not inherit the donor pack's slot count or rarity rules merely because they are shown in the source artwork.

## Scalable support family — Kenney `Fantasy UI Borders`

Sources:
- https://kenney.nl/assets/fantasy-ui-borders
- https://kenney-assets.itch.io/fantasy-ui-borders

License: CC0 1.0 Universal  
Cost: free  
Assets: 130+ fantasy window/dialog 9-slice sprites, tilesheets and source/vector material.

Project role: **secondary scalable support**.

Use this where the Lucifer pack does not provide a clean scalable panel shape, especially wide workstation windows, confirmation dialogs, side panels and variable-size list containers. Do not mix visual styles randomly: support borders must be recolored/weighted to match the primary Lucifer-derived visual language.

## Utility support — Kenney `UI Pack - Adventure`

Source: https://kenney.nl/assets/ui-pack-adventure  
License: CC0 1.0 Universal  
Cost: free  
Assets: 128+ buttons, panels, sliders, progress/UI elements and vector source.

Project role: **fallback control primitives only**.

Use only for missing low-level widgets such as scrollbars, toggles, check states or progress pieces when recreating them from the primary family would be wasteful. It is not a second full visual theme.

---

# 2. Visual rules

The UI is an RPG interface layered onto a voxel world, not a vanilla inventory reskin.

- Use crisp pixel-art scaling; avoid blur and arbitrary fractional scaling where practical.
- Base surfaces use dark stone/iron/charcoal with restrained bronze/gold trim.
- HP uses red; Mana uses cool blue/cyan; Stamina uses amber/gold-green depending final readability testing.
- The source pack's saturated red is reserved for danger, low HP, destructive actions, ultimate emphasis or selected high-impact states rather than every button.
- Normal selected/hover states should be readable without neon glow.
- Avoid large generic black translucent rectangles.
- Avoid gradient/glow decoration that is not present in the selected external family.
- Use one spacing/grid system across every screen rather than tuning each screen independently.
- Interactive states must include at least normal / hover / pressed / disabled where relevant.
- Icons must remain legible at Minecraft GUI scales and at common 1080p play resolution before decorative detail is added.
- Important information is shown once. Do not repeat the same currency, stats or instructions in several panels merely to fill space.

The player-facing interface must never contain development labels such as `R01`, `P0`, `debug`, `temporary`, internal tier IDs or implementation names.

---

# 3. HUD architecture

The combat HUD is intentionally compact so the 3D world remains dominant.

## Persistent combat information

- HP / Mana / Stamina resource presentation;
- 4 active-skill slots;
- 1 visually distinct ultimate slot/gauge;
- selected quick-use/consumable information only when needed;
- contextual status/buff/debuff information with strict density limits;
- compact target/boss information when combat requires it.

## Layout direction

- Resource bars stay together in one compact cluster rather than being scattered across corners.
- Normal skills form one readable group; the ultimate must be visually stronger and cannot look like a fifth identical skill slot.
- Spell Engine may provide skill-runtime/HUD behavior, but **Spell Engine's default visuals are not visual canon**. Skin/wrap the presentation into this project's selected UI family where technically possible.
- Vanilla HUD elements that duplicate project information should be hidden or replaced rather than stacked underneath the RPG HUD.
- Do not cover the center reticle/view with permanent RPG panels.

Cooldown, out-of-Mana and unusable states must be identifiable by more than color alone where practical.

---

# 4. Inventory / equipment screen

The inventory is a full RPG character screen, not the vanilla container with extra slots attached around it.

## Main hierarchy

1. character/equipment zone;
2. main backpack/grid;
3. material pouch/category access;
4. sorting/filter/search where it materially reduces inventory work;
5. item detail/comparison layer.

Canonical equipment slots remain:

- Main Weapon
- Off-hand
- Head
- Chest
- Legs
- Gloves
- Boots
- Necklace
- Ring 1
- Ring 2
- Charm
- Relic

Preferred composition:

- equipment arranged around a central character render/preview or a similarly readable silhouette;
- backpack to the right or lower-right as the largest repeated grid;
- material pouch as an explicit category/tab rather than forcing materials to compete with equipment for the same space;
- equipped-item comparison appears when relevant and disappears when irrelevant;
- no permanent giant stat wall beside every item.

Two-handed weapons may disable/repurpose Off-hand exactly as defined by the master canon.

## Technical candidates

### Trinkets Updated

- current 26.2 Fabric support observed;
- MIT;
- modern data-driven accessory slot API;
- candidate backend for Ring/Charm/Relic-type storage/equipment state if its server authority and slot behavior fit the canonical model.

It is a backend candidate, not visual authority.

### RPG Inventory

- MIT and useful architecture/reference for RPG equipment screens;
- latest observed release is Fabric 26.1.x, not confirmed 26.2;
- therefore **do not make it a required 26.2 dependency yet**;
- inspect/port only useful architecture if the M0 audit shows that is cleaner than implementing on top of current 26.2 APIs.

### Inventory Sorting

- current Fabric 26.2 release observed;
- MIT;
- optional QoL/reference candidate if the project backpack needs proven sorting behavior.

Sorting must not destroy intentional protected/favorite positions.

---

# 5. Skill / class / advancement screens

These screens use the same frame family as inventory rather than inventing a separate MMO menu style.

## Skill loadout

- clearly show the four equipped active skills and one ultimate;
- show Mana/Stamina cost, cooldown/cast behavior and weapon/condition requirements without paragraph walls;
- unavailable skills explain the actionable reason for being unavailable;
- skill icons may initially use the 100+ CC0 Lucifer skill-icon pool when the icon meaning matches, with project-specific additions created from the same pixel grammar rather than unrelated icon packs.

## Advancement

Use a readable depth-first tree matching the canonical class structure.

- current class/branch is visually obvious;
- acquired passives are readable without requiring hover on every node;
- locked future nodes communicate meaningful requirements;
- the screen never implies passive slot limits because all unlocked current-class passives are active;
- class switching history/progress is preserved and represented as saved progression, not reset characters.

Do not copy another mod's class names/progression assumptions into this UI.

---

# 6. Workstation screens

Forge, alchemy and cooking share one underlying UI grammar:

- same frame family;
- same title/header treatment;
- same button dimensions/states;
- same item-slot treatment;
- same typography hierarchy;
- service-specific accent/iconography only where it improves recognition.

### Forge

Primary hierarchy: input/equipment → resulting change/preview → cost/materials → confirm.
Do not make the player click through several intermediate confirmation pages for one ordinary upgrade.

### Alchemy / cooking

Primary hierarchy: known recipe/category → ingredients → result/effect → craft.
Recipes should be filterable by availability and usefulness rather than displaying an unmanageable flat wall.

The physical workstation in the world remains important; the UI should feel like that service's interface, not a global crafting app reachable from anywhere.

---

# 7. Map / navigation UI

The world map should use the selected UI family only for framing, tabs and controls. The map itself stays visually dominant.

- terrain/world shape visible enough for navigation;
- discovered POIs, shrines, hubs, dungeons and major threats layer on top;
- undiscovered rewards/enemies are not automatically revealed;
- suggested region/encounter Lv is compact metadata, not a hard-lock symbol;
- use separate visual treatment for normal region suitability vs elite/boss danger;
- discovered shrine/major-hub fast travel integrates directly into the map screen;
- ordinary camps remain non-fast-travel locations.

Avoid a giant ornate border that wastes a large fraction of screen area.

---

# 8. Death / respawn

Death is a focused decision, not a menu stack.

Show:

- defeat state;
- current respawn point;
- the two canonical penalty choices: currency payment or current-Lv EXP-progress loss;
- exact consequence before confirmation;
- multiplayer revive state when applicable.

The screen must make it impossible to accidentally believe an earned Lv will be lost.
Single-player skips fake downed waiting and goes directly to defeat/respawn flow.

---

# 9. Tooltip / item-information rules

A tooltip prioritizes decisions over lore density.

Recommended order:

1. item name + grade + relevant Lv/usage requirement;
2. primary combat value(s);
3. affixes / special mechanic;
4. class/skill/weapon synergy if relevant;
5. short source/flavor line only when useful;
6. sell/value/context actions at the bottom.

When comparing equipment, show only changed/relevant values beside the equipped item. Do not duplicate the entire character stat sheet inside a tooltip.

Color communicates category but never becomes the sole signal for important states.

---

# 10. Scaling / accessibility acceptance

Before a screen is called complete, inspect it in the actual Minecraft client at several GUI scales and common resolutions.

Minimum visual acceptance:

- 1920×1080 baseline;
- a smaller laptop-class resolution;
- multiple Minecraft GUI-scale settings;
- long Korean labels and numeric extremes;
- controller/mouse hover/keyboard focus states where supported;
- readable cooldown/resource text over bright and dark world backgrounds.

If a source asset does not scale cleanly, prefer 9-slice/vector-derived reconstruction from the verified external family instead of stretching one bitmap into blur.

---

# 11. Adoption boundary

Assets selected here are free and redistributable under CC0, so adapted versions may be stored in the public repository with source provenance recorded.

Do not mix in paid UI packs or scraped proprietary game UI textures merely because they visually match. Proprietary commercial-game UIs remain reference-only unless their actual terms permit reuse.

Implementation libraries are separate from visual assets:

- Spell Engine: skill runtime candidate, GPL dependency, not visual canon;
- Trinkets Updated: accessory backend candidate, MIT;
- RPG Inventory: MIT architecture/reference, 26.2 compatibility not yet established;
- Inventory Sorting: optional MIT QoL candidate.

The first in-game UI implementation should already use this selected visual language. There is no temporary vanilla-button/black-panel phase.

---

# 12. Next UI implementation checkpoint

Before source bootstrap or immediately at M0:

1. obtain the CC0 Lucifer RPG UI, Lucifer Equipment and Kenney support packs;
2. record exact downloaded versions/file hashes if assets are committed;
3. make a single visual atlas/folder convention rather than scattering raw source-pack files;
4. prototype only one complete player-facing screen first — inventory/equipment is preferred because it exercises slots, tooltips, tabs, character preview and scaling;
5. inspect the real Minecraft screenshot before propagating the style to every other screen;
6. once the visual grammar passes, reuse the same components for class, forge, alchemy, cooking and death screens.

No screen is approved merely because it compiles.