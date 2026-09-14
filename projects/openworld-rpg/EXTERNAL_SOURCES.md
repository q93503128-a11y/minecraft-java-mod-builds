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
| Better Combat | weapon animation / combo cadence | VERIFY / dependency or reference | use proven animation/combo concepts without importing unrelated code |
| RPG Inventory | RPG inventory/equipment slots | VERIFY / code candidate | inspect architecture and interaction model; final visual selected separately |
| open minimap implementations | minimap rendering/markers | VERIFY / code candidate | adapt proven rendering/marker logic rather than rebuilding a weak minimap |
| Veloren | open-source voxel RPG precedent | REFERENCE | study open-world traversal, towns, dungeons, combat state machines and progression |
| Wynncraft | Minecraft MMORPG precedent | REFERENCE | study region density, service buildings, dungeons/raids and class presentation; never copy proprietary assets |
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

Planet Minecraft currently has multiple downloadable 2026 inn/tavern schematics with full interiors or editable interiors. These are useful because the project can choose a final architecture instead of drawing its own temporary building. Every individual creator page still requires terms verification before actual use.

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
