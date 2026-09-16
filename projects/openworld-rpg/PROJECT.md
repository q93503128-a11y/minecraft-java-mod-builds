# Open-World RPG — Project Contract

> Working project slug: `openworld-rpg`
> Final player-facing title: **TBD**
> Current phase: **DESIGN / CANON BUILDING**

## Repository contract

This project follows the repository root `AGENTS.md`, `docs/BUILD_STANDARD.md`, and `docs/QUALITY_STANDARD.md`.
When this file or the design canon conflicts with older chat history, current `main` wins.

## Technical identity

- Slug: `openworld-rpg`
- Mod ID: `openworld_rpg`
- Namespace: `openworld_rpg`
- Bootstrap mod version: `0.1.0-alpha.1`
- Minecraft: **26.2**
- Java: **25**
- Loader: **Fabric — locked for this project**
- Fabric Loader: **0.19.5**
- Fabric API: **0.160.0+26.2**
- Gradle: **9.5.1**
- Fabric Loom: **1.17.20**
- Build plugin: `net.fabricmc.fabric-loom`
- Planned bootstrap JAR: `openworld-rpg-0.1.0-alpha.1.jar`
- Existing-world compatibility: no gameplay implementation/save format exists yet
- Required project/runtime stack: Fabric API, Player Animation Library, GeckoLib, Armor Model API, Ranged Weapon API, Trinkets Updated, Better Combat, Spell Engine + Spell Power Attributes + Cloth Config, MobFilter, Alex's Mobs Continued + CodxLib, Threateningly Mobs Continued — exact versions/boundaries are canonical in `M0_DEPENDENCY_AUDIT.md`
- Optional external mod: Essential `1.4.1.1` for hosting/social convenience only; gameplay/save correctness must not depend on it
- Not baseline: full RPG Series/Skill Tree/Runes, AzureLib as a second project animation engine, RPG Inventory, Shield API, Structure Pool API, external inventory-QoL mods as requirements
- Forbidden bundled dependencies: anything whose license/terms do not permit repository redistribution
- Datagen task: define during source bootstrap if/when project data generation warrants a dedicated task
- GameTest task: define during source bootstrap around the actual test harness
- Server smoke-test task: define during source bootstrap
- Client smoke-test task: define during source bootstrap

The exact M0 toolchain and dependency ownership boundaries are locked in `M0_DEPENDENCY_AUDIT.md`. Do not silently substitute newer versions, duplicate runtimes or donor-mod progression systems during implementation merely because they are convenient.

Fabric is no longer a provisional loader candidate. Do not reopen the loader choice during ordinary planning. Re-evaluate only if a hard technical blocker appears that prevents a required canonical feature from being delivered on Fabric 26.2.

## Product identity

This is not a vanilla-plus mod.
The intended product is a large open-world action RPG that uses Minecraft primarily as the world/runtime foundation while replacing most player-facing RPG systems with project-owned systems.

Core pillars:

1. Open-world exploration and discovery
2. Fast action combat with dodge / guard / parry / skills
3. Deep character growth through stats, equipment, classes and repeated class advancement
4. Distinct regions, monsters, bosses and dungeons
5. Strong external-first art/UI/structure/content direction from the first implementation

## Personal-use scope

The intended gameplay build is for the owner's private play and is not planned for public distribution.
However, this GitHub repository is public. Therefore:

- private-use-only or non-redistributable third-party assets MUST NOT be committed to this public repository;
- the repository may record their source and local installation/import instructions;
- assets/code committed here must still be redistributable under their actual terms;
- no paywall/access-control bypass or paid-asset piracy is allowed;
- if public distribution is ever planned, all third-party material must be re-audited first.

## Canon files

- `GAME_DESIGN.md` — single gameplay/design source of truth
- `EXTERNAL_SOURCES.md` — external code/assets/UI/map/structure provenance and adoption status
- `PROJECT.md` — technical/build identity and project-wide contracts

Subordinate references:

