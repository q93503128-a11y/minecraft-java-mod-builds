# Open-World RPG — M0 Fabric 26.2 Dependency Audit

> Status: **M0 TECHNICAL STACK CANON — dependency boundaries and composition strategy locked before source bootstrap**  
> Audit date: 2026-09-18  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Integration architecture: `M0_INTEGRATION_ARCHITECTURE.md`  
> Rule: this file owns technical dependency selection/boundaries. `M0_INTEGRATION_ARCHITECTURE.md` owns the adapter/data-overlay/validation structure. Neither overrides gameplay rules.

The purpose of M0 is not to maximize the mod count. It is to choose the smallest proven external stack that materially improves game quality, define exactly what each dependency is allowed to own, and prevent implementation from silently inheriting another mod's progression, balance, UI, loot, worldgen or authority model.

Project gameplay remains server-authoritative for damage, items, Gold, skills, progression, quests, world state and save data. External libraries may provide rendering, animation, targeting, delivery, inventory-slot storage or other infrastructure, but they do not become the design authority.

---

# 1. Locked base toolchain

Current 26.2 development baseline:

| Component | Locked baseline | Reason |
|---|---|---|
| Minecraft | **26.2** | project target |
| Java | **25** | current Fabric/Minecraft 26.x development requirement |
| Mod loader | **Fabric** | already locked by project canon |
| Fabric Loader | **0.19.5** | current official Fabric 26.2 example baseline |
| Fabric API | **0.160.0+26.2** | current official Fabric 26.2 example baseline |
| Gradle | **9.5.1** | current official Fabric 26.2 example wrapper |
| Fabric Loom | **1.17.20** | latest stable 1.17 release observed; pin stable instead of floating `1.17-SNAPSHOT` |
| Loom plugin | `net.fabricmc.fabric-loom` | current 26.x Loom path |
| Java release/source/target | **25** | keep compile/runtime language level aligned |

Primary verification sources:

- Fabric example mod `26.2` branch: `minecraft_version=26.2`, `loader_version=0.19.5`, `fabric_api_version=0.160.0+26.2`, official template currently points at `1.17-SNAPSHOT`.
- Fabric example wrapper: Gradle `9.5.1`.
- Fabric Maven publishes stable Loom `1.17.20`; this project pins the stable release for reproducibility instead of a moving snapshot.
- Fabric documentation for current 26.x uses JDK 25.

Do not auto-upgrade any of these during ordinary feature work. Change them only when a real dependency/security/compatibility reason exists, then re-run the dependency-focused build/smoke verification appropriate to that change.

---

# 2. Dependency classification

Every external mod/library belongs to one of these buckets.

## REQUIRED API / PROJECT COMPILE RUNTIME

Project-owned code intentionally targets this API/runtime.

## REQUIRED PLAYABLE RUNTIME

The private playable build expects the mod, but project Java code should avoid unnecessary source/API coupling where configuration, tags, registries or data can provide the integration.

## OPTIONAL CLIENT / HOSTING

Useful convenience but never part of gameplay correctness or save authority.

## REFERENCE / CODE CANDIDATE

Study/reuse permissive patterns when useful, but do not require the mod in the finished baseline.

## NOT BASELINE

Technically usable but rejected from the default stack because it duplicates another library, imports unwanted progression/content, is version-incompatible, or creates more complexity than value.

---

# 3. Locked dependency matrix

## 3.1 Foundation / animation / rendering APIs

| Dependency | Baseline | License/status observed | Classification | Project boundary |
|---|---|---|---|---|
| Fabric API | `0.160.0+26.2` | Apache-2.0 ecosystem | REQUIRED API | loader-native events/network/data/biome integration |
| Player Animation Library | `1.2.6+26.2` | MIT | REQUIRED API/RUNTIME | project/Better Combat/player-skill animation coordination |
| GeckoLib | `5.5.5` Fabric 26.2 | MIT | REQUIRED API/RUNTIME | project-owned animated entities, mounts and selected animated props |
| Armor Model API | `1.1.0+26.2` | MIT | REQUIRED API/RUNTIME | project-owned Bedrock/Gecko-style armor geometry through vanilla armor rendering |
| Ranged Weapon API | `4.0.0+26.2` | MIT | REQUIRED API/RUNTIME | bow/crossbow construction, pull/velocity/render fundamentals |
| Trinkets Updated | `4.1.0-rc.1+26.2` | MIT | REQUIRED API/RUNTIME | backend storage/equipment state for Ring/Charm/Relic-style accessory slots |

