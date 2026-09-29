package dev.moonseungjun.openworldrpg.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PlayerCurrencyStateTest {
    @Test
    void oneTimeCreditIsIdempotentByStableTransactionId() {
        var initial = PlayerCurrencyState.initial();
        var paid = initial.creditOnce(
                "openworld_rpg:r01/dust_on_quarry_road/gold",
                90
        );
        var retried = paid.creditOnce(
                "openworld_rpg:r01/dust_on_quarry_road/gold",
                90
        );

        assertEquals(90L, paid.gold());
        assertSame(paid, retried);
        assertTrue(paid.hasAppliedCredit(
                "openworld_rpg:r01/dust_on_quarry_road/gold"
        ));
    }

    @Test
    void distinctTransactionsAccumulateWithoutUsingDisplayTextAsIdentity() {
        var state = PlayerCurrencyState.initial()
                .creditOnce("openworld_rpg:r01/dust_on_quarry_road/gold", 90)
                .creditOnce("openworld_rpg:r01/earthloong_first_clear/gold", 180);

        assertEquals(270L, state.gold());
        assertEquals(2, state.appliedCreditTransactionIds().size());
    }

    @Test
    void invalidCreditsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerCurrencyState.initial().creditOnce("not_namespaced", 10)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerCurrencyState.initial().creditOnce("openworld_rpg:test", 0)
        );

        var maxed = new PlayerCurrencyState(1, Long.MAX_VALUE, java.util.Set.of());
        assertThrows(
                ArithmeticException.class,
                () -> maxed.creditOnce("openworld_rpg:overflow", 1)
        );
    }

    @Test
    void deathDebitMayCreateDebtWithoutGrantingPurchasePower() {
        var funded = PlayerCurrencyState.initial()
                .creditOnce("openworld_rpg:test/start", 20);
        var death = funded.debitIntoDebtOnce(
                "openworld_rpg:death_penalty/1/gold",
                100
        );
        var retry = death.state().debitIntoDebtOnce(
                "openworld_rpg:death_penalty/1/gold",
                100
        );
        var purchase = death.state().debitOnce(
                "openworld_rpg:test/purchase",
                1
        );

        assertEquals(-80L, death.state().gold());
        assertEquals(
                PlayerCurrencyState.DebitStatus.APPLIED,
                death.status()
        );
        assertEquals(
                PlayerCurrencyState.DebitStatus.ALREADY_APPLIED,
                retry.status()
        );
        assertEquals(-80L, retry.state().gold());
        assertEquals(
                PlayerCurrencyState.DebitStatus.INSUFFICIENT_GOLD,
                purchase.status()
        );

        var paidDown = death.state().creditOnce(
                "openworld_rpg:test/income",
                100
        );
        assertEquals(20L, paidDown.gold());
    }

    @Test
    void negativeGoldSurvivesCodecRoundTrip() {
        var original = PlayerCurrencyState.initial()
                .debitIntoDebtOnce(
                        "openworld_rpg:death_penalty/1/gold",
                        40
                )
                .state();

        var encoded = PlayerCurrencyState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerCurrencyState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertEquals(-40L, decoded.gold());
    }

    @Test
    void repeatableReceiptCleanupKeepsCreditedGold() {
        var credited = PlayerCurrencyState.initial()
                .creditOnce("openworld_rpg:r01/roadside_trouble/reward/3/gold", 20);
        var cleaned = credited.forgetCreditTransaction(
                "openworld_rpg:r01/roadside_trouble/reward/3/gold"
        );

        assertEquals(20L, cleaned.gold());
        assertTrue(cleaned.appliedCreditTransactionIds().isEmpty());
    }

    @Test
    void currencySurvivesCodecRoundTrip() {
        var original = PlayerCurrencyState.initial()
                .creditOnce("openworld_rpg:r01/dust_on_quarry_road/gold", 90);

        var encoded = PlayerCurrencyState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerCurrencyState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
