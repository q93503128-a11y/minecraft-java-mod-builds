# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`charge cast -> choose water -> catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish -> rebirth -> accelerate the next cycle`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.19 slice

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
- Hooked fish use five species fight styles: 꾸준한 힘싸움 / 연속 질주 / 깊은 잠수 / 묵직한 버팀 / 불규칙 난동.
- Fight style changes server-side burst cadence/force and the encounter fish's visible lateral/orbit/dive motion while keeping the same hold/release reel control.
- Server-authoritative fishing session, hotspot resolution, fish selection, tension/progress, catch value, coins, upgrades, rebirth and permanent collection records.
- Five transient encounter-fish silhouettes with species-specific textures and visible approach/fight motion.
- Persistent catch bag, selling and three-tier rod progression.
- Rod progression changes the actual held-rod presentation: 갈대 낚싯대 / 호수 전문가 / 블루워터 use adapted Fishing Frenzy MIT rod art, distinct material palettes and cast states; Bluewater adds an animated glint layer.
- The vanilla fishing rod remains the gameplay carrier, so custom visuals do not replace the proven bobber/line transport or server-owned fishing session.
- Permanent bestiary keeps species discovery, catch count and personal-best weight/length after selling and after rebirth.
- First discovery and location completion pay one-time rewards into the existing coin economy; repeat catches and later rebirth cycles cannot duplicate those rewards.
- Rebirth unlocks after reaching Bluewater, emptying the bag and meeting the current coin target. The first target is 10,000 C; later targets rise by 1,500 C.
- Rebirth resets coins, catch bag and rod tier and returns the player to Cheongram Lakeside, while permanently adding +30% ordinary fish sale income per rebirth.
- Post-rebirth rods keep a visible glint and rebirth count even after the tier resets, so permanent progression is not hidden behind a number-only bonus.
- Rebirth reuses the existing `B` progression panel; no second currency or extra management screen is introduced.
- HUD, bag, bestiary, travel and catch-result presentation share the bundled Kenney CC0 UI language.
- Essential remains optional and owns no game state.

## Controls

- Fishing rod right click: hold to charge, release to cast.
- During a hooked fight: hold/release right click to manage line tension.
- `B`: catch bag, selling, rod upgrade and rebirth.
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

Alpha.19 is BUILD VERIFIED and dedicated-server smoke verified by CI, but that is not a playtest declaration. Rebirth pacing, the readability of the rebirth state in the bag panel, post-rebirth rod glint/name readability, hotspot readability, fight-style feel, fish motion, sound balance and overall screen composition still require an actual Minecraft graphical/play review before PLAYTESTED or GRAPHICAL CLIENT REVIEWED is claimed.
