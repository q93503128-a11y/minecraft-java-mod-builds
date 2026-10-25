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
