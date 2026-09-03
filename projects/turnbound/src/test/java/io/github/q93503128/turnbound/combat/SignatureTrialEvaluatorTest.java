package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SignatureTrialEvaluatorTest {
    private static CombatantState unit(String instanceId, String definitionId, CombatantSide side) {
        return unit(instanceId, definitionId, side, 100);
    }

    private static CombatantState unit(String instanceId, String definitionId, CombatantSide side, int speed) {
        CombatantDefinition definition = PrototypeRoster.trainingEnemy(definitionId, definitionId, 1000, 100, 80, speed);
        return new CombatantState(instanceId, definition, side, 0);
    }

    private static BattleState victory(CombatantState... allies) {
        CombatantState enemy = unit("enemy", "TRIAL_ENEMY", CombatantSide.ENEMY);
        BattleState state = new BattleState(java.util.stream.Stream.concat(
                java.util.Arrays.stream(allies), java.util.stream.Stream.of(enemy)).toList());
        enemy.forceDown();
        state.addEvent(new BattleEvent("DOWN", allies[0].instanceId(), enemy.instanceId(), 0, "fixture"));
        assertEquals(BattleOutcome.ALLY_VICTORY, state.outcome());
        return state;
    }

    @Test
    void p01RejectsKnownPartyConstraintBeforeUnresolvedEliteIdentity() {
        CombatantState p01 = unit("p01", "P01", CombatantSide.ALLY);
        BattleState validKnownShape = victory(p01, unit("ally", "P04", CombatantSide.ALLY));
        var unresolved = SignatureTrialEvaluator.evaluate("P01", validKnownShape);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_EVALUABLE, unresolved.objectiveState());
        assertTrue(unresolved.detail().contains("partySize=2"));
        assertTrue(unresolved.detail().contains("canonical identity"));

        BattleState tooLarge = victory(p01,
                unit("ally1", "P03", CombatantSide.ALLY),
                unit("ally2", "P04", CombatantSide.ALLY));
        var failed = SignatureTrialEvaluator.evaluate("P01", tooLarge);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_MET, failed.objectiveState());
        assertTrue(failed.detail().contains("partySize=3/<=2"));
    }

    @Test
    void p02RejectsKnownSpeedAndActionConstraintsBeforeUnresolvedBossIdentity() {
        CombatantState p02 = unit("p02", "P02", CombatantSide.ALLY, 125);
        BattleState state = victory(p02,
                unit("slow1", "P03", CombatantSide.ALLY, 75),
                unit("slow2", "P04", CombatantSide.ALLY, 80));
        for (int i = 0; i < 22; i++) state.addEvent(new BattleEvent("ACTION", p02.instanceId(), "enemy", 0, "fixture"));

        var unresolved = SignatureTrialEvaluator.evaluate("P02", state);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_EVALUABLE, unresolved.objectiveState());
        assertTrue(unresolved.detail().contains("finalSpd80OrLessAllies=2"));
        assertTrue(unresolved.detail().contains("actions=22"));

        state.addEvent(new BattleEvent("ACTION", p02.instanceId(), "enemy", 0, "fixture"));
        var tooManyActions = SignatureTrialEvaluator.evaluate("P02", state);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_MET, tooManyActions.objectiveState());
        assertTrue(tooManyActions.detail().contains("actions=23/<=22"));
    }

    @Test
    void p03RequiresTenEnemyActionsBeforeProtectedNpcIdentityCanBeResolved() {
        CombatantState p03 = unit("p03", "P03", CombatantSide.ALLY);
        BattleState state = victory(p03);
        for (int i = 0; i < 9; i++) state.addEvent(new BattleEvent("ACTION", "enemy", p03.instanceId(), 0, "fixture"));
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_MET,
                SignatureTrialEvaluator.evaluate("P03", state).objectiveState());

        state.addEvent(new BattleEvent("ACTION", "enemy", p03.instanceId(), 0, "fixture"));
        var unresolved = SignatureTrialEvaluator.evaluate("P03", state);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_EVALUABLE, unresolved.objectiveState());
        assertTrue(unresolved.detail().contains("enemyActions=10"));
        assertTrue(unresolved.detail().contains("protected NPC"));
    }

    @Test
    void p04DetectsDeathThenFinalFullSurvivalButKeepsCanonSettlementBlocked() {
        CombatantState p04 = unit("p04", "P04", CombatantSide.ALLY);
        CombatantState ally = unit("ally", "P01", CombatantSide.ALLY);
        BattleState state = victory(p04, ally);
        ally.forceDown();
        state.addEvent(new BattleEvent("DOWN", "enemy", ally.instanceId(), 0, "fixture"));
        ally.revive(0.30);
        state.addEvent(new BattleEvent("REVIVE", p04.instanceId(), ally.instanceId(), ally.hp(), "fixture"));

        SignatureTrialEvaluator.Evaluation result = SignatureTrialEvaluator.evaluate("P04", state);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.MET, result.objectiveState());
        assertTrue(result.objectiveMet());
        assertTrue(result.canonBlocked());
        assertFalse(result.settlementEligible());
    }

    @Test
    void p05RequiresTenFollowupsWithinTwentyFiveTotalActions() {
        CombatantState p05 = unit("p05", "P05", CombatantSide.ALLY);
        BattleState state = victory(p05);
        for (int i = 0; i < 10; i++) {
            state.addEvent(new BattleEvent("REACTION_DAMAGE", p05.instanceId(), "enemy", 1, "P05_FOLLOW_UP"));
        }
        for (int i = 0; i < 25; i++) {
            state.addEvent(new BattleEvent("ACTION", p05.instanceId(), "enemy", 0, "fixture"));
        }
        assertEquals(SignatureTrialEvaluator.ObjectiveState.MET,
                SignatureTrialEvaluator.evaluate("P05", state).objectiveState());

        state.addEvent(new BattleEvent("ACTION", p05.instanceId(), "enemy", 0, "fixture"));
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_MET,
                SignatureTrialEvaluator.evaluate("P05", state).objectiveState());
    }

    @Test
    void p06RequiresSelfReviveAndFiveMemory() {
        CombatantState p06 = unit("p06", "P06", CombatantSide.ALLY);
        BattleState state = victory(p06);
        p06.setCounter("memory", 5);
        state.addEvent(new BattleEvent("SELF_REVIVE", p06.instanceId(), p06.instanceId(), 350, "P06_LAST_PAGE"));
        assertEquals(SignatureTrialEvaluator.ObjectiveState.MET,
                SignatureTrialEvaluator.evaluate("P06", state).objectiveState());

        p06.setCounter("memory", 4);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_MET,
                SignatureTrialEvaluator.evaluate("P06", state).objectiveState());
    }

    @Test
    void p07RequiresContractDeathThenManualResummonAndLivingMarion() {
        CombatantState p07 = unit("p07", "P07", CombatantSide.ALLY);
        BattleState state = victory(p07);
        state.addEvent(new BattleEvent("SUMMON_DOWN", "summon_p07", p07.instanceId(), 300, "P07_CONTRACT"));
        state.addEvent(new BattleEvent("ACTION", p07.instanceId(), p07.instanceId(), 0, "p07_summon_toto"));
        assertEquals(SignatureTrialEvaluator.ObjectiveState.MET,
                SignatureTrialEvaluator.evaluate("P07", state).objectiveState());
    }

    @Test
    void p08CanonContradictionCanNeverSettle() {
        BattleState state = victory(unit("p08", "P08", CombatantSide.ALLY));
        SignatureTrialEvaluator.Evaluation result = SignatureTrialEvaluator.evaluate("P08", state);
        assertEquals(SignatureTrialEvaluator.ObjectiveState.NOT_EVALUABLE, result.objectiveState());
        assertTrue(result.canonBlocked());
        assertFalse(result.settlementEligible());
        assertTrue(result.detail().contains("Awakening"));
    }
}
