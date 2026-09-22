package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterData;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterDataLoader;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalTimeline;
import org.junit.jupiter.api.Test;

class R01EarthloongPhysicalTimelineTest {
    @Test
    void physicalTimelinesMatchAuthoredAndAuditedDurations() {
        var bindings = R01EarthloongEncounterDataLoader.loadBundled().physicalBindingsById();
        var claw = R01EarthloongPhysicalTimeline.from(
                bindings.get(R01EarthloongEncounterData.ActionId.CLAW_SWEEP));
        assertEquals(9, claw.tellTicks());
        assertEquals(1, claw.activeTicks());
        assertEquals(10, claw.totalTicks());

        var rush = R01EarthloongPhysicalTimeline.from(
                bindings.get(R01EarthloongEncounterData.ActionId.QUARRY_RUSH));
        assertEquals(16, rush.tellTicks());
        assertEquals(6, rush.activeTicks());
        assertEquals(18, rush.recoveryTicks());
        assertEquals(40, rush.totalTicks());
    }
}
