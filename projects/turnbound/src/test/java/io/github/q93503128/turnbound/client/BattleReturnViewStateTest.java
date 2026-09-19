package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleReturnViewStateTest {
    @Test
    void parsesAuthoritativePreBattleViewFromSnapshot() {
        ClientBattleState.update(
                "H|1|0|1|RUNNING||0|1|1|1\n"
                        + "A|120.500|70.000|240.500|35.000\n"
                        + "V|-73.250|11.500\n");

        var snapshot = ClientBattleState.snapshot();
        assertTrue(snapshot.active());
        assertEquals(-73.25F, snapshot.returnYaw(), 0.001F);
        assertEquals(11.5F, snapshot.returnPitch(), 0.001F);
    }

    @Test
    void oldSnapshotShapeRemainsExplicitlyUnknownRatherThanInventingAView() {
        ClientBattleState.update(
                "H|1|0|1|RUNNING||0|1|1|1\n"
                        + "A|120.500|70.000|240.500|35.000\n");

        var snapshot = ClientBattleState.snapshot();
        assertFalse(Float.isFinite(snapshot.returnYaw()));
        assertFalse(Float.isFinite(snapshot.returnPitch()));
    }
}
