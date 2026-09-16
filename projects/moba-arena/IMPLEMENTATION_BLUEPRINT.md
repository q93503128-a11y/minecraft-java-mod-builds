# MOBA Arena — External Assembly Implementation Blueprint

This is the normative technical annex for `moba-arena`.

It exists so implementation does not begin with vague instructions such as “make minion AI” or “make a shop.” Each major subsystem is assigned to an external runtime or a specific licensed donor before project code is written.

`GAME_DESIGN.md` remains the gameplay conflict authority. This file owns technical donor mapping, port scope and adapter boundaries.

## 1. Platform decision

### Conditional first-slice lock

- Minecraft: **1.19.2**
- Loader: **Forge**
- Java: **17**
- Forge: **43.3.13 provisional compatibility target**

This is a donor-driven choice, not a general claim that Forge 1.19.2 is superior to modern loaders.

Reason: Anime Assembly 1.1.4 already supplies far more of the finished player-facing MOBA than the audited 26.2 alternatives. Rebuilding character kits, selection, animation/VFX, health bars, shop and minimap on 26.2 would increase project-owned architecture rather than game quality.

Forge 43.3.13 is not declared proven yet. It is the initial compatibility target because Pehkui 3.8.2 explicitly reports successful testing on Minecraft 1.19.2 Forge 43.3.13. Anime Assembly and every required dependency must still pass the M0 profile smoke test.

## 2. Runtime dependency lock

| Runtime | Exact first target | License / terms | Role | Intake rule |
|---|---|---|---|---|
| Anime Assembly | 1.1.4, CurseForge project 1169249, file 7514535, `AnimeAssembly+1.1.4.jar` | AFL-3.0 for the project-listed work; underlying franchise/content rights still require caution | player characters, abilities, character select, animation/VFX presentation, health bars, team helpers, Ready/Start, shop, MOBA minimap | direct runtime dependency; do not commit JAR until redistribution is separately justified |
| GeckoLib | Forge 1.19.2 3.1.40, CurseForge file 4407241 | MIT | Anime Assembly animation runtime | direct dependency |
| playerAnimator | Forge 1.19.2 `player-animation-lib-forge-1.0.2.jar` | verify repository notice at intake; established external runtime | Anime Assembly player animation runtime | direct dependency |
| Pehkui | 3.8.2+1.19.2-forge, CurseForge file 5393090 | MIT | scaling required by Anime Assembly | direct dependency |
| Kleider Custom Renderer | **exact 1.19.2 build TBD** | verify before intake | required Anime Assembly renderer | **hard blocker: do not guess version** |
| SmartBrainLib | 1.9, Git branch `1.19.2`, commit `3d1263fe39bc96c84fe920632208e8958d24b13f` | MPL-2.0 | minion sensing/targeting/path/combat primitives | direct dependency; prefer unmodified library |

### Dependency fingerprint file

M0 must create a machine-readable local/checked-in metadata file containing for each dependency:

- project/source URL;
- exact version/file ID;
- expected filename;
- SHA-256 of the locally tested JAR;
- license identifier;
- whether bytes may be redistributed;
- required/optional relation;
- tested Forge version.

Do not use floating `latest` dependencies for the playable slice.

## 3. Donor-code lock

### A. `c0mbit/mc-dota` / SimpleLaneWars

- Commit audited: `cacd3625b8a0066d6085bbaa0c81a18ac58254fc`
- License: MIT
- Use mode: narrow source port from Paper/Bukkit concepts into Forge 1.19.2

Port provenance from `Main.java` / `MinionListener.java`:

- wave scheduling/state;
- team + minion identity tagging;
- per-team spawn lifecycle;
- no vanilla drops/XP for match minions;
- killer/last-hit reward event flow;
- same-team target rejection concepts.

Explicit rejections:

- `generateArenaStructure()` and generated flat arena: **do not port**;
- `removeAllMinions()` before spawning each new wave: **do not port**;
- `moveTowardsEnemySpawn()` raw `setVelocity`: **do not port**;
- hard-coded asymmetric player-team logic: **do not port**;
- Bukkit scoreboard UI: **do not port as production HUD**.

