# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`charge cast -> catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.13 slice

- Adventure-mode fishing-only rules: no survival damage, hunger chores, mining/crafting loop or survival HUD.
- Right click now charges the cast before release instead of throwing at full vanilla speed immediately. Charge changes throw distance only; it does not improve rarity or catch odds.
- A compact bottom-center cast meter shows the real charge state and caps after 18 ticks, so holding longer gives no hidden advantage.
- Cast duration is validated from server game time; the client only requests release/cancel and renders the local meter.
- Three dedicated fishing locations are connected to progression:
  - Cheongram Lakeside — starting freshwater location.
  - Gull Harbor — unlocks with `호수 전문가` rod tier.
  - Deepwater Channel — unlocks with `블루워터` rod tier.
- `M` opens a Kenney-based travel screen; destination and required rod tier are validated server-side.
- Gull Harbor has an arrival promenade, layered shoreline rocks, breakwater arms, entrance beacons, three fishing stations and a stronger lighthouse silhouette.
- Deepwater Channel reads as an offshore fishing facility with three outward fishing pods, hazard guides, rail-protected circulation, submerged guide lights and a signal mast.
- Server-authoritative fishing session, species, tension/progress, pull bursts, catch value, coins, upgrades and collection records.
- Vanilla hook remains line/bobber transport only in every dedicated fishing dimension; its independent bite cycle is suppressed.
- Five transient encounter-fish silhouettes: small, tall, fat, long and angler, with species-specific textures.
- Persistent catch bag, selling and three-tier rod progression.
- Permanent bestiary tracks discovered species, catch counts and personal-best weight/length even after selling.
- Catch results distinguish first discoveries and new personal bests and grade fish as `일반`, `대형`, `트로피` or `괴물급` from species-specific size ranges.
- Dedicated HUD, catch bag, bestiary and travel UI reuse bundled Kenney CC0 assets.
- Essential remains optional and owns no game state.

## Controls

- Fishing rod right click: hold to charge, release to cast.
- During a hooked fight: hold/release right click to manage line tension.
- `B`: catch bag, selling and rod upgrade.
- `J`: fish bestiary / records.
- `M`: fishing-location travel.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1

## Quality gate

Compile/build success is not a playtest-ready declaration. Alpha.13 closes the missing cast-feel step in the core loop, but the three authored locations, cast distance, fish motion and screen composition still need actual Minecraft graphical/play review before a JAR is handed to the user as a meaningful playtest milestone.
