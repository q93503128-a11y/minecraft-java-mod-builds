package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LumeaV1RuntimeTest {
    private static CombatantState lumea(boolean awakened) {
        return new CombatantState("lumea",
                CanonicalData.definition("P02", 1, 5, awakened), CombatantSide.ALLY, 0);
    }

    private static CombatantState ally(String id, int speed, int seed) {
        return new CombatantState(id,
                PrototypeRoster.trainingEnemy(id.toUpperCase(), id, 9_999, 100, 80, speed),
                CombatantSide.ALLY, seed);
    }

    private static CombatantState enemy(String id, int speed, int seed) {
        return new CombatantState(id,
                PrototypeRoster.trainingEnemy(id.toUpperCase(), id, 9_999, 90, 60, speed),
                CombatantSide.ENEMY, seed);
    }

    @Test
    void basicAdvanceRewardsSlowerAllyAndBlocksSelfWhileAnotherAllyLives() {
        CombatantState lumea = lumea(false);
        CombatantState slow = ally("slow", 90, 1);
        CombatantState enemy = enemy("enemy", 100, 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(lumea, slow, enemy)));
        lumea.setGauge(1000);

        engine.nextReady();
        assertThrows(IllegalArgumentException.class,
                () -> engine.useSkill("lumea", "p02_accelerate", "lumea"));
        engine.useSkill("lumea", "p02_accelerate", "slow");

        assertEquals(160L, slow.gauge());
        assertEquals(114, lumea.speed());
    }

    @Test
    void timeLeapAdvancesInsteadOfForcingInstantReady() {
        CombatantState lumea = lumea(false);
        CombatantState slow = ally("slow", 90, 1);
        CombatantState enemy = enemy("enemy", 100, 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(lumea, slow, enemy)));
        lumea.setGauge(1000);
        slow.setGauge(100);

        engine.nextReady();
        engine.useSkill("lumea", "p02_time_leap", "slow");

        assertEquals(460L, slow.gauge());
        assertTrue(slow.gauge() < BattleEngine.TURN_THRESHOLD);
    }

    @Test
    void delayFieldIsNowSingleTargetDamageAndDelay() {
        CombatantState lumea = lumea(false);
        CombatantState ally = ally("ally", 100, 1);
        CombatantState first = enemy("first", 100, 2);
        CombatantState second = enemy("second", 100, 3);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(lumea, ally, first, second)));
        lumea.setGauge(1000);
        first.setGauge(500);
        second.setGauge(500);
        int hpBefore = first.hp();

        engine.nextReady();
        engine.useSkill("lumea", "p02_delay_field", "first");

        assertEquals(320L, first.gauge());
        assertEquals(500L, second.gauge());
        assertTrue(first.hp() < hpBefore);
    }

    @Test
    void awakenedPreciseAdvanceRewardsActualTimelineChange() {
        CombatantState lumea = lumea(true);
        CombatantState slow = ally("slow", 80, 1);
        CombatantState enemyA = enemy("enemy_a", 100, 2);
        CombatantState enemyB = enemy("enemy_b", 100, 3);
        BattleState state = new BattleState(List.of(lumea, slow, enemyA, enemyB));
        BattleEngine engine = new BattleEngine(state);

        lumea.setGauge(1000);
        slow.setGauge(400);
        enemyA.setGauge(700);
        enemyB.setGauge(600);

        engine.nextReady();
        engine.useSkill("lumea", "p02_time_leap", "slow");

        assertEquals(760L, slow.gauge());
        assertEquals(60L, lumea.gauge());
        assertTrue(state.events().stream().anyMatch(event ->
                "PASSIVE_GAUGE".equals(event.type()) && "P02_TEMPO_WINDOW".equals(event.detail())));
    }

    @Test
    void basicCanTargetSelfOnlyAsLastLivingRegularAlly() {
        CombatantState lumea = lumea(false);
        CombatantState enemy = enemy("enemy", 100, 1);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(lumea, enemy)));
        lumea.setGauge(1000);

        engine.nextReady();
        engine.useSkill("lumea", "p02_accelerate", "lumea");

        assertEquals(120L, lumea.gauge());
    }
}
