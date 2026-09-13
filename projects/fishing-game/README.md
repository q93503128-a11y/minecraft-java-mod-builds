# Fishing Game

A Minecraft Java 26.2 Fabric standalone fishing progression game focused on:

`catch -> collect -> sell -> improve rod -> reach better water -> hunt bigger/rarer fish`

The project deliberately avoids survival chores, stacked currencies and unrelated systems. Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game.

## Current alpha.7 slice

- Adventure-mode fishing-only rules: no survival damage, hunger management, mining/crafting loop or survival HUD.
- Dedicated `fishinggame:lakeside` Cheongram Lakeside instead of ordinary Overworld roaming.
- Server-authoritative fishing session, tension/progress, fish pull bursts and catch results.
- Vanilla hook remains cast/line/bobber only; its separate lure/nibble/bite cycle is suppressed in the dedicated lake.
- Visible fish curve toward the bobber, burst against the line and converge as catch progress rises.
- Vanilla cod/salmon/tropical-fish encounter proxies are replaced by dedicated transient small/fat/long fish body families.
- Species id is synchronized on the encounter entity and selects a species-specific project texture.
- Encounter fish are no-AI, unsaved, short-lived visual entities, not permanent ambient populations.
- Persistent catch bag, coins, selling and three-tier rod progression.
- Dedicated HUD and catch-bag screen using bundled Kenney CC0 UI assets.
- Successful catches get a compact result card with species, rarity, weight, length and value.
- Essential remains optional and does not own game state.

## Fishing interaction

1. Spawn into Cheongram Lakeside with a fishing rod.
2. Cast from one of the lake piers.
3. The custom bite timer starts only when the bobber reaches valid water.
4. Watch the species-specific fish approach the bobber.
5. When hooked, hold right click to raise tension and release to let it fall.
6. Keep the marker inside the visible safe band while reacting to fish surges.
7. The catch enters the separate catch bag and gets an immediate result card.
8. Open the bag with `B`, sell catches, then buy the next rod tier.

The server owns bite timing, species, tension, burst impulses, progress, catch size/value, coins and upgrades. Client state presents those results and sends input requests.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1
- Essential: optional external convenience mod, not a dependency

## Visual-source policy

The encounter fish small/fat/long geometry is adapted from Sea Life under MIT; the full notice is bundled under `META-INF/licenses/fishinggame/`. UV-compatible largemouth/carp/catfish/perch/tuna textures reuse Sea Life assets under MIT; the remaining alpha.7 species textures are project-authored. See `THIRD_PARTY_ASSETS.md`.

## Quality gate

A successful compile is not a playtest-ready declaration. Before handing the user a test JAR, Cheongram Lakeside, fish proportions/textures, fishing HUD composition and the complete catch -> sell -> upgrade loop still need actual Minecraft client review.
