package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.MetaUiCodec;
import io.github.q93503128.turnbound.world.MetaUiSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetaEquipmentSaleCodecTest {
    @Test
    void equipmentSaleAuthorityReachesClientWithoutClientPriceDuplication() {
        var normal = new MetaUiSnapshot.EquipmentRow("eq_1", "W05", "결투자의 문장", "T3", "WEAPON", 7, "",
                "ATK_PCT", 0.10, "SPD_FLAT", 3.0, 0.162, 6.0, 15_000, true);
        var equipped = new MetaUiSnapshot.EquipmentRow("eq_2", "A01", "훈련 방어각인", "T1", "ARMOR", 0, "P01",
                "HP_PCT", 0.06, "DEF_PCT", 0.03, 0.108, 0.06, 2_000, false);
        var signature = new MetaUiSnapshot.EquipmentRow("eq_3", "sig_p01_unending_vow", "끝나지 않는 서약", "SIGNATURE", "SIGNATURE", 0, "",
                "ATK_PCT", 0.12, "SPD_FLAT", 4.0, 0.216, 8.0, 0, false);
        var snapshot = new MetaUiSnapshot(0,0,0,0,0,false,0,false, List.of(),List.of(),List.of(),
                List.of(normal,equipped,signature),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());

        ClientMetaState.update(MetaUiCodec.encode(snapshot));
        var rows = ClientMetaState.snapshot().equipment();
        assertEquals(3, rows.size());
        assertEquals(15_000, rows.get(0).salePrice());
        assertTrue(rows.get(0).sellable());
        assertEquals(2_000, rows.get(1).salePrice());
        assertFalse(rows.get(1).sellable());
        assertEquals(0, rows.get(2).salePrice());
        assertFalse(rows.get(2).sellable());
    }
}
