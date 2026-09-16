# MOBA Arena — Master Game Design Canon

This file is the gameplay conflict authority for `projects/moba-arena/`.

The game is assembled around adopted external systems. A mechanic already supplied coherently by the adopted runtime is preferred over a newly invented replacement.

## 1. High concept

A full Minecraft Java MOBA played on existing third-party arena maps, with external character/combat/UI/animation foundations and externally sourced or ported MOBA systems.

The intended feel is a standalone match game running on Minecraft, not survival Minecraft with MOBA-themed items layered on top.

## 2. Non-negotiable rules

1. External maps only.
2. External final UI/design only.
3. External models/textures/animations/VFX/audio only.
4. Major gameplay code is dependency/donor based.
5. Original code is mainly adapters, ports, compatibility, metadata, authority and orchestration.
6. No player/champion bots in the initial scope.
7. Lane minions and genre-required non-player AI remain mandatory.
8. Team size and map choice are independent.
9. No map resizing, lane deletion or geometry edits based on team size.
10. Never create a second project-owned system merely because bridging the adopted system is inconvenient.

## 3. Match configuration

Each team accepts 1–5 human players. Every asymmetric combination is legal: `1v1` through `5v5`, including `1v5`, `2v4`, `5v1`, etc.

Unused player slots stay empty. The initial ruleset does not automatically buff the smaller team. A handicap may exist later only as an explicit separate option.

Practice sessions may be run with one human for technical testing, but they are not treated as a competitive 1v1 match.

## 4. First-slice player/combat authority — Anime Assembly

The first-slice character layer is sourced from **Anime Assembly 1.1.4 for Forge 1.19.2**.

The donor documents 22 playable characters, character-selection UI, four character abilities, an additional skill, health bars, blue/red team assignment helpers, all-non-spectator Ready/Start behavior, an M-key equipment shop, NPC forms and an improved MOBA minimap.

Therefore the first slice does **not** design its own champion kits, skill engine, animation language, cooldown HUD, shop presentation or character art.

The initial roster is the set of Anime Assembly characters that pass runtime and multiplayer compatibility testing. A character may be temporarily disabled for a reproducible compatibility defect, but it must not be replaced by an internally designed substitute just to preserve roster count.

Player/champion NPC bots supplied by Anime Assembly may be used for donor inspection and automated combat observation, but they do not fill empty human team slots in the shipped first-slice ruleset.

## 5. Map selection

Before a match, players select one installed supported external map.

The first integration candidate is Anime Assembly's modified MOBA map because its MOBA mode is documented around a Summoner's Rift derivative. It remains a **local-only candidate** until its exact terms and checksum are recorded.

A map definition contains metadata, not rebuilt geometry:

- source identifier/version/checksum;
- expected local world folder;
- blue/red spawn anchors;
- lane IDs and ordered waypoint chains;
- structure IDs/types/team/positions;
- shop/base/respawn anchors;
- jungle/neutral objective anchors where supported;
- compatibility notes.

A three-lane map remains the same three-lane map in 1v1, 2v1 or 5v5. No lane closes because fewer humans joined.

## 6. Canonical match loop

```text
select installed external map
→ configure 1–5 human slots per team
→ choose Anime Assembly character + additional skill
→ donor/profile readiness validation
→ spawn at team base
→ all participating humans Ready
→ match starts
→ lane minion waves and donor combat run
→ earn server-authoritative match resources
→ use the adopted visible shop
→ destroy lane structures / open base access
→ destroy final enemy core objective
→ result state
→ reset imported-map match state / return to setup
```

No system should exist as a disconnected menu feature. Resources, kills, minions, structures and shop progression must feed this loop.

## 7. Abilities, animation, VFX and combat feel

Anime Assembly owns the first-slice player ability and presentation layer. Its required animation/render dependencies remain part of that stack.

Project code may:

- validate whether a cast is legal for current match/team state;
- synchronize match authority around donor events;
- suppress donor debug/test affordances from normal player flow;
- bridge donor damage/death/resource signals into match accounting;
- fix integration defects when legally/technically allowed.

Project code must not:

- rewrite the 22 kits into a second ability framework;
- replace donor animation clips with project-created finals;
- recreate donor VFX using generic vanilla particles and call it equivalent;
- expose creative-only donor controls as the finished UX without an external-quality wrapper/flow.

## 8. Lane minions

Minions are mandatory and are built from three external layers rather than a new monolith.

### Wave/state donor — SimpleLaneWars

Port the useful MIT-licensed lifecycle concepts from `c0mbit/mc-dota`:

- wave scheduling;
- minion/team identity tagging;
- spawn-per-team lifecycle;
- death cleanup and no vanilla drops/XP;
- last-hit reward event flow;
- friendly-target rejection.

Explicitly **do not port** its generated tiny arena, its `removeAllMinions()`-before-every-wave behavior, its raw velocity-to-enemy-spawn movement, or its asymmetric hard-coded player-team assumptions.

Multiple waves must coexist naturally until units die.

### AI runtime — SmartBrainLib 1.9 / Forge 1.19.2

Use SmartBrainLib directly for sensing, targeting, walk-target/path movement, unreachable-target handling and melee/ranged behavior primitives. Project code supplies only MOBA-specific predicates and lane waypoint memory.

Target priority must be source-locked during implementation, but the architecture must be able to distinguish at least:

- enemy minions;
- enemy human players;
- valid enemy structures;
- invalid/friendly targets.

