package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First-route feel contract.
 *
 * <p>The exact AUTO choices are not treated as optimal play. They are a deterministic lower-fidelity probe that
 * protects the authored route from drifting into HP-sponge combat before client playtest.</p>
 */
class FirstRouteBalanceContractTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        CampaignProgressStore.removeRuntime(playerId);
    }

    @Test
    void firstVisibleEncounterStaysBrief() {
        assertPacing("CV_FIRST_COMMON", 3, 6);
    }

    @Test
    void drabyelRoadEncounterAddsOneLayerWithoutDragging() {
        assertPacing("CV_DRABYEL_ROAD", 6, 12);
    }

    @Test
    void warningCaveEliteFeelsSubstantialWithoutBecomingAnHpSponge() {
        assertPacing("CV_WARNING_CAVE_ELITE", 10, 18);
    }

    @Test
    void firstRouteGoldMatchesV1FieldAndEliteEconomy() {
        assertEquals(120, V04Catalogs.battleGold(V04Catalogs.encounter("CV_FIRST_COMMON")));
        assertEquals(160, V04Catalogs.battleGold(V04Catalogs.encounter("CV_DRABYEL_ROAD")));
        assertEquals(600, V04Catalogs.battleGold(V04Catalogs.encounter("CV_WARNING_CAVE_ELITE")));
    }

    private void assertPacing(String encounterId, int minAllyActions, int maxAllyActions) {
        BattleState state = CampaignEncounterCatalog.createBattle(playerId, encounterId);
        BattleEngine engine = new BattleEngine(state);
        int allyActions = 0;
        int enemyActions = 0;
        int totalActions = 0;

        while (state.outcome() == BattleOutcome.RUNNING && totalActions < 120) {
            CombatantState actor = engine.nextReady();
            if (!actor.definition().summon()) {
                if (actor.side() == CombatantSide.ALLY) allyActions++;
                else enemyActions++;
            }
            BattleAutoController.chooseAutoAction(engine, state, actor);
            totalActions++;
        }

        assertEquals(BattleOutcome.ALLY_VICTORY, state.outcome(),
                () -> encounterId + " did not resolve as an early-route victory; allyActions=" + allyActions
                        + ", enemyActions=" + enemyActions + ", totalActions=" + totalActions);
        assertTrue(allyActions >= minAllyActions && allyActions <= maxAllyActions,
                () -> encounterId + " ally actions=" + allyActions + " outside " + minAllyActions + ".." + maxAllyActions
                        + " (enemyActions=" + enemyActions + ", totalActions=" + totalActions + ")");
    }
}
