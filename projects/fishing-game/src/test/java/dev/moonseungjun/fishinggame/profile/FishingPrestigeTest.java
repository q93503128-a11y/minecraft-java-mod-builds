package dev.moonseungjun.fishinggame.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.moonseungjun.fishinggame.progression.FishingRods;
import org.junit.jupiter.api.Test;

class FishingPrestigeTest {
    @Test
    void rebirthRequiresMaxRodEmptyBagAndEnoughCoins() {
        PlayerFishingProfile notReady = new PlayerFishingProfile(20_000, 1, List.of(), List.of(), 0);
        assertFalse(FishingPrestige.canRebirth(notReady));

        PlayerFishingProfile bagNotEmpty = new PlayerFishingProfile(
                20_000,
                FishingRods.maxTier(),
                List.of(new CatchEntry("bluegill", 400, 200, 100)),
                List.of(),
                0
        );
        assertFalse(FishingPrestige.canRebirth(bagNotEmpty));

        PlayerFishingProfile shortOnCoins = new PlayerFishingProfile(
                FishingPrestige.BASE_REBIRTH_COST - 1,
                FishingRods.maxTier(),
                List.of(),
                List.of(),
                0
        );
        assertFalse(FishingPrestige.canRebirth(shortOnCoins));

        PlayerFishingProfile ready = new PlayerFishingProfile(
                FishingPrestige.BASE_REBIRTH_COST,
                FishingRods.maxTier(),
                List.of(),
                List.of(),
                0
        );
        assertTrue(FishingPrestige.canRebirth(ready));
    }

    @Test
    void rebirthPreservesCollectionAndAcceleratesTheNextCycle() {
        PlayerFishingProfile discovered = PlayerFishingProfile.empty()
                .addCatch(new CatchEntry("bluegill", 420, 210, 100))
                .sellAll();
        PlayerFishingProfile ready = new PlayerFishingProfile(
                FishingPrestige.BASE_REBIRTH_COST,
                FishingRods.maxTier(),
                List.of(),
                discovered.records(),
                0
        );

        PlayerFishingProfile reborn = ready.rebirth();
        assertEquals(0, reborn.coins());
        assertEquals(0, reborn.rodTier());
        assertEquals(1, reborn.rebirths());
        assertEquals(1, reborn.records().size());
        assertTrue(reborn.catches().isEmpty());
        assertEquals(1.30, reborn.saleMultiplier(), 0.0001);

        PlayerFishingProfile caughtAgain = reborn.addCatch(new CatchEntry("bluegill", 500, 230, 100));
        assertEquals(130, caughtAgain.bagValue());
        assertEquals(2, caughtAgain.recordFor("bluegill").orElseThrow().caughtCount());
    }

    @Test
    void laterRebirthsIncreaseCostButKeepPermanentSaleGrowth() {
        assertEquals(10_000, FishingPrestige.nextCost(0));
        assertEquals(11_500, FishingPrestige.nextCost(1));
        assertEquals(1.60, FishingPrestige.saleMultiplier(2), 0.0001);
        assertEquals(160, FishingPrestige.boostedSaleValue(100, 2));
    }
}
