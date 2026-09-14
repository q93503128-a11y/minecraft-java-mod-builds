# Open-World RPG — External Sources Registry

> Provenance/adoption registry for external code, UI, models, maps, dungeons, structures, audio and design precedents.  
> `GAME_DESIGN.md` remains the gameplay canon.

## Why this file exists

The project deliberately uses external high-quality solutions instead of improvising weaker replacements. The playable build is intended for private use, while this GitHub repository is public. Every adopted external source must therefore be classified before files/code are committed.

## Status labels

- `REFERENCE` — design/behavior reference only; no source asset copied into the public repo.
- `CODE_CANDIDATE` — source implementation worth studying/reusing; license/version must be checked before integration.
- `EDITABLE_BASE` — asset intended to be modified; terms must permit the intended use.
- `DIRECT_USE_REDISTRIBUTABLE` — may be committed/redistributed under confirmed terms and attribution requirements.
- `LOCAL_ONLY` — may be used in the owner's local private build when terms permit, but must not be committed to the public repo.
- `DEPENDENCY` — use as an external dependency rather than copying implementation/assets.
- `REJECTED` — unsuitable because of quality, terms, compatibility, overlap or design mismatch.
- `VERIFY` — promising lead whose current terms/version have not yet been fully audited for adoption.

## Required record

```text
Name:
Category:
Author:
Source URL:
Status:
License / Usage Terms:
Minecraft / Loader Version:
Files or code used:
Modified:
Used in:
Attribution required:
Public-repo safe:
Verification date:
Notes:
```

Do not infer permission from "downloadable" or "open source" alone.

---

# Research/adoption queue

