# Solo Leveling-style Minecraft RPG

- Slug: `solo-leveling-minecraft`
- Mod ID: `TBD`
- Namespace: `TBD`
- Mod version: `0.1.0-alpha.1` (planned)
- Minecraft: `TBD`
- Java: `TBD`
- Loader: `TBD` — choose per current ecosystem/project needs; do not assume NeoForge or Fabric by default
- Loader version: `TBD`
- Gradle: `TBD`
- Build plugin: `TBD`
- Final JAR: `TBD`
- Existing-world compatibility: `TBD`
- Required dependencies: `TBD`
- Optional external mods: `TBD`
- Forbidden bundled dependencies: copyrighted Solo Leveling assets without redistribution permission; Minecraft original distribution files
- Datagen task: `TBD`
- GameTest task: `TBD`
- Server smoke-test task: `TBD`
- Client smoke-test task: `TBD`

Status: **Planning**

This project adapts the core progression fantasy, System UI, gate/dungeon loop, skill growth, and shadow-summoning structure of Solo Leveling into Minecraft Java while preserving Minecraft survival where it does not conflict with the intended RPG experience.

## Canon / reference priority

1. Solo Leveling webtoon — primary gameplay, System, visual-language and progression reference.
2. Anime — motion, timing, combat animation and audiovisual reference when the webtoon cannot show motion.
3. Solo Leveling: Ragnarok — secondary monster/content pool where compatible.
4. Official games — optional implementation/UX references only; they are not the gameplay canon for this project.

When a source mechanic does not fit Minecraft literally, preserve its purpose and player experience rather than forcing a direct copy.

## Current direction

- Visual presentation should feel as close as practical to the Solo Leveling webtoon atmosphere and System UI.
- Social/hunter-association simulation is omitted; the System itself shows the player's rank.
- Player rank uses `E / D / C / B / A / S`.
- Gates naturally occur in the Overworld.
- Special gates occur probabilistically.
- A craftable gate detector is planned.
- Nether and End are not required for core progression.
- Vanilla survival remains the ordinary-world layer; the System and gates provide the RPG layer.
- Keep a **single real Minecraft player inventory** rather than separate vanilla/System inventories by default.
- Re-theme/redesign the inventory screen in the System visual language.
- Inventory capacity should grow through a mix of automatic milestone/level progression and extra System quest, special reward, or expansion-item rewards. Neither path should be the only source; exact pacing and caps are tuned later.
- Replace the vanilla heart HUD with a System-style HP bar. MP and fatigue are separate System resources.
- Keep hunger as an ordinary survival/life resource, but do not let food dominate RPG combat recovery. Combat healing should primarily come from System potions, skills, level-up recovery and other RPG mechanics; vanilla-style food spam/natural regeneration should be reduced or suppressed during active combat where needed.
- Keep Minecraft crafting meaningful for survival, construction, tools, gate-detector progression, and mana-crystal/essence-stone crafting or upgrades.
- Keep the System Store as a separate RPG economy using System Gold, not emeralds. It primarily supplies potions, consumables, selected System items/keys, and broadly useful baseline combat gear rather than replacing crafting or dungeon drops.
- Dungeon and boss drops remain the main source of rare or distinctive combat equipment; crafting/upgrading is used to improve or transform equipment with dungeon materials where appropriate.
- System Store stock can expand with Player progression instead of exposing the entire catalog from the start.
- Villager trade remains emerald-based and separate from System Gold.
- Minecraft death is not permadeath; content-specific failure rules are used for gates, System quests and the Penalty Zone.
- The Penalty Zone must never soft-lock a player.
- Daily quests use a separate System-time cadence rather than Minecraft's 20-minute day so they do not become constant chores.
- Enemy name color follows the source's relative-danger presentation: white / orange / red relative to the player's current strength.
- Skills can level and, where source-appropriate, evolve into stronger forms.
- Shadow Extraction, Shadow Storage, Shadow Exchange and Monarch's Domain are growing job skills, not static menu features.
- Shadow-army limits should stay close to the source's progression fantasy rather than small-party limits from mobile adaptations. Technical entity budgets must be solved without turning the army into a 3-5 summon party.
- Named shadows must not become permanently unobtainable because of bad extraction RNG. Named encounters may recur.
- Igris: in the job-change sequence, the first two extraction attempts fail and the third succeeds, preserving the source scene without forcing the entire job-change dungeon to be repeated.
- Mana crystals exist as dungeon resources and are candidates for crafting, upgrades, potions and gate-detector progression. Essence stones are candidates for rarer/higher-value recipes.
- Multi-weapon combat and late-game telekinetic weapon control through the Ruler's Hand / Ruler's Authority progression are intended if implementation quality is sufficient.
- Named dungeons/bosses may recur because this is a replayable game rather than a one-time narrative world.
- Monster-source priority: Solo Leveling webtoon → compatible Ragnarok additions → Minecraft dungeon variants → mythic/classic fantasy creatures where needed.

