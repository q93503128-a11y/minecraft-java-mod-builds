# Open-World RPG — External Sources Registry

> This is the provenance/adoption registry for external code, UI, models, maps, dungeons, structures, audio and other assets.
> `GAME_DESIGN.md` remains the gameplay canon.

## Why this file exists

The project deliberately uses external high-quality solutions instead of improvising weaker replacements.
At the same time, the repository is public even though the playable project is intended for private use.

Every adopted external source must therefore be classified before files/code are committed.

## Status labels

- `REFERENCE` — visual/design/behavior reference only; no source asset copied into the public repo.
- `CODE_CANDIDATE` — source implementation worth studying/reusing; license/version must be checked before integration.
- `EDITABLE_BASE` — asset intended to be modified; actual terms must permit the intended use.
- `DIRECT_USE_REDIStributable` — may be committed/redistributed under confirmed terms and attribution requirements.
- `LOCAL_ONLY` — may be used in the owner's local private build, but must not be committed to the public repo.
- `DEPENDENCY` — use as an external dependency rather than copying its implementation/assets.
- `REJECTED` — unsuitable because of quality, terms, compatibility, overlap, or design mismatch.
- `VERIFY` — promising lead whose current license/version/terms have not yet been re-audited for adoption.

## Required record

For every adopted source, record:

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

# Initial research queue

These are leads already identified during design discussion. They are **not automatically approved dependencies/assets**. Re-check current upstream source, version and terms before use.

| Candidate | Area | Current project intent | Status |
|---|---|---|---|
| AcroWield | dodge / guard / parry / defensive combat | inspect as a basis for dodge, guard and perfect-guard behavior | VERIFY / CODE_CANDIDATE |
| Better Combat | weapon attack animation/combo structure | reference or dependency candidate for data-driven attack cadence | VERIFY |
| RPG Inventory | custom RPG inventory/equipment slots | inspect architecture and interaction model; choose final visual separately | VERIFY / CODE_CANDIDATE |
| open minimap implementations | minimap rendering/markers | adapt a proven implementation instead of rebuilding a weak minimap | VERIFY / CODE_CANDIDATE |
| external open-world RPG maps | terrain/region skeleton | select terrain first, then derive 10–14-ish major regions from its actual geography | VERIFY / REFERENCE or LOCAL_ONLY depending on terms |
| external dungeon/structure packs | dungeons, ruins, shrines, towns | retain high-quality architecture and replace encounters/rewards with project systems | VERIFY |
| external camp/tent/campfire assets | open-world travel atmosphere | use final-quality camp visuals from first playable version | VERIFY |
| external RPG UI designs/assets | inventory, skill HUD, forge, alchemy, cooking, stats/class | select proven final designs and minimize visual redesign | VERIFY |

---

# Selection policy by category

## UI / HUD

For each important screen:

1. collect strong external examples/assets;
2. identify a preferred final design;
3. verify usage terms and source-file availability;
4. if direct use is allowed, keep the design as intact as practical;
5. only alter information/interaction necessary for this game;
6. record the exact source here.

No temporary player-facing UI is allowed while waiting for the final design.

Priority screens:

- inventory/equipment;
- skill HUD + ultimate slot;
- stats;
- class/advancement;
- forge;
- alchemy;
- cooking;
- shrine/respec;
- death/respawn choice;
- minimap/world map;
- dungeon/quest presentation where needed.

## Maps / structures / dungeons

Prefer external high-quality builds/worlds.
Before adoption determine whether the material is:

- safe to commit and redistribute;
- allowed only as a dependency/download instruction;
- usable only in the owner's local private instance;
- reference-only.

If an asset is `LOCAL_ONLY`, keep its bytes outside this public repository and document local installation/import steps instead.

## Models / characters / monsters

Important enemies should not ship as vanilla entity recolors.
Prioritize models that include or can support:

- suitable topology/box structure for Minecraft;
- rig;
- attack/idle/movement animation capability;
- texture source;
- modification permission.

Design targets already requested include:

- desert earth/sand worm;
- dragons;
- biome/region-specific monster families;
- field bosses and dungeon bosses.

## Code

External code is valuable when it is better tested or structurally stronger than a fresh implementation.
Do not import an entire mod source tree for one mechanic.
Extract/adapt only the necessary architecture when terms permit, remove unrelated code, and preserve required notices.

After replacement is live and verified, delete superseded duplicate/prototype implementations from this project.

---

# Public repository boundary

Never commit to this public repo:

- paid asset files without redistribution permission;
- ARR/non-redistributable map or dungeon files;
- private-use-only ripped game UI/textures/models;
- assets obtained through access-control or DRM bypass;
- third-party source code whose license does not allow the intended redistribution.

The local private game can still use separately obtained assets when their actual terms permit that private use; this registry must make the boundary explicit.
