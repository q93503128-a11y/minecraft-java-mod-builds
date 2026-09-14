# Open-World RPG

Large Minecraft Java open-world action RPG project with very low dependence on vanilla progression systems.

## Current status

`DESIGN CANON BUILDING / NO SOURCE BOOTSTRAP YET`

No implementation exists yet. Do not treat the project as buildable until M0 explicitly creates the loader/toolchain/source layout.

## Canon start here

1. [`GAME_DESIGN.md`](./GAME_DESIGN.md) — gameplay/design source of truth
2. [`PROJECT.md`](./PROJECT.md) — technical/build/project contracts
3. [`EXTERNAL_SOURCES.md`](./EXTERNAL_SOURCES.md) — external code/assets/UI/maps/structures provenance and adoption status

## Locked direction summary

- open-world action RPG rather than vanilla-plus;
- vanilla XP/levels removed from the core game;
- custom progression uses `EXP` and `Lv` notation for player and enemies;
- custom RPG inventory/equipment presentation;
- HP + Mana + Stamina;
- dedicated dodge key, guard and parry;
- basic attacks cost no Stamina;
- sprint consumes a low amount of Stamina;
- most skills use Mana, with a small concept-driven minority allowed to use Stamina;
- 4 normal active skills + 1 high-impact ultimate;
- six primary stats: VIT / END / STR / DEX / INT / WIL;
- Attack Speed remains an independent gear/class/build axis;
- few root classes with deep, repeated multi-stage advancement;
- weapons are broadly class-independent and differentiated by scaling/cadence/mechanics;
- respec/advancement uses an in-world shrine-like location;
- death offers a choice between losing currency or losing EXP;
- external open-world terrain/map determines the final major-region layout; target roughly 10–14 major regions;
- project-owned monster ecosystem with regional threats, including desert worm and dragon directions;
- external high-quality dungeons/structures preferred;
- campfire/tent/camp/base direction accepted;
- minimap planned;
- Essential-friendly multiplayer is a major technical goal;
- no temporary player-facing visual design: external final-quality design/assets/reference are used from the first visible implementation;
- dead/superseded/duplicate implementation is removed after safe replacement.

## Next design checkpoint

Continue from `GAME_DESIGN.md#23-next-design-decisions`.
The next major decision is the root-class and advancement-tree topology, followed by weapon families and combat-resource timing.