The port should preserve donor logic only where it remains good MOBA behavior. Each ported file must carry MIT provenance/notice and a modification note.

### B. `cadox8/LoM`

- Commit audited: `5ae2b4b747989dc74ebe1af17869a11879cceecb`
- License: Apache-2.0
- Use mode: narrow source port / data-structure donor

Primary files/concepts:

- `structures/Structure.java`
- `structures/TowerType.java`
- `structures/InhibType.java`
- `task/InhibTask.java`
- `managers/GameManager.java` only for missing match-state shape
- `managers/Teams.java`
- `utils/TeamData.java`
- `shop/Shop.java`, `ShopManager.java`, `shop/item/ShopItem.java`, `ItemEffects.java`, `ShopItemType.java` only if Anime Assembly shop cannot be made authoritative

Keep:

- common structure type/team/health/state abstraction;
- tower attack parameters;
- structure reward hook;
- inhibitor delayed regeneration concept;
- final-core lifecycle hook;
- item price/effect/component data shape if fallback shop backend is needed.

Reject/replace:

- Bukkit API plumbing;
- old particle/reflection utility stack;
- LoM champion/skill implementation because Anime Assembly owns that layer;
- unfinished/no-op minion attack branches in `Structure.attack()`;
- any old visual effect that conflicts with adopted map/asset presentation.

Apache-2.0 ported files must retain required notices/license information and prominent modification notices.

### C. `lol-minecraft`

Status: **REFERENCE ONLY**.

The project is useful as a modern behavioral reference because it contains broad MOBA concepts, but no usable repository license was found during audit. Do not copy classes, method bodies, assets or substantial implementation from it. It may only help answer questions such as “what states should a modern Minecraft MOBA expose?”

## 4. Subsystem ownership matrix

| Subsystem | Primary owner | Project-owned work allowed | Fallback |
|---|---|---|---|
| character roster/models/skins | Anime Assembly | compatibility filter + selection gating | stop and re-audit; no original final character art |
| player abilities/cooldowns/casting | Anime Assembly | match legality bridge, server validation signals | stop/re-audit; Spell Engine is not baseline |
| character animation/VFX | Anime Assembly + required render libs | integration fixes only | external replacement pack only |
| health bars | Anime Assembly | visibility/config bridge | external HUD donor |
| player team membership | Anime Assembly team data first | authoritative roster adapter | LoM Teams/TeamData shape |
| Ready/Start | Anime Assembly first | asymmetric roster validation + phase wrapper | LoM GameManager shape |
| minimap | Anime Assembly | map metadata feed if exposed | external minimap donor after audit |
| visible shop | Anime Assembly first | server transaction adapter | external UI + LoM shop backend |
| lane wave scheduling | SimpleLaneWars port | Forge event/scheduler adapter | none needed |
| minion identity/reward | SimpleLaneWars port | correct team predicates + server currency bridge | LoM economy shapes |
| minion pathing/target/combat | SmartBrainLib | lane waypoint memory + MOBA target predicates | Vanilla Brain only if SBL compatibility fails and re-audit approves |
| structure state | LoM port | map binding + server sync | another licensed MOBA donor after re-audit |
| tower target selection | LoM state + project adapter + entity/AI queries | fill known incomplete donor behavior | re-audit donor |
| imported map | Anime Assembly modified MOBA map local candidate | metadata only | Matter Overdrive public-domain map / other audited map |
| map/team/result UI | external UI asset family | layout wiring, scaling, text | Kenney CC0 family currently primary free fallback |
| result/reset | project orchestration around donor states | allowed because no donor currently owns the integration boundary | LoM GameManager pattern |

## 5. `AnimeAssemblyBridge` contract

Do **not** write code against guessed Anime Assembly package/class names.

### M0 symbol audit

Before bridge code:

1. obtain the exact Anime Assembly 1.1.4 JAR from its original source;
2. record SHA-256;
3. inspect `META-INF/mods.toml`, resources and `jar tf` class list;
4. inspect only as allowed/needed to locate stable integration surfaces;
5. write `docs/ANIME_ASSEMBLY_SYMBOL_MAP.md` from actual symbols;
6. record which surfaces are public API, Forge event, scoreboard/team state, entity capability/data, command, item interaction, or otherwise internal.

### Bridge preference order

Use the least fragile integration surface available:

1. public methods/events/data exposed by Anime Assembly;
2. stable vanilla/Forge-visible state it deliberately uses, e.g. scoreboard team/entity data;
3. stable commands/items only behind a server-owned adapter;
4. targeted access transformer/mixin/reflection only when there is no clean public boundary and the exact version is fingerprinted.

Never spread reflective access throughout gameplay classes. All dependency-specific internals belong behind `AnimeAssemblyBridge` so one donor update has one repair surface.

### Required bridge capabilities

The bridge should expose project-level operations/events, not raw donor internals:

- enumerate compatible donor characters;
- read/validate selected character and additional skill;
- read/set project team through the donor's actual team mechanism;
- observe Ready/Start state;
- observe authoritative death/damage or the nearest reliable server signal;
- query/open/use the existing shop path if technically possible;
- query match-relevant currency/item state if exposed;
- detect whether donor minimap/healthbar systems are active;
- reset donor state safely between matches.

If one capability is unavailable, document it before designing a replacement. Do not silently duplicate the donor system.

## 6. Minion implementation contract

### Data shape

Every match minion needs at least:

- `matchId`
- `teamId`
- `laneId`
- `waveId`
- `waypointIndex`
- `minionRole` (melee/ranged/siege or donor-equivalent)
- reward metadata

### Brain composition

Use available SmartBrainLib 1.19.2 primitives such as:

- `NearbyLivingEntitySensor`
- `GenericAttackTargetSensor`
- `SetAttackTarget`
- `SetWalkTargetToAttackTarget`
- `MoveToWalkTarget` / `WalkOrRunToWalkTarget`
- `StayWithinDistanceOfAttackTarget`
- `AnimatableMeleeAttack` or ranged equivalent when the selected minion entity supports it
- `ReactToUnreachableTarget`
- `LookAtTarget`

Project code supplies:

- `isEnemyFor(team)` predicate;
- `isValidStructureTarget` predicate;
- lane waypoint selection;
- leash/return-to-lane rule;
- priority policy chosen from donor behavior;
- reward dispatch.

Do not run a global full-world entity search every tick. Sensors/queries should be local and cadence-controlled.

## 7. Structure implementation contract

`MobaStructureState` should be a Forge-era port of the LoM concept, not a new unrelated model.

Required state:

- structure ID/type;
- team;
- max/current health;
- destroyed/active state;
- attack range/damage/cadence for towers;
- reward;
- optional respawn/regen timer for inhibitor-like structures;
- map binding position/region;
- prerequisite relationship if the selected map uses lane gating.

Tower targeting must be server-owned. A target adapter resolves enemy minions/players according to the adopted priority rule and performs a visible attack whose presentation comes from adopted assets/map/runtime.

Final-core destruction emits the victory event exactly once and freezes further match-critical transactions before result/reset.

## 8. Economy/shop integration contract

The first implementation task is **discovery**, not a second economy.

1. Inspect what Anime Assembly's M shop reads/writes.
2. If it exposes stable currency/item state, make that the single transaction source of truth.
3. Connect minion/kill/structure rewards through that adapter.
4. Validate purchases server-side.
5. Only if the donor shop cannot be bridged safely, activate a fallback shop backend based on LoM data structures and pair it with an admitted external visual UI.

Never maintain two balances that can diverge.

## 9. External map metadata contract

Recommended data-driven shape:

```json
{
  "id": "external_map_id",
  "source": "original source URL or source key",
  "expectedWorldFolder": "local-folder-name",
  "sha256": "local verified hash",
  "teams": {
    "blue": {"spawn": [0, 0, 0], "base": [0, 0, 0]},
    "red": {"spawn": [0, 0, 0], "base": [0, 0, 0]}
  },
  "lanes": {
    "top": {"blueWaypoints": [], "redWaypoints": []},
    "mid": {"blueWaypoints": [], "redWaypoints": []},
    "bottom": {"blueWaypoints": [], "redWaypoints": []}
  },
  "structures": [
    {"id": "...", "team": "blue", "type": "tower", "pos": [0, 0, 0]}
  ],
  "shops": [],
  "neutralObjectives": []
}
```

