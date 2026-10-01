package dev.moonseungjun.openworldrpg.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01FixedMerchantStateTest {
    @Test
    void purchaseSerialIsMonotonicAndPendingIdentitySurvivesRetry() {
        String playerId = UUID.randomUUID().toString();
        var initial = R01FixedMerchantState.initial();

        var first = initial.begin(
                playerId,
                R01FixedMerchantService.Offer.HEALING_POTION.id()
        );
        var repeated = first.state().begin(
                playerId,
                R01FixedMerchantService.Offer.FOCUS_DRAUGHT.id()
        );

        assertTrue(first.created());
        assertFalse(repeated.created());
        assertEquals(first.purchase(), repeated.purchase());
        assertEquals(1L, first.state().nextPurchaseSerial());

        var cleared = first.state().clearPending(
                first.purchase().transactionId()
        );
        var second = cleared.begin(
                playerId,
                R01FixedMerchantService.Offer.FOCUS_DRAUGHT.id()
        );
        assertTrue(second.created());
        assertEquals(2L, second.state().nextPurchaseSerial());
        assertFalse(
                first.purchase().transactionId().equals(
                        second.purchase().transactionId()
                )
        );
    }

    @Test
    void fixedMerchantStateSurvivesCodecRoundTrip() {
        String playerId = UUID.randomUUID().toString();
        var original = R01FixedMerchantState.initial()
                .begin(
                        playerId,
                        R01FixedMerchantService.Offer.GLOW_BROTH.id()
                )
                .state();

        var encoded = R01FixedMerchantState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01FixedMerchantState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void canonicalFixedOffersPreservePricesAndStackCategories() {
        assertEquals(
                30L,
                R01FixedMerchantService.Offer.HEALING_POTION.priceGold()
        );
        assertEquals(
                35L,
                R01FixedMerchantService.Offer.FOCUS_DRAUGHT.priceGold()
        );
        assertEquals(
                40L,
                R01FixedMerchantService.Offer.CLEANSING_TONIC.priceGold()
        );
        assertEquals(
                25L,
                R01FixedMerchantService.Offer.HERBED_LOUXIA_ROAST.priceGold()
        );
        assertEquals(
                20L,
                R01FixedMerchantService.Offer.TRAIL_SKEWERS.priceGold()
        );
        assertEquals(
                25L,
                R01FixedMerchantService.Offer.GLOW_BROTH.priceGold()
        );
        assertEquals(
                20,
                R01FixedMerchantService.Offer.HEALING_POTION
                        .item().stackCap()
        );
        assertEquals(
                50,
                R01FixedMerchantService.Offer.GLOW_BROTH
                        .item().stackCap()
        );
    }
}
