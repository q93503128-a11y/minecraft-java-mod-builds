package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentPackLoader;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductionRegion01BossPhaseSemanticsTest {
    private static final String REGION_RESOURCE = "/data/riftfrontier/riftfrontier/content/region_01.json";
    private static final ContentId BOSS = ContentId.parse("riftfrontier:boss/region_01_first_apex");
    private static final ContentId COMMITTED = ContentId.parse("riftfrontier:attack/boss/region_01_committed_strike");
    private static final ContentId LINE = ContentId.parse("riftfrontier:attack/boss/region_01_line_displacement");
    private static final ContentId PRESSURE = ContentId.parse("riftfrontier:attack/boss/region_01_arena_pressure");

    @Test
    void packagedPhasePolicyUsesOnlyPublishedBossAttacksAndAddsArenaPressureInPhaseTwo() throws Exception {
        BossCombatSemanticProfile semantics = Region01BossProductionSemantics.load();

        assertEquals(BOSS, semantics.bossProfile());
        assertEquals(Set.of(COMMITTED, LINE), semantics.attacksForPhase(1));
        assertEquals(Set.of(COMMITTED, LINE, PRESSURE), semantics.attacksForPhase(2));
        assertNotEquals(semantics.attacksForPhase(1), semantics.attacksForPhase(2),
            "phase change must alter attack composition rather than only multipliers");

        var stream = ProductionRegion01BossPhaseSemanticsTest.class.getResourceAsStream(REGION_RESOURCE);
        assertNotNull(stream, "production Region 01 pack must be packaged");

        ContentPackLoader.LoadedPack pack;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            pack = new ContentPackLoader().load(reader, REGION_RESOURCE);
        }
        pack.requireValid();

        var boss = (CoreDefinition.BossProfile) pack.registry()
            .find(CoreDefinition.Kind.BOSS_PROFILE, BOSS).orElseThrow();
        assertEquals(2, boss.phaseCount());
        assertEquals(boss.attackPatterns(), semantics.attacksForPhase(2));
        semantics.phaseAttackPatterns().forEach((phase, attacks) ->
            assertTrue(boss.attackPatterns().containsAll(attacks), "phase " + phase + " contains attack outside production boss profile"));
    }
}
