# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`charge cast -> choose water -> catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.17 slice

- Adventure-mode fishing-only rules: no survival damage, hunger chores, mining/crafting loop or survival HUD.
- Hold/release right click for charge casting; cast charge changes distance only, never rarity or catch odds.
- Three dedicated progression locations: Cheongram Lakeside -> Gull Harbor -> Deepwater Channel.
- `M` travel is validated server-side against rod tier; active fishing blocks travel.
- Each location contains three server-resolved fishing hotspots. The bobber's actual water position determines the hotspot.
- Hotspots bias compatible species instead of hard-gating the pool: every species in the location remains catchable from every valid spot, but targeted hunting becomes meaningfully faster from the recommended water.
- Cheongram Lakeside: 서쪽 얕은 물 / 깊은 물골 / 바위 그늘.
- Gull Harbor: 방파제 안쪽 / 항로 중앙 / 외해 끝부두.
- Deepwater Channel: 유도등 수역 / 심해 골 / 고대 해구.
- Landing the bobber reports the hotspot and a short ecological hint; undiscovered bestiary entries show only a recommended hotspot while keeping the species name hidden.
- Hooked fish now use five species fight styles: 꾸준한 힘싸움 / 연속 질주 / 깊은 잠수 / 묵직한 버팀 / 불규칙 난동.
- Fight style changes server-side burst cadence/force and the encounter fish's visible lateral/orbit/dive motion while keeping the same hold/release reel control.
- Server-authoritative fishing session, hotspot resolution, fish selection, tension/progress, catch value, coins, upgrades and permanent collection records.
- Five transient encounter-fish silhouettes with species-specific textures and visible approach/fight motion.
- Persistent catch bag, selling and three-tier rod progression.
- Permanent bestiary keeps species discovery, catch count and personal-best weight/length after selling.
- First discovery and location completion pay one-time rewards into the existing coin economy; repeat catches cannot duplicate those rewards.
- HUD, bag, bestiary, travel and catch-result presentation share the bundled Kenney CC0 UI language.
- Essential remains optional and owns no game state.

## Controls

- Fishing rod right click: hold to charge, release to cast.
- During a hooked fight: hold/release right click to manage line tension.
- `B`: catch bag, selling and rod upgrade.
- `J`: fish bestiary / records / hotspot hints.
- `M`: fishing-location travel.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1

## Quality gate

Compile/build success is not a playtest-ready declaration. Alpha.17 gives species distinct fight behavior without adding another management system, but hotspot readability, fight-style feel, fish motion, sound balance and screen composition still need actual Minecraft graphical/play review before a JAR is handed to the user as a meaningful playtest milestone.