Coordinates above are schema examples only, never canonical values. Real coordinates must come from inspection of the imported map.

## 10. Missing-screen UI contract

Anime Assembly should remain visible wherever it already has production-facing UI.

For project-only screens (map select, team setup, result), use an admitted external asset family. Current free fallback research:

- Kenney `UI Pack` — CC0, 430 files;
- Kenney `UI Pack (RPG Expansion)` — CC0, 85 files;
- Kenney `Pixel UI Pack` — CC0, 750 files.

These are **not automatically final** merely because the license is clean. Build one real Minecraft screen, compare it visually with Anime Assembly's in-game presentation, and reject the family if the styles clash badly. If rejected, research another directly usable external family rather than inventing one.

## 11. Ordered implementation stages

### M0 — donor runtime preflight

- resolve exact Kleider 1.19.2 file/version;
- assemble Anime Assembly + GeckoLib + PlayerAnimator + Pehkui + Kleider on Forge 1.19.2;
- boot client;
- test multiplayer/dedicated-server viability where supported;
- load the donor MOBA map locally;
- manually verify character selection, four skills/additional skill, health bars, blue/red team behavior, all-player Start, M shop and minimap;
- record every dependency SHA-256;
- build `ANIME_ASSEMBLY_SYMBOL_MAP.md`;
- stop and reassess on a critical incompatibility.

### M1 — project bootstrap + donor bridge

- create Forge mod skeleton only after M0 passes;
- add dependency fingerprint validator;
- implement narrow `AnimeAssemblyBridge` from real symbols;
- implement map metadata loader and match roster/phase state;
- no minion/structure code yet until donor state is stable.

### M2 — lane vertical slice

- port SimpleLaneWars wave/tag/reward logic with MIT notice;
- add SmartBrainLib minion brain;
- bind one lane waypoint chain;
- prove two simultaneous waves can meet, fight, die and reward correctly;
- then expand to every lane in metadata.

### M3 — structure objective slice

- port LoM structure abstractions with Apache notices;
- bind one tower and final core;
- complete target adapter instead of copying LoM's unfinished minion branches;
- prove minion/player/structure interactions and victory event;
- add inhibitor/regen only if map/ruleset actually uses it.

### M4 — economy + match closure

- connect rewards to the single adopted shop/currency path;
- implement death/respawn accounting if donor does not fully own it;
- result freeze, reset and return flow;
- verify no stale donor/minion/structure state survives reset.

### M5 — UI closure + real playtest

- fill only missing screens with adopted external UI assets;
- real Minecraft screenshot review;
- 1v1, asymmetric and 5v5-capacity state review;
- profiler pass for minion/entity load;
- actual multiplayer test before marking multiplayer verified.

## 12. Hard prohibitions for implementation

Do not:

- port the project to 26.2 first just because it is newer;
- add Spell Engine while Anime Assembly already owns player combat;
- author original champion kits to fill donor gaps;
- duplicate Anime Assembly's shop/minimap/healthbar without a demonstrated bridge failure;
- copy unlicensed `lol-minecraft` implementation code;
- copy SimpleLaneWars' raw velocity movement or wave deletion bug;
- preserve LoM no-op/incomplete attack branches as if they were finished;
- guess dependency versions or Anime Assembly class names;
- commit restricted map/JAR bytes to the public repo;
- make final UI from temporary black panels;
- treat build success as playtest success.

## 13. Validation state

This blueprint is source-audit/planning output only.

- code compilation: **NOT RUN**
- donor runtime smoke test: **NOT RUN**
- map loaded: **NOT RUN**
- multiplayer: **NOT TESTED**
- external dependency checksums: **NOT YET RECORDED**

Those states change only after M0 is actually executed.