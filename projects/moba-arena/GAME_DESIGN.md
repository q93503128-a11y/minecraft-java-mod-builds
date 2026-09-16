# MOBA Arena — Master Game Design Canon

This file is the gameplay conflict authority for `projects/moba-arena/`.

The project deliberately minimizes original invention. Where a mechanic can be taken from an adopted external MOBA implementation, that donor behavior is preferred over a newly invented replacement.

## 1. High concept

A full Minecraft Java MOBA played inside existing third-party arena maps, using existing external combat/UI/animation/art foundations and externally sourced or ported MOBA mechanics.

The intended feel is a real standalone match-based game running on Minecraft, not a loose survival mod with a few MOBA-themed items.

## 2. Non-negotiable project rules

1. **External maps only.**
2. **External UI/design only.**
3. **External models/textures/animations/VFX/audio only.**
4. **Major gameplay code is external-library or donor-code based.**
5. Original code is restricted mainly to adapters, ports, compatibility, metadata and orchestration.
6. No player/champion bots in the initial scope.
7. Minions and other genre-required non-player AI are required.
8. Team size and map choice are independent.
9. No automatic map resizing, lane removal or geometry edits based on team size.
10. Personal-use target does not override the public repository's license/redistribution obligations.

## 3. Match configuration

A match contains two teams.

Each team supports 1–5 human player slots.

Legal examples include:

- 1v1
- 1v2
- 1v3
- 1v4
- 1v5
- 2v1
- 2v2
- 2v3
- 2v4
- 2v5
- 3v1 through 3v5
- 4v1 through 4v5
- 5v1 through 5v5

No symmetry requirement exists.

The default ruleset does not secretly buff the smaller team merely because team sizes differ. If a future external donor includes an optional handicap mode, it may be exposed as a separate selectable rule only after explicit approval.

## 4. Human players only for champion slots

Initial champion/player slots are human-only.

There is no requirement to fill unused slots.

Examples:

- a 1v1 match has two human players;
- a 2v1 match has three human players;
- a 5v5 match has ten human players.

The owner may still run development/practice sessions alone for map inspection, minion testing, skill testing or structure testing, but that does not count as a real 1v1 match.

Player-bot support is a separate future feature, not hidden baseline scope.

## 5. Map selection

Before match start, players select one installed supported external map.

A map definition contains metadata only, such as:

- source identifier;
- expected world/save folder identity;
- team spawn coordinates;
- lane waypoint sets;
- tower/base/objective coordinates if required by the adopted mechanics;
- shop/respawn anchors if the donor system requires them;
- neutral monster/objective anchors if the donor map/ruleset has them;
- compatibility notes.

The map's geometry is not redesigned by this project.

### Team size does not change the map

If a selected map is a full three-lane arena, then 1v1, 2v1 and 5v5 all use that same full map.

Do not:

- close lanes because player count is small;
- move towers inward;
- shrink jungle space;
- scale distances;
- crop terrain;
- generate a special duel variant unless that duel map is itself a separate external map.

### Multiple maps

If several suitable external maps are available, each remains a distinct selectable map.

Examples of acceptable future behavior:

```text
Map Select
- External Map A
- External Map B
- External Map C
```

The actual screen art/layout must itself come from an adopted external UI system/assets rather than a project-invented visual design.

## 6. Core match loop

The canonical loop is MOBA-standard and should be inherited as far as possible from an adopted donor implementation:

```text
select map
→ configure teams
→ select externally sourced playable kit/champion
→ spawn at team base
→ minion waves begin
→ lane/neutral combat
→ gain donor-defined experience/resources
→ buy or obtain donor-defined upgrades/items
→ destroy or bypass defensive structures
→ attack the final enemy base/core objective
→ match ends
→ reset/return to pre-match state
```

Exact names, currencies, structure names, shop rules and level curves must come from the adopted donor system or a later source-locked adaptation. Do not invent them casually during implementation.

## 7. Champion/playable-character rule

A playable character is not admitted just because we can code four skills for it.

Before production admission, it should have viable external sources for:

- model or approved external player visual basis;
- texture;
- animation set;
- ability implementation or an ability framework/donor that can express the full kit;
- skill VFX;
- skill/audio feedback;
- HUD icons or externally sourced icon family.

The roster should preferentially be built by adopting existing external content packs/mod implementations rather than inventing character art and then searching for assets later.

The initial roster size is intentionally **not locked yet**. It will be determined by the number of coherent external character/skill/animation packages we can legally and technically reuse.

## 8. Combat and ability framework

Do not build a bespoke ability engine first.

The external-source audit should choose a framework or donor code that already handles as much as possible of:

- casting;
- targeting;
- projectiles;
- area effects;
- cooldowns;
- resource costs;
- status effects;
- hit detection;
- animation hooks;
- VFX/audio hooks;
- HUD cooldown information;
- networking/server authority.

Current leading audit candidate: Spell Engine on Minecraft 26.2, but it is not yet locked.

Any project-owned combat code should sit around the adopted framework rather than duplicate it.

## 9. Minions

Minions are mandatory.

Minimum gameplay responsibilities:

- spawn in waves;
- belong to a team;
- follow externally defined or ported lane movement behavior;
- acquire valid opposing minions/players/structures according to the adopted donor logic;
- fight without direct player control;
- contribute to lane pressure;
- die and respawn only through normal wave generation.

The project should preferentially port an existing open-source lane-entity/minion implementation. The old Matter Overdrive MOBA setup is an initial donor lead because its public-domain map explicitly used team Android spawners whose units traveled from one side to the other and fought the opposing team; the underlying Matter Overdrive source is GPL-3.0 and requires a compatibility/port audit before code adoption.

Do not introduce player-like LLM/fake-player bot stacks for minions.

## 10. Towers, base and objectives

Structures and objectives should be taken from the selected donor map/mechanics when available.

Expected MOBA roles can include:

- automated defensive towers;
- intermediate base structures;
- final core/base objective;
- neutral major objectives;
- jungle/neutral camps.

The exact set is map/ruleset dependent. A map is not rejected just because it uses a slightly different structure vocabulary than another MOBA.

If donor mechanics already exist in command blocks/datapacks/mod code, port or integrate those mechanics instead of replacing them merely for architectural neatness.

## 11. Economy, levels and shop

These systems are expected in a full MOBA, but their exact formulas are **source-locked later**.

The donor audit should prefer an implementation already providing:

- kill/minion reward accounting;
- experience/level progression;
- purchasable upgrades/items;
- shop availability rules;
- death/respawn interaction;
- scoreboard/kill tracking.

Do not invent a new item economy while an adopted MOBA donor already has one that can be used or adapted.

## 12. UI/HUD

No internally designed final UI is allowed.

Required eventual screens/components include, as applicable to the adopted donor stack:

- pre-match/team configuration;
- map selection;
- character selection;
- in-match health/resource display;
- abilities/cooldowns;
- level/experience;
- team score/kill information;
- shop;
- death/respawn state;
- match result/return flow.

Implementation may use an external UI framework such as UI Lib, but **a framework alone is not a visual design**. Production visuals must come from an external UI/HUD asset or mod family with usable terms.

RPG-HUD is an initial code/layout donor candidate, not an automatic final choice.

Do not create placeholder black panels and later call them final.

## 13. Animation and visual presentation

GeckoLib and Player Animation Library are initial external technology candidates because both have Minecraft 26.2 support and permissive licenses, but technology selection does not authorize original art creation.

Final player-facing animation clips must come from external usable animation sources or adopted character packs.

The same rule applies to:

- spell effects;
- projectile visuals;
- hit effects;
- structure effects;
- icons;
- sound effects.

## 14. Multiplayer authority

Even though this is personal-use oriented, match correctness should remain server-authoritative for:

- team assignment;
- match phase;
- damage;
- death/respawn;
- gold/resources;
- level/experience;
- item purchases;
- ability success;
- minion ownership/spawn;
- structure health/state;
- victory condition.

Client code handles input, rendering, animation and safe presentation/prediction only.

Do not claim multiplayer success until actual multiplayer testing occurs.

## 15. Local/private usage and content intake

A non-redistributable external map or asset may be used locally for the owner's private game if its actual terms permit that use, but it must not be committed to this public repository.

The project should eventually support a local intake/install process where:

1. the user downloads the original asset/map from its source;
2. the project verifies expected identity/version where practical;
3. local metadata binds it to gameplay systems;
4. Git stores only allowed integration code/metadata, not prohibited third-party bytes.

## 16. Initial donor strategy

The first implementation should not mix twenty unrelated systems immediately.

Preferred order:

1. identify the strongest broad MOBA donor/map package;
2. identify the strongest modern 26.2 combat/skill framework;
3. identify a viable external minion implementation to port or adopt;
4. select one external UI family;
5. select one animation runtime and external animation/content family;
6. compare Fabric vs NeoForge using the actual chosen donor set;
7. bootstrap only after the stack can form one coherent vertical slice.

## 17. Initial vertical slice target

The first playable slice should demonstrate the external-assembly thesis, not content quantity.

Target slice:

- one imported external arena map;
- two teams with configurable human slots 1–5;
- one externally sourced playable kit per side or one shared test kit if the donor content only supports that initially;
- working external/ported minion waves;
- at least the selected donor's basic defensive/base objective loop;
- donor-based combat/abilities;
- external HUD/UI presentation;
- match start and victory/reset loop.

No player bots are required for this slice.

## 18. Out of scope for initial implementation

- player/champion bots;
- AI-generated map building;
- original UI art;
- original character modeling;
- original animation production;
- original VFX production;
- original sound production;
- automatic map scaling by player count;
- separate 1v1/2v2/3v3 geometry made by us;
- public distribution package containing restricted third-party content.

## 19. Design-completion gate

Before gameplay source bootstrap, close these decisions from real donor evidence:

- primary external arena map;
- backup/selectable map candidates;
- loader;
- core combat/ability donor;
- minion AI donor;
- tower/base/objective donor;
- economy/shop/level donor;
- UI framework and actual UI visual family;
- animation runtime and animation/content sources;
- exact license/public-repository boundary for each adopted source.

If a major category has no acceptable external basis, do not silently invent one. Continue the external-source audit.