### Animation-engine rule

Project-owned entity animation uses **GeckoLib** as the baseline. Do not author the same project-owned entity against both GeckoLib and AzureLib.

AzureLib 4.x also supports Fabric 26.2 and is MIT, but it is **NOT BASELINE** because maintaining two overlapping project-owned entity animation engines increases conversion/debug/update work with no corresponding gameplay gain. An external optional dependency may carry AzureLib for its own assets; that does not authorize new project content to depend on both engines.

Armor Model API is preferred for player equipment because it uses the vanilla armor pipeline while accepting custom `.geo.json` geometry. Do not add a second armor renderer merely because another mod pack uses one.

---

## 3.2 Combat / skill runtime

| Dependency | Baseline | License/status observed | Classification | Project boundary |
|---|---|---|---|---|
| Better Combat | `3.2.2+26.2` Fabric | ARR | REQUIRED PLAYABLE RUNTIME | melee animation/combo/reach presentation foundation; not gameplay balance authority |
| Spell Engine | `1.10.5+26.2` Fabric | GPL-3.0 | REQUIRED PLAYABLE RUNTIME | data-driven casting, targeting, delivery, project spell presentation and selected impact routing |
| Spell Power Attributes | `1.6.2+26.2` Fabric | LGPL-3.0 | REQUIRED via Spell Engine | engine-side spell attribute bridge; project stats remain canonical |
| Cloth Config | `26.2.155+fabric` | LGPL-3.0 | REQUIRED via runtime stack | dependency configuration support; not project UI canon |
| AcroWield | `1.8.1-MC26.2` observed | MIT | REFERENCE / CODE CANDIDATE | dodge, just-guard, dual-wield and shield-control implementation ideas only |

### Better Combat boundary

Better Combat owns:

- proven player melee animation playback;
- swing/combo cadence hooks;
- weapon attack presentation and the low-level mechanics that materially reduce custom animation code.

The project owns:

- stamina cost;
- dodge/guard/parry state;
- invulnerability windows;
- poise/stagger;
- final damage formula;
- class/skill requirements;
- weapon-family balance;
- hit feedback/VFX/sound direction;
- server validation of successful combat results.

Do not copy Better Combat ARR source/assets into the public repository. Use the mod as a separate runtime dependency and its supported data/config/API surface.

### Dodge / guard / parry decision

Do **not** make AcroWield a required runtime dependency at M0. Its current 26.2 MIT implementation is valuable reference, but the project needs one server-authoritative combat state machine shared by stamina, skills, shields, poise, revive/down state and multiplayer.

Implementation direction:

```text
client input / safe prediction
→ project CombatState request
→ server validates stamina/state/window
→ project state becomes authoritative
→ Player Animation Library / Better Combat renders the accepted action
→ project damage/guard/poise result resolves on server
```

Permissive AcroWield patterns may be adapted with preserved license notice when they reduce risk, but the finished state machine remains project-owned.

### Spell Engine boundary

Spell Engine is retained because it already provides data-driven cast modes, targeting, projectiles/meteor/cloud/delivery, synchronized spell data, animations/VFX hooks and **CUSTOM** delivery/impact handlers.

Use it as follows:

- spells and skill presentation are defined primarily through project data;
- project Mana/Stamina/cooldown/class rules remain authoritative;
- project stats/equipment are mapped into the values Spell Engine needs rather than replacing VIT/END/STR/DEX/INT/WIL with another progression system;
- built-in DAMAGE/HEAL actions may be used only when they produce the canonical project formula;
- otherwise use a narrow custom impact/adapter so final damage/heal/poise/status resolution reaches project server logic;
- disable/ignore default Spell Engine spell-binding/scroll/enchantment/loot/progression features that conflict with project acquisition rules;
- Spell Engine HUD/default visuals are not visual canon; `UI_DIRECTION.md` remains authoritative.

Because Spell Engine is GPL, do not copy its source into this repository. Keep direct Java integration narrow and isolated. Before any public binary distribution, re-audit the licensing implications of the exact integration and distribution form. The current intended private playable build may use it as a separate installed dependency.

---

## 3.3 Inventory / equipment backend

Trinkets Updated **`4.1.0-rc.1+26.2`** is the currently verified 26.2 baseline. The earlier live wording that promoted this to stable `4.1.0+26.2` was incorrect. Spell Engine's maintained 26.2 branch pins the same RC build. Do not rewrite this as stable until an exact stable 26.2 artifact is deliberately admitted and revalidated.

