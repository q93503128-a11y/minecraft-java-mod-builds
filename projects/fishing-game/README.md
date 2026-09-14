# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`charge cast -> catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.15 slice

- Adventure-mode fishing-only rules: no survival damage, hunger chores, mining/crafting loop or survival HUD.
- Hold/release right click for charge casting; cast charge changes distance only, never rarity or catch odds.
- Three dedicated progression locations: Cheongram Lakeside -> Gull Harbor -> Deepwater Channel.
- `M` travel is validated server-side against rod tier; active fishing blocks travel.
- Server-authoritative fishing session, fish selection, tension/progress, catch value, coins, upgrades and permanent collection records.
- Five transient encounter-fish silhouettes with species-specific textures and visible approach/fight motion.
- Persistent catch bag, selling and three-tier rod progression.
- Permanent bestiary keeps species discovery, catch count and personal-best weight/length after selling.
- First discovery now awards one-time coins from the existing economy: Common 12 C, Uncommon 20 C, Rare 35 C, Epic 65 C, Legendary 120 C.
- Completing a location collection pays one one-time bonus: Lakeside 180 C, Harbor 300 C, Deep Sea 500 C.
- Re-catching known fish cannot repeat discovery/completion rewards; legacy records do not retroactively print coins.
- HUD shows current-location collection progress; bestiary and travel screens expose remaining collection goals without revealing undiscovered species names.
- Catch-result feedback can call out discovery reward, location completion, personal records and trophy/monster size.
- HUD, bag, bestiary, travel and catch-result presentation share the bundled Kenney CC0 UI language.
- Essential remains optional and owns no game state.

## Controls

- Fishing rod right click: hold to charge, release to cast.
- During a hooked fight: hold/release right click to manage line tension.
- `B`: catch bag, selling and rod upgrade.
- `J`: fish bestiary / records / collection progress.
- `M`: fishing-location travel.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1

## Quality gate

Compile/build success is not a playtest-ready declaration. Alpha.15 closes the first collection-motivation loop, but the authored locations, fish motion, cast/reel feel, sound balance and screen composition still need actual Minecraft graphical/play review before a JAR is handed to the user as a meaningful playtest milestone.
