# Design Notes

Use this file for confirmed design decisions that are too detailed for the project summary. Keep uncertain ideas in PROJECT.md under Open design questions until agreed.

## Inventory

- Keep one real Minecraft player inventory rather than maintaining separate vanilla and System inventories by default.
- Replace/re-theme the inventory screen into the Solo Leveling System visual language.
- If capacity becomes a gameplay problem, prefer System-driven slot/capacity expansion over a second parallel inventory.
- Inventory capacity may grow through both automatic level/milestone progression and extra System quest, special reward, or expansion-item rewards.
- Preserve normal save persistence across death unless a specific content rule explicitly overrides pending dungeon rewards.

## Death / failure

- No permadeath.
- Penalty Zone death must not allow escape and must not cause a permanent loop. The zone should preserve remaining penalty time or otherwise guarantee eventual release.
- Personal System spaces should control their own failure/respawn behavior.

## Named shadow extraction

- Power disparity may reduce extraction success as in the source.
- Bad RNG must not make a named shadow permanently unobtainable.
- Named encounters can recur.
- Igris in the job-change sequence: first two extraction attempts fail, third attempt succeeds for source-faithful presentation without forcing a repeat of the entire job-change dungeon.

## Experience

- Player EXP and vanilla Minecraft experience are separate systems.
- Player EXP drives System level progression and comes primarily from relevant combat, dungeon clears, bosses, and System quests.
- Vanilla XP remains for vanilla mechanics such as enchanting and anvils; it does not increase Player Level.
- Enemies far below the player's effective strength should grant sharply reduced or negligible Player EXP so trivial farming does not dominate progression.
- Vanilla enchanting may remain useful early, but it is not required to remain competitive with late-game System/dungeon equipment.

## Player rank

- Player rank uses `E / D / C / B / A / S` and is shown directly by the System; there is no social re-evaluation mechanic.
- Rank is an automatic System judgment of persistent personal growth, not a direct Player-level band and not a live gear-score readout.
- Primary inputs may include Player level, permanent/base and allocated stats, skill growth, job progression, and later Monarch progression.
- Temporary buffs, potions, party buffs, and ordinary equipment changes should not make rank fluctuate.
- `S` remains the highest letter rank. Do not add `S+`, `SS`, or `SSS` just to express post-S growth; later gaps are expressed through level, stats, skills, job/Monarch progression, and actual combat performance.
- Player rank and Gate rank share the E-S language for readability, but equal letters do not guarantee equal solo difficulty.

## Gate generation

- Natural Gates can spawn from `E` through `S` from the beginning of a world; they are not scaled to the current Player level.
- Lower-rank Gates are common, while `A` and especially `S` Gates are progressively rarer.
- Special Gate behavior is a low-probability variation rather than a separate always-available category.
- A newly generated Gate should remain dormant/stable until a player reasonably discovers or activates it through proximity, loading, or a Gate detector.
- Dungeon Break timing should not advance for undiscovered distant Gates that players could not reasonably know about.
- Gate density must be capped and spatially separated so the world does not become saturated; multiplayer may scale the active/world cap modestly with player count.
- Gate detectors are intended to make locating and identifying Gates part of Minecraft exploration rather than exposing every Gate globally by default.

## Gate appearance and measurement

- Ordinary Gate rank is not encoded by a fixed E-S color palette. Normal Gates use the source-like blue/cyan dimensional-rift language.
- Red is reserved for the special Red Gate state/type rather than being a normal rank color.
- Gate size may trend larger at higher rank, but size is not the authoritative rank measurement and cannot be used as a reliable formula.
- Gate rank is determined through mana output/System measurement. Small but dangerous Gates and large but lower-than-expected Gates remain possible.
- The detector/measuring system exists partly because visual size/color alone must not reveal exact rank.

## Daily Quest