- `REGIONS.md` — Azari regional content expansion for the regional work explicitly queued by `GAME_DESIGN.md`. It does not override the master canon; when any detail conflicts, `GAME_DESIGN.md` wins.
- `UI_DIRECTION.md` — selected external CC0 UI family, screen architecture, scaling rules and visual acceptance criteria for the UI work indexed by `GAME_DESIGN.md`. It does not override gameplay rules in the master canon.
- `LOOT_ECONOMY.md` — detailed equipment-generation, affix, drop-rate, target-farming, signature-material and loot-presentation rules for the loot-economy work explicitly queued by `GAME_DESIGN.md`. It does not override the master canon.
- `EQUIPMENT_BALANCE.md` — concrete Item-Lv/base-stat curves, exact affix ranges/caps, forge/reforge rules, R01 resource/equipment catalog and external visual bindings. It closes implementation-time equipment-number invention but does not override `GAME_DESIGN.md`.
- `MOUNTS.md` — external-first mount roster, traversal-speed/handling balance, stable economy, summon/Resolve/combat rules, flight model and multiplayer authority. It expands `GAME_DESIGN.md` §18 without overriding the master canon.
- `M0_DEPENDENCY_AUDIT.md` — exact Fabric 26.2 toolchain, pinned dependency/version set, license/public-repository boundaries, integration ownership, server-authority contract and first bootstrap acceptance matrix.
- `COMBAT_BALANCE.md` — exact combat-stat formulas, damage/mitigation math, dodge/guard/perfect-guard timing, player/enemy poise, attack commitment, encounter TTK/damage bands, multiplayer boss scaling and revive timing. It expands `GAME_DESIGN.md` §§5–7 and 23 without overriding the master canon.
- `CLASS_COMBAT_KITS.md` — exact five root-class mechanics, starting actives/passives/ultimates, costs/cooldowns/coefficients, class ultimate-charge rules, first specialization branches, required class statuses and external animation/VFX/icon directions. It expands `GAME_DESIGN.md` §9 on top of `COMBAT_BALANCE.md` without overriding the master canon.
- `CLASS_PROGRESSION.md` — per-root Class Rank/XP and catch-up, five advancement beats, branch switching, 30-point passive economy, all root/branch passive nodes, Rank-20 actives, Rank-32 doctrines, Rank-44 ascendant mechanics, world-discovered skills, Hidden Techniques and Class Insight challenges. It expands `GAME_DESIGN.md` §9 without overriding the master canon.
- `STATUS_AND_R01_ENCOUNTERS.md` — element/status layering, buildup/repeat-resistance/cleanse rules, Poison/Bleed/Frostbite/Shock behavior, and exact R01 ecology/elite/field-boss/first-dungeon encounter stats, attacks, rewards and external-asset boundaries. It expands the R01/combat work in `GAME_DESIGN.md` without overriding the master canon.
- `R01_VERTICAL_SLICE.md` — external-first player-motion bindings, coherent starting-settlement architecture/NPC/prop direction, first 55–75 minute route, first Trail Stag event, Regalhart discovery, quarry dungeon room flow, opening economy/audio/presentation and first-playable acceptance rules. It expands the opening/R01 work in `GAME_DESIGN.md` without overriding the master canon.
- `RECOVERY_PRODUCTION_APPEARANCE.md` — exact HP recovery, four-dose quick-recovery belt, R01 potion/food/alchemy/cooking rules, light profession mastery, external-first armor/apparel/NPC clothing, Wardrobe and armor-motion compatibility requirements. It expands `GAME_DESIGN.md` §§5, 12, 15, 16 and 24 without overriding the master canon.
- `R01_ASSET_INTAKE.md` — current R01 model/animation/VFX/audio intake manifest: exact public-safe source locators where verified, package-license/public-repo boundaries, Quaternius license-drift handling, conversion/acceptance checks and unresolved archive/clip gates. It expands the R01 external-first work queued by `GAME_DESIGN.md` without overriding gameplay canon.
- `GATHERING_FISHING_CAMP_HOUSING.md` — exact Tool Pouch/tool-tier/mastery rules, node interaction/respawn, fishing cast-hook-tension flow, reusable Field Camp Kit, physical-property housing, Home Storage, furnishing/trophy rules, server authority and R01 acceptance targets. It expands `GAME_DESIGN.md` §§21–22 without overriding the master canon.
- `FISHING_COLLECTION_HOUSING_MARKET.md` — authoritative user-directed refinement of the fishing/housing portions of `GATHERING_FISHING_CAMP_HOUSING.md`: Fish Codex/records/trophy catches/cooking/selling, external fishing code/art/UI candidates, one-residence-at-a-time ownership, fixed vacant-house tiers, 80% resale/trade-up, safe furniture migration, tiered Home Storage and external 26.2 furnishing/property-protection candidates. Where those specific fishing/housing details conflict with the older subordinate file, this refinement wins; `GAME_DESIGN.md` still remains master conflict authority.
- `QUEST_WORLD_STATE.md` — exact personal/shared/encounter state ownership, objective credit, split-party progression, dialogue/choice rules, dynamic events, late-join handling, R01 quest-state mapping, idempotent reward transactions and multiplayer acceptance tests. It expands `GAME_DESIGN.md` §§20 and 23 without overriding the master canon.
- `R02_IMPLEMENTATION_PACKAGE.md` — implementation-ready expansion of the existing R02 western forest/river-basin canon: local pressure map, logging/trade hamlet, Town House upgrade presence, gathering/fishing package, external-first fauna and guardian candidate, common/elite/field-boss/Lich combat targets, woodland-sanctum dungeon flow, regional quest/events, reward roles, asset gates and multiplayer/performance contracts. It refines R02 without overriding `GAME_DESIGN.md` or the region graph in `REGIONS.md`.
- `DESIGN_COMPLETENESS_AUDIT.md` — design-closure and finished-game quality audit, external open-world RPG production lessons, POI/world-density concerns, quality gates and remaining content/narrative gaps. It measures completeness without overriding gameplay canon.
- `WORLD_STORY_CANON.md` — customizable protagonist frame, Anchor-network world premise, recurring character roles, modern ideological conflict, non-linear main-story acts, region-story contract, POI/travel cadence, three personal endings and multiplayer-safe postgame state. It supplies the narrative spine that later region packages must follow.
- `R03_IMPLEMENTATION_PACKAGE.md` — narrative-ready Whitecrest highlands package: mining-town conflict, vertical route/shortcut progression, Silver/Refined Pick loop, external Griffin/Rock Golem candidates, field/dungeon encounter targets, observatory evidence, regional aftermath and multiplayer/performance contracts.
- `R04_IMPLEMENTATION_PACKAGE.md` — implementation-ready Frozen Crown package: external open-world RPG content precedents, geothermal-refuge conflict, whiteout/waystation navigation without an always-on cold meter, frozen ecology, cold-water fishing/material loops, Ferox Iceworm field-boss and Icebroodmother dungeon contracts, Act-II restoration evidence, regional aftermath, audio-state requirements and multiplayer/performance gates.
- `R05_IMPLEMENTATION_PACKAGE.md` — implementation-ready Jungle Greenbelt package: external jungle/open-world content precedents, readable river/canopy/root navigation, river-market culture, Jungle Komodo mount integration, dense ecology without hostile spam, mature Earthloong field-boss escalation, stepped-temple/root-vault autonomy evidence, external-gated dungeon guardian, regional aftermath, audio and multiplayer/performance contracts.

