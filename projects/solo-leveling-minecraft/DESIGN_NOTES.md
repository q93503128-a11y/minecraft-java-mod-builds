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