Use it for:

- accessory slot storage/state;
- equip/unequip callbacks;
- server-owned slot-count/equipment synchronization where appropriate.

Do **not** use its default screen as final inventory design.

The player still sees the project-owned Lucifer-derived inventory/equipment screen. The project UI reads/writes the approved backend state and presents Main Weapon, Off-hand, Head, Chest, Legs, Gloves, Boots, Necklace, Ring 1, Ring 2, Charm and Relic coherently.

`Inventory Sorting 3.0.0+fabric-26.2` and similar mods remain **REFERENCE / OPTIONAL QoL**, not hard dependencies. Project sorting, locked/favorite protection and Material Pouch behavior must work without asking the player to install a separate inventory-management mod.

`RPG Inventory` remains **NOT BASELINE** because current 26.2 support has not been confirmed and the project already owns its final screen/slot model.

---

# 4. Creature / ecology dependencies

## MobFilter

- baseline: `0.28.0+26.2` Fabric;
- Apache-2.0;
- server-side/singleplayer;
- classification: **REQUIRED PLAYABLE RUNTIME / SAFETY LAYER**.

Use MobFilter to reject forbidden vanilla/default spawn paths and dependency leakage. It is not the only ecology system.

The project still owns:

- Fabric biome spawn-table editing;
- region/biome creature lists and densities;
- elite/boss/event authored controllers;
- imported-structure spawner replacement;
- loot/stat/encounter definitions.

Final acceptance is “no vanilla living mob leaks into authored gameplay,” not “MobFilter is installed.”

## Alex's Mobs Continued

Current 26.2 baseline candidate observed: **2.1.13 Fabric 26.2**.

Role:

- major wildlife and selected mount/ecology dependency;
- provides high-quality existing models/animations/behavior for curated regional use;
- project overrides or suppresses default progression/spawn/loot/recipe/worldgen assumptions where they conflict.

Required companion observed for Fabric: **CodxLib**, current 26.2 line observed at `1.6.0`.

Important license boundary:

- store metadata currently conflicts: Alex's Mobs Continued is shown as GPL-3.0-only on Modrinth while CurseForge currently reports LGPLv3;
- CodxLib store metadata also differs between Modrinth and CurseForge versions of its Creative Commons terms.

Therefore both are **DEPENDENCY-ONLY** until the canonical upstream license files for the exact installed versions are inspected. Do not copy source, textures, models or other bytes into the public repository based only on store metadata. Runtime use in the private instance is kept separate from public-repo redistribution.

Project code should prefer registry IDs/tags/configuration adapters over importing Alex-specific implementation classes unless an exact integration requirement proves otherwise.

## Threateningly Mobs Continued

- baseline: `1.1.1+fabric.26.2`;
- exact Modrinth project ID: `JXjyo7k6`;
- exact current Fabric 26.2 Version ID verified 2026-09-18: `Bdd8lkUM`;
- exact Modrinth Maven coordinate: `maven.modrinth:JXjyo7k6:Bdd8lkUM`;
- Modrinth project/version listing reports MIT, while the separate storefront conflict recorded in `PRODUCTION_ASSET_BINDING_MATRIX.md` still prevents assuming raw-byte redistribution rights;
- classification: **REQUIRED PLAYABLE CONTENT DEPENDENCY**.

Use its fantasy roster for approved regional creature/boss identities. Project region rules remain authoritative. Default structures, drops, recipes, equipment progression and spawn tables are not automatically accepted merely because the dependency supplies them.

The exact dependency artifact to inspect for R01 Earthloong/Regalhart runtime acceptance is now pinned by Version ID. The current research environment still has not materialized that JAR, so model JSON/animation/hitbox inspection remains **NOT DONE** rather than being inferred from an older release.

---

# 5. RPG Series decision

The following current 26.2 modules are useful references/content sources but are **NOT REQUIRED BASELINE DEPENDENCIES**:

- Rogues & Warriors;
- Archers;
- Paladins & Priests;
- Wizards;
- Skill Tree;
- Runes and their associated progression/content chain.

Reason:

- the project already owns five root classes, class advancement, skill loadouts, loot, equipment, economy and UI;
- importing the complete RPG Series class stack would also import its own item/progression/loot/world assumptions and substantially enlarge the dependency graph;
- Skill Tree itself requires multiple class packs and Pufferfish Skills, which would duplicate the project-owned progression layer.

