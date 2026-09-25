package dev.moonseungjun.openworldrpg.gathering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import org.junit.jupiter.api.Test;

class R01GatheringStateTest {
    private static final String IRON_NODE =
            "openworld_rpg:r01/old_quarry_road/iron_ore_01";
    private static final String CRYSTAL_NODE =
            "openworld_rpg:r01/rootshade_grove/verdant_crystal_01";

    @Test
    void r01DefinitionsMatchLockedYieldCooldownToolAndTiming() {
        var iron = R01GatheringRules.requireResource(R01GatheringRules.IRON_ORE);
        var hardwood = R01GatheringRules.requireResource(R01GatheringRules.HARDWOOD);
        var herb = R01GatheringRules.requireResource(R01GatheringRules.HEALING_HERB);
        var crystal = R01GatheringRules.requireResource(
                R01GatheringRules.VERDANT_CRYSTAL
        );

        assertEquals(2, iron.minBaseYield());
        assertEquals(4, iron.maxBaseYield());
        assertEquals(7L * 60L * 20L, iron.respawnActiveTicks());
        assertEquals(31, iron.baseInteractionTicks());
        assertEquals(R01GatheringRules.ToolTier.FIELD, iron.requiredToolTier());

        assertEquals(2, hardwood.minBaseYield());
        assertEquals(3, hardwood.maxBaseYield());
        assertEquals(5L * 60L * 20L, hardwood.respawnActiveTicks());
        assertEquals(27, hardwood.baseInteractionTicks());

        assertEquals(1, herb.minBaseYield());
        assertEquals(2, herb.maxBaseYield());
        assertEquals(4L * 60L * 20L, herb.respawnActiveTicks());
        assertEquals(13, herb.baseInteractionTicks());

        assertEquals(1, crystal.minBaseYield());
        assertEquals(1, crystal.maxBaseYield());
        assertEquals(18L * 60L * 20L, crystal.respawnActiveTicks());
        assertEquals(36, crystal.baseInteractionTicks());
        assertEquals(R01GatheringRules.ToolTier.FIELD, crystal.requiredToolTier());
        assertTrue(crystal.rareOrDense());
        assertFalse(crystal.dustQuestCredit());
    }

    @Test
    void personalCooldownAndFirstDiscoveryMasteryPersist() {
        R01GatheringState initial = R01GatheringState.initial();
        long start = 1_000L;

        var begun = initial.beginHarvest(
                IRON_NODE,
                R01GatheringRules.IRON_ORE,
                3,
                start
        );

        assertFalse(begun.state().isNodeAvailable(IRON_NODE, start));
        assertFalse(begun.state().isNodeAvailable(
                IRON_NODE,
                start + (7L * 60L * 20L) - 1L
        ));

        R01GatheringState finalized =
                begun.state().finalizeHarvest(begun.pending().transactionId());

        assertEquals(
                11,
                finalized.masteryXp(R01GatheringRules.GatheringDiscipline.MINING)
        );
        assertTrue(finalized.discoveryFlags().contains(
                R01GatheringRules.discoveryFlag(R01GatheringRules.IRON_ORE)
        ));
        assertTrue(finalized.isNodeAvailable(
                IRON_NODE,
                start + (7L * 60L * 20L)
        ));

        var repeat = finalized.beginHarvest(
                IRON_NODE,
                R01GatheringRules.IRON_ORE,
                2,
                start + (7L * 60L * 20L)
        );
        R01GatheringState repeatFinal =
                repeat.state().finalizeHarvest(repeat.pending().transactionId());
        assertEquals(
                12,
                repeatFinal.masteryXp(R01GatheringRules.GatheringDiscipline.MINING)
        );
    }

    @Test
    void rareCrystalAwardsThreePlusFirstDiscoveryTen() {
        var begun = R01GatheringState.initial().beginHarvest(
                CRYSTAL_NODE,
                R01GatheringRules.VERDANT_CRYSTAL,
                1,
                0L
        );
        var finalized =
                begun.state().finalizeHarvest(begun.pending().transactionId());

        assertEquals(
                13,
                finalized.masteryXp(R01GatheringRules.GatheringDiscipline.MINING)
        );
    }

