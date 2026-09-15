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
| Azari 30k x 30k | primary open-world terrain | free download; usage/redistribution terms not explicit; map uses some third-party assets | `VERIFY / LOCAL_ONLY` primary map candidate; use local world bytes only, derive 10–14 regions from its terrain, do not commit map bytes until permission is explicit |
| external open-world RPG maps | terrain/region skeleton | VERIFY / REFERENCE or LOCAL_ONLY | Azari is current first choice; keep Theia/other free maps only as fallback if Azari import/terms fail |
| DeCubed Dungeons | biome-specific dungeon architecture | CC-BY-NC-SA-4.0; current 26.2 datapack + Fabric/Forge/NeoForge/Quilt mod release observed | `DEPENDENCY / LOCAL_ONLY / REFERENCE`; strong free architecture pool, but replace every vanilla spawner/loot assumption before use in this project's no-vanilla ecology |
| external dungeon/structure packs | dungeons / ruins / shrines / towns | VERIFY | retain strong architecture, replace encounters/rewards |
| external RPG UI designs/assets | inventory / skill HUD / forge / alchemy / cooking / stats / class | VERIFY | select proven final designs and minimize redesign |
| MobFilter | spawn suppression / authored ecology | Apache-2.0; current 26.2 Fabric + NeoForge listing observed | strong `DEPENDENCY / CODE_CANDIDATE`; use or adapt rule-based spawn blocking so all vanilla mobs stay out of the authored RPG ecology, with project code remaining authoritative |
| Icy's Better Horses | riding/progression code precedent | MIT; current 26.2 Fabric release observed | **reference/code candidate only under current design**; useful ownership/bonding/QoL ideas, but visible vanilla horses cannot be the finished mount population |
| Vehicle Upgrade | mount/vehicle quality-of-life | MPL-2.0; current 26.2 Fabric release observed | inspect generic riding/QoL architecture; only retain features that also work with non-vanilla mounts |
| Horse Combat Controls | mounted-combat controls | MIT | code/reference candidate for Mount & Blade-like steering/combat concepts; do not make vanilla horse visuals canonical |
| Jasmine Dragons | flying dragon behavior/riding reference | ARR; current 26.2 Fabric release observed | behavior/design reference and possible normal dependency use under its terms for later dragon/high-speed traversal |
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
| Alex's Mobs Continued | broad wildlife + creature roster | GPL-3.0-only; current Fabric 26.2 release observed; full ~116-mob roster | strong `DEPENDENCY / REFERENCE`; curate non-vanilla wildlife by region and override/suppress unwanted defaults; useful R01 candidates include gazelle, bison, raccoon, crow, grizzly, rattlesnake and cave centipede |
| Nemo's Creatures | biome-specific hostile variants/creatures | MIT; current Fabric 26.2 release observed | secondary `DEPENDENCY / CODE_CANDIDATE`; current 26.2 v2.0 removed Mummy and Scorched Skeleton, and vanilla-derived variants are lower priority under the no-vanilla-identity rule |
| Threateningly Mobs Continued | fantasy monsters, breedable creatures, heavy creatures, bosses | MIT; current Fabric 26.2 listing observed | strong `DEPENDENCY / CODE_CANDIDATE`; R01 uses Louxia as food ecology and considers Steelboar/Nature Spirit/Regalhart/Earthloong for authored elite/boss roles; default global progression/spawns are not inherited |
| Mob Champions | elite modifier/champion behavior | MIT; current Fabric 26.2 release observed | `REFERENCE / CODE_CANDIDATE`; study elite spawning/modifier/readability logic, but project owns elite reward tables and regional identities |
| Mutant Monsters | large mutant miniboss creatures | AGPL-3.0-only; current Fabric 26.2 release observed | low-priority `DEPENDENCY / REFERENCE`; vanilla-mutant identity conflicts with the current no-vanilla visible ecology, so do not use as normal regional content |
| Ambient Creatures | passive ambient wildlife | MIT; current Fabric 26.2 release observed | optional `DEPENDENCY / CODE_CANDIDATE` for world life where it does not dilute stronger regional ecology |

---

# Primary world candidate

## Azari — 30k x 30k

- Source: https://www.planetminecraft.com/project/azari-30k-x-30k-world-painter-map/
- Author: itzvmbie / vmbie.
- Published map size: 30,000 x 30,000 blocks.
- Current page: free download by entering `$0`; creator profile states world downloads are intended to remain free.
- Current compatibility note: 1.21+ world; later 2026 update added underground and above-ground structures.
- Published terrain includes 30+ custom biomes and 15+ cave variants.
- Published biome/terrain examples include plains, meadows, badlands, desert, oasis, jungle, bamboo forest, barrier reef, multiple ocean types, fairy/whimsical forests, pirate cove, great mountains, ice spikes/ocean/abyss, freezing taiga, windswept hills, multiple shore types, rich forest, eroding waterfall, overgrown caves, obsidian spikes, volcanic ash island, pollinating cliffs and flower forest.
- Vanilla structures are not expected inside the authored 30k area; this is useful for the project because settlements/dungeons can be deliberately authored instead of inheriting random vanilla villages.
- The page states some terrain assets come from other creators. Therefore free download does **not** establish redistribution permission for the complete world.
- Project status: **primary `VERIFY / LOCAL_ONLY` world candidate**. Use the downloaded world only in the private playable instance until modification/redistribution terms are explicit. GitHub stores provenance and integration notes, not world bytes.
- Design intent: derive roughly 12 major RPG regions from the visible terrain instead of inventing an unrelated continent layout.
- Exact borders remain provisional until the world is imported and inspected in-game; biome-based boundaries should follow actual rivers, mountain chains, coastlines and terrain transitions.

