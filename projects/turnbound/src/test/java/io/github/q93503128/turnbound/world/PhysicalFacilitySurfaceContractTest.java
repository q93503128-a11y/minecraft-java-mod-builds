package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Physical economy/service actions must never become ordinary global-menu commands. */
class PhysicalFacilitySurfaceContractTest {
    @Test
    void economyMutationsRemainBoundToPhysicalFacilities() {
        assertEquals("SUMMON", MetaFacilityActionGate.requiredFacilityHint("SUMMON10"));
        assertEquals("SUMMON", MetaFacilityActionGate.requiredFacilityHint("ESSENCE_CRYSTAL"));
        assertEquals("MARKET", MetaFacilityActionGate.requiredFacilityHint("BUY|EQ_T1_01"));
        assertEquals("MARKET", MetaFacilityActionGate.requiredFacilityHint("SELL|instance"));
        assertEquals("FORGE", MetaFacilityActionGate.requiredFacilityHint("ENHANCE|instance"));

        for (String management : new String[]{"OPEN", "SYNC", "PARTY|P01,P02", "EQUIP|P01|instance", "PRESET_LOAD|1"}) {
            assertTrue(MetaFacilityActionGate.requiredFacilityHint(management).isBlank(), management);
        }
    }
}
