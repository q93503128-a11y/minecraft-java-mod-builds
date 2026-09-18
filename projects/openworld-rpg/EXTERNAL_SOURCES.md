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

## Quaternius 2026 license-drift override

As of the 2026-09-15 review, Quaternius public metadata is not safe to compress into a blanket `Quaternius = CC0` rule:

- the central `https://quaternius.com/license.html` publishes **Quaternius Asset License (QAL) v1.0**, last updated 2026-08-28;
- QAL permits use, modification and incorporation into a completed Product, but prohibits redistribution of the raw Assets themselves as assets;
- QAL §7 states later license changes are not retroactive to assets already obtained under an earlier license;
- at the same time, multiple individual pack pages still display `CC0`, including `Modular Character Outfits - Fantasy` and `Medieval Village MegaKit` during this review.

Therefore every Quaternius adoption is package/acquisition specific. A current pack page showing `CC0` is evidence to preserve, but it does not justify blindly committing newly downloaded raw Quaternius bytes while the central QAL also exists. For the public repository, exact package/license-at-acquisition evidence wins. If that evidence is not preserved, use `VERIFY / LOCAL_ONLY` or the stricter state recorded in `R01_ASSET_INTAKE.md`.

R01-specific evidence is recorded in `R01_ASSET_PHASE_B_EVIDENCE_2026-09-15.md`.

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
| Quaternius Modular Weapons Pack | weapon models | package-specific `VERIFY / LOCAL_ONLY` unless acquisition-time CC0 evidence is preserved; central QAL v1.0 now exists | coherent medieval weapon source family; do not assume newly downloaded raw files are public-repo redistributable |
| Quaternius Fantasy Props MegaKit | props / crafting / town dressing | package-specific `VERIFY / LOCAL_ONLY` unless acquisition-time CC0 evidence is preserved; central QAL v1.0 now exists | weapons, tools, potions, stalls, chests, furniture and service props; private Product use remains a strong candidate |
| Quaternius Medieval Village Pack / MegaKit | buildings / settlement | pack pages still show CC0, while central QAL v1.0 also exists; acquisition evidence required | coherent visual base for service buildings and settlements; raw repo inclusion is not assumed |
| KayKit Fantasy Weapons Bits | weapon models | current authoritative itch page CC0; exact A/B/C filename families inspected, source archive SHA-256 and final mesh selection pending | strong public-safe R01 weapon family; acquire authoritative package before import and choose final variants after 3D review |
| KayKit RPG Tools Bits | tools / crafting props | current authoritative itch page CC0; exact tool filenames inspected, archive SHA-256/model review pending | smith/mining/workstation prop family |
| KayKit Restaurant Bits | food / kitchen props | CC0; official public GitHub tree inspected at commit `153c8a7535b48237854cb54ff6890679f8c574d1` | R01 meal/ingredient family; exact roast/stew candidates pinned, Trail Skewers still unresolved |
| KayKit Character Animations 1.1 | humanoid animation | current authoritative itch page CC0; 161 clips; exact 1.1 work/fishing names published | primary public-safe motion pool; eating/drinking are still described as planned, not shipped |
| Kenney Fantasy Town Kit | town/building family | CC0 candidate | alternate coherent town/building visual family |
| Quaternius Animated Fish — OpenGameArt creator snapshot | historical fish source family / provenance | creator-uploaded OpenGameArt page/file explicitly CC0; exact ZIP `Animated Fish Pack by @Quaternius.zip`; project-local hash pending | old Poly Pizza-visible Fish entries reviewed for R01 and rejected as clownfish/Sea-Life/tuna identity; retain source as provenance/reference, not current R01 finalist |
| CDmir Esox - Animated Fish | R01 freshwater Rare candidate | OpenGameArt creator upload CC0; exact `esox.zip` | strong direct-review priority: freshwater predator silhouette, 1,632 tris, Idle/Slow Swim/Fast Swim |
| CDmir Fish (Animated) | R01 Common/Uncommon candidate | OpenGameArt creator upload CC0; exact `fish.zip` | rigged/animated/game-ready candidate; style compatibility must be checked in 3D/Minecraft |
| Quaternius Armored Catfish | R01 freshwater Common/Uncommon candidate | Poly Pizza model `mtd9QK5yCe`; Public Domain / CC0; FBX/GLTF; Animated | priority direct-review candidate because it keeps the broader Quaternius low-poly family while reading freshwater; binary animation/scale review pending |
| Quaternius Goldfish | fish reference / possible later pond content | Poly Pizza model `qS6CgsWFAh`; Public Domain / CC0; Animated | `REJECTED_R01_BASELINE` for current river/ford catches; ornamental/domesticated read is wrong for Heartland baseline |
| gfroad 3d low poly catfish | R01 freshwater alternate | OpenGameArt creator upload CC0; exact `catfish.zip` / `catfish_obj.zip` | distinct freshwater silhouette; animation conversion/source-format risk before acceptance |
| joyfulsquirrel Fish | R01 lightweight Common alternate | OpenGameArt creator upload CC0; exact `fish.zip` | 328-triangle rigged Swim candidate; accept only if visual quality matches R01 |
| Small Fish — Common Minnow | R01 small-stream Common direct-review candidate | s&box `Small Fish` listing marks Common Minnow CC0; collection says fish models are handmade from real-world references | exact fourth morphology target; collection is ragdoll-model based, so exact package locator + mesh/texture review + clean swim-rig adaptation remain required |
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
- Historical/current pack-page metadata has described Quaternius assets as CC0, but the central site now also publishes QAL v1.0.
- Project status: **package-specific verification required**. Preserve the actual package/license-at-acquisition before deciding raw public-repo handling.
- Includes swords, daggers, bows, shields, hammers and more.

