package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionStartContextPreservationTest {
    private static final kr.moonseungjun.riftfrontier.content.ContentId REGION = kr.moonseungjun.riftfrontier.content.ContentId.rift("region/region_01");
    private static final kr.moonseungjun.riftfrontier.content.ContentId CONTRACT = kr.moonseungjun.riftfrontier.content.ContentId.rift("contract/region_01_salvage_recovery");
    private static final java.util.UUID OWNER = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void immutableTransitionsPreserveAuthoritativeStartContext() {
        ExpeditionStartContext context = new ExpeditionStartContext(4, 3, 3, 2, 1, 140, 1);
        ExpeditionRun preparing = ExpeditionRun.preparing(21L, REGION, CONTRACT, OWNER, "fingerprint", context, 100L);
        ExpeditionRun failed = preparing.deploy().fail(150L, ExpeditionRun.EndReason.PLAYER_ABORT);

        assertEquals(context, preparing.startContext().orElseThrow());
        assertEquals(context, failed.startContext().orElseThrow());
        assertEquals(4, failed.startContext().orElseThrow().regionPressure());
        assertEquals(3, failed.startContext().orElseThrow().preparationSupplyCost());
    }

    @Test
    void legacyRunsRemainExplicitlyContextless() {
        ExpeditionRun legacy = ExpeditionRun.preparing(22L, REGION, CONTRACT, OWNER, "legacy", 100L);
        assertTrue(legacy.startContext().isEmpty());
        assertTrue(legacy.deploy().startContext().isEmpty());
    }
}
