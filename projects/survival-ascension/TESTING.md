# Survival Ascension 0.59 — Gameplay Test Matrix

Back up any long-lived world first. Test with the 0.59 JAR and matching 0.59 content-preview pack.

## 1. Boot / save compatibility
- Start one fresh world and one existing 0.58 world.
- PASS: no load crash, registry/data error, migration prompt or repeated login exception.
- Run `/ascension stats`; all six skills plus expedition/operation/apex/logistics/outpost/recovery summaries must render.

## 2. Precision vs physical scale
- Mining Lv10/30/60/90/100: test normal interior and chunk border. Shift=one block. Non-Shift plane/vein/extract/bore must not load or generate an unloaded neighboring chunk.
- Woodcutting: normal and Shift on a natural tree. Only loaded connected logs may queue; unloading the area must not keep/force it loaded.
- Harvesting: normal vs Shift mature crops; irrigation replant must consume a real eligible seed source.
- Construction: Shift/single, line, plane, volume and causeway. Only loaded/interactable targets place and only real material is consumed.
- Mobility: ground dash, air-dash limit, fall safety and logout/login reset.

## 3. Expedition accounting
- Woodland: one valid manual/Shift log break advances `LOGS_FELLED` by exactly1. Bulk fell advances exactly once per log actually broken.
- Arid: the first valid placement advances `BLOCKS_BUILT` by exactly1. Each successful bulk follow-up adds exactly1; skipped/denied/out-of-material targets add0.
- Deep/Wetland: one mined block / one mature crop advances exactly1; Shift precision still counts.
- Mobility/Combat/Ocean: travel, dash, hostile kill and voyage counters move only from their matching real action.

## 4. Combat identities
- Spear: vanilla Jab/Charge stays authoritative; Survival drive line requires forward momentum, deals0 secondary damage/XP and Shift suppresses it.
- Mace: vanilla smash remains intact; only the outer hostile ring is added with0 outer damage/XP.
- Shield: only a successful block can emit the zero-damage guard wave; Shift keeps precision block only.
- Bow/Crossbow: launch-time affix/Shift snapshot survives weapon swapping; kill XP/rewards stay with the still-online shooter.

## 5. Physical logistics / frontline
- Nearby usable physical Barrel cluster first, then player inventory where that system allows it.
- Linked warehouses stay same-dimension, loaded, physical and interactable.
- Outpost/recovery/operation/defense exact-local-stock rules remain authoritative.
- Freight remains a real Chest Minecart flow; no remote item teleportation/generated cargo.

## 6. World / external content
- Check Biomes O' Plenty expedition biomes and Minecraft26.2 Sulfur Caves regional detection; Sulfur/Cinnabar must not become valuable-ore vein/extract targets.
- Check The Birth of Steve dungeon/major targets if encountered; no optional-class loading crash and bounded major-target credit only inside a real expedition region.
- Check Amethyst Resonance tagged tools; imprint/reforge must preserve original item behavior/components.

## 7. Death / encounter cleanup
- Normal death near an armed operational outpost: one pending recovery, one post-respawn teleport, contract consumed only after successful move.
- Death/logout/timeout during incident, defense, apex hunt and ascension trial: encounter mobs/boss bars/state clean up without duplicate rewards.

## Stop-and-report signals
Capture screenshot/log + reproduction steps for any crash, save corruption, item duplication/loss, unloaded-chunk hitch/forced generation, repeated reward, objective increase larger than real actions, bulk work crossing Shift precision, invisible/remote storage consumption, stale encounter state after failure, or external-mod classloading error.

## 0.58 focused checks
- Two players: trigger eligible incidents near each other; the second incident must not start within 112 blocks of the first center, and neither player's kills/actions may complete the other's incident.
- Incident perimeter: verify the 48-block ring appears about once per second without chunk generation or hitching; rare incidents use the distinct rare presentation.
- Rare incident: verify target scale is larger, reward is stronger, and completion still consumes only that region's one incident reward.
- Construction LINE/CAUSEWAY: Shift+click cycles only through currently unlocked 5/9/17/33/49/65 lengths; relog preserves the choice; a spoofed client cannot request a locked numeric length because the packet has no length field.
- Actual placement Shift still creates only the origin block even after selecting a bulk length.
- Existing 0.57 save with no construction selection starts at its maximum currently unlocked length.
- With TBS installed, first guarded login removes at most one initial `tbos:archivists_journal`; later legitimately obtained journals must survive relog because the guard is already marked complete.

## 0.59 focused checks
- Start a non-Ocean Apex hunt with the locked content pack present. Exactly one initial vanilla escort slot may become a glowing `이변 호위`; the total initial escort attempt count must stay equal to the archetype's existing count.
- Stage-tier census at dedicated-server ready state must show `apex_escort_tier_0/1/2`; the resolved IDs must come only from the curated optional tags.
- If an optional escort cannot find a valid spawn position, the same slot must fall back to the original vanilla escort rather than shrinking the hunt below its normal attempt count or failing solely because the content mob was unavailable.
- Start an Ocean Apex hunt: it must keep the existing aquatic vanilla roster and report no `이변 호위` until an external aquatic mob has been separately audited.
- Remove the optional content mod and boot standalone Survival Ascension: no classloading/linkage failure, empty Apex escort pools, and normal vanilla Apex composition.
- Confirm Minotaur, Hour Cantor and Phoenix Guardian never appear through the Apex escort tags.
- During logout/death/timeout, a substituted content escort must be removed by the same owner-scoped Apex cleanup as vanilla escorts; no glowing orphan mob may remain.
