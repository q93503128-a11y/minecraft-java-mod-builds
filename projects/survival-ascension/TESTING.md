# Survival Ascension 0.60 — First Gameplay Test Matrix

Back up any long-lived world first. Test with the 0.60 JAR and matching 0.60 content-preview pack.

## 1. Boot / save compatibility
- Start one fresh world and one existing 0.59 world.
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
- Death/logout/timeout during incident, defense, apex hunt, ascension trial and 최후의 승천: encounter mobs/boss bars/state clean up without duplicate rewards.

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

## 0.60 focused checks
- Before all four canonical requirements are complete, choose `M → 인프라 → 최후의 승천`. It must refuse entry and print the same `최후의 승천 준비` checklist; do not observe a second independent readiness counter.
- With Dragon stage2 + Expedition 9/9 + Apex first-clear 9/9 + Ascension Nexus complete, start `최후의 승천` in an open loaded area. The existing `승천 중추` re-selection must still start the old repeatable 승천 시련, not silently redirect to the final encounter.
- 1막 채굴: six purple mining blocks form a compact real wall. Normal high-rank area/vein behavior may clear multiple real targets; 웅크리기 must preserve the existing precision/single-block behavior.
- 1막 건축: place real blocks above all four amethyst markers. Construction line/plane follow-up may satisfy multiple target cells only when it actually places blocks there. The four player repair blocks must remain after the temporary amethyst markers are cleaned.
- 1막 전투/기동: three marked guards are real hostile entities and the three glowstone checkpoints require actual player movement through the arena. No GUI-only submit button or numeric skill check may advance the phase.
- 2막: verify exactly three compressed sets — 삼림/건조/습지, 고산/대양/심층, 빙설/네더/엔드 — rather than nine Apex bosses. Snapshot Apex 진행 before/after: unique first-clear count, defeated mask semantics and Apex rewards must not change from these echo kills.
- 3막: each of the three collapse anchors spawns one high-threat guard. After that guard dies, only standing near the actual purple anchor while 웅크리기 is held should build the precision-seal channel; releasing Shift or leaving the radius must reduce/reset progress rather than auto-complete remotely.
- Start with a second player nearby: helpers may fight/build, but only the owner movement/checkpoint and owner Shift seal drive those owner-specific objectives. A second Final Ascension center inside 128 blocks must be rejected.
- While Final Ascension is active, trying to start a repeatable Ascension Trial or Apex hunt for the same player must be rejected. If another conflicting large activity appears, Final Ascension must fail-clean rather than stack both state machines.
- Kill the owner, leave the 72-block arena, log out, or force a phase timeout. All Survival-owned temporary marker blocks and marked encounter mobs must disappear; already placed player repair blocks must not be deleted.
- Restart after an interrupted run and revisit the area. Any persisted orphan mob carrying the final-ascension owner marker must be rejected on entity join. No new SavedData file/id should appear from 0.60 acts 1-3.
- Run the complete 1-3 sequence. Completion must not grant `World Final Ascension Complete` yet and must not duplicate Apex/Trial rewards; the final unique boss remains a separate later stage.
