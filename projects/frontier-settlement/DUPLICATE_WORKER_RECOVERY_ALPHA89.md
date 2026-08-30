# Frontier Settlement Alpha.89 — duplicate worker recovery

Version: `0.1.0-alpha.89`

Alpha.89 is a save-compatible lifecycle correction over Alpha.88. It adds no required settlement SavedData field.

## Local production workers
- A completed lumber camp, farm, quarry, or mine authorizes exactly one local production worker of that role.
- Historical excess workers are cleaned only while the complete work/storage lookup envelope is loaded.
- UUID order deterministically selects the workers that remain; an unloaded resident is never treated as missing or excess.
- Cleanup runs before the existing 600-tick worker-attraction gate, preventing an excess worker from being removed and immediately recreated.
- If an excess worker carries a real MAINHAND ItemStack, an exact ItemEntity copy must successfully enter the world before the hand is cleared and the worker is discarded.
- Population is recomputed after cleanup only when the existing transporter/workshop/advanced-worker evidence is also fully loaded.

## Shared builder
- The historical `NoAI=true` + `invulnerable=true` duplicate-builder quarantine is removed.
- Once the complete legal builder envelope is loaded, UUID order keeps one shared builder and excess builders are physically cleaned with the same exact MAINHAND cargo preservation rule.
- The retained builder is normalized to `NoAI=false` and `invulnerable=false`.
- Builder cleanup also runs from the ordinary settlement worker tick, so a duplicate can recover even when no new building project is currently active.

## Boundaries
- Natural Minecraft villagers are untouched; cleanup only targets FrontierWorkerEntity role/name evidence.
- No teleport, chunk force-load, virtual refund, duplicate cargo minting, or second resident ledger is introduced.
- Same-world update: fully close Minecraft, back up the world, install Alpha.89, and reopen the same save.
