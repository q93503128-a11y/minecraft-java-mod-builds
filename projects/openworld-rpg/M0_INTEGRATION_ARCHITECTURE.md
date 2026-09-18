# Open-World RPG — M0 External-Mod Integration Architecture

> Status: **DESIGN CANON — external-mod composition / compatibility / data-overlay / authority boundaries closed before source bootstrap**  
> Date: **2026-09-18**  
> Project authority: `PROJECT.md`  
> Gameplay authority: `GAME_DESIGN.md`  
> Dependency/version authority: `M0_DEPENDENCY_AUDIT.md`  
> Asset/provenance authority: `EXTERNAL_SOURCES.md`, `PRODUCTION_ASSET_BINDING_MATRIX.md`  
> Rule: this document does **not** add a second RPG progression system. It defines how selected external mods/libraries become components of one Openworld RPG instead of competing games installed beside each other.

---

# 1. Why this architecture exists

The project intentionally uses strong external work where it improves final quality:

- Better Combat for proven melee presentation/cadence;
- Spell Engine for casting/targeting/delivery infrastructure;
- Player Animation Library and GeckoLib for animation foundations;
- Armor Model API / Ranged Weapon API / Trinkets Updated for narrow reusable backends;
- Alex's Mobs Continued / Threateningly Mobs Continued for selected creature presentation and behavior;
- external models/UI/animation/VFX/audio/structures where production binding accepts them.

The danger is not dependency count by itself.

The danger is allowing each dependency to retain its own independent:

- damage truth;
- progression;
- loot economy;
- spawn rules;
- quest state;
- currencies;
- UI hierarchy;
- save ownership;
- duplicated recipes/material identities.

The finished game must feel like **one authored RPG**, not a launcher containing several overlapping mods.

Canonical composition rule:

```text
external runtime/content
        ↓
Openworld integration boundary
        ↓
project data / project authority / project state
        ↓
one coherent player-facing game
```

---

# 2. External architecture precedents reviewed

This pass studies architecture and integration patterns. A useful precedent does **not** automatically become a runtime dependency or authorize source copying.

## 2.1 All The Mods 10 — pack-level unification layer

Repository:

- `https://github.com/AllTheMods/ATM-10`
- observed snapshot: `ab6f65e07b88423cdae1724864ba42a573ba758a`

Observed structure:

- `config/` for dependency configuration;
- `datapacks/` for data-level changes;
- `kubejs/server_scripts/` for recipes, tags, loot and integration;
- `kubejs/startup_scripts/` for registry-time additions/aliases;
- `kubejs/client_scripts/` for client/tooltips/presentation helpers;
- per-mod integration folders rather than one giant undifferentiated script;
- dedicated unification/tweak layers that remove duplicate recipes, normalize tags and blacklist unsafe cross-mod interactions.

Useful lesson:

> keep external mods independent, then place one explicit pack/game integration layer above them.

Important boundary:

- ATM10's repository scripts contain All Rights Reserved notices; they are **REFERENCE_ONLY** here;
- Openworld RPG does not copy ATM scripts;
- current KubeJS 26.x public releases are NeoForge 26.1.2 while current Fabric releases shown publicly remain on older Minecraft generations, so KubeJS is **not** a Fabric 26.2 baseline dependency for this project as of this pass.

KubeJS itself is not the lesson. The integration-layer architecture is.

## 2.2 Mine & Slash — compatibility mode and damage ownership

Repository:

- `https://github.com/RobertSkalko/Mine-And-Slash-Rework`
- inspected branch/snapshot: `1.20-Forge` / `13dab4218dbcfc7a4f93b51ca6f1ec5274977507`

Observed architecture:

- a dedicated compatibility mode exists specifically because an overhaul RPG that replaces damage/stat logic otherwise conflicts with unrelated combat/spell mods;
- external/vanilla damage can be converted into the overhaul's canonical damage pipeline;
- datapack-driven features are expected to be modified by modpacks rather than treated as immutable hardcoded content;
- damage hooks are centralized instead of every feature inventing a separate combat formula.

Useful lesson:

> define one final damage/state authority and adapt external actions into it.

Openworld adoption:

- Better Combat and Spell Engine may produce action/cast events and presentation;
- the project still resolves canonical damage/heal/poise/status/resource results;
- never stack two full RPG damage models and then attempt to balance their totals.

This pass treats Mine & Slash as an architecture reference. Code reuse requires a separate exact license/file review.

## 2.3 Spell Engine / RPG Series — engine/content separation