| Candidate | Area | Known status / license at latest research | Project intent |
|---|---|---|---|
| AcroWield | dodge / guard / parry | VERIFY / code candidate | inspect as a basis for dodge, guard and perfect-guard behavior |
| Better Combat | weapon animation / combo cadence | ARR; current 26.2 Fabric release observed | use as dependency/reference for proven attack movement, animation and combo cadence; do not copy ARR source/assets |
| RPG Inventory | RPG inventory/equipment slots | VERIFY / code candidate | inspect architecture and interaction model; final visual selected separately |
| open minimap implementations | minimap rendering/markers | VERIFY / code candidate | adapt proven rendering/marker logic rather than rebuilding a weak minimap |
| Veloren | open-source voxel RPG precedent | REFERENCE | study open-world traversal, towns, dungeons, combat state machines and progression |
| Wynncraft | Minecraft MMORPG precedent | REFERENCE | study region density, service buildings, gathering-node gameplay, dungeons/raids and class presentation; never copy proprietary assets |
| Wynntils | Wynncraft client augmentation | LGPL-3.0-only observed; source linked | reference implementation for highlighting/discovering gathering nodes and map/HUD interaction |
| JD Resource Nodes | renewable ore-node system | Apache-2.0 observed; NeoForge 1.21.1 | strong code/behavior reference for permanent renewable nodes, depletion states, regeneration and scanner concepts |
| Deep Drilling | biome-based rare ore nodes | MIT observed; Fabric/Forge 1.20.1 | reference/code candidate for biome-conditioned resource nodes and region-linked extraction concepts |
| DarkMining | RPG-style probabilistic mining drops | current 26.2 Fabric/Forge/NeoForge listing observed | loot probability reference only; does not replace the project's node-based gathering direction |
| Quaternius Modular Weapons Pack | weapon models | CC0 / strong direct-use-editable candidate | coherent medieval weapon source family |
| Quaternius Fantasy Props MegaKit | props / crafting / town dressing | CC0 / strong direct-use-editable candidate | weapons, tools, potions, stalls, chests, furniture and service props |
| Quaternius Medieval Village Pack / MegaKit | buildings / settlement | CC0 / strong direct-use-editable candidate | coherent visual base for service buildings and settlements |
| Kenney Fantasy Town Kit | town/building family | CC0 candidate | alternate coherent town/building visual family |
| Planet Minecraft schematics | inns / blacksmiths / villages / shrines / castles / dungeons | VERIFY; creator terms vary | primary pool for private/local structure use |
| external open-world RPG maps | terrain/region skeleton | VERIFY / REFERENCE or LOCAL_ONLY | select terrain first, then derive about 12 regions |
| external dungeon/structure packs | dungeons / ruins / shrines / towns | VERIFY | retain strong architecture, replace encounters/rewards |
| external RPG UI designs/assets | inventory / skill HUD / forge / alchemy / cooking / stats / class | VERIFY | select proven final designs and minimize redesign |
| Icy's Better Horses | mount progression/travel/UI/code precedent | MIT; current 26.2 Fabric release observed | strong code/reference candidate for meaningful mount progression and riding UX |
| Vehicle Upgrade | mount/vehicle quality-of-life | MPL-2.0; current 26.2 Fabric release observed | inspect mount handling/QoL architecture; use as dependency/reference if appropriate |
| Horse Combat Controls | mounted-combat controls | MIT | code/reference candidate for Mount & Blade-like horse controls; version compatibility must be rechecked |
| Jasmine Dragons | flying dragon behavior/riding reference | ARR; current 26.2 Fabric release observed | behavior/design reference only unless terms permit dependency use |
| Musket Mod | flintlock/musket mechanics | public source; license/version must be re-audited | reference/code candidate for non-modern black-powder firearm behavior |
| Spell Engine | skill/spell runtime, weapon skills, data-driven casting | GPL-3.0-only; current 26.2 Fabric + NeoForge listing observed | preferred dependency candidate for data-driven active skills and weapon-skill execution; avoid copying source unless GPL implications are intentionally accepted |
| Rogues & Warriors (RPG Series) | warrior/rogue skills, martial weapons, combat presentation | ARR; current 26.2 Fabric + NeoForge listing observed | `DEPENDENCY / LOCAL_ONLY / REFERENCE`; use high-quality martial skills directly when they fit the project, but do not copy ARR code/assets into the public repo |
| Archers (RPG Series) | bow/ranged skills and equipment | ARR; current 26.2 Fabric + NeoForge listing observed | `DEPENDENCY / LOCAL_ONLY / REFERENCE`; source for Hunter ranged skill packages and presentation |
| Paladins & Priests (RPG Series) | healing, holy combat, shields/support | ARR; current 26.2 Fabric + NeoForge listing observed | `DEPENDENCY / LOCAL_ONLY / REFERENCE`; source for Cleric and selected Guardian skill packages |
| Wizards (RPG Series) | arcane/fire/frost spell packages | ARR; current 26.2 Fabric + NeoForge listing observed | `DEPENDENCY / LOCAL_ONLY / REFERENCE`; source for Mage spell packages and presentation |
| Skill Tree (RPG Series) | class skill-tree content/UI behavior | ARR; current 26.2 Fabric + NeoForge listing observed | dependency/reference only; study 100+ node class-tree organization without copying ARR content into repo |
| RPG Class Selection (RPG Series Tweaks) | data-driven class selection / upgrades | MIT; Fabric 1.21.1 source | strong `CODE_CANDIDATE`; port only the useful class-definition/state/UI architecture to 26.2 if it fits the project-owned progression model |
| Archetypes | server-authoritative active/passive class skill implementation | MIT; current 26.2 Fabric/Forge/NeoForge listing observed | strong `CODE_CANDIDATE`; selectively reuse/port skill, targeting, networking and server-authority patterns while rejecting its vanilla-XP progression rules |
| Pufferfish's Skills | configurable skill-tree framework | custom license; current 26.2 Fabric + NeoForge releases observed | `VERIFY / DEPENDENCY`; evaluate terms and UX before adoption, especially if RPG Series Skill Tree becomes useful |
| Ranged Weapon API | bow/crossbow construction and ranged weapon behavior | MIT; current 26.2 Fabric + NeoForge listing observed | strong dependency/code candidate for Hunter ranged families instead of rebuilding bow/crossbow fundamentals |

---

# Class / skill reuse strategy