    @Test
    void blockedContextAndCooldownFailClosed() {
        R01GatheringState state = R01GatheringState.initial();
        var blocked = R01GatheringAuthority.evaluate(
                state,
                IRON_NODE,
                R01GatheringRules.IRON_ORE,
                0L,
                new R01GatheringAuthority.GatherContext(
                        true, false, false, false, false
                ),
                0,
                99
        );
        assertEquals(R01GatheringAuthority.Status.ACTION_BLOCKED, blocked.status());

        var begun = state.beginHarvest(
                IRON_NODE,
                R01GatheringRules.IRON_ORE,
                2,
                0L
        );
        var cooldown = R01GatheringAuthority.evaluate(
                begun.state(),
                IRON_NODE,
                R01GatheringRules.IRON_ORE,
                1L,
                R01GatheringAuthority.GatherContext.clear(),
                0,
                99
        );
        assertEquals(R01GatheringAuthority.Status.NODE_COOLDOWN, cooldown.status());
    }

    @Test
    void materialDeliveryIsAtomicIdempotentAndReceiptCanBeCleaned() {
        PlayerInventoryState inventory = PlayerInventoryState.initial();
        String tx = "openworld_rpg:gather_delivery/r01/test/1";

        var delivered = inventory.deliverMaterialToPouchOnce(
                tx,
                R01GatheringRules.IRON_ORE,
                4
        );
        var retried = delivered.state().deliverMaterialToPouchOnce(
                tx,
                R01GatheringRules.IRON_ORE,
                4
        );

        assertEquals(
                PlayerInventoryState.MaterialDeliveryStatus.DELIVERED,
                delivered.status()
        );
        assertEquals(4, delivered.delivered());
        assertEquals(
                PlayerInventoryState.MaterialDeliveryStatus.ALREADY_COMPLETED,
                retried.status()
        );
        assertSame(delivered.state(), retried.state());
        assertEquals(
                4,
                delivered.state().materialPouch().get(R01GatheringRules.IRON_ORE)
        );

        PlayerInventoryState nearCap = PlayerInventoryState.initial()
                .addMaterialToPouch(R01GatheringRules.IRON_ORE, 998)
                .state();
        var blocked = nearCap.deliverMaterialToPouchOnce(
                "openworld_rpg:gather_delivery/r01/test/2",
                R01GatheringRules.IRON_ORE,
                2
        );
        assertEquals(
                PlayerInventoryState.MaterialDeliveryStatus.CAPACITY_BLOCKED,
                blocked.status()
        );
        assertEquals(998, blocked.state().materialPouch().get(
                R01GatheringRules.IRON_ORE
        ));

        PlayerInventoryState cleaned = delivered.state()
                .forgetCompletedDeliveryReceipt(tx);
        assertFalse(cleaned.completedDeliveryIds().contains(tx));
    }

    @Test
    void gatheringStateSurvivesCodecRoundTripWithPendingPlan() {
        var original = R01GatheringState.initial().beginHarvest(
                IRON_NODE,
                R01GatheringRules.IRON_ORE,
                4,
                250L
        ).state();

        var encoded = R01GatheringState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01GatheringState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void masteryThresholdsAndOrdinaryBonusChanceMatchCanon() {
        assertEquals(1, R01GatheringRules.masteryRankForXp(0));
        assertEquals(2, R01GatheringRules.masteryRankForXp(20));
        assertEquals(3, R01GatheringRules.masteryRankForXp(60));
        assertEquals(4, R01GatheringRules.masteryRankForXp(130));
        assertEquals(5, R01GatheringRules.masteryRankForXp(240));
        assertEquals(0, R01GatheringRules.ordinaryBonusChancePercent(2));
        assertEquals(5, R01GatheringRules.ordinaryBonusChancePercent(3));
        assertEquals(10, R01GatheringRules.ordinaryBonusChancePercent(5));
    }
}
