# Open-World RPG — External Sources Registry

> Provenance/adoption registry for external code, UI, models, maps, dungeons, structures, audio and design precedents.  
> `GAME_DESIGN.md` remains the gameplay canon.

## Why this file exists

The project deliberately uses external high-quality solutions instead of improvising weaker replacements.
The playable build is intended for private use, while this GitHub repository is public.
Every adopted external source must therefore be classified before files/code are committed.

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
| AcroWield | dodge / guard / parry | VERIFY / code candidate | inspect as a basis for dodge, guard and perfect-guard behavior; re-check current 26.2 compatibility and license before adoption |
| Better Combat | weapon animation / combo cadence | VERIFY / dependency or reference | use proven animation/combo concepts without importing unrelated code |
| RPG Inventory | RPG inventory/equipment slots | VERIFY / code candidate | inspect architecture and interaction model; final visual design selected separately |
| open minimap implementations | minimap rendering/markers | VERIFY / code candidate | adapt proven rendering/marker logic rather than rebuilding a weak minimap |
| Veloren | open-source voxel RPG precedent | REFERENCE | study combat state-machine thinking, open-world traversal, towns, dungeons, class/skill progression and horizontal-progression lessons |
| Wynncraft | Minecraft MMORPG precedent | REFERENCE | study world-region density, professions, dungeons/raids, class/ability-tree presentation and service buildings; do not copy proprietary assets |
| Quaternius Modular Weapons Pack | weapon models | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 24 medieval weapon models including swords, daggers, bows, shields, hammers and related assets; convert/rebuild for Minecraft style if visually suitable |
| Quaternius Fantasy Props MegaKit | weapons / props / crafting / town dressing | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 200+ low-poly props including weapons, tools, potions, market stalls, chests and furniture; strong coherent source family |
| Quaternius Medieval Village Pack | buildings / settlement | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 44 medieval buildings/props; possible service-building visual source |
| Quaternius Medieval Village MegaKit | modular settlement buildings | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 300+ modular environment pieces; useful as editable base for coherent town/service-building design |
| Quaternius Ultimate Fantasy RTS | buildings / environment | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 128 buildings/nature assets in multiple evolution stages; potential settlement progression reference/base |
| Kenney Fantasy Town Kit | town / building visual family | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 160 3D files; evaluate style fit before adoption |
| Kenney Retro Fantasy Kit | castle/town/building visual family | `DIRECT_USE_REDISTRIBUTABLE` candidate; CC0 | 100 retro-fantasy assets; evaluate style fit before adoption |
| Planet Minecraft downloadable schematics | shrines / castles / towns / dungeons | `VERIFY`; terms vary per creator | large source pool for private/local structures; each build must be individually audited and many may remain `LOCAL_ONLY` |
| external open-world RPG maps | terrain/region skeleton | `VERIFY` / `REFERENCE` or `LOCAL_ONLY` | select terrain first, then derive roughly 12 major regions from actual geography |
| external dungeon/structure packs | dungeons / ruins / shrines / towns | `VERIFY` | retain strong architecture and replace encounters/rewards with project systems |
| external RPG UI designs/assets | inventory / skill HUD / forge / alchemy / cooking / stats / class | `VERIFY` | select proven final designs and minimize redesign; private-use assets may be `LOCAL_ONLY` |

---

# Verified research notes

## Quaternius Modular Weapons Pack

- Source: https://quaternius.com/packs/medievalweapons.html
- Published source page identifies 24 models.
- Formats: FBX / OBJ / Blend.
- License shown by source: CC0.
- Stated categories include swords, daggers, bows, shields, hammers and more.
- Adoption status: strong `EDITABLE_BASE` / `DIRECT_USE_REDISTRIBUTABLE` candidate after visual-fit and Minecraft-conversion review.

## Quaternius Fantasy Props MegaKit

- Source: https://quaternius.com/packs/fantasypropsmegakit.html
- 200+ medieval/fantasy props.
- Includes weapons, tools, potions, market stalls, chests, furniture and other world dressing.
- Formats include FBX / OBJ / Blend / glTF.
- License shown by source: CC0.
- Adoption status: strong coherent asset family candidate for world services and props.

