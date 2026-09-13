# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`charge cast -> catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.14 slice

- Adventure-mode fishing-only rules: no survival damage, hunger chores, mining/crafting loop or survival HUD.
- Right click charges the cast before release. Charge changes throw distance only; it does not improve rarity or catch odds.
- A compact bottom-center cast meter shows the real charge state and caps after 18 ticks, with a single full-charge audio confirmation.
- Cast duration is validated from server game time; the client only requests release/cancel and renders local feedback.
- Three dedicated fishing locations are connected to progression:
  - Cheongram Lakeside — starting freshwater location.
  - Gull Harbor — unlocks with `호수 전문가` rod tier.
  - Deepwater Channel — unlocks with `블루워터` rod tier.
- `M` opens the fishing-location travel screen; destination and required rod tier are validated server-side.
- Gull Harbor has an arrival promenade, layered shoreline rocks, breakwater arms, entrance beacons, three fishing stations and a stronger lighthouse silhouette.
- Deepwater Channel reads as an offshore fishing facility with three outward fishing pods, hazard guides, rail-protected circulation, submerged guide lights and a signal mast.
- Server-authoritative fishing session, species, tension/progress, pull bursts, catch value, coins, upgrades and collection records.
- Vanilla hook remains line/bobber transport only in every dedicated fishing dimension; its independent bite cycle is suppressed.
- Five transient encounter-fish silhouettes: small, tall, fat, long and angler, with species-specific textures.
- Persistent catch bag, selling and three-tier rod progression.
- Permanent bestiary tracks discovered species, catch counts and personal-best weight/length even after selling.
- Catch results distinguish first discoveries and new personal bests and grade fish as `일반`, `대형`, `트로피` or `괴물급` from species-specific size ranges.
- HUD, catch bag, bestiary, travel and catch-result presentation now share one Kenney-based UI theme for panel language, hierarchy, colors, separators and disabled states.
- The always-on HUD is widened to keep coin, bag, rod, location and B/J/M navigation readable without text overflow.
- Trophy/rare catches, first discoveries, personal records, selling and rod upgrades have distinct lightweight reward cues; ordinary bite/catch sounds remain unchanged.
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

Compile/build success is not a playtest-ready declaration. Alpha.14 closes the first shared UI/audio-feedback pass, but the three authored locations, cast distance, fish motion, sound balance and screen composition still need actual Minecraft graphical/play review before a JAR is handed to the user as a meaningful playtest milestone.