## Quaternius Fantasy Props MegaKit

- Source: https://quaternius.com/packs/fantasypropsmegakit.html
- 200+ medieval/fantasy props; FBX / OBJ / Blend / glTF.
- Do not reuse the old blanket `Source license: CC0` statement without package-specific acquisition evidence because the central QAL v1.0 now exists.
- Strong coherent family for weapons, tools, potions, stalls, chests and furniture; remains a strong private-play candidate under applicable terms.

## Quaternius Medieval Village Pack / MegaKit

- Sources:
  - https://quaternius.com/packs/medievalvillage.html
  - https://quaternius.com/packs/medievalvillagemegakit.html
- Village Pack: 44 models.
- MegaKit: 300+ modular environment pieces.
- The MegaKit page still displayed CC0 during the 2026-09-15 review while the central site also published QAL v1.0.
- Project status: **package-specific acquisition/license evidence required** before raw source files are treated as public-repo redistributable.
- Useful as building/environment editable bases if converted appropriately to Minecraft and handled under the applicable package terms.

## KayKit R01 Phase-B asset families

- Fantasy Weapons Bits: https://kaylousberg.itch.io/fantasy-weapons-bits — current page CC0; exact bow/sword/hammer/spear/staff/shield filename families have been inspected as tree evidence, but final model selection and authoritative archive SHA-256 remain pending.
- RPG Tools Bits: https://kaylousberg.itch.io/rpg-tools-bits — current page CC0; evidence-backed names include `hammer.gltf`, `anvil.gltf`, `pickaxe.gltf`, `grindstone.gltf`, `tongs.gltf` and `mallet.gltf`; source package hash/model acceptance pending.
- Restaurant Bits official repository: https://github.com/KayKit-Game-Assets/KayKit-Restaurant-Bits-1.0 — CC0 public tree; observed commit `153c8a7535b48237854cb54ff6890679f8c574d1`; R01 candidate entries include `food_dinner.gltf` and `food_stew.gltf`; Trail Skewers remains unresolved.
- Character Animations: https://kaylousberg.itch.io/kaykit-character-animations — CC0, 161 current animations; exact 1.1 `Hammer/Hammering`, `Pickaxe/Pickaxing`, `Work_*`, `Working_*` and fishing clip names are published. Current page still describes eating/drinking as planned rather than shipped.
- Detailed R01 evidence: `R01_ASSET_PHASE_B_EVIDENCE_2026-09-15.md`.

## R01 fish candidate pool

The dedicated authoritative intake state is `R01_ASSET_INTAKE.md` §3.4.

Creator/source-specific references:

- Quaternius creator upload — https://opengameart.org/content/animated-fish — CC0; exact published ZIP `Animated Fish Pack by @Quaternius.zip`;
- Quaternius Poly Pizza bundle — https://poly.pizza/bundle/Animated-Fish-Bundle-ZkGbjS8m8g — CC0/Public Domain; 3 generic Fish + Dolphin/Shark/Whale/Manta ray;
- CDmir Esox — https://opengameart.org/content/esox-animated-fish — CC0; `esox.zip`;
- CDmir Fish (Animated) — https://opengameart.org/content/fish-animated — CC0; `fish.zip`;
- gfroad catfish — https://opengameart.org/content/3d-low-poly-catfish — CC0;
- joyfulsquirrel Fish — https://opengameart.org/content/fish-1 — CC0.

