# Fishing Game

Minecraft Java 26.2 Fabric standalone fishing progression game:

`charge cast -> choose water -> catch -> collect -> sell -> improve rod -> unlock better water -> hunt bigger/rarer fish -> rebirth -> accelerate the next cycle`

Minecraft supplies the runtime/world renderer; the player experience is a dedicated fishing game rather than survival.

## Current alpha.21 slice

- Adventure-mode fishing-only rules: no survival damage, hunger chores, mining/crafting loop or survival status HUD.
- The vanilla hotbar and held-item tooltip remain visible because Fishing Game still uses real Minecraft inventory slots; health/food/armor/air/XP layers stay hidden.
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
- HUD, bag, bestiary, travel and catch-result presentation share the bundled Kenney CC0 UI language. Alpha.21 uses the actual outer 4px frame of the 100x100 panel texture and clamps all major screens to the current Minecraft logical GUI size.
- Preferred logical UI sizes are intentionally compact: HUD 174x62, bag 340x220, bestiary 348x220 and travel 320x206, with additional clamping on smaller GUI sizes.
- Cheongram Lakeside remains an authored inland lake. Alpha.21 adds a one-time water cleanup pass that removes stray natural terrain blocks floating in/above the lake while preserving the wooden fishing structures.
- Initial dedicated-dimension placement is deferred out of the connection JOIN callback and valid players already saved in Lakeside are not redundantly teleported, reducing chunk-tracking lifecycle risk during join/exit.
- Essential remains optional and owns no game state.

## Controls

- Fishing rod right click: hold to charge, release to cast.
- During a hooked fight: hold/release right click to manage line tension.
- `B`: catch bag, selling, rod upgrade and rebirth.
- `J`: fish bestiary / records / hotspot hints.
- `M`: fishing-location travel.
- Vanilla number keys / mouse wheel: normal hotbar selection remains available.

## Technical stack

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.159.0+26.2
- Loom 1.17.19
- Gradle 9.5.1

## Quality gate

Alpha.20 passed automated build/server checks but failed the next real graphical review: major screens were still oversized at the user's GUI scale, the hotbar removal was not coherent with the retained inventory, stray terrain blocks remained in the lake, and integrated-server shutdown produced a player/chunk-tracking NPE. Alpha.21 is the corrective slice. Automated CI can verify code/build/server startup, but actual screen scale, lake cleanup and the shutdown error must be re-tested in a real Minecraft client before PLAYTESTED, GRAPHICAL CLIENT REVIEWED or shutdown-regression-verified status is claimed.