The class system is intentionally **external-first at the mechanic/content level**, not only at the visual-reference level.

Rules:

- If a current external mod already provides a high-quality class skill, animation, targeting behavior or weapon interaction that fits the project, prefer using it directly as a dependency/local-installed module or adapting legally reusable source rather than rebuilding a weaker clone.
- ARR RPG Series content may be used as a normal installed dependency/reference for the private playable build, but its source/assets are not copied into this public repository.
- MIT sources such as `RPG Class Selection` and `Archetypes` may be selectively ported/adapted with required notices; import only the necessary architecture/mechanics instead of copying entire mod trees.
- Spell Engine is currently the strongest runtime candidate for data-driven active skills and weapon skills. Prefer dependency use. Copying GPL source into project code is not the default because it changes licensing obligations.
- The project keeps ownership of its canonical EXP/Lv, five root classes, per-class progression, advancement history, class-switch economy, ultimate rules and server-authoritative state. External vanilla-XP or advancement assumptions are not imported merely because a source uses them.
- External class names do not automatically become player-facing canon. Their best skill packages may be mapped into the project's Warrior / Hunter / Cleric / Mage / Guardian roles and advancement branches.
- Weapon hard locks from external mods are not inherited by default. The canonical weapon-freedom rule remains: equipment can be used broadly, while stats, skill compatibility, passives and cadence create natural specialization.
- External skills must still satisfy the project's readable telegraph, visible-range = actual-hitbox, multiplayer authority, resource-cost and animation-quality standards.
- When a dependency already supplies polished effects/animations/models that are permitted for normal use, preserve them when they outperform a custom replacement; do not redesign only to make the result more original.

## Spell Engine

- Modrinth: https://modrinth.com/mod/spell-engine
- Source: https://github.com/ZsoltMolnarrr/SpellEngine
- License observed: GPL-3.0-only.
- Current listing observed on 2026-09-14: Minecraft 26.2, Fabric and NeoForge.
- Provides data-driven spells, hot-reload/network synchronization, weapon-spell assignment and built-in class-agnostic weapon skills.
- Project status: strong `DEPENDENCY`; use as the leading M0 active-skill runtime candidate.

## RPG Series combat/class content

Current 26.2 candidates observed on 2026-09-14:

- Rogues & Warriors — https://modrinth.com/mod/rogues-and-warriors
- Archers — https://modrinth.com/mod/archers
- Paladins & Priests — https://modrinth.com/mod/paladins-and-priests
- Wizards — https://modrinth.com/mod/wizards
- Skill Tree — https://modrinth.com/mod/skill-tree

The content projects are listed ARR. Treat them as normal dependencies/local content/reference, not source-asset donor repositories. Their strongest skills, spell presentation and equipment interactions may be used directly in the private build when compatibility is good, then wrapped/mapped into the project's class/progression contract.

## RPG Class Selection

- Modrinth: https://modrinth.com/mod/rpg-class-selection
- Source: https://github.com/TheRedBrain/rpg-class-selection
- License: MIT.
- Observed version: Fabric 1.21.1; direct 26.2 compatibility is not established.
- Classes and upgrades are data-defined and can combine Spell Engine spells with attribute modifiers; unlocking and screen access are configurable.
- Project status: strong `CODE_CANDIDATE` for class definitions, saved selection/upgrades and service-triggered class screens. Port concepts/code selectively rather than adopting its RPG Inventory coupling unchanged.

## Archetypes

- Modrinth: https://modrinth.com/mod/archetypes
- Source: https://github.com/balovich-matje/archetypes
- License: MIT.
- Current Modrinth listing observed on 2026-09-14 includes Minecraft 26.2 on Fabric/Forge/NeoForge.
- Source contains implemented server-side class/skill logic, active and passive abilities, networking, mana, targeting and specialization logic despite an outdated planning-oriented repository description/README.
- Useful examples include whirlwind-style melee, shadow-step targeting, marksman/deadeye logic, protector/colossus-style defensive mechanics and elementalist magic.
- Project status: strong `CODE_CANDIDATE`, not a wholesale dependency decision. Reuse only mechanics that fit; reject vanilla XP mirroring, vanilla advancement progression and other incompatible progression assumptions.