Current policy:

- these are **candidate sources**, not final four R01 species;
- R01 never uses Dolphin/Shark/Whale/Manta merely to fill the early river roster;
- the three old individually reviewed Quaternius `Fish` entries are also out of the R01 queue: clownfish visual (`BEcU9rjiAq`), Sea-Life deep-bodied fish (`Ymu8ftrmuT`), and Tuna/Sea-Life (`XWl86YFtpF`);
- current four-role R01 direct-review set is **Common Minnow + CDmir Fish + Quaternius Armored Catfish + CDmir Esox**; broad fish discovery stops here unless one role fails direct review;
- final player-facing species names are written only after exact accepted model inspection;
- exact archive bytes and project-local SHA-256 remain mandatory before raw asset admission;
- icons derive from accepted models rather than unrelated fish illustrations.

## Kenney Fantasy Town Kit

- Source: https://kenney.nl/assets/fantasy-town-kit
- 160 files; source license CC0.
- Alternate coherent town/building family candidate.


## Quaternius downstream exact-file corroboration snapshots — R01 Phase E

These entries are **technical corroboration only**. They do not replace creator-controlled acquisition and they do not authorize raw-byte admission into this repository by themselves.

### Modular Character Outfits extracted snapshot

- downstream repository: `dustinc555/mygame`
- pinned commit: `6f12ffb2f924af86d910ade13e6e2ba3df8cd3df`
- inspected path: `assets/vendor/quaternius/modular_character_outfits_fantasy/modular_parts/`
- directly parsed in Phase E: Wizard, Ranger and Knight selected glTF parts;
- observed structure: 65-joint humanoid skins, external BIN payloads, authored BaseColor/Normal/ORM texture families, low-thousands triangle counts;
- authoritative acquisition remains: Quaternius creator page / current intended archive;
- project-local archive SHA-256: pending.

### Fantasy Props extracted snapshot

- downstream repository: `dustinc555/mygame`
- pinned commit: `6f12ffb2f924af86d910ade13e6e2ba3df8cd3df`
- inspected path: `assets/vendor/quaternius/fantasy_props_megakit/gltf/`
- directly parsed: `Potion_1.gltf`, `Potion_2.gltf`, `Potion_3.gltf`, `Potion_4.gltf` and their external BIN references;
- Phase E confirms four distinct geometry payloads rather than four color-only aliases;
- authoritative acquisition remains the creator Fantasy Props MegaKit / creator OpenGameArt Standard snapshot;
- final R01 three-potion assignment remains a visual/hand-pivot/Minecraft acceptance decision.

### Universal Animation Library corroboration

Historical snapshot:

- repository: `J-Ponzo/gltf-universal-animation-library`;
- stated distribution date: 2025-06-10;
- glTF blob: `d9e132ad1d41089f8f96488775829d220a4beb05`;
- parsed animation count: 46;
- exact `Drink`: absent;
- consequence: this old Standard mirror is not valid proof for the current UAL1 `Drink` candidate.

Newer integration snapshot:

- repository: `DyingStar-game/DyingStar`;
- pinned commit: `f8a783b1f6a5387652e99ec12823bb2ae7600f30`;
- `UAL1_Standard.glb` Git blob: `473e59080288428d0b6da826ba19324d07b191f0`, 7,618,436 bytes;
- `UAL1.glb` Git blob: `df3d91e3ec69cd2ac61a91f83c8cf81f1bd44c22`, 21,378,992 bytes;
- `UAL2.glb` Git blob: `bb3d392ebbc07363eca57e76ef4f4e6853bb37b9`, 20,717,364 bytes;
- its animation mapping source explicitly maps `emote_drink -> "Drink"` and `emote_consume -> "Consume"`;
- its emote catalog explicitly classifies consume among UAL2-resolved emotes;
- current creator archive acquisition and direct clip timing/root-motion/retarget review remain pending.

Full evidence: `R01_ASSET_PHASE_E_APPAREL_POTION_MOTION_EXACT_REVIEW_2026-09-18.md`.


