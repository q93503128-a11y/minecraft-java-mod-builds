# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.9 slice

- Adventure-mode fishing-only rules: no survival damage, hunger chores, mining/crafting loop or survival HUD.
- Three dedicated fishing locations are now connected to progression:
  - Cheongram Lakeside — starting freshwater location.
  - Gull Harbor — unlocks with `호수 전문가` rod tier.
  - Deepwater Channel — unlocks with `블루워터` rod tier.
- `M` opens a Kenney-based travel screen. The client requests travel; the server validates destination and required rod tier.
- Coast and deep-sea species already present in the catalog are now reachable in their intended locations.
- Harbor environment includes a stone quay, three fishing piers, shelter, lighthouse, lamps and dock props.
- Deep-sea environment uses an offshore dark deck, long fishing arms, observation structure and sea-lantern lighting.
- Server-authoritative fishing session, species, tension/progress, pull bursts, catch value, coins and upgrades.
- Vanilla hook remains cast/line/bobber only in every dedicated fishing dimension; its independent bite cycle is suppressed.
- Five transient encounter-fish silhouettes: small, tall, fat, long and angler, with species-specific textures.
- Persistent catch bag, selling and three-tier rod progression.
- Dedicated HUD, catch bag and travel UI reuse bundled Kenney CC0 assets.
- Essential remains optional and owns no game state.

## Controls

- Fishing rod right click: cast / reel input.
- `B`: catch bag, selling and rod upgrade.
- `M`: fishing-location travel.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1

## Quality gate

Compile/build success is not a playtest-ready declaration. The new harbor and deep-sea spaces, fish proportions and complete screen composition still need actual Minecraft graphical review before a JAR is handed to the user as a meaningful playtest milestone.
