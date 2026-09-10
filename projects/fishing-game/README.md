# Fishing Game

A Minecraft Java 26.2 Fabric standalone fishing progression game focused on a compact loop:

`catch -> collect -> sell -> improve rod -> reach better water -> hunt bigger/rarer fish`

The project deliberately avoids survival chores, stacked currencies and unrelated systems. Minecraft supplies the world/rendering/runtime; the player experience is a dedicated fishing game.

## Current alpha.4 slice

- Adventure-mode fishing-only rules: no survival damage, hunger management, mining/crafting loop or survival HUD.
- Dedicated `fishinggame:lakeside` location instead of ordinary Overworld roaming.
- Authored lakeside ring with a central fishable lake, three piers, pavilion, tackle shelter, paths, trees and shoreline dressing.
- Server-authoritative fishing session, tension/progress and catch result.
- A real fish entity becomes visible underwater before the bite, approaches the bobber, then moves around the hook during the fight. Vanilla fish are visual proxies for now; final species models remain future visual work.
- Persistent catch bag, coins, selling and three-tier rod progression.
- Dedicated fishing HUD and catch-bag screen using bundled Kenney CC0 UI assets.
- Reeling now exposes the real safe-tension band and gives live reel/release/maintain guidance instead of making the player guess the valid range.
- Successful catches get a compact result card showing species, rarity, weight, length and value.
- Essential remains optional and does not own game state.

## Fishing interaction

1. Spawn into Cheongram Lakeside with a fishing rod.
2. Cast from one of the lake piers.
3. The bite timer starts only when the bobber reaches valid water.
4. Watch for a fish moving toward the bobber before the bite.
5. When hooked, hold right click to raise tension and release to let it fall.
6. Keep the tension marker inside the visible green safe band; the HUD tells you when to reel, release or maintain rhythm.
7. The caught fish enters the separate catch bag rather than the vanilla inventory and gets an immediate catch-result card.
8. Open the bag with `B`, sell catches, then buy the next rod tier.

The server owns tension, progress, selected species, catch size/value, coins and upgrades. Client UI only presents and requests actions.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1
- Essential: optional external convenience mod, not a dependency

## Quality gate

A successful compile is not a playtest-ready declaration. Before handing the user a JAR, the dedicated location, fish visibility/motion, fishing HUD and complete catch -> sell -> upgrade loop still need actual Minecraft screen/play review. Tiny technical-only builds are not user test milestones.

Repository root `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`, this project's `PROJECT.md`, and `THIRD_PARTY_ASSETS.md` are the working references.
