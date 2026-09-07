package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionStartContextTest {
    @Test
    void validatesAndReportsPlannedThreatTotal() {
        ExpeditionStartContext context = new ExpeditionStartContext(6, 3, 4, 3, 1, 160, 2);
        assertEquals(8, context.plannedThreats());
        assertEquals(6, context.regionPressure());
        assertEquals(3, context.preparationSupplyCost());
    }

    @Test
    void rejectsImpossibleStartFacts() {
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionStartContext(-1, 1, 1, 1, 1, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionStartContext(0, 0, 1, 1, 1, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionStartContext(0, 1, -1, 1, 1, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionStartContext(0, 1, 1, 1, 1, -1, 0));
    }
}
