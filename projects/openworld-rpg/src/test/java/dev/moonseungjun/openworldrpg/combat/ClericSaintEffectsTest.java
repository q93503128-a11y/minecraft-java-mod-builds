package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.runtime.ClericSaintEffects;
import org.junit.jupiter.api.Test;

class ClericSaintEffectsTest {
    @Test
    void saintNumericContractsMatchCanonicalPassiveRanks() {
        assertEquals(
                1.09,
                ClericSaintEffects.healingOutputMultiplierForRank(3),
                0.0001
        );
        assertEquals(
                0.09,
                ClericSaintEffects.barrierOutputBonusForRank(3),
                0.0001
        );
        assertEquals(
                4.0,
                ClericSaintEffects.overhealBarrierAmountForRanks(
                        20.0,
                        100.0,
                        0,
                        0
                ),
                0.0001
        );
        assertEquals(
                12.0,
                ClericSaintEffects.overhealBarrierAmountForRanks(
                        100.0,
                        100.0,
                        3,
                        3
                ),
                0.0001
        );
        assertEquals(2, ClericSaintEffects.BENEDICTION_SUPPORT_PIPS);
    }

    @Test
    void invalidSaintRanksFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ClericSaintEffects
                        .healingOutputMultiplierForRank(4)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ClericSaintEffects
                        .overhealBarrierAmountForRanks(
                                10.0,
                                100.0,
                                -1,
                                0
                        )
        );
    }
}
