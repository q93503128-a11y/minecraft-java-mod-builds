# Design Notes

Use this file for confirmed design decisions that are too detailed for the project summary. Keep uncertain ideas in PROJECT.md under Open design questions until agreed.

## Inventory

- Keep one real Minecraft player inventory rather than maintaining separate vanilla and System inventories by default.
- Replace/re-theme the inventory screen into the Solo Leveling System visual language.
- If capacity becomes a gameplay problem, prefer System-driven slot/capacity expansion over a second parallel inventory.
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