Allowed use:

- study combat timing/skill packaging;
- use a specific module in a private optional content profile only when one concrete spell/animation/item package materially exceeds what the project can deliver with Spell Engine + project data;
- never make project saves/progression depend on an ARR module that can be removed without a migration plan.

Preferred baseline is:

```text
project classes / project progression / project skill data
                    ↓
               Spell Engine
                    ↓
      project-owned damage/resource rules
```

not:

```text
project classes
+ another complete external class/progression game
```

---

# 6. Essential multiplayer boundary

Essential `1.4.1.1` supports Fabric 26.2 and is client-side.

Classification: **OPTIONAL / RECOMMENDED HOSTING CONVENIENCE**.

Essential may provide:

- friend/world hosting flow;
- connection/social convenience.

Essential may **not** own:

- damage;
- item/currency truth;
- skill success;
- progression;
- quest/world state;
- save authority;
- party reward eligibility.

The host's integrated server remains the authority exactly as a normal Minecraft server would. The project must also work in normal singleplayer and normal Fabric server/LAN development without Essential installed.

Acceptance rule:

> Removing Essential may remove the convenient hosting UI, but must not change gameplay rules or corrupt/invalidate a world.

---

# 7. Explicit non-baseline / rejected dependencies

## Shield API

**REJECTED** for the baseline. Current observed support does not cover 26.2, and the project needs a custom guard/parry/stamina state anyway.

## AzureLib as a second project animation engine

**NOT BASELINE.** GeckoLib owns project entity animation. Armor Model API owns project armor rendering. Avoid duplicate animation/render pipelines.

## Full RPG Series / Skill Tree / Runes

**NOT BASELINE** for reasons in §5.

## Structure Pool API

**NOT BASELINE.** Azari is an authored external continent and important settlements/dungeons are authored/imported deliberately; the project is not trying to inject a second vanilla-style village ecosystem.

## Inventory Sorting / Inventory Management mods

**REFERENCE / OPTIONAL only.** Project QoL must work by itself.

## Icy's Better Horses / Vehicle Upgrade / Horse Combat Controls

Remain **REFERENCE / CODE CANDIDATE** for ownership, controls and riding QoL. Visible mount identity and project mount state remain defined by `MOUNTS.md`.

## KubeJS

**NOT BASELINE for the current Fabric 26.2 project.**

The 2026-09-18 architecture review found current public 26.x KubeJS builds on NeoForge 26.1.2, while the public Fabric line shown by the current distribution pages remains on older Minecraft generations. Do not switch loader or add an unsupported scripting layer merely to imitate a modpack.

All The Mods 10 remains a useful **architecture precedent** for:

- per-mod integration folders;
- recipe/tag/loot unification;
- startup-time registry additions/aliases;
- blacklist/deny-tag reuse;
- conditional integration when a dependency is loaded.

Openworld RPG implements those ideas with Fabric/Java project code + datapack/data registries instead of making KubeJS a baseline dependency.

## FTB Quests / Cobblemon / Create / Mine & Slash

These are **ARCHITECTURE REFERENCES, NOT BASELINE DEPENDENCIES**.

- FTB Quests: typed task/reward composition and server-owned progression/sync;
- Cobblemon: JSON data registries, additive overlay files, event surfaces and server→client data synchronization;
- Create: stable addon/public-API boundary, useful tags and explicit encapsulation of internal registrars;
- Mine & Slash: compatibility modes and one canonical damage pipeline when multiple combat/spell sources exist.

Exact adoption rules and reviewed snapshots live in `M0_INTEGRATION_ARCHITECTURE.md`. Their presence in that research does not authorize copying source or adding their runtime systems.

---

# 8. Runtime dependency graph

Target default playable instance:

```text
openworld_rpg
├─ Fabric Loader 0.19.5
├─ Fabric API 0.160.0+26.2
├─ Player Animation Library 1.2.6+26.2
├─ GeckoLib 5.5.5
├─ Armor Model API 1.1.0+26.2
├─ Ranged Weapon API 4.0.0+26.2
├─ Trinkets Updated 4.1.0-rc.1+26.2
├─ Better Combat 3.2.2+26.2
│  └─ Cloth Config / Player Animation dependencies
├─ Spell Engine 1.10.5+26.2
│  ├─ Spell Power Attributes 1.6.2+26.2
│  ├─ Cloth Config 26.2.155+fabric
│  ├─ Player Animation Library
│  └─ Trinkets Updated
├─ MobFilter 0.28.0+26.2
├─ Alex's Mobs Continued 2.1.13 Fabric 26.2
│  └─ CodxLib 1.6.0 Fabric 26.2
└─ Threateningly Mobs Continued 1.1.1+fabric.26.2

optional client/hosting:
└─ Essential 1.4.1.1
```