- Preserve the source-facing routine: 100 push-ups, 100 sit-ups, 100 squats, and a displayed 10 km run target.
- Daily Quest cadence uses separate active-play System time rather than Minecraft's 20-minute day. Offline time does not progress the cadence.
- The exact System-day length is tuned later around roughly one meaningful play session, not every Minecraft day.
- Exercise actions should use dedicated full-body player animations if the selected Minecraft version/loader has a sufficiently stable player-animation path.
- As of Minecraft 26.2, Player Animation Library supports both Fabric and NeoForge and can load Blockbench/GeckoLib-style animation data, so exercise animation is considered technically feasible rather than a fallback-only idea.
- Do not force the player to sit through literal real-time 100-repetition animations. Keep the displayed source counts while compressing physical execution through repeated animation batches and progression-aware animation speed.
- The run target should be converted from Minecraft travel distance into System distance so the routine remains recognizable without requiring a literal 10,000-block run.
- The routine should become faster/easier as the Player grows rather than increasing to ever-larger repetition targets.
- Daily Quest completion and counters are server-authoritative; client animation is presentation and cannot grant progress by itself.

## Skill acquisition and growth

- Avoid a generic large skill tree as the default acquisition model.
- Skills may be acquired through System/job/quest rewards, Rune Stones, or source-appropriate progression events.
- Skills that canonically improve through use/proficiency may level with meaningful usage, but not every skill needs an identical grind meter.
- Some skills evolve into stronger forms at appropriate proficiency/progression milestones rather than remaining static forever.
- Job skills such as Shadow Extraction, Shadow Storage, Shadow Exchange, and Monarch's Domain may share job/progression growth where that better matches the source.
- Examples such as mobility, dagger throwing, and Ruler's Hand / Ruler's Authority should preserve source-style progression instead of being flattened into unrelated menu unlocks.
- Server authority decides skill acquisition, level/proficiency changes, cooldowns, resource costs, and success; animation/VFX remain presentation layers.

## Shadow army behavior

- Shadow extraction itself does not consume MP by default; stronger targets and time since death can affect success chance.
- Generic extraction attempts are limited per corpse rather than infinitely spammable. Named shadows use special anti-frustration handling where needed; Igris follows the fixed three-attempt sequence already defined.
- Shadow storage, ordinary summon, and recall do not consume MP by default.
- A shadow destroyed in combat is not permanently lost. Re-forming/reviving destroyed shadows consumes MP; if MP is insufficient, that shadow returns to storage/inactive state until it can be summoned again.
- Shadow soldiers gain meaningful combat growth and may rise in rank where appropriate. Named shadows preserve distinct roles and abilities instead of becoming generic stat blocks.
- Early shadow capacity is meaningfully limited, then expands through progression into dozens, hundreds, and eventually army-scale numbers. Exact numeric caps are a later balance task.
- Performance solutions must preserve the visible army fantasy through controller AI, grouping, LOD, aggregation, and selective simulation rather than imposing a tiny party cap.

## Dungeon Break

- A Gate that has not been reasonably discovered/activated does not advance toward Dungeon Break.
- Once activated, an uncleared Gate eventually breaks if neglected long enough; exact timing is tuned by rank and play flow later.
- Dungeon Break releases surviving dungeon monsters and eventually the boss into the Overworld.
- Break timers do not progress while the relevant server/world is offline. Players should not return to unavoidable destruction caused solely by offline time.
- Dungeon Break monsters are strongly constrained from terrain/build destruction. Combat can feel destructive without allowing ordinary raids to erase player builds.
- Higher-rank Breaks are combat disasters, not griefing systems.

## Multiplayer rewards and shadow ownership

- Normal Gates are shared cooperative spaces, but important progression rewards should avoid winner-takes-all ownership.
- Player EXP, quest completion, and most dungeon/boss reward rolls are resolved per eligible player or contribution state.
- Mana-crystal veins remain physical world resources and can therefore be shared/contested unless a later mining rule says otherwise.
- Named-shadow extraction is personal to each eligible Player. Multiple Players may each own their own copy of the same named shadow in multiplayer.
- One Player may not stack duplicate copies of the same unique named shadow simply by repeatedly farming that boss.
- All authoritative reward, extraction, and ownership state is server-side.

## Mana crystals and essence stones

