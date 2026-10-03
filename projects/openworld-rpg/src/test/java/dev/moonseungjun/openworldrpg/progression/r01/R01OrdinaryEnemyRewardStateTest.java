package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01OrdinaryEnemyRewardStateTest {
    private static final String ENCOUNTER =
            "openworld_rpg:r01/ordinary_enemy/11111111-2222-3333-4444-555555555555";
    private static final String ONLINE =
            UUID.fromString("99999999-2222-3333-4444-555555555555").toString();
    private static final String OFFLINE =
            UUID.fromString("88888888-2222-3333-4444-555555555555").toString();

    @Test
    void firstContributionClassWinsAndOnlyKillPresentPlayersReceivePendingPlan() {
        var state = R01OrdinaryEnemyRewardState.initial()
                .recordParticipation(
                        ENCOUNTER,
                        R01OrdinaryEnemyRewardRules.EnemySource.BISON,
                        ONLINE,
                        RootClass.GUARDIAN
                )
                .recordParticipation(
                        ENCOUNTER,
                        R01OrdinaryEnemyRewardRules.EnemySource.BISON,
                        ONLINE,
                        RootClass.MAGE
                )
                .recordParticipation(
                        ENCOUNTER,
                        R01OrdinaryEnemyRewardRules.EnemySource.BISON,
                        OFFLINE,
                        RootClass.HUNTER
                );

        assertEquals(
                RootClass.GUARDIAN,
                state.encounter(ENCOUNTER).orElseThrow().participants().get(ONLINE)
        );

        var plan = new R01OrdinaryEnemyRewardRules.RewardPlan(
                0L,
                Map.of("openworld_rpg:tough_hide", 2),
                Optional.empty()
        );
        state = state.completeEncounter(ENCOUNTER, Map.of(ONLINE, plan));

        assertTrue(state.encounter(ENCOUNTER).isEmpty());
        assertEquals(1, state.pendingForPlayer(ONLINE).size());
        assertTrue(state.pendingForPlayer(OFFLINE).isEmpty());
        assertEquals(
                R01OrdinaryEnemyRewardRules.EnemySource.BISON,
                state.pendingForPlayer(ONLINE).getFirst().source()
        );
    }
}