Movement follows imported-map waypoint chains; it is not a straight velocity shove toward the enemy base.

## 9. Towers, inhibitors and final core

Port the Apache-2.0 structural state skeleton from `cadox8/LoM`, especially `Structure`, `TowerType`, `InhibType` and `InhibTask` concepts:

- team ownership;
- structure type;
- health/state;
- attack range/damage parameters for automated towers;
- destruction and team reward hooks;
- inhibitor regeneration timer where the selected ruleset uses it;
- final-core victory hook.

Do not copy incomplete old Bukkit behavior blindly. LoM's structure code contains unfinished minion-target branches, so targeting is completed through the project target adapter and SmartBrainLib/entity queries rather than preserving no-op branches.

Structure visuals come from the adopted map/assets. State code does not authorize rebuilding ugly project-original towers on top of a donor map.

## 10. Economy, level and shop authority

There must be exactly one visible purchasing experience in the first slice.

Priority:

1. bridge Anime Assembly's existing M-key shop and whatever transaction/resource state it actually exposes;
2. if it cannot be made server-authoritative/reliable, reuse the LoM `Shop` / `ShopManager` / `ShopItem` / `ItemEffects` data skeleton as a backend while keeping an adopted external visible UI;
3. never run Anime Assembly shop and a second custom shop in parallel.

SimpleLaneWars' last-hit reward flow may feed the authoritative currency adapter after team/friendly logic is corrected.

Exact gold/XP/item values are not guessed during coding. First extract what Anime Assembly's MOBA mode already assumes; only missing values are filled from the selected donor ruleset after a documented balance pass.

## 11. UI/HUD ownership

### Reuse directly from Anime Assembly where viable

- character-selection screen;
- character health bars;
- MOBA minimap;
- visible equipment shop;
- character/skill presentation already supplied by the dependency.

### Missing project-flow screens

Likely still required:

- external-map selection;
- asymmetric human team configuration;
- ready roster overview if donor UX cannot express the project setup cleanly;
- match result / return flow.

These screens may not use improvised black translucent rectangles as final design. Their final frames/buttons/icons must come from an admitted external visual family. Kenney UI Pack / RPG Expansion are the current CC0 fallback candidates, but visual fit must be accepted in an actual Minecraft screenshot before they are marked final.

A UI framework is not a design source. Do not add UI Lib/LDLib2 merely because it exists; use it only if the final adopted asset/interaction implementation actually benefits.

## 12. Multiplayer authority

Server/match authority is final for:

- team assignment and participating-human roster;
- match phase / Ready state;
- damage acceptance and death/respawn accounting;
- match currency / XP / purchase success;
- minion ownership/spawn/death;
- structure health/destruction/regen;
- victory/result/reset state.

Anime Assembly remains the ability executor, but the bridge must not trust client-only presentation state for match-critical outcomes.

Do not claim multiplayer success until it is actually tested.

## 13. Local/private content intake

Restricted maps/assets stay outside Git. The intended flow is:

1. user downloads the original third-party file from its source;
2. project verifies expected filename/version/checksum where practical;
3. local intake copies or binds it into the development/runtime profile;
4. map metadata binds spawns/lanes/structures/objectives;
5. Git stores only allowed metadata/integration code.

## 14. Donor precedence and rejected duplication

For the initial stack:

- Anime Assembly beats Spell Engine for player combat because it already includes complete characters and MOBA-facing presentation;
- SmartBrainLib owns reusable minion AI primitives;
- SimpleLaneWars owns narrow wave/tag/reward lifecycle provenance;
- LoM owns the structure/state skeleton and fallback shop-state provenance;
- `lol-minecraft` may be studied for behavior only; no code copying without a license;
- Matter Overdrive remains a backup historical map/minion donor, not the primary implementation path.

Do not mix additional overlapping combat/UI/AI libraries unless a concrete missing capability survives this stack.

## 15. First playable vertical slice

A slice is accepted only when it demonstrates the whole external-assembly thesis:

- donor-only dependency profile proven to boot;
- one imported external arena;
- configurable human teams 1–5 vs 1–5;
- Anime Assembly character selection and at least two tested playable kits;
- multiple persistent lane waves using SmartBrainLib pathing;
- at least one working tower line plus final core;
- last-hit/resource path connected to the single shop path;
- Ready → Start → win → result → reset loop;
- adopted external-facing UI for any project-created screens.

No player bots are required.

## 16. Out of scope for the initial implementation

- player/champion bot filling;
- original champion models/skins/animations/VFX/audio;
- original final UI art;
- AI-generated arena construction;
- automatic map scaling by player count;
- a separate custom ability engine;
- a second visible shop/economy;
- porting the project to 26.2 before the 1.19.2 donor slice proves the game;
- bundling restricted maps or dependency JARs into the public repository without permission.

## 17. Preflight closure gate

Before source bootstrap, prove and record:

- exact Kleider Custom Renderer 1.19.2 dependency;
- Anime Assembly 1.1.4 + GeckoLib + PlayerAnimator + Pehkui + Kleider compatibility;
- actual Anime Assembly JAR symbol/resource map;
- donor-only character select / skills / health bars / team / Ready / shop / minimap behavior;
- primary map terms and checksum;
- final external asset basis for map/team/result screens.

The ordered engineering procedure is normative in `IMPLEMENTATION_BLUEPRINT.md`.