# Fishing Game

## Identity

Minecraft version: 26.2
Mod loader: Fabric
Loader/API baseline: Fabric Loader >=0.19.3, Fabric API >=0.159.0+26.2
Java: 25
Multiplayer: ordinary server-authoritative Fabric logic; Essential is an optional connection/hosting convenience only.

## Game statement

Fishing Game is a standalone fishing progression game built on Minecraft, not a survival expansion.

Core loop:

> cast -> hook -> reel -> catch -> bag/collection -> sell -> improve rod -> reach better fishing locations -> catch rarer/larger fish

## Non-survival rule

The player is not expected to mine, craft, fight, manage hunger, or survive nights. Player damage is disabled, hunger/health are stabilized, Adventure mode is enforced, and survival HUD layers are removed. Minecraft is the runtime/world renderer for a dedicated fishing game.

## UI / HUD rule

Do not invent the visual language ad hoc. The first UI system uses Kenney CC0 UI assets, with information architecture informed by proven fishing games such as Fish It / Fisch. External assets and their licenses are tracked in THIRD_PARTY_ASSETS.md.

## World direction

Use dedicated fishing locations/maps rather than ordinary survival roaming. Prefer existing high-quality fishing maps or environment assets when their redistribution/use terms are explicit. Unknown-license maps may be used only as references or local editing bases until permission is clear.

Location architecture starts with:
- Lakeside / freshwater
- Coast / harbor
- Deep Sea

## User-test gate

Do not hand the user another JAR for a tiny technical check. A user-facing test build must have, at minimum:
- dedicated non-survival HUD
- catch bag UI
- persistent catches and coins
- selling
- meaningful rod progression
- visible/credible fish presentation during the catch
- at least one dedicated fishing location with acceptable visual quality
- complete cast -> catch -> sell -> upgrade loop

Build success alone does not satisfy this gate.
