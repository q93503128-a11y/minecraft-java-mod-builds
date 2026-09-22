package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongThreatTable;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01EarthloongThreatTableTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void damageThreatAndHysteresisFollowCanon() {
        var table = new R01EarthloongThreatTable();
        table.engageInitial(A, 0);
        table.addThreat(A, 10.0, 0);
        assertEquals(A, table.selectTarget(List.of(A), 0).orElseThrow());
        table.engageInitial(B, 0);
        table.addThreat(B, 14.0, 0);
        assertEquals(A, table.selectTarget(List.of(A, B), 0).orElseThrow());
        table.addThreat(B, 2.0, 0);
        assertEquals(B, table.selectTarget(List.of(A, B), 0).orElseThrow());
    }

    @Test
    void threatDecayStartsAfterSixSecondGraceAndFloorsAtTen() {
        var table = new R01EarthloongThreatTable();
        table.engageInitial(A, 0);
        table.addThreat(A, 10.0, 0);
        assertEquals(20.0, table.threatOf(A, 120), 0.000001);
        assertEquals(18.0, table.threatOf(A, 140), 0.000001);
        assertEquals(16.2, table.threatOf(A, 160), 0.000001);
        assertEquals(10.0, table.threatOf(A, 2000), 0.000001);
    }
}