## Ranged Weapon API

- Modrinth: https://modrinth.com/mod/ranged-weapon-api
- License: MIT.
- Current listing observed on 2026-09-14 includes Minecraft 26.2 on Fabric and NeoForge.
- Provides bow/crossbow construction, damage, pull-time, projectile velocity and first/third-person behavior.
- Project status: strong `DEPENDENCY / CODE_CANDIDATE` for Hunter ranged foundations.

---

# Gathering / resource-node precedents

## JD Resource Nodes

- Source: CurseForge project `resource-nodes`; source link is published on the project page.
- License shown by the current project page: Apache-2.0.
- Observed supported game version: Minecraft 1.21.1, NeoForge.
- Adds permanent renewable vanilla ore nodes.
- A harvested node enters a depleted/base-block state and regenerates after a configurable delay.
- Nodes are immovable/indestructible in ordinary survival and are explicitly described as useful for map-oriented play.
- Includes node purity tiers and an unlockable scanner concept.
- Project status: strong `CODE_CANDIDATE / REFERENCE` for the project's field-resource-node state machine, regeneration and map-authored placement; not a direct 26.2 Fabric dependency candidate without porting.

## Deep Drilling

- Source: Modrinth project `deepdrilling`; source is linked.
- License shown: MIT.
- Observed platform/version: Fabric/Forge 1.20.1.
- Uses rare ore nodes associated with different biomes.
- Project status: useful `REFERENCE / CODE_CANDIDATE` for region/biome-linked resource identity; version is old enough that direct dependency is not the current plan.

## Wynncraft / Wynntils gathering precedent

- Wynncraft is reference-only proprietary game content, not an asset source.
- Its gathering-profession model demonstrates a Minecraft open-world RPG loop where players locate dedicated gathering resources rather than treating arbitrary cave mining as the entire profession system.
- Wynntils is open source and its current changelog includes gathering-node highlighting functionality.
- Project status: use Wynncraft for gameplay/UX precedent and Wynntils for inspectable client-side node-discovery/highlight interaction patterns where useful.

## DarkMining

- Current project listing observed for Minecraft 26.2 across Fabric/Forge/NeoForge.
- Focuses on chance-based extra mining drops rather than persistent world nodes.
- Project status: secondary probability/loot reference only. It does not match the canonical gathering loop as closely as renewable/placed resource nodes.

---

# Building / settlement candidates

## Medieval Tavern & Interior — Itzz_Aspect

- Source: Planet Minecraft, project `medieval-tavern-amp-interior-free-schematic`.
- Current page explicitly allows use on private or public servers and modification to fit the world/resource pack.
- Current page prohibits re-uploading the schematic/world as one's own and selling the build by itself or in a paid pack.
- Contains a tavern/inn with common room, bar, fireplace and guest rooms.
- Project status: strong `LOCAL_ONLY` inn/tavern candidate for the private playable build; do not commit the schematic bytes to this public repository unless redistribution terms are separately confirmed.

## Medieval Village — 130RED

- Source: Planet Minecraft, project `medieval-village-6444733`.
- Download formats listed include Litematic / Schem / World.
- Current listed contents include shop, tavern, barn, fountain, warehouse, houses and NPC locations.
- Current page was updated in 2026 with interiors/routes.
- Permission/redistribution terms are not yet sufficiently established for repo inclusion.
- Project status: `VERIFY / LOCAL_ONLY` candidate for starting settlement architecture or layout inspiration.

## Medieval Smith's Shop — V&M Builds / Digital-Archivist

- Source: Planet Minecraft, project `medieval-smith-s-shop-schematic-file-v-amp-m-builds`.
- Dedicated smith's shop + living quarters, based on historical-building references.
- Project status: `VERIFY / LOCAL_ONLY` candidate for forge/smith building.

## Other inn/tavern pool