Do not install both GeckoLib and AzureLib merely for project-owned content. If a future third-party dependency hard-requires AzureLib, keep that dependency's renderer self-contained and do not migrate the project art pipeline without a documented quality/maintenance reason.

---

# 9. Project-owned authority map

Detailed composition, adapter, overlay, semantic-tag and validation rules are canonical in `M0_INTEGRATION_ARCHITECTURE.md`.

The summary rule here is:

```text
external action/content
→ integration adapter/data overlay
→ project domain request
→ server validation/transaction
→ client presentation sync
```

A dependency callback or donor entity state is never sufficient proof of a permanent project result.

## Server-owned canonical state

- character Lv / EXP;
- Gold, including negative balance;
- class/advancement state;
- equipment and affix state;
- inventory / Material Pouch / Key Items;
- cooldown/resource expenditure;
- hit/damage/heal/guard/parry/poise result;
- quest/discovery state;
- boss/first-clear/signature-material eligibility;
- personal loot and resource-node state;
- mount ownership/Resolve/unlock state;
- merchant rotation state;
- housing/world progression/save data.

## Client-owned or predicted presentation

- input capture;
- animation playback;
- camera;
- HUD/rendering;
- safe cosmetic prediction;
- non-authoritative local VFX/audio.

A third-party library callback is never accepted as sufficient proof that a player should receive Gold, damage, a boss reward or permanent progression.

---

# 10. Integration rules by system

## Melee

```text
project weapon data
→ Better Combat presentation/cadence
→ project server hit/damage/poise validation
→ project VFX/audio/UI feedback
```

## Dodge / guard / parry

```text
project input + project server state machine
→ Player Animation Library/Better Combat presentation
→ project stamina/iframe/guard/poise authority
```

## Skills / magic

```text
project skill/class data
→ Spell Engine cast/target/delivery/visual pipeline
→ project-authorized resource/cooldown
→ canonical damage/heal/poise/status result
```

Use Spell Engine CUSTOM impact/delivery handlers when a canonical project result cannot be represented safely by its built-in formulas.

## Ranged

```text
project weapon/item data
→ Ranged Weapon API bow/crossbow mechanics
→ project damage/skill/affix rules
```

## Armor

```text
external-first accepted armor model
→ Armor Model API rendering
→ project equipment/affix/state backend
→ Lucifer project UI
```

## Accessories

```text
project 12-slot equipment model
→ Trinkets backend for approved accessory storage/state
→ project custom inventory screen
```

## Creatures

```text
external dependency entity/model/animation
→ project region spawn ownership
→ project stats/encounter role
→ project reward table
```

---

# 11. Build/source-layout policy

Keep integrations isolated so one dependency update does not spread implementation-specific classes through the whole codebase.

Recommended package boundary:

```text
openworld_rpg/
  core/                 # gameplay domain; no donor-mod assumptions
  combat/               # project damage/stamina/guard/poise state
  progression/          # Lv/class/quests/economy
  inventory/            # backpack/pouch/key-item/project slots
  content/
    registry/           # data-backed project registries
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
    api/                # project-facing integration contracts
    common/             # normalized helpers; no one donor owns this layer
    bettercombat/
    spellengine/
    trinkets/
    ranged/
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

Core gameplay classes must not directly scatter third-party class names everywhere. Integrations translate between external runtime state and project domain state.

External creature/content customization should normally target registry IDs through project-owned overlays/tags rather than copying and editing donor files. The overlay/policy contract is defined in `M0_INTEGRATION_ARCHITECTURE.md`.

Prefer, in order:

```text
public API
→ registry/tag/data
→ published event/callback
→ supported config
→ narrow compatibility shim
→ donor-internal mixin/reflection only as a documented last resort
```

This lowers update risk and makes dependency replacement local to its adapter instead of game-wide.

---

# 12. Development profiles

## `dev-core`

Purpose: catch accidental hard bindings.

Contains:

- Fabric base;
- required project APIs/render runtimes;
- Better Combat / Spell Engine stack needed by project code;
- no Essential;
- external creature content may be excluded when the test does not require it.

Expectation: project bootstrap, UI/state/data loading and non-creature unit/integration paths remain functional.

## `dev-gameplay`

Default gameplay development profile.

Contains all baseline dependencies in §8 except Essential.

Use for:

- ordinary singleplayer;
- dedicated-server checks;
- ecology/combat/loot/content testing.

## `dev-essential`

`dev-gameplay` + Essential.

Use only for hosted-world compatibility/multiplayer checks. Do not make every normal build/test depend on Essential.

---

# 13. M0 bootstrap acceptance matrix

M0 planning is complete here. Once source bootstrap begins, the first dependency integration checkpoint must verify the stack as one coherent system before large gameplay implementation.

Required checks at that checkpoint:

1. **Build** — Java 25 / Loader 0.19.5 / Fabric API 0.160.0 / Loom 1.17.20 / Gradle 9.5.1 compile together.
2. **Client start** — full `dev-gameplay` profile reaches a world without dependency/mixin crash.
3. **Dedicated server start** — no accidental client-only class loading from UI/animation integrations.
4. **Combat coexistence** — Better Combat basic swing works while project dodge/guard/parry state does not double-trigger or double-consume stamina.
5. **Spell authority** — a sample Spell Engine cast spends project-owned resource/cooldown and resolves damage on the server.
6. **Ranged** — one project bow/crossbow sample uses Ranged Weapon API and project damage data.
7. **Armor render** — one external-first R01 armor sample renders through Armor Model API at first and third person scales.
8. **Animated entity** — one project-owned GeckoLib test entity/mount plays idle/move/action animation without replacing donor creature pipelines.
9. **Accessories** — Ring/Charm/Relic backend state synchronizes in singleplayer and dedicated server while the default Trinkets UI is not the final player screen.
10. **Ecology** — a controlled test area produces approved custom creatures and no normal vanilla living-mob leakage.
11. **Donor override** — at least one Alex/TMC creature proves that project spawn/stat/loot ownership can replace donor defaults without forking the whole mod.
12. **Essential profile** — host/join smoke test when multiplayer testing becomes available; gameplay result remains server-owned.
13. **No-Essential profile** — the same world still runs through normal singleplayer/dedicated/LAN development without Essential.
14. **Compatibility manifest** — required mod/version/registry contracts are checked from one bounded manifest/validation layer rather than scattered string checks.
15. **Data registry** — one project JSON registry loads valid content and rejects malformed required content with a useful error.
16. **External overlay** — one dependency actor is targeted by project-owned overlay data without editing the donor JAR/files.
17. **Donor suppression** — that actor proves donor random spawn/loot/progression behavior can be suppressed or overridden where canon requires.
18. **Semantic tags** — shared boss/no-capture/no-duplicate/no-random-spawn rules use project tags rather than duplicated per-mod ID lists.
19. **Server→client data sync** — one client-required project registry subset synchronizes from the server without making the client authoritative.
20. **Optional isolation** — an absent optional integration disables cleanly; a missing required target fails clearly and never becomes a vanilla placeholder.
21. **Core isolation** — a focused source review confirms ordinary core/domain classes are not importing creature/spell/combat donor implementation classes directly.

Do not call this `PLAYTESTED` merely because the dependency stack boots.

---

# 14. License / public repository boundary

The intended playable build is private, but this repository is public.

Therefore:

- MIT/Apache-compatible code may be adapted only with required notices preserved;
- ARR assets/source are dependency/reference only unless their terms explicitly permit the exact reuse;
- GPL/LGPL dependencies are not copied into this repository merely because source is visible;
- store-page license metadata is not sufficient when sources conflict;
- Alex's Mobs Continued / CodxLib exact upstream license files must be rechecked before any source or asset reuse;
- local/private map/mod/assets that cannot be redistributed stay outside Git;
- public-binary distribution, if ever planned, triggers a fresh dependency/license audit.

This is a project provenance rule, not legal advice.

---

# 15. Update policy

Do not chase every dependency release automatically.

Update only when one of these is true:

- required compatibility/security fix;
- a bug affecting this project is fixed;
- a feature materially improves final quality or removes custom code;
- Minecraft 26.2 ecosystem compatibility requires it.

For an update:

```text
read changelog
→ inspect API/save/network/render impact
→ change one coherent dependency cluster
→ run focused integration checks
→ run build once if warranted by BUILD_STANDARD
→ record the new pinned version
```

Avoid updating Better Combat, Spell Engine, animation APIs and custom combat code simultaneously unless the dependency graph forces it; that destroys failure isolation.

---

# 15.1 Verified M0 core bootstrap checkpoint — 2026-09-18

> Historical checkpoint. Later §15.3 supersedes its unresolved-ID/runtime-status lines.

The first narrow source bootstrap now exists at commit `b98ab3650b6e594693df3c1170d1de1e1ea169f2`.

Implemented in this checkpoint:

- Fabric 26.2 / Java 25 / Loader 0.19.5 / Fabric API 0.160.0+26.2 / Loom 1.17.20 project skeleton;
- `openworld_rpg` mod entrypoint;
- machine-readable dependency manifest with `core / gameplay / essential` runtime profiles;
- structural dependency validation through Fabric Loader metadata;
- integration-policy vocabulary and module registry;
- actor-integration overlay schema/validator;
- JUnit coverage for manifest, overlay and module-registry contracts;
- dedicated Openworld RPG GitHub Actions workflow.

Verification:

```text
COMMIT: b98ab3650b6e594693df3c1170d1de1e1ea169f2
WORKFLOW: Build Openworld RPG
RUN: 35309048646
UNIT TESTS: PASS
CLEAN BUILD: PASS
BOOTSTRAP JAR VERIFY: PASS
DEDICATED SERVER CORE-PROFILE BOOT: PASS
FULL DEV-GAMEPLAY DEPENDENCY STACK: NOT TESTED
BETTER COMBAT ADAPTER: NOT IMPLEMENTED
SPELL ENGINE ADAPTER: NOT IMPLEMENTED
EXTERNAL CREATURE RUNTIME BINDING: NOT TESTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

