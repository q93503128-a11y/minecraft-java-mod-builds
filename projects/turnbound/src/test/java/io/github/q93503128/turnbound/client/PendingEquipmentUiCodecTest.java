package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.MetaUiCodec;
import io.github.q93503128.turnbound.world.MetaUiSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PendingEquipmentUiCodecTest {
    @Test
    void pendingRewardAuthorityReachesClient() {
        var pending = new MetaUiSnapshot.PendingEquipmentRow("eq_301", "W05", "결투자의 문장", "T3", "WEAPON",
                15_000, false, true);
        var snapshot = new MetaUiSnapshot(0,0,0,0,0,false,0,false, List.of(),List.of(),List.of(),List.of(),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(pending));

        ClientMetaState.update(MetaUiCodec.encode(snapshot));
        var row = ClientMetaState.snapshot().pendingEquipment().getFirst();
        assertEquals("eq_301", row.instanceId());
        assertEquals(15_000, row.salePrice());
        assertFalse(row.claimable());
        assertTrue(row.immediateSellable());
    }
}