---

# Creature / monster reuse strategy

The world ecology is external-first, but external mobs are not dumped into the map indiscriminately.

Rules:

- **No vanilla living mob is part of the normal visible world population.** Vanilla hostile, passive, neutral, aquatic, villager/trader/golem and ambient mob spawn paths are disabled/replaced inside authored RPG play.
- Region identity owns spawning. A dependency creature is enabled only where it strengthens that region's ecology, difficulty profile or encounter role.
- Normal wildlife, normal hostile enemies, elites, minibosses, field bosses and dungeon bosses are separate content tiers.
- Early-region normal enemies should be readable and fair; the sharp difficulty spikes belong mainly to elites and bosses.
- Ordinary enemies do not become the main equipment-drop source. Their rewards focus on EXP, currency, materials and selected consumables; equipment farming is concentrated in elites/bosses/dungeons/quests/chests/merchants/crafting.
- External mods' default spawn tables, item progression, dimensions, crafting recipes and loot are not canonical. Disable, override or ignore conflicting parts where technically possible.
- Imported structure packs must have vanilla mob spawners/encounters replaced before they are considered finished content.
- Prefer direct dependency use for large polished rosters. Prefer permissive source adaptation only when a narrow project-owned behavior is needed.
- Important bosses still need project-level telegraph, hitbox, reward, phase and region-fit review even when their model/animation comes from a dependency.
- Vanilla-derived reskins/variants may be rejected even when technically non-vanilla entities if their visual identity undermines the replacement goal.
- Creature-derived food/materials are supplied by the custom ecology; the game must never require cows/pigs/sheep/chickens or vanilla fish just to keep food/crafting functional.

## Spawn suppression — MobFilter

- Source: https://modrinth.com/mod/mobfilter
- Current listing observed 2026-09-15: Minecraft 26.2; Fabric and NeoForge; Apache-2.0; server-side/singleplayer.
- Supports completely preventing selected mobs from spawning and contextual rules by biome/time/light.
- Project status: strong `DEPENDENCY / CODE_CANDIDATE` for M0. It can provide an immediate robust external solution for vanilla spawn suppression while project-owned spawn/region rules are integrated.
- Acceptance rule: no visible vanilla mob may leak into authored gameplay through normal natural spawn, imported structures or dependency spawn side effects.

## Alex's Mobs Continued

- Source: https://modrinth.com/mod/alexs-mobs-continued
- Current listing observed 2026-09-15: Fabric 26.2, client+server, GPL-3.0-only, Fabric API + CodxLib.
- Carries the full Alex's Mobs roster with original models/textures/animations/behaviors.
- The current Fabric build keeps the full roster rather than a reduced port.
- Strong biome-specific pool includes examples such as grizzly bear, gazelle, crocodile, orca, gorilla, rattlesnake, hammerhead shark, komodo dragon, cave centipede, moose, seal, elephant, snow leopard, snapping turtle, catfish, rhinoceros, caiman and numerous fantastical creatures.
- R01 shortlist: gazelle and bison for large open-meadow wildlife/resource roles; raccoon/crow for ambient life; grizzly for rare territorial danger; rattlesnake and cave centipede for readable low-level threats.
- Project status: strong **dependency** for wildlife/secondary monsters. Do not turn all ~116 creatures on by default; curate by region.

## Nemo's Creatures

- Source: https://modrinth.com/mod/nemos-creatures
- Current listing observed 2026-09-15: Fabric 26.2, client+server, MIT.
- Important correction: current `26.2-2.0` explicitly removed **Mummy** and **Scorched Skeleton**. Do not design current content around old gallery/wiki references to those removed entities.
- The mod still supplies biome-specific variants/creatures, but many are visually tied to vanilla hostile archetypes.
- Project status: secondary candidate rather than a core ecology dependency. Use only creatures that remain visually strong under the project's no-vanilla-mob identity.

## Threateningly Mobs Continued

