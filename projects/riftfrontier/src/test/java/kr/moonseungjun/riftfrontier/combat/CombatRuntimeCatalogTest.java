package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class CombatRuntimeCatalogTest {
    @Test
    void resolvesBossPatternsDeterministicallyAndStartsExecutionFromPublishedContent() {
        ContentRegistry registry = new ContentRegistry();
        ContentId alpha = ContentId.parse("riftfrontier:alpha_strike");
        ContentId beta = ContentId.parse("riftfrontier:beta_strike");
        ContentId boss = ContentId.parse("riftfrontier:apex_test");

        registry.register(new CoreDefinition.AttackPattern(beta, "line_charge", 6, 2, 5, Set.of("sidestep"), "lowered_charge"));
        registry.register(new CoreDefinition.AttackPattern(alpha, "slam", 8, 3, 7, Set.of("backstep"), "raised_slam"));
        registry.register(new CoreDefinition.BossProfile(boss, 2, Set.of(beta, alpha), "keep_escape_lane"));

        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);
        assertEquals(alpha, catalog.bossAttackTimelines(boss).getFirst().pattern().id());
        assertEquals(beta, catalog.bossAttackTimelines(boss).get(1).pattern().id());

        AttackExecution execution = catalog.startAttack(beta, 200);
        assertEquals(beta, execution.patternId());
        assertEquals(AttackTimeline.Phase.TELEGRAPH, execution.sample(205).presentationPhase());
        assertEquals(AttackTimeline.Phase.ACTIVE, execution.sample(206).presentationPhase());
        assertTrue(execution.sample(206).mayApplyHit());
    }

    @Test
    void missingRuntimeContentFailsClosed() {
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(new ContentRegistry());
        ContentId missing = ContentId.parse("riftfrontier:missing");
        assertThrows(IllegalStateException.class, () -> catalog.startAttack(missing, 0));
        assertThrows(IllegalStateException.class, () -> catalog.bossAttackTimelines(missing));
    }
}