## Prologue

If feasible at sufficient quality, the first Double Dungeon should be implemented as a short playable/cinematic prologue that leads to System acquisition. It should not rely on normal Minecraft death for its story outcome.

If a full recreation cannot meet the quality bar, a shorter directed opening is preferable to a low-quality recreation.

The later return to the Double Dungeon remains important because the contrast between the helpless first visit and the powerful return is part of the intended progression fantasy.

## Gate / dungeon direction

- Natural gates exist in the Overworld.
- Gate rank is E-S.
- Special gates can replace/transform normal expectations with a low probability.
- Dungeon Break is retained, but undiscovered distant gates must not create unavoidable worldwide maintenance.
- Gate activation/proximity/chunk rules should prevent every natural gate from eventually breaking while nobody could reasonably respond.
- Natural gates and System dungeons should share underlying dungeon infrastructure where practical; their important differences are access rules and narrative/System context.
- Mana-crystal veins can appear inside suitable dungeons and provide a Minecraft-native reason to mine inside gates.

## Death / failure direction

- No permadeath.
- Normal progression, skills and persistent inventory are retained across death unless a specific content rule says otherwise.
- Dungeon death may fail/exit the current run and discard only unconfirmed run rewards.
- Personal System spaces may override normal Minecraft respawn behavior.
- Penalty Zone death must not allow escape and must not cause an endless death loop; the design must guarantee eventual release.

## Multiplayer principles

- Player progression state is per-player and server authoritative.
- World gates and world-side Dungeon Break state are shared.
- Ordinary gates can be entered cooperatively.
- Personal System tests such as the Penalty Zone and job-change trials remain individually instanced unless a later design explicitly requires otherwise.
- Loot and named-shadow acquisition must avoid multiplayer griefing. Per-player reward/extraction state is preferred to winner-takes-all ownership.
- The architecture must support multiplayer from the beginning even if a particular milestone is only playtested in singleplayer.

## Open design questions

Work through these slowly; do not lock them all at once.

1. Exact pacing and final cap for inventory-capacity expansion.
2. Vanilla gear vs System gear and equipment slots.
3. Player EXP vs vanilla experience.
4. Exact E-S rank thresholds and whether rank is level-derived or a broader combat rating.
5. Gate spawn frequency, spacing, persistence and activation.
6. Daily quest cadence and compressed exercise/run implementation.
7. Skill acquisition, proficiency, level-up and evolution rules.
8. Shadow extraction probability, retry/pity rules, storage, resurrection/recovery, rank growth and commands.
9. Dungeon Break timing and world-safety constraints.
10. Multiplayer ownership, rewards, party EXP and duplicate named shadows.
11. Mana-crystal / essence-stone economy and crafting sinks.
12. Dungeon roster coverage across E-S ranks.
13. Loader/version/dependency selection after current 26.x ecosystem verification.
