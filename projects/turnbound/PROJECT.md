# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.17`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62
- Java 25
- Gradle 9.2.1
- GeckoLib retained for authored model/animation

## Canon authority

Current overhaul canon:
1. latest explicit user decisions
2. `01_GAME_DESIGN_v1.md`
3. `02_BALANCE_RULES_v1.md`
4. `03_CHARACTER_DESIGN_v1.md`
5. `UI_DESIGN_SYSTEM.md`
6. `WORLD_OVERHAUL_DREHMAL.md`
7. current source/resources

The old v0.4 Aster March physical-world canon and alpha-by-alpha design deltas are superseded.
Historical intent remains available in Git history; they are not current production authority.

## Game identity

TURNBOUND is not Minecraft survival with turn combat added.
It is an independent 3D party turn-based RPG hosted inside Minecraft.

Core loop:
world exploration → NPC/event/visible encounter → 4-person turn battle → reward/growth → new route/character/content.

## Combat contract

Preserve:
- max 4 allies / normal max 5 enemies
- Turn Gauge threshold 1000
- action subtracts 1000, overflow survives
- player decision time does not advance logical combat time
- cooldowns tick on owner's later regular actions, not reactions
- Basic cooldown 0
- server-authoritative combat
- AUTO and 1x/2x do not alter logical results

Overhaul:
- current whole-integer pulse scheduler is superseded by deterministic fixed-point logical time
- actual next actor and UI timeline must share one TurnScheduler
- SPD is action-frequency power, not cosmetic ordering
- Gauge/Speed buffs, revive gauge and action-advance use the same scheduler semantics

## Battle camera contract

Camera pivot is the **battle center**, never an arbitrary player-shell coordinate.

Battle center is derived from terrain-resolved ally/enemy formation anchors.
Skill cameras may temporarily move, then return to the battle-center view.

Required:
- terrain-aware footprint
- slope/wall/cliff/camera collision handling
- no Aster-specific authored yaw/coordinate assumptions
- 4v5 must remain readable
- player camera state restored after battle

## Battle UX contract

- 3D battlefield remains dominant.
- 3D model click is primary target input; keyboard/HUD are fallbacks.
- single-target skill never silently commits first target.
- current actor, target and next-order must be immediately readable.
- portrait-based Turn Order is preferred when final portrait quality is available.
- skill details use tooltip/detail state instead of permanent dense text.
- AUTO/speed/flee have lower visual priority than actions.

## Character contract

P01~P08 IDs may be retained for save continuity, but old kits/numbers are not automatically canonical.
The v1 character document defines concept direction.

- no deliberate fodder playable characters
- ★3~★5 all have a usable niche
- one readable signature mechanic per hero
- no duplicate requirement for core functionality
- models/animations/VFX are part of character completion

## Economy contract

Core currencies are kept small:
- Gold
- Summon Crystal
- Star Essence

No equipment gacha.
No real-money purchases.
No time-limited FOMO banners in the initial game.
Old v0.4 summon rates and ★1~2 filler pool are superseded.

## UI contract

Production UI must be based on verified external UI/game references and reusable assets, not improvised black rectangles.

- redesign typography/readability
- minimize truncation
- support GUI Scale changes
- shared portrait/icon system
- redesigned minimap/world map
- external assets tracked in `EXTERNAL_ASSETS.md`
- keep asset paths shallow and predictable

See `UI_DESIGN_SYSTEM.md`.

## World contract

Production base is separately installed Drehmal: APOTHEOSIS v2.2.2f.

- TURNBOUND: RE is not a current project dependency/canon.
- do not vendor original world/resource pack without confirmed permission.
- arbitrary saves fail closed.
- map survey precedes NPC/encounter/quest binding.
- do not flatten/build an Aster replacement.
- semantic anchors are promoted only after actual 26.2 terrain inspection.

See `WORLD_OVERHAUL_DREHMAL.md`.

## Cleanup contract

When replacement is complete:
- obsolete Aster builders/routers/maps are removed
- dead UI implementations are removed
- duplicate helpers are consolidated
- historical design delta docs are not accumulated
- compatibility/migration code remains only when an active save/network compatibility reason exists

Git history is the archive.

## Validation

Validation labels must remain distinct:
- CODE REVIEWED
- TESTED
- BUILD VERIFIED
- JAR PRODUCED
- PLAYTESTED
- MULTIPLAYER TESTED

Docs/visual-only changes do not trigger build merely to create a green badge.

Last verified code checkpoint before v1 planning rewrite:
- TESTED: YES — Gradle tests + NeoForge server smoke, Build TURNBOUND #754
- BUILD VERIFIED: YES — commit `47cd25027fe26ea27f1ce5688372ab7102f7da18`
- JAR PRODUCED: YES
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