- Mana Crystals are dungeon-mined materials, not a currency replacement. They are relatively common dungeon resources used for repeatable crafting/upgrading sinks such as ordinary System equipment improvement, potions, detector progression, and other scalable recipes.
- Essence Stones come primarily from magic beasts, elites, and bosses. Higher-grade monsters can yield better stones or higher-value variants.
- Essence Stones are reserved more heavily for rare equipment, higher-tier upgrades, special effects, and advanced System crafting.
- System Gold remains the purchase currency; Mana Crystals and Essence Stones remain physical crafting materials.
- Equipment upgrading should generally avoid destructive failure that deletes the item. Resource cost, progression requirements, and recipe rarity are preferred over gear-destruction RNG.
- Suitable dungeons may include optional mining side rooms/veins so players choose between rushing combat and spending time gathering resources.

## Dungeon roster structure

- Do not permanently bind an entire species to exactly one Gate rank. Species can have stronger variants, elites, champions, commanders, and boss forms.
- Canon/source monsters have first priority, followed by compatible Ragnarok additions, redesigned Minecraft dungeon variants, then classic myth/fantasy creatures if coverage is still insufficient.
- Each dungeon should have a coherent faction/ecology/theme rather than mixing random unrelated mobs solely to fill a rank table.
- Gate rank describes the dungeon's overall danger, not a guarantee that every individual monster inside has that same exact rank.
- Named/source bosses are verified individually before final rank assignment because secondary sources and adaptations may disagree.

## Gate detection and exploration

- Gate detection is a craftable exploration system that replaces the omitted Hunter Association measurement layer.
- A basic detector gives nearby Gate presence, approximate direction, and signal strength without exposing exact coordinates or guaranteed exact rank.
- An advanced detector may provide direction, distance, measured Gate rank, and stability/activation information while still avoiding a global all-Gates world map.
- Detector progression should connect vanilla materials/redstone with dungeon Mana Crystals, Essence Stones, and selected dungeon components.
- Discovery flow remains exploratory: detect a signal, move through the Overworld, locate the Gate, measure/inspect it, then decide whether to enter.
- Special states such as Red Gate behavior can invalidate or complicate an apparently ordinary measurement; exact rank reading must not imply complete knowledge of every internal danger.

## Equipment rank and drops

- System/dungeon combat equipment uses the source-like `E / D / C / B / A / S` rank language instead of adding a separate generic Common/Rare/Epic/Legendary ladder.
- Exceptional late-game items may be unmeasurable or use a special unknown/exception presentation rather than inventing `S+ / SS / SSS` tiers.
- Avoid turning equipment into a random-affix loot treadmill. Important weapons and armor should have authored identities, predictable core stats, and source-appropriate unique effects.
- Generic monsters primarily supply materials and occasional ordinary gear; elites improve material/gear quality; bosses are the primary source of unique equipment, Rune Stones, and signature crafting components.
- System Store gear remains broadly useful baseline equipment; dungeon/boss drops provide the distinctive upper end.
- Duplicate equipment can be sold for System Gold or dismantled for a partial return of appropriate materials rather than becoming pure trash.
- Equipment rank is communicated mainly through its letter rank and System presentation; avoid unnecessary rainbow-rarity coloring that conflicts with the source-like UI language.

## Player stats and level-up

- Core permanent stats are Strength, Stamina, Agility, Intelligence, and Sense/Perception.
- Strength primarily drives physical attack output and strength-oriented weapon/skill scaling.
- Stamina primarily drives maximum HP, durability/survivability, and appropriate fatigue/endurance interactions.
- Agility drives movement/combat mobility, attack handling speed where appropriate, dodge/dash-style recovery or cooldown reduction, and bow draw/charge-time reduction. Avoid replacing player skill with a large automatic dodge chance.
- Intelligence primarily drives maximum MP and mana-dependent abilities; after the shadow job unlocks it becomes especially valuable for shadow regeneration and sustained army use.
- Sense/Perception supports enemy detection, threat awareness, accuracy/precision, hidden or stealthy target recognition, and appropriate weak-point/critical interactions.
- Level-ups provide broad baseline stat growth so every level feels meaningful; additional freely allocated stat points are earned through System rewards such as Daily Quests and selected milestones.
- Stat growth is not given a small artificial cap. The early-to-late power gap should become very large, matching the intended progression fantasy.