## Quaternius Medieval Village Pack / MegaKit

- Sources:
  - https://quaternius.com/packs/medievalvillage.html
  - https://quaternius.com/packs/medievalvillagemegakit.html
- Village Pack: 44 models, CC0.
- MegaKit: 300+ modular environment pieces, CC0.
- Useful for forge, alchemist, tavern, guild/service-building and settlement visual bases if converted well to Minecraft's visual language.

## Kenney Fantasy Town Kit

- Source: https://kenney.nl/assets/fantasy-town-kit
- 160 files.
- License shown by source: CC0.
- Adoption status: alternate coherent town/building family candidate.

## Planet Minecraft schematics

- Search pool: https://www.planetminecraft.com/projects/tag/fantasy/?platform=1&share=schematic
- Thousands of downloadable fantasy schematics exist, but permission varies by creator.
- Example: some creators allow server/build use with credit and prohibit resale; others provide no broad redistribution license.
- Treat each individual structure as `VERIFY` until its page/author terms are checked.
- For this private-play project, a creator-permitted private/local schematic can be `LOCAL_ONLY` even when it cannot be committed to GitHub.

---

# Design precedent notes

## Veloren

Use as a structural reference, not a Minecraft asset source.
Useful lessons identified from current public docs/code history:

- voxel open-world RPG can support towns, caves, dungeons, mounts and multiplayer as one loop;
- combat was deliberately moved toward a state-machine architecture;
- current roadmap explicitly discusses greater horizontal progression rather than pure vertical inflation;
- UI/assets/combat/balance are treated as distinct specialist disciplines.

## Wynncraft

Use as a Minecraft-specific RPG reference.
Useful lessons:

- large detailed world can sustain classes, thousands of items, dungeons, raids and exploration;
- professions are separated from Combat level and can remain optional rather than mandatory combat gates;
- crafting/gathering can be split into distinct roles;
- dungeons/raids gain identity from exclusive reward types rather than being only alternate EXP farms;
- real towns/world locations act as access points for services rather than every feature living in a generic menu.

Do not inherit Wynncraft's class-locked weapon rule because this project's canon explicitly allows broad weapon freedom.

---

# Selection policy by category

## UI / HUD

For every important screen:

1. collect strong external examples/assets;
2. select a preferred final design before implementation;
3. verify terms and source-file availability;
4. if direct use is allowed, preserve the proven visual as intact as practical;
5. only alter information/interaction required by this game;
6. record the exact source here.

No temporary player-facing UI is allowed while waiting for the final design.

Priority screens:

- inventory/equipment;
- skill HUD + ultimate;
- stats;
- class/advancement;
- forge;
- alchemy;
- cooking;
- shrine/class change;
- death/respawn penalty choice;
- minimap/world map;
- quest/dungeon presentation.

## Maps / structures / dungeons

Prefer high-quality external builds/worlds.
For every candidate classify whether it is:

- safe to commit and redistribute;
- dependency/download-instruction only;
- local/private-instance only;
- reference only.

`LOCAL_ONLY` bytes stay outside this public repository.

## Models / characters / monsters

Important enemies do not ship as vanilla recolors.
Prioritize models that include or can support:

- suitable topology/box conversion;
- rigging;
- attack/idle/movement animations;
- texture source;
- modification permission.

Accepted design targets include desert earth/sand worm, dragons, region-specific monster families, field bosses and dungeon bosses.

## Code

External code is valuable when better tested or structurally stronger than a fresh implementation.
Do not import an entire mod tree for one mechanic.
Adapt only necessary architecture when terms permit, preserve required notices, and remove unrelated/dead code.
After replacement is verified, delete duplicate/prototype implementations from this project.

---

# Public repository boundary

Never commit:

- paid asset files without redistribution permission;
- ARR/non-redistributable map or dungeon files;
- private-use-only ripped UI/textures/models;
- assets obtained through DRM/access-control bypass;
- source code whose license forbids intended redistribution.

The local private game may use separately obtained assets when their real terms permit private use; this registry must keep that boundary explicit.
