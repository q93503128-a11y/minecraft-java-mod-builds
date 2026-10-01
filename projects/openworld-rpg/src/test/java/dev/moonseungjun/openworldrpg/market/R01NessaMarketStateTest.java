package dev.moonseungjun.openworldrpg.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01NessaMarketStateTest {
    @Test
    void soldCommitIsIdempotentForTheExactDeterministicPurchase() {
        String playerId = UUID.randomUUID().toString();
        var state = R01NessaMarketState.initial()
                .ensureCycle(987654321L, playerId, 3L);
        var plan = state.purchasePlan(playerId, 3L, 1)
                .orElseThrow();

        var sold = state.markSold(
                playerId,
                plan.cycleIndex(),
                plan.slotIndex(),
                plan.transactionId()
        );
        var retry = sold.markSold(
                playerId,
                plan.cycleIndex(),
                plan.slotIndex(),
                plan.transactionId()
        );

        assertTrue(
                sold.currentCycle().orElseThrow()
                        .soldSlots().contains(1)
        );
        assertTrue(sold.purchasePlan(playerId, 3L, 1).isEmpty());
        assertSame(sold, retry);
    }

    @Test
    void purchaseIdentityIsStableAcrossRetriesAndDoesNotDependOnDisplayData() {
        String playerId = UUID.randomUUID().toString();
        var state = R01NessaMarketState.initial()
                .ensureCycle(1234L, playerId, 7L);

        var first = state.purchasePlan(playerId, 7L, 4)
                .orElseThrow();
        var second = state.purchasePlan(playerId, 7L, 4)
                .orElseThrow();

        assertEquals(first, second);
        assertEquals(
                R01NessaMarketState.transactionId(
                        playerId,
                        7L,
                        4
                ),
                first.transactionId()
        );
    }
}