Do not create parallel competing design documents. Subordinate reference files may expand a workstream that is explicitly indexed/queued by `GAME_DESIGN.md`, but `GAME_DESIGN.md` remains the master index and conflict authority.

## Pre-code design-completion contract

The target is not “start coding once the broad concept is understandable.” The target is that **the finished canon can be handed to an implementer and the remaining work is implementation, integration and validation rather than game-design invention.**

Before gameplay source bootstrap is considered design-complete:

- player-facing systems have canonical behavior, state transitions, failure/edge behavior and multiplayer authority;
- balance-sensitive systems have concrete formulas/tables/default values rather than `decide during coding` placeholders;
- every major UI screen has selected external visual family, information hierarchy, interaction states and scaling behavior;
- every player-visible creature, boss, mount, weapon family, important armor/accessory family, workstation, structure and traversal object has an external design/model/asset/reference direction;
- every player/NPC-visible combat armor, robe, civilian outfit, class/trainer clothing and profession apparel has an accepted external 3D design/model family before implementation;
- every player-visible locomotion/combat/work/mount action — including dash, dodge, roll, jump, landing, guard/parry, attack movement, cast gestures, revive and mount transitions — has an accepted external animation/motion source or external reference before implementation;
- every production resource/material/node/item family has a viable external model/icon/reference mapping before its final name and role are locked;
- important VFX and sound families have an external source/reference direction appropriate to their importance;
- region content identifies actual encounter/reward/resource families rather than vague `add something here later` placeholders;
- data schemas needed for tuning are specified so implementation does not hard-code content lists;
- unresolved `TBD` values that affect player-facing gameplay are closed before implementation of that subsystem.

