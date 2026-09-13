package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossFieldImpactProfileTest {
    @Test
    void authoredBossRolesHaveDistinctFieldCalibrationShapesAndPhysicalResponses() {
        var committed = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.COMMITTED_STRIKE).orElseThrow();
        var line = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.LINE_DISPLACEMENT).orElseThrow();
        var area = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.ARENA_PRESSURE).orElseThrow();

        assertEquals(Region01BossFieldImpactProfile.Shape.FORWARD_ARC, committed.shape());
        assertEquals(Region01BossFieldImpactProfile.Shape.FORWARD_LANE, line.shape());
        assertEquals(Region01BossFieldImpactProfile.Shape.LOCAL_AREA, area.shape());
        assertTrue(committed.reach() < line.reach(), "line displacement must own the longer forward lane");
        assertTrue(committed.halfWidth() > line.halfWidth(), "committed strike must remain broader than the line attack");
        assertTrue(area.reach() > committed.reach(), "arena pressure must threaten a visibly larger local area");

        assertEquals(0.0D, committed.activeForwardStep(), "committed strike must not inherit line-charge travel");
        assertTrue(line.activeForwardStep() > 0.0D, "authored line_charge role must visibly travel during ACTIVE");
        assertTrue(line.activeForwardStep() < line.reach(), "one charge step must remain inside the threat lane calibration");
        assertEquals(0.0D, area.activeForwardStep(), "arena pressure must remain facing-independent local pressure");

        assertEquals(0.0D, committed.activeEntryRadialImpulse(), "committed strike must not inherit arena displacement");
        assertEquals(0.0D, line.activeEntryRadialImpulse(), "line displacement must not inherit arena displacement");
        assertTrue(area.activeEntryRadialImpulse() > 0.0D, "arena pressure must expose its provisional outward displacement");
    }

    @Test
    void compatibilityConstructorsOptOutOfNewPhysicalAxesRatherThanInventingValues() {
        var geometryOnly = new Region01BossFieldImpactProfile.Profile(
            Region01BossFieldImpactProfile.Shape.FORWARD_ARC, 2.0D, 1.0D, 1.0D
        );
        var travelOnly = new Region01BossFieldImpactProfile.Profile(
            Region01BossFieldImpactProfile.Shape.FORWARD_LANE, 4.0D, 1.0D, 1.0D, 0.25D
        );

        assertEquals(0.0D, geometryOnly.activeForwardStep());
        assertEquals(0.0D, geometryOnly.activeEntryRadialImpulse());
        assertEquals(0.25D, travelOnly.activeForwardStep());
        assertEquals(0.0D, travelOnly.activeEntryRadialImpulse());
    }

    @Test
    void radialImpulseCannotBeAuthoredOnDirectionalFieldShapes() {
        assertThrows(IllegalArgumentException.class, () -> new Region01BossFieldImpactProfile.Profile(
            Region01BossFieldImpactProfile.Shape.FORWARD_LANE,
            4.0D,
            1.0D,
            1.0D,
            0.0D,
            0.5D
        ));
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