Planet Minecraft currently has multiple downloadable inn/tavern schematics with full interiors or editable interiors. These are useful because the project can choose a final architecture instead of drawing its own temporary building. Every individual creator page still requires terms verification before actual use.

---

# Mount candidates

## Icy's Better Horses

- Current public listing observed: Minecraft 26.2, Fabric maintained, source linked, MIT license.
- Features include horse progression, ownership/bonding, multiple breeds/genetics, tack, carts, command wheel, owned-horse roster and riding fixes.
- Useful to study or reuse under license for differentiated ground mounts rather than cosmetic reskins.

## Vehicle Upgrade

- Current public listing observed: Minecraft 26.2 Fabric, MPL-2.0, source linked.
- Focuses on mount/vehicle QoL and handling.
- Candidate for dependency/reference rather than importing unrelated source.

## Horse Combat Controls

- GitHub project with MIT license and Mount & Blade-style horse steering/combat-control concept.
- Version fit must be checked before adoption.
- Useful architecture/interaction reference for combat mounts.

## Jasmine Dragons

- Current listing observed for Minecraft 26.2 Fabric.
- ARR, so treat primarily as behavior/reference unless terms permit dependency use.
- Useful reference for large flying predators, mounted flight, dive/climb control and ranged breath attacks.

---

# Weapon / firearm direction

The project now permits an **early black-powder firearm family** when it fits the Hunter line and coherent external design can be sourced. Examples: hand cannon, matchlock/flintlock pistol, musket. Modern/automatic firearms remain outside the baseline fantasy direction.

`mc-musketmod` is a public-source precedent for craftable flintlock weapons and bullet/headshot behavior. Before reusing source, re-audit its exact license and current Minecraft/loader compatibility. Until then it remains `VERIFY / CODE_CANDIDATE`.

---

# Existing verified asset notes

## Quaternius Modular Weapons Pack

- Source: https://quaternius.com/packs/medievalweapons.html
- 24 models; FBX / OBJ / Blend.
- Source license: CC0.
- Includes swords, daggers, bows, shields, hammers and more.

## Quaternius Fantasy Props MegaKit

- Source: https://quaternius.com/packs/fantasypropsmegakit.html
- 200+ medieval/fantasy props; FBX / OBJ / Blend / glTF.
- Source license: CC0.
- Strong coherent family for weapons, tools, potions, stalls, chests and furniture.

## Quaternius Medieval Village Pack / MegaKit

- Sources:
  - https://quaternius.com/packs/medievalvillage.html
  - https://quaternius.com/packs/medievalvillagemegakit.html
- Village Pack: 44 models, CC0.
- MegaKit: 300+ modular environment pieces, CC0.
- Useful as building/environment editable bases if converted appropriately to Minecraft.

## Kenney Fantasy Town Kit

- Source: https://kenney.nl/assets/fantasy-town-kit
- 160 files; source license CC0.
- Alternate coherent town/building family candidate.

---

# Selection policy

## UI / HUD

For every important screen, collect strong external examples/assets, select the preferred final design before implementation, verify terms/source-file availability, and preserve a directly usable strong design as intact as practical. No temporary player-facing UI is allowed while waiting.

## Maps / structures / dungeons

Prefer high-quality external builds/worlds. For every candidate classify whether it is safe to commit, dependency/download-instruction only, local/private-instance only, or reference only. `LOCAL_ONLY` bytes stay outside this public repo.

## Models / characters / monsters

Important enemies do not ship as vanilla recolors. Prioritize models that support suitable conversion/topology, rigging, animations, textures and modification permission.

## Code

Do not import an entire mod tree for one mechanic. Adapt only necessary architecture when terms permit, preserve required notices, and remove unrelated/dead code. After replacement is verified, delete duplicate/prototype implementations.

---

# Public repository boundary

Never commit paid/non-redistributable asset files, private-use-only ripped UI/textures/models, assets obtained through DRM/access-control bypass, or source code whose license forbids intended redistribution. The local private game may use separately obtained assets when their real terms permit private use; this registry must keep that boundary explicit.