M0 has closed the Minecraft/Java/Fabric Loader/API/Gradle/Loom/mod-ID baseline and the dependency/integration boundaries in `M0_DEPENDENCY_AUDIT.md`. Remaining tooling task names or artifact details that genuinely require the source tree/test harness to exist may be finalized during bootstrap; they do not authorize gameplay, balance, UI or content invention during coding.

If implementation exposes a hard technical constraint that invalidates canon, stop that subsystem, revise the canon first, then implement the revised rule. Do not silently let code become the new game design.

## External-first content admission contract

External-first applies to **content design itself**, not only to the final art pass.

For a new player-facing resource, ore, herb, material, consumable, weapon, armor piece, robe/outfit/apparel piece, accessory, monster, boss drop, mount, structure, workstation or major prop:

```text
find/verify strong external visual/model/icon/reference candidate
→ classify license / dependency / local-only boundary
→ decide how it visually fits the project
→ then lock player-facing name, role, region, source, stats/reward use
→ record the source mapping
→ implement
```

Do not do this in reverse:

```text
invent a list of generic resources/items
→ code them
→ later search for any art that vaguely fits
```

A mechanic may identify the *need* for a resource before art research, but the production-canonical resource definition is not final until its visual source is viable.

Directly usable or editable external design should be preserved when it already looks better than an internally invented replacement. Modification exists to fit Minecraft scale, gameplay readability and the shared art direction—not to replace good design merely for originality.

## Implementation cleanliness contract

Git history is the archive. The live source tree is not.

After a replacement is verified at the appropriate risk level, remove:

- dead code;
- unused prototype systems;
- superseded managers/handlers;
- duplicate implementations;
- obsolete fallback paths;
- stale test-only registries/resources that are no longer used;
- commented-out old implementations.

Do not delete code that is still required for save migration, compatibility, or an active feature merely because it looks old. Replacement flow is: implement replacement → migrate callers/data if needed → narrow verification → remove superseded code.

## Design cleanliness contract

There is no "temporary ugly UI/model because this is only a test" stage for player-facing content.

From the first player-visible implementation:

- UI/HUD uses selected external final-quality design/reference/assets;
- inventory/equipment specifically follows `UI_DIRECTION.md` and the Foozle `Lucifer - RPG UI` + `Lucifer - Equipment` visual family rather than an internally redesigned Minecraft inventory;
- characters, monsters, bosses, structures, workstations and major props use selected external design/assets/reference;
- combat armor, robes, civilian clothes, class/trainer outfits and NPC profession apparel use accepted external 3D design/model families; recolored vanilla armor or flat skin recolors are not accepted as the finished apparel solution;
- player locomotion, dash/dodge/roll, attack transitions, casting, interaction/work actions, revive and mount transitions use selected external animation/motion sources; code-only displacement with a vanilla running/static pose or throwaway two-keyframe substitute is not accepted as finished presentation;
- ores, herbs, resource nodes, material pickups, important loot icons/models and signature boss materials also follow the external-first content-admission rule;
- no improvised AI black-panel/card/glow UI;
- no placeholder vanilla entity + particles for important enemies;
- no vanilla crafting-screen reskin for major RPG workstations unless the selected external reference genuinely uses that structure.

Technical debug commands/logging may exist during development, but must not become player-facing design.