## Kenney exact-file corroboration snapshots — R01 Phase F

These snapshots are **filename/byte/timing corroboration only**. Official Kenney asset pages remain the intended acquisition/license authority.

### Image-library mirror

- repository: `shorepine/kenney`;
- pinned commit: `3694c6879e487c108f55677be7dd2ca75b07cc3b`;
- used only to read exact Kenney all-in-one paths and selected PNG bytes;
- Phase-F selected image inputs: `circle_03.png`, `spark_04.png`, `slash_01.png`, `light_03.png`, `magic_03.png`, `whitePuff06.png`, `gas04.png`, `flash04.png`;
- direct SHA-256 and dimensions are recorded in `R01_ASSET_PHASE_F_KENNEY_EXACT_SHORTLIST_2026-09-18.md`.

### Impact Sounds mirror

- repository: `Boyquotes/kenney-impact-sounds-for-godot`;
- pinned commit: `999dd1684873f8b020a3aa5b26e713da21688924`;
- exact Ogg candidates directly read: `impact_mining_002.ogg`, `impact_metal_heavy_001.ogg`, `impact_punch_medium_000.ogg`, `impact_soft_heavy_000.ogg`;
- direct SHA-256, sample rate and duration recorded;
- audio audition remains pending.

### RPG Audio mirror

- repository: `Boyquotes/kenney-rpg-audio-for-godot`;
- pinned commit: `22eb79bb843bbcadcaa6ed119353a33265ffad11`;
- exact Ogg candidates directly read: `metal_click.ogg`, `creak_2.ogg`;
- direct SHA-256, sample rate and duration recorded;
- audio audition remains pending.

### Interface Sounds mirror

- repository: `Calinou/kenney-interface-sounds`;
- pinned commit: `4596a49eaf5a533948d49a47467f606bcdea70ff`;
- mirror README links the official Kenney Interface Sounds pack, identifies 100 interface sounds and states that the original Ogg files were converted to WAV for Godot;
- directly read mirror WAV candidates: `confirmation_001.wav`, `error_001.wav`, `select_001.wav`;
- recorded SHA-256 values identify the mirror WAV binaries, **not** the original official Ogg archive bytes.

### Signature-layer boundary

No Phase-F Kenney candidate closes:

- Earthloong electrical charge/strike sound;
- Earthloong body-anchored electrical arcs;
- final shaped Burning VFX;
- final shaped Frostbite VFX.

Generic files may support those composites, but cannot be promoted to complete signature presentation without actual Minecraft visual/audio acceptance.

Full evidence: `R01_ASSET_PHASE_F_KENNEY_EXACT_SHORTLIST_2026-09-18.md`.

---

# Architecture / large-mod integration precedents — 2026-09-18

These sources informed `M0_INTEGRATION_ARCHITECTURE.md`.

They are **architecture precedents**, not blanket runtime-adoption or code-copy authorization.

## All The Mods 10

- repo: `https://github.com/AllTheMods/ATM-10`
- reviewed snapshot: `ab6f65e07b88423cdae1724864ba42a573ba758a`
- role: modpack-level integration architecture reference
- observed useful patterns:
  - per-mod KubeJS integration folders;
  - tag/recipe/loot unification;
  - startup registry additions/aliases;
  - cross-mod deny/blacklist tags;
  - conditional integration when a mod is present.
- boundary: inspected scripts carry All Rights Reserved notices; **REFERENCE_ONLY**.
- project adoption: reproduce the architecture with Fabric/Java + datapack/data registries; do not copy ATM scripts.

## Mine & Slash Rework

- repo: `https://github.com/RobertSkalko/Mine-And-Slash-Rework`
- reviewed branch/snapshot: `1.20-Forge` / `13dab4218dbcfc7a4f93b51ca6f1ec5274977507`
- role: overhaul compatibility / canonical-damage-pipeline reference
- observed useful patterns:
  - explicit compatibility modes;
  - central damage conversion/override layer;
  - datapackable RPG content;
  - one damage authority instead of adding unrelated RPG damage models together.
- boundary: architecture reference in this pass; exact file-level license must be checked before any code reuse.

## Spell Engine

- repo: `https://github.com/ZsoltMolnarrr/SpellEngine`
- reviewed snapshot: `76cd9e128468ebe005463c729ec73eff7de5fb68`
- role: existing project runtime dependency + engine/content-separation precedent
- useful pattern:
  - generic cast/target/delivery/sync engine beneath data-defined content;
  - project progression/balance stays separate.
