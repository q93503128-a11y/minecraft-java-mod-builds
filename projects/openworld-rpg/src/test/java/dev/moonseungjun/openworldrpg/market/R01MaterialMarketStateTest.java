package dev.moonseungjun.openworldrpg.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01MaterialMarketStateTest {
    @Test
    void canonMaterialSellValuesAndSignatureRulesStayLocked() {
        assertEquals(
                5L,
                R01MaterialMarketRules.sellValue(
                        "openworld_rpg:iron_ore"
                ).orElseThrow()
        );
        assertEquals(
                24L,
                R01MaterialMarketRules.sellValue(
                        "openworld_rpg:verdant_crystal"
                ).orElseThrow()
        );
        assertEquals(
                45L,
                R01MaterialMarketRules.sellValue(
                        R01MaterialMarketRules.EARTHLOONG_SCALE
                ).orElseThrow()
        );
        assertTrue(R01MaterialMarketRules.requiresSignatureConfirmation(
                R01MaterialMarketRules.REGALHART_ANTLER
        ));
        assertFalse(R01MaterialMarketRules.bulkEligible(
                R01MaterialMarketRules.EARTHLOONG_SCALE
        ));
        assertTrue(R01MaterialMarketRules.bulkEligible(
                "openworld_rpg:healing_herb"
        ));
    }

    @Test
    void pendingMaterialSaleIsStableAndCodecRoundTrips() {
        String playerId = UUID.randomUUID().toString();
        var begin = R01MaterialMarketState.initial().begin(
                playerId,
                Map.of(
                        "openworld_rpg:iron_ore", 2,
                        "openworld_rpg:hardwood", 3
                ),
                22L,
                true
        );
        var retry = begin.state().begin(
                playerId,
                Map.of("openworld_rpg:healing_herb", 1),
                5L,
                false
        );

        assertTrue(begin.created());
        assertFalse(retry.created());
        assertEquals(begin.sale(), retry.sale());

        var encoded = R01MaterialMarketState.CODEC
                .encodeStart(JsonOps.INSTANCE, begin.state())
                .getOrThrow();
        var decoded = R01MaterialMarketState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(begin.state(), decoded);

        var cleared = begin.state().clearPending(
                begin.sale().transactionId()
        );
        assertTrue(cleared.pending().isEmpty());
        assertEquals(1L, cleared.nextSaleSerial());
    }
}
