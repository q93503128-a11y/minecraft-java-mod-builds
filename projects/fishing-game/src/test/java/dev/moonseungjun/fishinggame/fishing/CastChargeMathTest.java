package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastChargeMathTest {
    @Test
    void chargeIsClampedAtFull() {
        assertEquals(0.0f, CastChargeMath.normalizedCharge(0));
        assertEquals(1.0f, CastChargeMath.normalizedCharge(CastChargeMath.FULL_CHARGE_TICKS));
        assertEquals(1.0f, CastChargeMath.normalizedCharge(CastChargeMath.FULL_CHARGE_TICKS * 4L));
    }

    @Test
    void longerChargeThrowsFartherWithoutChangingCatchOdds() {
        float tap = CastChargeMath.speedMultiplier(1);
        float half = CastChargeMath.speedMultiplier(CastChargeMath.FULL_CHARGE_TICKS / 2L);
        float full = CastChargeMath.speedMultiplier(CastChargeMath.FULL_CHARGE_TICKS);

        assertTrue(tap >= CastChargeMath.MIN_SPEED_MULTIPLIER);
        assertTrue(half > tap);
        assertTrue(full > half);
        assertEquals(CastChargeMath.MAX_SPEED_MULTIPLIER, full);
    }

    @Test
    void overchargingDoesNotIncreaseThrowSpeed() {
        assertEquals(
                CastChargeMath.MAX_SPEED_MULTIPLIER,
                CastChargeMath.speedMultiplier(CastChargeMath.FULL_CHARGE_TICKS * 10L)
        );
    }
}
