package dev.moonseungjun.openworldrpg.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class R01NourishmentStateTest {
    @Test
    void mealReplacesPreviousBuffAndUsesActiveWorldTimeOnly() {
        var roast = R01NourishmentState.initial().apply(
                R01NourishmentMeal.HERBED_LOUXIA_ROAST,
                1_000L,
                R01NourishmentService.DURATION_ACTIVE_TICKS
        );
        assertEquals(
                R01NourishmentMeal.HERBED_LOUXIA_ROAST,
                roast.activeMeal(24_999L).orElseThrow()
        );
        assertTrue(roast.activeMeal(25_000L).isEmpty());

        var skewers = roast.apply(
                R01NourishmentMeal.TRAIL_SKEWERS,
                2_000L,
                R01NourishmentService.DURATION_ACTIVE_TICKS
        );
        assertEquals(
                R01NourishmentMeal.TRAIL_SKEWERS,
                skewers.activeMeal(2_000L).orElseThrow()
        );
        assertEquals(26_000L, skewers.expiresAtActiveTick());
    }

    @Test
    void expirationClearsExactlyAtTwentyActiveMinutes() {
        var state = R01NourishmentState.initial().apply(
                R01NourishmentMeal.GLOW_BROTH,
                40L,
                R01NourishmentService.DURATION_ACTIVE_TICKS
        );

        assertSame(state, state.clearExpired(24_039L));
        assertEquals(
                R01NourishmentState.initial(),
                state.clearExpired(24_040L)
        );
    }

    @Test
    void nourishmentStateSurvivesCodecRoundTrip() {
        var original = R01NourishmentState.initial().apply(
                R01NourishmentMeal.GLOW_BROTH,
                500L,
                R01NourishmentService.DURATION_ACTIVE_TICKS
        );
        var encoded = R01NourishmentState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01NourishmentState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