Repository:

- `https://github.com/ZsoltMolnarrr/SpellEngine`
- observed snapshot: `76cd9e128468ebe005463c729ec73eff7de5fb68`

Useful structure:

```text
generic cast / target / delivery / sync engine
        ↓
data-defined spell/content packages
        ↓
game/mod-specific progression and balance
```

The associated RPG content ecosystem uses generated/data-driven spell definitions, tags and weapon attributes instead of requiring every spell to become an isolated Java class.

Openworld adoption:

- Spell Engine remains execution infrastructure;
- project skill definitions own class/resource/cooldown/balance identity;
- custom delivery/impact adapters route results into project authority when built-in actions cannot exactly represent canon;
- external class/progression/loot systems are not imported simply because the spell engine can support them.

## 2.4 Cobblemon — data registries, additive overlays, event API and sync

Repository:

- `https://github.com/Cobblemon-Global/Cobblemon`
- observed source snapshot: `75bb1a6c2fe92ef54951fb68d16d21f290a9ee88`
- repository license observed: MPL-2.0.

Observed architecture:

- reusable JSON data registries load named entries from datapacks;
- `SpeciesAdditions` applies targeted additions/overrides to an existing species instead of requiring every addon to replace the complete base file;
- broad public event surfaces expose lifecycle/action hooks;
- server-side data registries have explicit synchronization packets for client-required data;
- public addon documentation recommends unique namespaces and additive files to reduce datapack collisions.

Useful lessons:

1. **overlay instead of fork**;
2. **named data registry instead of scattered hardcoded constants**;
3. **stable event boundary instead of cross-module internal calls**;
4. **server canonical data with intentional client synchronization**.

Openworld adoption:

- external creature files/JAR internals are not copied and edited merely to change stats/spawn/loot;
- project-owned integration overlay data targets registry IDs and describes what Openworld overrides;
- server loads/resolves the canonical project data and sends only client-required resolved presentation/state data.

## 2.5 FTB Quests — typed objectives/rewards and server-owned progression

Repository:

- `https://github.com/FTBTeam/FTB-Quests`
- observed snapshot: `622091bbe07bc5bce151c8bb4ba99b81f6f7c312`
- current repository metadata/source declares All Rights Reserved.

Observed architecture:

- task types and reward types are registered, extensible object families;
- quest progress, completion, repeat cooldowns and reward claims live in explicit server-side data;
- client quest/progress views are synchronized rather than trusted as authority;
- per-team and per-player state are separate concepts;
- completion/reward operations mark persistent state dirty and synchronize changes.

Useful lesson:

> compose quests from typed objectives/rewards while keeping persistence/ownership separate from UI.

Openworld adoption:

- project quests remain project-owned; FTB Quests is **REFERENCE_ONLY / NOT BASELINE**;
- quest definitions use reusable objective/reward types;
- personal/shared/encounter ownership remains the explicit project state model from `QUEST_WORLD_STATE.md`;
- rewards remain idempotent server transactions.

## 2.6 Create — stable addon surface and internal encapsulation

Repository:

- `https://github.com/Creators-of-Create/Create`
- observed snapshot: `fc9535d82a29419164a1e9dc9c678bdcddeab30d`

Observed developer-facing lesson:

- Create exposes addon/developer APIs and useful tags;
- its source explicitly warns addon developers not to reach into certain internal registrars and instead points them at registration callbacks/public surfaces;
- the project actively supports an addon ecosystem rather than requiring forks of the base mod.

Useful lesson:

> integration code should prefer documented/public API, registries, tags and events. Reaching into another mod's internal implementation is a last resort.

Openworld adoption:

```text
public API
→ registry/tag/data
→ published event/callback
→ narrow compatibility shim
→ mixin/reflection into donor internals only as a documented last resort
```

A donor-internal mixin must never become the default path merely because it was fast to write.

---

# 3. Project authority firewall

External dependencies are classified by **what they may provide**, not merely whether they are installed.

## 3.1 External systems may provide

Depending on the accepted dependency:

- animation playback;
- hit-sweep/cast timing primitives;
- projectile/delivery infrastructure;
- model/texture/rig/animation presentation;
- storage backend;
- registry/data utilities;
- selected creature locomotion/AI foundations;
- optional hosting/social convenience.

## 3.2 Openworld RPG always owns

Unless a later canon revision explicitly changes this:

- damage result;
- HP/Mana/Stamina truth;
- cooldown/resource success;
- dodge/guard/parry validity;
- poise/stagger;
- player Lv/EXP/Class XP;
- skill unlock/progression;
- equipment stats/affixes;
- Gold/economy;
- loot eligibility and final reward tables;
- quest/evidence state;
- encounter state;
- world-state progression;
- mount ownership/registration;
- housing/fishing/gathering progression;
- save/rejoin authority.

Canonical rule:

> **dependency callback != gameplay truth**

A donor mod reporting an attack, drop, spawn or interaction is input to project logic. It is not automatic permission to commit a permanent result.

---

# 4. Source/package architecture

The project core must not accumulate donor implementation classes.

Canonical package direction:

```text
openworld_rpg/
  core/
    actor/
    combat/
    quest/
    economy/
    progression/
    worldstate/
  content/
    registry/
    loader/
    validation/
  network/
    c2s/
    s2c/
    sync/
  presentation/
    animation/
    vfx/
    audio/
    ui/
  integration/
    api/
    common/
    bettercombat/
    spellengine/
    ranged/
    trinkets/
    armormodel/
    ecology/
      alexs_mobs/
      threateningly/
      mobfilter/
  diagnostics/
    compatibility/
    data/
    authority/
```

## 4.1 Core isolation rule

Packages under `core/` should not import donor-specific classes except through an explicitly approved shared abstraction where avoiding it would be artificial.

Normal core code should understand concepts such as:

- `ActorRole`;
- `DamageRequest`;
- `EncounterId`;
- `QuestObjective`;
- `ProjectEquipmentSlot`;

not:

- one specific external mob implementation class;
- one dependency's spell class;
- one dependency's UI class.

If a donor update changes implementation details, the expected repair surface is the adapter, not the entire game.

---

# 5. Integration module contract

Each dependency integration behaves like a small bounded module.

Conceptual contract:

```text
IntegrationModule
  id
  target_mod_id
  required_or_optional
  supported_version_range
  register_adapters()
  register_data_overlays()
  register_event_bridges()
  validate_registry_contract()
  validate_runtime_contract()
```

This is a design contract; the final Java interface names may change if Fabric APIs make a cleaner implementation obvious.

Required modules fail clearly when their validated contract is missing.

Optional modules disable themselves cleanly.

Do not silently substitute a vanilla placeholder for a missing required dependency actor.

---

# 6. External content policy matrix

Each adopted external system/entity receives explicit policies instead of a vague `use this mod` decision.

Allowed policy values:

- `PASS_THROUGH` — donor behavior is intentionally accepted for this dimension;
- `ADAPT` — donor behavior is retained but translated/limited by project integration;
- `OVERRIDE` — project owns the final result;
- `SUPPRESS` — donor behavior must not occur in the authored game;
- `REFERENCE_ONLY` — installed/researched only as implementation precedent; not gameplay content.

Minimum dimensions for an external creature/content binding:

```text
presentation
animation
movement_ai
combat_ai
spawn
stats
damage
loot
recipes
worldgen
capture_or_duplication
progression
save_ownership
```

Example intent for an accepted dependency boss:

```text
target: threateningly:<accepted_earthloong_id>

presentation: PASS_THROUGH
animation: PASS_THROUGH or ADAPT after runtime review
movement_ai: ADAPT
combat_ai: ADAPT / OVERRIDE by encounter role
spawn: OVERRIDE
stats: OVERRIDE
damage: OVERRIDE
loot: OVERRIDE
recipes: SUPPRESS unless explicitly adopted
worldgen: SUPPRESS unless explicitly adopted
capture_or_duplication: SUPPRESS
progression: OVERRIDE
save_ownership: OVERRIDE
```

This matrix is recorded in project data/documentation before the actor becomes production content.

---

# 7. Data overlays — modify behavior without forking donor files

Inspired by Cobblemon's addition/overlay model, project integration data targets external registry IDs instead of replacing donor source files.

Preferred conceptual path:

```text
data/openworld_rpg/integration/
  actors/
  items/
  structures/
  recipes/
  spawn/
  loot/
```

Example conceptual actor overlay:

```json
{
  "target": "external_mod:entity_id",
  "role": "r01_dungeon_boss",
  "required": true,
  "policy": {
    "spawn": "override",
    "stats": "override",
    "damage": "override",
    "loot": "override",
    "movement_ai": "adapt",
    "presentation": "pass_through"
  },
  "tags": [
    "openworld_rpg:actors/boss",
    "openworld_rpg:actors/no_capture",
    "openworld_rpg:actors/no_random_spawn"
  ],
  "encounter": "openworld_rpg:r01/earthloong"
}
```