- license/boundary remains the dedicated `M0_DEPENDENCY_AUDIT.md` rule: GPL dependency is not copied wholesale into this public repository.

## Cobblemon

- repo: `https://github.com/Cobblemon-Global/Cobblemon`
- reviewed snapshot: `75bb1a6c2fe92ef54951fb68d16d21f290a9ee88`
- license observed: MPL-2.0
- role: data-registry / addon / overlay / event / synchronization architecture reference
- reviewed source families:
  - `JsonDataRegistry`;
  - `SpeciesAdditions`;
  - `CobblemonEvents`;
  - data-registry synchronization packets.
- useful pattern:
  - target existing content with additive/override files instead of replacing donor base files;
  - use unique namespaces;
  - expose stable event surfaces;
  - keep server data canonical and sync client-required data intentionally.
- project adoption: project-owned external-actor overlays and normalized integration events. Cobblemon itself is **NOT BASELINE**.

## FTB Quests

- repo: `https://github.com/FTBTeam/FTB-Quests`
- reviewed snapshot: `622091bbe07bc5bce151c8bb4ba99b81f6f7c312`
- license observed from current project metadata: All Rights Reserved
- role: typed quest-object / server-progression architecture reference
- reviewed source families:
  - `TaskType`;
  - `RewardType`;
  - `TeamData`;
  - `ServerQuestFile`.
- useful pattern:
  - reusable objective/reward type registries;
  - separate progress/claim/repeat state;
  - server persistence + explicit client synchronization.
- boundary: **REFERENCE_ONLY / NOT BASELINE**. Openworld RPG keeps its own quest state, narrative rules and UI.

## Create

- repo: `https://github.com/Creators-of-Create/Create`
- reviewed snapshot: `fc9535d82a29419164a1e9dc9c678bdcddeab30d`
- role: addon API / encapsulation / tag architecture reference
- useful pattern:
  - public addon/developer surfaces;
  - useful semantic tags;
  - explicit warning against external code reaching into certain internal registrars when a callback/API exists.
- project adoption: prefer public API → registry/tag/data → event/callback → config → narrow shim; donor-internal mixin/reflection is last resort.
- boundary: reference-only unless an exact file/license review later authorizes reuse.

## KubeJS version boundary

Current public 26.x KubeJS distribution observed on 2026-09-18 is NeoForge 26.1.2, while the publicly listed Fabric line remains on older Minecraft generations.

References:

- `https://www.curseforge.com/minecraft/mc-mods/kubejs/files/all`
- `https://github.com/kube-mods/kubejs/tree/2601`

Decision:

- **KubeJS = NOT BASELINE for Fabric 26.2**;
- its value here is the integration-layer pattern demonstrated by large modpacks, not the runtime itself.

---

# Selection policy

## UI / HUD

For every important screen, collect strong external examples/assets, select the preferred final design before implementation, verify terms/source-file availability, and preserve a directly usable strong design as intact as practical. No temporary player-facing UI is allowed while waiting.

## Maps / structures / dungeons

Prefer high-quality external builds/worlds. For every candidate classify whether it is safe to commit, dependency/download-instruction only, local/private-instance only, or reference only. `LOCAL_ONLY` bytes stay outside this public repo.

## Models / characters / monsters

Important enemies do not ship as vanilla recolors. Prioritize models that support suitable conversion/topology, rigging, animations, textures and modification permission. Under the current canon, vanilla living mobs are not part of normal visible world population.

## Code

Do not import an entire mod tree for one mechanic. Prefer a dependency's documented public API, registry IDs/tags/data and events before touching internals. Keep donor-specific code inside the integration layer defined by `M0_INTEGRATION_ARCHITECTURE.md`.

Adapt or copy code only when the exact source/file license permits it, preserve required notices, and keep the imported surface as small as practical. After replacement is verified, delete duplicate/prototype implementations.

Visible source is not a license. Architecture ideas may be learned from reference-only projects without copying their code.

---

# Public repository boundary

Never commit paid/non-redistributable asset files, private-use-only ripped UI/textures/models, assets obtained through DRM/access-control bypass, or source code whose license forbids intended redistribution. The local private game may use separately obtained assets when their real terms permit private use; this registry must keep that boundary explicit.