The dependency manifest intentionally leaves the runtime mod IDs for Alex's Mobs Continued, CodxLib and Threateningly Mobs Continued unresolved instead of guessing them. The `core` profile can boot without those content dependencies; the future `gameplay` profile must fail clearly until the pinned artifacts/IDs are verified and installed.

This checkpoint proves the architecture can compile and boot. It does **not** close the full M0 acceptance matrix in §13.

---

# 15.2 Verified gameplay-foundation artifact resolution — 2026-09-18

> Historical checkpoint for the initial nine-JAR resolution gate. Later §15.3 adds MobFilter, resolves all creature IDs and verifies the actual gameplay dependency runtime.

The nine non-creature M0 gameplay-foundation artifacts are now independently resolvable in CI without placing them on the `core` runtime classpath.

Implementation:

- `build.gradle` owns a resolvable-only `gameplayFoundation` configuration;
- `verifyGameplayFoundationDependencies` resolves exactly nine pinned primary artifacts with transitive resolution disabled for this gate;
- each resolved JAR is checked non-empty and SHA-256 hashed;
- the core server profile remains isolated, so this gate cannot accidentally make a dependency part of gameplay authority merely by resolving it.

Verified commit / workflow:

```text
IMPLEMENTATION COMMITS:
  979e674dedaa276b086b76c1ed53a2bb2d4eca95
  067e47500d1f71e77f4c97907f0257a5214a21dc

SUCCESSFUL WORKFLOW:
  Build Openworld RPG
  run 35310816215

GAMEPLAY FOUNDATION ARTIFACT RESOLUTION: PASS
UNIT TESTS: PASS
CLEAN BUILD: PASS
BOOTSTRAP JAR VERIFY: PASS
DEDICATED SERVER CORE-PROFILE BOOT: PASS
FULL GAMEPLAY RUNTIME: NOT TESTED
TRANSITIVE RUNTIME GRAPH: NOT YET ADMITTED
BETTER COMBAT ADAPTER: NOT IMPLEMENTED
SPELL ENGINE ADAPTER: NOT IMPLEMENTED
CREATURE DEPENDENCY RUNTIME IDS: STILL OPEN WHERE MARKED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

The first resolution attempt (workflow run `35310725627`) failed only because GeckoLib's creator Cloudsmith endpoint returned HTTP 401 to the CI runner. The exact pinned Fabric 5.5.5 file was then changed to public CurseMaven coordinate `curse.maven:geckolib-388172:8819017`. No gameplay code or version pin changed.

Exact resolved filenames, byte sizes and SHA-256 hashes are recorded in `M0_GAMEPLAY_FOUNDATION_RESOLUTION_2026-09-18.md`.

This closes **artifact availability for the nine foundation JARs**, not their runtime integration. The next M0 gate is exact creature artifact/runtime-ID inspection plus an actual `gameplay` profile with explicit dependency graph and bounded adapters.

---

# 15.3 Verified gameplay dependency runtime boot — 2026-09-18

The pinned dependency stack is now admitted to an isolated Fabric 26.2 `gameplay` development runtime and dedicated-server co-load verified.

Canonical evidence:

- `M0_GAMEPLAY_RUNTIME_BOOT_2026-09-18.md`
- successful code state: `d35658c21bde609001e6d440ef0089be058b74b7`
- successful workflow: **Build Openworld RPG**, run `35313724183`

Current verified runtime contract:

```text
FOUNDATION / SAFETY PRIMARY JARS: RESOLVED + HASHED — 10/10
CURATED CREATURE PRIMARY JARS: RESOLVED + METADATA INSPECTED — 3/3
DEPENDENCY MANIFEST RUNTIME IDS: RESOLVED — 13/13
GAMEPLAY-PROFILE DEDICATED SERVER BOOT: PASS
PINNED TOP-LEVEL DEPENDENCY CO-LOAD: PASS
SERVER READY STATE: PASS
```

Verified curated creature IDs:

```text
Alex's Mobs Continued 2.1.13 → alexsmobs
CodxLib 1.6.0 → codxlib
Threateningly Mobs Continued 1.1.1+fabric.26.2 → threateningly_mobs
```

The 10 foundation/safety set includes MobFilter `0.28.0+26.2` in addition to the earlier nine foundation JARs.

The successful gameplay server reported the expected current runtime families together:

- Better Combat;
- Player Animation Library;
- GeckoLib;
- Armor Model API;
- Ranged Weapon API;
- Trinkets Updated;
- Spell Engine;
- Spell Power Attributes;
- Cloth Config/runtime support;
- MobFilter;
- Alex's Mobs Continued;
- CodxLib;
- Threateningly Mobs Continued;
- Fabric API/Loader and nested runtime libraries required by those JARs.

Important limitation:

> **dependency co-load is not an authority adapter.**

Still open:

```text
BETTER COMBAT → PROJECT DAMAGE AUTHORITY: NOT IMPLEMENTED
SPELL ENGINE → PROJECT MANA/CD/IMPACT AUTHORITY: NOT IMPLEMENTED
MOBFILTER PROJECT ECOLOGY RULES: NOT IMPLEMENTED
ALEX/TMC PROJECT SPAWN-STAT-LOOT OVERRIDES: NOT IMPLEMENTED
CLIENT RUNTIME: NOT TESTED
R01 PLAYER-FACING IMPLEMENTATION: NOT STARTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