The exact JSON schema is finalized during bootstrap, but these concepts are mandatory.

## 7.1 Overlay rules

- target by registry/resource ID;
- never depend on a display name;
- project namespace owns project overlay filenames;
- missing required target ID is a validation failure;
- two project overlays may not silently fight over the same property;
- donor defaults are never assumed safe simply because the target exists;
- if the dependency cannot expose a needed behavior through stable surfaces, isolate the compatibility shim in its integration module.

---

# 8. Canonical tags and unification

ATM demonstrates why a large combined game needs a semantic layer above individual mod IDs.

Openworld RPG should create project tags for gameplay meaning.

Illustrative tag families:

```text
openworld_rpg:actors/boss
openworld_rpg:actors/elite
openworld_rpg:actors/wildlife
openworld_rpg:actors/no_random_spawn
openworld_rpg:actors/no_capture
openworld_rpg:actors/no_duplicate

openworld_rpg:equipment/light
openworld_rpg:equipment/medium
openworld_rpg:equipment/heavy
openworld_rpg:weapons/guard_capable
openworld_rpg:weapons/finesse

openworld_rpg:materials/ore
openworld_rpg:materials/herb
openworld_rpg:materials/signature
openworld_rpg:materials/quest_protected
```

Rules:

- systems check semantic tags/roles before hardcoding long donor-ID lists;
- tags do not replace exact content data where individual tuning matters;
- one project tag may unify several dependency IDs;
- blacklist/deny rules such as capture/duplication should reuse common tags instead of duplicating lists across integrations.

---

# 9. Normalized event boundary

External events are translated once.

Bad shape:

```text
Better Combat callback → modifies quest directly
Spell Engine callback → gives EXP directly
Creature mod death callback → gives Gold directly
UI callback → edits save directly
```

Required shape:

```text
external event
→ dependency adapter
→ project domain request/event
→ server authority validates
→ canonical state transaction
→ client presentation sync
```

Useful project-domain events/requests may include concepts such as:

- attack started / hit candidate;
- canonical damage request;
- heal/barrier request;
- entity spawned;
- encounter participant changed;
- entity defeated;
- objective contribution;
- reward transaction;
- equipment changed;
- world-state transition.

The exact Java event framework may use Fabric events, project listeners or another narrow mechanism. The important rule is one normalization boundary.

---

# 10. Combat integration pipeline

## 10.1 Melee

```text
player input
→ project combat-state eligibility
→ Better Combat presentation/swing primitive
→ project hit candidate / server validation
→ project damage + poise + status
→ project feedback
```

No double damage from both Better Combat and project damage.

## 10.2 Skills / spells

```text
project skill definition
→ project resource/cooldown eligibility
→ Spell Engine cast/target/delivery
→ custom/built-in impact bridge
→ project damage/heal/status/poise transaction
→ synchronized result/presentation
```

Spell Engine progression, scroll/loot assumptions or default HUD do not become project canon automatically.

## 10.3 Dodge / guard / parry

```text
input
→ project server-authoritative combat state
→ accepted animation presentation
→ project iframe/guard/perfect-guard/poise result
```

Do not install another full combat-overhaul state machine merely to obtain one dodge feature.

---

# 11. Creature/ecology integration pipeline

```text
dependency actor registry ID
→ ExternalActorBinding
→ region/ecology admission
→ spawn controller
→ stat/combat policy
→ encounter controller if authored
→ project loot/reward transaction
→ project save/world-state result
```

## 11.1 Spawn ownership

Normal authored-region population is project-owned.

External default spawn rules are:

- accepted only where explicitly reviewed;
- otherwise suppressed/filtered;
- never allowed to leak vanilla or donor progression assumptions into authored regions.

## 11.2 Loot ownership

Creature appearance does not imply donor drop tables are accepted.

Signature materials, Gold, equipment and first-clear rewards use project rules.

## 11.3 Boss ownership

A dependency boss can keep strong model/animation/body behavior while the project replaces:

- spawn path;
- activation condition;
- phase controller;
- damage coefficients;
- loot;
- first-clear state;
- repeat eligibility;
- world aftermath.

---

# 12. Quest/content composition

FTB Quests demonstrates the value of typed task/reward composition, but Openworld RPG keeps its own narrative/state/UI model.

Project quest data should conceptually separate:

```text
QuestDefinition
├ metadata / player-facing text keys
├ prerequisites
├ objectives[]
├ rewards[]
├ state_owner
├ sharing/participation rules
├ failure/rejoin policy
├ aftermath/world-state commits
└ presentation hints
```

Reusable objective families may include:

- interact;
- reach/discover;
- defeat/encounter contribution;
- gather/possess/turn-in;
- fish/catch record;
- escort/protect only where authored;
- world-state observation;
- dialogue/scene commit.

Reusable reward families may include:

- EXP;
- Class XP;
- Gold;
- item/material;
- deterministic role choice;
- unlock;
- discovery/codex;
- world-state/service change.

State ownership remains explicit:

```text
PERSONAL
WORLD_SHARED
ENCOUNTER
CLIENT_PRESENTATION_ONLY
```

A quest screen never owns progression truth.

---

# 13. Server data registry and client synchronization

Project-tuneable content should be data-owned where practical.

Candidate registries include:

- actor bindings;
- skill definitions;
- equipment families/affixes;
- encounters;
- quests/objectives/rewards;
- region spawn tables;
- loot/reward tables;
- merchants;
- fish;
- gathering nodes;
- audio/VFX bindings.

Server rules:

1. load and validate canonical data;
2. resolve external registry references;
3. reject invalid required bindings;
4. own runtime state;
5. synchronize only what the client needs for presentation/input prediction.

Client rules:

- does not decide rewards;
- does not decide cooldown success;
- does not decide quest/world progression;
- may cache synchronized definitions for UI/animation/VFX.

Data reload must not create a second unsynchronized truth between integrated server and clients.

---

# 14. Dependency update containment

An external update is not allowed to force game-wide rewrites.

Expected update surface:

```text
dependency update
→ integration module
→ binding/overlay validation
→ focused runtime checks
→ core unchanged unless project behavior intentionally changes
```

Prefer, in order:

1. public API;
2. registry/resource ID;
3. tag/data pack;
4. documented event/callback;
5. supported config;
6. narrow compatibility shim;
7. mixin/reflection into donor internals only with written reason and focused regression tests.

If a required integration depends on unstable donor internals, record it as a maintenance risk.

---

# 15. Startup / reload validation

Required integrations should fail loudly during development instead of silently producing wrong gameplay.

Validate at minimum:

- required mod present;
- supported version/range;
- required registry IDs resolve;
- required tags resolve;
- required external actor exists;
- project overlay schema parses;
- no duplicate project binding for an exclusive slot;
- critical donor default spawn/loot/progression behavior is actually suppressed where required;
- client-required synchronized data can encode/decode.

Validation severity:

- **ERROR / abort dev-gameplay start** — required gameplay contract missing;
- **ERROR / content disabled** — optional content cannot load safely;
- **WARN** — non-critical presentation/diagnostic mismatch with safe canonical fallback;
- **INFO** — expected optional dependency absent.

A missing boss dependency never becomes a zombie/pig/armor-stand fallback.

---

# 16. Compatibility manifest

Keep one machine-readable project manifest or equivalent source-of-truth for the dependency contract.

Conceptual fields:

```text
mod_id
required
expected_version_or_range
integration_module
public_api_used
registry_bindings[]
config_requirements[]
known_conflicts[]
validation_profile
license_boundary
```

The exact file format is chosen during M0 bootstrap.

Do not spread supported-version strings across unrelated gameplay classes.

---

# 17. Internal extension points

The project should be modular toward its **own** content too.

Stable internal extension points are useful for:

- objective types;
- reward types;
- encounter mechanics;
- actor-policy resolvers;
- VFX/audio bindings;
- equipment effect hooks.

This prevents R08/R11/R12 special content from becoming giant switch statements in core managers.

Rule:

> region-specific novelty plugs into reusable domain hooks; it does not bypass authority.

---

# 18. Development profiles and isolation tests

Existing `M0_DEPENDENCY_AUDIT.md` profiles remain authoritative.

Add integration-focused intent:

## `dev-core`

Proves that core/domain/data/network code is not accidentally coupled to unrelated creature/content integrations.

## `dev-gameplay`

Loads the full baseline dependency graph and all required adapters.

## `dev-essential`

Adds Essential only for host/join convenience testing.

Focused integration tests should also be able to enable one dependency cluster at a time where practical.

---

# 19. M0 integration acceptance gates

Before broad R01 gameplay implementation, bootstrap must prove at least:

1. integration modules register without scattering donor classes into core;
2. required dependency/version/registry validation works;
3. one project JSON registry loads and rejects malformed required entries;
4. one server-owned registry subset synchronizes correctly to a client;
5. one external creature is targeted by a project overlay without editing donor files;
6. its donor random spawn is suppressed while project-authored spawn works;
7. its donor loot is suppressed/replaced by project loot;
8. it cannot be captured/duplicated through known installed cross-mod mechanics when tagged forbidden;
9. Better Combat presentation produces exactly one project-authorized damage result;
10. Spell Engine cast produces exactly one project-authorized resource/cooldown/damage result;
11. one project quest composed from reusable objective/reward types survives save/reload/rejoin;
12. shared vs personal quest ownership remains distinct;
13. an absent optional integration disables cleanly;
14. a missing required registry binding fails clearly instead of silently using a placeholder;
15. dedicated server loads without client-only integration classes.

These are technical integration checks, not proof of finished combat/UI/game feel.

---

# 20. What is explicitly **not** adopted

This architecture pass does **not** add:

- KubeJS as a baseline dependency;
- FTB Quests as the project's quest system;
- Mine & Slash as the project's damage/progression system;
- Create as a runtime requirement;
- Cobblemon as a runtime requirement;
- full RPG Series / Skill Tree / Runes progression;
- a second damage model;
- a second equipment progression;
- a second class tree;
- a second quest UI;
- a second economy.

The project learns from their architecture while preserving its already-closed gameplay canon.

---

# 21. License / code-reuse rule for architecture references

Visible public source is not automatically reusable source.

For every architecture precedent:

- using the **idea/pattern** is allowed as reference;
- copying exact code requires its exact repository/file license to permit it;
- attribution/notice requirements are preserved where applicable;
- ARR code such as ATM pack scripts/FTB Quests is reference-only unless explicit permission says otherwise;
- GPL/MPL/LGPL code requires deliberate license-compatible handling before copying;
- when no code copy is necessary, prefer a clean project implementation of the architectural pattern.

This document does not itself authorize copying any reviewed source.

---

# 22. Production consequence

The project's next implementation architecture is now:

```text
closed gameplay canon
+ accepted external runtime/content dependencies
+ explicit integration modules
+ project data overlays/tags
+ server authority firewall
+ validation/synchronization
= one coherent Openworld RPG
```

The correct next step is **not** another broad search for a complete RPG mod to install.

The narrow M0 core integration skeleton is now implemented and verified at commit `b98ab3650b6e594693df3c1170d1de1e1ea169f2`. It includes the Fabric entrypoint, dependency manifest/runtime profiles, integration-policy/module primitives, actor-overlay schema validation, unit tests and a dedicated CI/server-smoke workflow.

The dependency runtime gate is now materially further: the 10 foundation/safety JARs plus the three curated creature JARs co-load in the isolated `gameplay` development profile on a Fabric 26.2 dedicated server. All 13 dependency-manifest runtime IDs are resolved and the server reaches ready state. Canonical evidence is `M0_GAMEPLAY_RUNTIME_BOOT_2026-09-18.md` (workflow run `35313724183`). This proves compatibility/co-load, **not** authority routing. The next M0 work is bounded Better Combat / Spell Engine / creature-overlay adapters against the acceptance gates below. Player-facing R01 implementation still waits for the remaining asset/spatial gates.

Verification state for this document:

```text
EXTERNAL ARCHITECTURE SOURCES REVIEWED: YES
PROJECT INTEGRATION CONTRACT CLOSED: YES
M0 CORE INTEGRATION SKELETON IMPLEMENTED: YES
FOUNDATION / SAFETY PRIMARY ARTIFACTS RESOLVED: YES — 10/10
CURATED CREATURE PRIMARY ARTIFACTS RESOLVED: YES — 3/3
DEPENDENCY MANIFEST RUNTIME IDS RESOLVED: YES — 13/13
DEV-GAMEPLAY DEPENDENCY RUNTIME LOADED: YES — dedicated server co-load, run 35313724183
BETTER COMBAT AUTHORITY ADAPTER: NO
SPELL ENGINE AUTHORITY ADAPTER: NO
EXTERNAL CREATURE OVERRIDE ADAPTER: NO
FULL DEV-GAMEPLAY INTEGRATIONS IMPLEMENTED: NO
CODE REVIEWED: YES — bootstrap/runtime-profile scope
TESTED: YES — unit tests + core/gameplay dedicated-server profiles
BUILD VERIFIED: YES
JAR PRODUCED: YES
CLIENT RUNTIME TESTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