## Quest rewards and Random Boxes

- Daily Quest completion grants the source-like reward package together rather than forcing a choice: full condition recovery, free stat points, and one Random Box.
- Quest families may include Daily, Penalty/Survival, Main, Urgent/Emergency, Job Change, Collection, Hidden, and context-triggered System quests.
- Random Boxes are gameplay rewards only, never monetized loot boxes.
- Ordinary Random Boxes can contain System Gold, potions/consumables, Mana Crystals/Essence Stones, equipment, dungeon keys, and selected special items.
- Mandatory progression must not depend on an unbounded low-probability roll. Story/progression boxes can use controlled candidate pools or guaranteed required contents.
- Source-inspired one-time hidden conditions, such as over-completing a Daily routine and revealing an unusual reward choice, are desirable discovery content when they do not become repeatable chores.
- Reward resolution, stat-point grants, keys, and progression state are server-authoritative.

## Combat feel and accessibility

- Combat should feel fast, skillful, and visually precise without requiring high mechanical execution from every player. The target experience is that an average or even weak action-game player can feel highly competent.
- Do not achieve accessibility by making visible attack ranges dishonest. Keep visible attack coverage and real hit detection closely aligned.
- Use generous input buffering, sensible combo queues, forgiving cancel windows, light target magnetism/aim correction, short melee approach correction where appropriate, and latency-aware server validation instead of demanding frame-perfect inputs.
- Dodge/dash timing should be readable and forgiving enough to reward intent, not just exact-frame reactions.
- Enemy telegraphs should be clear enough to react to while still looking dangerous and cinematic.
- Avoid long player stun-lock chains and excessive loss of control. Failure should usually come from bad decisions or severe power disparity, not from one missed input leading to several seconds of helplessness.
- Assist behavior must remain subtle enough that the player attributes success to their own action rather than feeling the game is playing itself.

## Core combat structure

- Replace vanilla click-trading as the primary RPG combat loop with authored weapon animations, combos, skills, movement, dodge/dash, and appropriate guarding.
- The combat tempo itself scales with progression: early combat is more deliberate and readable; late combat supports extremely fast approach, chaining, repositioning, area attacks, and high-mobility skill use.
- Hit reactions, hit-stop, sound, VFX, camera response, and animation timing are treated as core combat feedback, not optional polish.
- Damage, hit confirmation, resource use, cooldowns, and authoritative movement/state changes are server-decided; client effects predict/present them without becoming authoritative.

## Weapon families and dual wielding

- Players are not locked out of major weapon families by class. Core families may include daggers, one-handed swords, great weapons, spears, axes, bows, magic/special weapons, and later Monarch-related weapons.
- Dual wielding is a real combat mode for suitable weapon families, especially daggers and selected one-handed weapons. It changes animation, combo structure, tempo, and skill interactions rather than simply doubling damage.
- Physically implausible combinations such as dual greatswords are not required merely for consistency.
- Bow draw/charge time benefits from Agility so ranged combat remains viable and visibly accelerates with growth.
- Ruler's Hand / Ruler's Authority later extends weapon control beyond the two physical hand slots through telekinetic weapon manipulation.

## Damage, defense, and scaling

- Do not level-scale ordinary enemies to the player. A low-rank enemy remains low rank and should eventually become trivial as the Player grows.
- Basic damage derives from authored weapon performance, relevant permanent stats, skill coefficients, and equipment effects.
- Defense should use a diminishing-return structure rather than unrestricted flat subtraction so very large late-game values remain stable and tunable.
- Large power gaps must be felt directly: much weaker enemies die quickly, peers sustain normal fights, and much stronger enemies are genuinely dangerous while resisting weak attacks.
- Boss difficulty must not be created primarily by multiplying HP into a damage sponge. Patterns, movement, defense/stagger behavior, phases, special abilities, summons, arena control, and readable high-impact attacks carry most of the encounter identity.
- Multiplayer boss scaling may increase survivability and stagger resistance with party size, but should not naively multiply outgoing damage by player count.
- Previously dangerous bosses are allowed to become trivial later. Do not preserve old content as permanently equal-level encounters at the expense of progression fantasy.