The next M0 technical gate is one bounded authority bridge at a time, beginning with Better Combat and Spell Engine. Do not add another broad RPG runtime before those bridges are proven.

---

# 16. What M0 closes

This audit closes implementation-time uncertainty about:

- Java/Fabric/Gradle/Loom baseline;
- player and entity animation engines;
- melee/ranged/spell runtime boundaries;
- armor and accessory backends;
- vanilla-spawn suppression safety layer;
- primary creature-content dependencies;
- Essential's optional hosting role;
- RPG Series not being the project progression baseline;
- which external code may only be reference;
- project/server ownership boundaries;
- external-mod composition through adapters/data overlays/tags instead of donor-file forks;
- public-repository dependency/license discipline.

The next planning work must **not** reopen these selections from zero unless a bootstrap test finds a hard blocker or a dependency's 26.2 support materially changes.

The old queue that still called for combat-formula design, five root-class design and frequent-action/keybind design is obsolete. Those are already closed in the later combat/class/accessibility canon.

Current production sequence:

1. keep `M0_INTEGRATION_ARCHITECTURE.md` as the source-bootstrap composition contract;
2. continue the already-booting M0 stack with bounded authority adapters — Better Combat first, then Spell Engine, then one real external-creature overlay;
3. in parallel, finish the remaining exact asset and actual-Azari spatial gates required before player-facing R01 implementation;
4. remove stale live-document contradictions as encountered;
5. implement R01 as the first full vertical-slice proof only after its asset/spatial gates close;
6. scale the proven architecture across later regions rather than redesigning the stack.

Do not add another complete RPG/progression mod to shorten implementation. Reuse strong primitives/content, but keep one project authority.
