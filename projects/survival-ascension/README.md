# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages, exploration goals and shared infrastructure back against that growth.

## 0.24.0-alpha.1 — Regional Field Objectives
Expeditions are no longer completed by merely stepping into a biome. A region is first **discovered**, then completed only by performing its own in-region field objective with the systems Survival Ascension already taught the player to use.

### Nine field objectives
- Stage 0 `삼림권`: bulk-fell 96 natural-tree logs through the smart-tree queue.
- Stage 0 `건조권`: place 128 secondary blocks through scaled Construction jobs.
- Stage 0 `습지권`: harvest 96 actually mature crops.
- Stage 0 `고산권`: cover 600 blocks through legitimate on-foot sprint traversal.
- Stage 0 `대양권`: travel 800 blocks while swimming, in water or riding a vessel; normal land sprinting does not count.
- Stage 1 `심층권`: mine 192 valid pickaxe blocks in deep-cave biome families.
- Stage 1 `빙설권`: cover 600 blocks through legitimate on-foot sprint traversal.
- Stage 1 `네더권`: kill 24 hostile mobs while inside Nether expedition biomes.
- Stage 2 `엔드권`: kill 32 hostile mobs while inside End expedition biomes.

Actions only progress the matching region while the player is physically inside that region and its world-stage requirement is unlocked. Creative/spectator actions do not progress expeditions. `/ascension stats` shows discovered/completed counts and every currently active objective value.

### Anti-shortcut rules
- Woodland counts successful queued natural-tree breaks, not inventory logs or plain placed-log demolition.
- Arid counts successful protected/material-backed bulk placements, not manual single blocks.
- Wetland counts mature block harvest events; tick-queued secondary harvests count individually.
- Highlands/Frozen reuse the legitimate Mobility traversal rule, excluding riding, flight, elytra, swimming and teleport-like deltas.
- Ocean has its own water/vessel travel tracker with a per-second displacement sanity cap and cannot be advanced by the normal Mobility objective path.
- Nether/End count only entities implementing the hostile `Enemy` role, not livestock or passive mobs.

### Save compatibility
`expedition_v1` remains the SavedData ID. 0.24 only extends each player entry with optional completion/progress/reward fields.
- Existing 0.23 discoveries remain discovered.
- Existing 0.23 per-region skill XP is marked as already paid so completing an old discovery cannot duplicate the XP.
- A player who already claimed the 0.23 nine-region master milestone is migrated to all nine completed regions, preserving Field Mastery.
- Existing milestone claim bits remain authoritative, preventing duplicate material/Mythic rewards.

### Field Mastery remains the final Lv.100 layer
Completing all nine field objectives at Stage 2 unlocks the same final physical-scale rewards introduced in 0.23:
- Mining Quarry tunnel `7x7x10 -> 7x7x12`, still 12 blocks/player/tick and 64 globally.
- Woodcutting `384 -> 448` natural-tree logs, still 12/player and 64/global.
- Harvesting `11x11 -> 13x13`, tick-drained 12/player and 64/global.
- Combat Academy shockwave `6.5/16 -> 7.5/20`, same 55% fraction and 50-tick cooldown.
- Construction line `49 -> 65`, plane `11x11 -> 13x13`; volume remains `7x7x7` and real-material/protection checks remain.
- Mobility Stage-2 Nexus air dashes `3 -> 4`; landing reset and normal cooldown remain.

## Existing Stage-2 loop
- Complete the Ascension Nexus, then re-select it from `M -> Infrastructure` to open an Ascension Trial.
- Entry consumes 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath.
- Four waves, 60 seconds per wave, 5-second setup between waves, randomized `쇄도 / 추격 / 봉쇄` doctrine and one bounded mid-wave reinforcement.
- Completion guarantees one Mythic III affix item, 2 Netherite Scraps, 4 Diamonds and 200 XP; nearby helpers receive XP without duplicating owner loot.
- Valid Mythic III gear can be awakened once into four-affix Awakened Mythic gear through the existing Equipment radial and expensive endgame materials.

### Expedition milestone rewards
- Complete four Stage-0 objectives: 4 Diamonds + 16 Emeralds + 32 Amethyst Shards.
- Stage 1, seven completed including Deep and Nether: 2 Netherite Scraps + 16 Diamonds + 32 Echo Shards.
- Stage 2, all nine completed: guaranteed Mythic III + 4 Netherite Scraps + 64 Echo Shards + 16 Dragon's Breath + 500 XP + Field Mastery.

The field-objective structure is independently implemented after studying objective/reward and persistent quest-progression patterns from other mods. Bountiful and FTB Quests are reference-only here; no source, assets, quest data or namespaces from those projects are bundled.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
