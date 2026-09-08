package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class BossCombatControllerTest {
    @Test
    void deterministicSelectionUsesStableBossAttackOrderAndCompletionCount() {
        Fixture fixture = fixture();
        BossCombatController controller = fixture.controller();

        AttackExecution.Snapshot first = controller.beginNextAttack(100);
        assertEquals(fixture.alpha(), first.patternId());
        assertEquals(1, controller.phase());
        assertThrows(IllegalStateException.class, () -> controller.beginNextAttack(101));

        BossCombatController.Step completed = controller.advance(115);
        assertTrue(completed.attackStep().finished());
        assertEquals(1L, controller.completedAttackCount());
        assertEquals(fixture.alpha(), controller.lastAttack().orElseThrow());

        AttackExecution.Snapshot second = controller.beginNextAttack(200);
        assertEquals(fixture.beta(), second.patternId());
    }

    @Test
    void phaseTransitionInterruptsOldAttackAndClosesItsLifecycle() {
        Fixture fixture = fixture();
        BossCombatController controller = fixture.controller();

        AttackExecution.Snapshot active = controller.beginNextAttack(40);
        assertEquals(fixture.alpha(), active.patternId());

        BossCombatController.PhaseTransition transition = controller.transitionToPhase(2);
        assertEquals(1, transition.previousPhase());
        assertEquals(2, transition.newPhase());
        assertEquals(fixture.alpha(), transition.interruptedAttack().orElseThrow());
        assertFalse(controller.attackExecuting());
        assertEquals(0L, controller.completedAttackCount(), "interrupted attacks are not completed attacks");

        BossCombatController.Step idle = controller.advance(999);
        assertTrue(idle.attackStep().snapshot().isEmpty(), "old attack must not reopen after transition");

        AttackExecution.Snapshot phaseTwoAttack = controller.beginNextAttack(1000);
        assertEquals(fixture.alpha(), phaseTwoAttack.patternId(), "selection remains deterministic after interruption");
        assertEquals(2, controller.phase());
    }

    @Test
    void explicitCancelAndInvalidSelectionFailClosed() {
        Fixture fixture = fixture();
        BossCombatController controller = fixture.controller();
        controller.beginNextAttack(5);
        assertEquals(fixture.alpha(), controller.cancelAttack().orElseThrow());
        assertTrue(controller.cancelAttack().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> controller.transitionToPhase(1));
        assertThrows(IllegalArgumentException.class, () -> controller.transitionToPhase(3));

        BossAttackSelectionPolicy invalidPolicy = context -> ContentId.parse("riftfrontier:not_owned_by_boss");
        BossCombatController invalid = new BossCombatController(fixture.catalog(), fixture.boss(), invalidPolicy);
        assertThrows(IllegalStateException.class, () -> invalid.beginNextAttack(0));
    }

    private static Fixture fixture() {
        ContentRegistry registry = new ContentRegistry();
        ContentId alpha = ContentId.parse("riftfrontier:alpha_strike");
        ContentId beta = ContentId.parse("riftfrontier:beta_strike");
        ContentId boss = ContentId.parse("riftfrontier:apex_test");

        registry.register(new CoreDefinition.AttackPattern(beta, "line_charge", 6, 2, 5, Set.of("sidestep"), "lowered_charge"));
        registry.register(new CoreDefinition.AttackPattern(alpha, "slam", 8, 3, 4, Set.of("backstep"), "raised_slam"));
        registry.register(new CoreDefinition.BossProfile(boss, 2, Set.of(beta, alpha), "keep_escape_lane"));

        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);
        BossCombatController controller = new BossCombatController(
            catalog,
            boss,
            BossAttackSelectionPolicy.deterministicRoundRobin()
        );
        return new Fixture(catalog, controller, alpha, beta, boss);
    }

    private record Fixture(
        CombatRuntimeCatalog catalog,
        BossCombatController controller,
        ContentId alpha,
        ContentId beta,
        ContentId boss
    ) {}
}
