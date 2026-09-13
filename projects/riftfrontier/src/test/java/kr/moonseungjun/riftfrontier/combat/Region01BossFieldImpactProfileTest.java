package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossFieldImpactProfileTest {
    @Test
    void authoredBossRolesHaveDistinctFieldCalibrationShapes() {
        var committed = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.COMMITTED_STRIKE).orElseThrow();
        var line = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.LINE_DISPLACEMENT).orElseThrow();
        var area = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.ARENA_PRESSURE).orElseThrow();

        assertEquals(Region01BossFieldImpactProfile.Shape.FORWARD_ARC, committed.shape());
        assertEquals(Region01BossFieldImpactProfile.Shape.FORWARD_LANE, line.shape());
        assertEquals(Region01BossFieldImpactProfile.Shape.LOCAL_AREA, area.shape());
        assertTrue(committed.reach() < line.reach(), "line displacement must own the longer forward lane");
        assertTrue(committed.halfWidth() > line.halfWidth(), "committed strike must remain broader than the line attack");
        assertTrue(area.reach() > committed.reach(), "arena pressure must threaten a visibly larger local area");
    }

    @Test
    void profileIdsMatchCanonicalRegion01ProductionSemantics() {
        var semantics = Region01BossProductionSemantics.load();
        assertTrue(semantics.phaseAttackPatterns().get(1).contains(Region01BossFieldImpactProfile.COMMITTED_STRIKE));
        assertTrue(semantics.phaseAttackPatterns().get(1).contains(Region01BossFieldImpactProfile.LINE_DISPLACEMENT));
        assertTrue(semantics.phaseAttackPatterns().get(2).contains(Region01BossFieldImpactProfile.ARENA_PRESSURE));
    }

    @Test
    void unrelatedAttackHasNoBossFieldImpactProfile() {
        assertTrue(Region01BossFieldImpactProfile.find(
            ContentId.parse("riftfrontier:attack/player/mobile_pressure_entry")
        ).isEmpty());
    }
}
