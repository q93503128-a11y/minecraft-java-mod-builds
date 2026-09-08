package kr.moonseungjun.riftfrontier.content;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class CombatDefinitionTest {
    private final ContentDocumentCodec codec = new ContentDocumentCodec();

    @Test
    void decodesAttackPatternCadenceAndCounterplay() {
        CoreDefinition.AttackPattern pattern = (CoreDefinition.AttackPattern) codec.decode("""
            {"kind":"attack_pattern","id":"riftfrontier:boss_sweep","delivery":"arc_melee","telegraph_ticks":18,"active_ticks":6,"recovery_ticks":14,"counterplay":["backstep","guard"],"presentation_cue":"full_body_windup"}
            """);
        assertEquals(38, pattern.totalTicks());
        assertEquals(Set.of("backstep", "guard"), pattern.counterplay());
        assertEquals(CoreDefinition.Kind.ATTACK_PATTERN, pattern.kind());
    }

    @Test
    void bossProfileRequiresResolvableAttackPatterns() {
        ContentRegistry registry = new ContentRegistry();
        ContentId bossId = ContentId.parse("riftfrontier:region_01_apex");
        ContentId patternId = ContentId.parse("riftfrontier:boss_sweep");
        registry.register(new CoreDefinition.BossProfile(bossId, 2, Set.of(patternId), "keep_center_lane_open"));
        ContentValidator.Report report = new ContentValidator().validate(registry);
        assertTrue(report.hasErrors());
        assertEquals(1, report.byCode(ContentValidator.Code.MISSING_REFERENCE).size());

        registry.register(new CoreDefinition.AttackPattern(patternId, "arc_melee", 18, 6, 14, Set.of("backstep"), "full_body_windup"));
        assertFalse(new ContentValidator().validate(registry).hasErrors());
    }

    @Test
    void attackPatternRejectsMissingCounterplayAndInvalidTiming() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode("""
            {"kind":"attack_pattern","id":"riftfrontier:bad","delivery":"slam","telegraph_ticks":0,"active_ticks":4,"recovery_ticks":8,"counterplay":["dodge"],"presentation_cue":"raise_arm"}
            """));

        ContentRegistry registry = new ContentRegistry();
        ContentId id = ContentId.parse("riftfrontier:no_counterplay");
        registry.register(new CoreDefinition.AttackPattern(id, "slam", 10, 4, 8, Set.of(), "raise_arm"));
        ContentValidator.Report report = new ContentValidator().validate(registry);
        assertEquals(1, report.byCode(ContentValidator.Code.EMPTY_ATTACK_COUNTERPLAY).size());
    }
}