- Source: https://modrinth.com/mod/threateningly-mobs-continued
- Current listing observed 2026-09-15: Fabric 26.2, client+server, MIT.
- Useful published heavy-creature placements include Nature Spirit in forests, Earthloong in forests/jungles, Desert Beetle and Armor of Desert in deserts/badlands, Beast Horseshoe Crab in oceans/beaches, Hydra in oceans/rivers/swamps, Steelboar in savannas/meadows/dark forests/badlands, Regalhart in meadows/taigas/forests, Riptooth in oceans at night and Flamehorn in savannas.
- Summonable larger bosses include Titan Rabbit, Inferno, Terradragon and Abyss Fang.
- The companion wiki documents breedable/passive creatures that can replace vanilla resource animals. **Louxia** naturally fits plains/sunflower-plains-like environments, is passive/breedable and drops Louxia meat plus a luminous material; it is the current strongest R01 food-ecology candidate.
- Other documented non-vanilla resource creatures such as Copas, Hippofish, Red Triplefish, Giant Sea Cucumber and Diplocaulus can support later hot/coastal/swamp regions instead of vanilla livestock/fish.
- Project status: strong **dependency/code candidate** for custom food ecology, elites, field bosses and selected threats. Its original stats, loot, summon-stone progression, structures/dimension and default spawn frequencies are not automatically canonical.

## Mob Champions

- Source: https://modrinth.com/mod/mobchampions
- Current Fabric 26.2 release observed; MIT.
- Creates enhanced champion versions of mobs with equipment/stats/effects.
- Project status: study as an elite-generation/readability implementation reference. The project's own elite taxonomy, visuals and rewards remain authoritative; do not allow random champion spam to erase authored region identity.

## Mutant Monsters

- Source: https://modrinth.com/mod/mutant-monsters
- Current Fabric 26.2 release observed; AGPL-3.0-only.
- Provides polished large mutant Zombie/Skeleton/Creeper/Enderman-style encounters.
- Project status: currently **not planned for normal world content** because its identity is explicitly derived from vanilla mobs. Keep only as a distant reference unless a later special corruption concept justifies it without undermining the no-vanilla ecology.

---

# Dungeon / structure sources

## DeCubed Dungeons

- Source: https://modrinth.com/datapack/dungeons%2B
- Current listing observed 2026-09-15: Minecraft 26.2; Data Pack + Fabric/Forge/NeoForge/Quilt; server-side/singleplayer.
- License: CC-BY-NC-SA-4.0.
- Adds 30+ biome-specific underground dungeons/structures and also ships a loader-mod release.
- Project use: strong **free private-use architecture/reference candidate** for dungeon shells and layout ideas.
- Critical integration rule: its vanilla spawner types and standard vanilla dungeon loot are **not** adopted unchanged. Any selected shell is manually integrated/converted so project enemies, rewards, Lv rules and no-vanilla ecology remain authoritative.
- R01 use: inspect lush/mouldy/forest-compatible or quarry-compatible structures as a shell/source for the first root-overgrown quarry dungeon rather than building a generic AI dungeon from scratch.

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

## Mount policy after vanilla-mob removal

Visible mounts must use non-vanilla creature/model identities. Horse-centric projects may still supply useful permissive code/UX ideas, but ordinary horses, camels, pigs or other vanilla mobs are not restored merely because a riding library targets them.

## Icy's Better Horses

- Current public listing observed: Minecraft 26.2, Fabric maintained, source linked, MIT license.
- Features include horse progression, ownership/bonding, multiple breeds/genetics, tack, carts, command wheel, owned-horse roster and riding fixes.
- Project status changed from direct visible-mount candidate to **CODE_CANDIDATE / REFERENCE**. Reuse/port generic ownership, bonding, roster or riding UX where worthwhile, but do not use vanilla horse presentation as final game content.

## Vehicle Upgrade

- Current public listing observed: Minecraft 26.2 Fabric, MPL-2.0, source linked.
- Focuses on mount/vehicle QoL and handling.
- Candidate for generic dependency/reference if its improvements work with custom rideable entities.

## Horse Combat Controls

- GitHub project with MIT license and Mount & Blade-style horse steering/combat-control concept.
- Version fit must be checked before adoption.
- Useful architecture/interaction reference for custom combat mounts rather than a reason to reintroduce horses.

## Jasmine Dragons

- Current listing observed for Minecraft 26.2 Fabric.
- ARR, so treat primarily as behavior/reference unless terms permit normal dependency use.
- Useful reference/candidate for large flying predators, mounted flight, dive/climb control and ranged breath attacks in later progression.

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

Important enemies do not ship as vanilla recolors. Prioritize models that support suitable conversion/topology, rigging, animations, textures and modification permission. Under the current canon, vanilla living mobs are not part of normal visible world population.

## Code

Do not import an entire mod tree for one mechanic. Adapt only necessary architecture when terms permit, preserve required notices, and remove unrelated/dead code. After replacement is verified, delete duplicate/prototype implementations.

---

# Public repository boundary

Never commit paid/non-redistributable asset files, private-use-only ripped UI/textures/models, assets obtained through DRM/access-control bypass, or source code whose license forbids intended redistribution. The local private game may use separately obtained assets when their real terms permit private use; this registry must keep that boundary explicit.
