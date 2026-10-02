package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import io.github.q93503128.turnbound.world.CharacterProgression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic Chapter 1 combat telemetry.
 *
 * <p>AUTO is intentionally not an optimal-play oracle. These checks use a same-level, no-equipment starter party
 * as a conservative regression baseline so field packs cannot silently drift into one-turn wipes or HP sponges.
 * Client feel remains the final authority.</p>
 */
final class ChapterOneCombatPacingTest {
    private final List<UUID> players = new ArrayList<>();

    @AfterEach
    void cleanup() {
        players.forEach(CampaignProgressStore::removeRuntime);
        players.clear();
    }

    @Test
    void capitalValleyAndWestRoadStayInsideReadableActionBudgets() {
        assertAll(
                pacing("CV_FIRST_COMMON", 2, 6),
                pacing("CV_DRABYEL_ROAD", 3, 8),
                pacing("CV_HOUND_ROAM", 3, 10),
                pacing("CV_SPORE_GROVE", 3, 10),
                pacing("CV_WARNING_CAVE_ELITE", 8, 18),
                pacing("CV_BRIAR_STAG", 8, 18),
                pacing("CV_WORLD_BOSS_GRAUL", 24, 42),
                pacing("AV_CH1_GATE_SKIRMISH", 3, 8),
                pacing("AV_CH1_BOAR_TRAIL", 4, 13),
                pacing("AV_CH1_HOUND_PACK", 5, 14),
                pacing("AV_ROAD_HOUNDS", 3, 10),
                pacing("AV_CH1_CROSSROAD_RAIDERS", 4, 12),
                pacing("AV_ROAD_PATROL", 4, 12),
                pacing("AV_CH1_GORGE_BREAKER", 8, 18),
                pacing("AV_CH1_BROKEN_ESCORT", 5, 14),
                pacing("AV_ROAD_ELITE", 10, 20)
        );
    }

    @Test
    void avsalOutskirtsRelayAndFirstBossStayTacticalWithoutDragging() {
        assertAll(
                pacing("AV_CH1_SPORE_EDGE", 3, 10),
                pacing("AV_CH1_WAYMARK_WALKERS", 4, 12),
                pacing("AV_CH1_RUST_SCOUTS", 3, 10),
                pacing("AV_RELAY_SENTRIES", 3, 10),
                pacing("AV_CH1_OUTSKIRT_HOUNDS", 4, 12),
                pacing("AV_CH1_WATERLINE_SWARM", 5, 14),
                pacing("AV_CH1_RUSTED_COLUMN", 5, 15),
                pacing("AV_RELAY_GUARD", 4, 13),
                pacing("AV_FIRST_BOSS", 24, 42)
        );
    }

    private Executable pacing(String encounterId, int minAllyActions, int maxAllyActions) {
        return () -> {
            UUID playerId = UUID.randomUUID();
            players.add(playerId);
            V04Catalogs.Encounter spec = V04Catalogs.encounter(encounterId);
            setStarterPartyLevel(playerId, spec.level());

            BattleState state = CampaignEncounterCatalog.createBattle(playerId, encounterId);
            BattleEngine engine = new BattleEngine(state);
            int allyActions = 0;
            int enemyActions = 0;
            int totalActions = 0;

            while (state.outcome() == BattleOutcome.RUNNING && totalActions < 180) {
                CombatantState actor = engine.nextReady();
                if (!actor.definition().summon()) {
                    if (actor.side() == CombatantSide.ALLY) allyActions++;
                    else enemyActions++;
                }
                BattleAutoController.chooseAutoAction(engine, state, actor);
                totalActions++;
            }

            long livingAllies = state.living(CombatantSide.ALLY).stream()
                    .filter(unit -> !unit.definition().summon()).count();
            String metrics = encounterId + " level=" + spec.level()
                    + " allyActions=" + allyActions
                    + " enemyActions=" + enemyActions
                    + " totalActions=" + totalActions
                    + " livingAllies=" + livingAllies
                    + " outcome=" + state.outcome();

            assertEquals(BattleOutcome.ALLY_VICTORY, state.outcome(), metrics);
            assertTrue(allyActions >= minAllyActions && allyActions <= maxAllyActions,
                    metrics + " outside ally target " + minAllyActions + ".." + maxAllyActions);
            assertTrue(enemyActions <= Math.max(4, maxAllyActions),
                    metrics + " enemy action count is too high for this encounter class");
            assertTrue(livingAllies >= 2, metrics + " leaves the starter party too close to a wipe for baseline AUTO");
        };
    }

    private static void setStarterPartyLevel(UUID playerId, int level) {
        CampaignProgressStore.Snapshot base = CampaignProgressStore.snapshot(playerId);
        Map<String, CharacterProgression.State> characters = new LinkedHashMap<>(base.characters());
        for (String characterId : base.activeParty()) {
            CharacterProgression.State current = characters.get(characterId);
            characters.put(characterId, new CharacterProgression.State(
                    Math.max(1, Math.min(60, level)), 0, current == null ? 0 : current.bonusLevel()));
        }
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                base.profile(), characters, base.growth(), base.equipment(), base.quests(), base.activeParty(),
                base.clearedEncounters(), base.orphanedCharacterIds(), base.orphanedEquipmentIds()));
    }
}
