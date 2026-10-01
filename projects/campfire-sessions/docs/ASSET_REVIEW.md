# Campfire Sessions — External Asset Review Run

This is an internal development workflow for visually accepting third-party dependencies before Campfire treats them as production art.

It is **not** a player-facing game mode and is disabled in the normal client/server run.

## Start

From `projects/campfire-sessions/`:

```bash
./gradlew runAssetReviewClient
```

The run uses its own `run-asset-review/` game directory so review worlds/config do not contaminate the normal development save.

Create a disposable Creative test world with commands enabled.

## Review kits

The command exists only in this asset-review JVM profile:

```text
/campfire_review
/campfire_review furniture
/campfire_review kitchen
/campfire_review crops
/campfire_review mushrooms
/campfire_review storage
/campfire_review boats
```

Each kit reads the **actual runtime item registry**. It does not duplicate or repackage third-party assets.

Current content namespaces intentionally inspected:

- `skniro_furniture`
- `peterwolfs_boats_and_ships`
- `croptopia`
- `cookingforblockheads`
- `shroomcraft`
- `sophisticatedbackpacks`
- `travelertoolbelt`

Furniture and kitchen kits collapse obvious wood/color variants before selecting the first 27 items, so the first pass prioritizes different silhouettes/functions instead of showing many recolors of the same bed/cabinet.

## What to record

For each candidate family, inspect:

- visual cohesion with the Kogtyv Greece village language and Campfire's cozy direction;
- world scale beside player, doors, houses and paths;
- item/held-model readability;
- placement footprint and rotation;
- collision/path obstruction;
- seating/interaction anchors where relevant;
- animation and interaction feel;
- UI quality where the dependency opens its own screen;
- duplicated functionality against already selected dependencies;
- obvious performance or rendering issues.

For boats also inspect docking footprint, mounting/dismounting and passenger positions.

For crops/mushrooms inspect authored-island worldgen behavior separately; a visually good asset still fails if uncontrolled generation damages the canonical map.

For backpacks/tool belts inspect the actual screen/radial UX and whether it can be wrapped into Campfire without exposing a conflicting technical visual language.

## Acceptance boundary

A successful build or server launch does **not** count as client visual acceptance.

Record states separately:

- BUILD VERIFIED
- SERVER RUNTIME VERIFIED
- CLIENT VISUAL TESTED
- GAMEPLAY TESTED
- MULTIPLAYER TESTED

Only promote a dependency from trial to adopted after the relevant visual/gameplay checks pass.
