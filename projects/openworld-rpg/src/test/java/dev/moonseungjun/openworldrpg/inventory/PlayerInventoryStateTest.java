package dev.moonseungjun.openworldrpg.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerInventoryStateTest {
    @Test
    void ordinaryPurchaseDeliveryRequiresBackpackSpaceAndIsIdempotent() {
        var item = ProjectInventoryItem.ordinary(
                "openworld_rpg:merchant_item",
                1,
                1,
                25
        );
        var initial = PlayerInventoryState.initial();

        assertTrue(initial.canAcceptInBackpack(item));
        var delivered = initial.deliverBackpackOnce(
                "openworld_rpg:nessa_purchase/test/0/1",
                item
        );
        var retried = delivered.state().deliverBackpackOnce(
                "openworld_rpg:nessa_purchase/test/0/1",
                item
        );

        assertEquals(
                PlayerInventoryState.BackpackDeliveryStatus.DELIVERED,
                delivered.status()
        );
        assertEquals(1, delivered.state().backpack().usedSlots());
        assertTrue(delivered.state().personalStorage().occupied().isEmpty());
        assertTrue(delivered.state().pendingItemRewards().isEmpty());
        assertEquals(
                PlayerInventoryState.BackpackDeliveryStatus.ALREADY_COMPLETED,
                retried.status()
        );
        assertSame(delivered.state(), retried.state());
    }

    @Test
    void ordinaryPurchaseDoesNotSpillIntoStorageWhenBackpackIsFull() {
        ProjectBackpackState fullBackpack = fullGrid(
                "openworld_rpg:merchant_full_"
        );
        var full = new PlayerInventoryState(
                1,
                fullBackpack,
                ProjectBackpackState.personalStorage(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Set.of()
        );
        var item = ProjectInventoryItem.ordinary(
                "openworld_rpg:merchant_item",
                1,
                1,
                25
        );

        assertFalse(full.canAcceptInBackpack(item));
        var blocked = full.deliverBackpackOnce(
                "openworld_rpg:nessa_purchase/test/0/2",
                item
        );

        assertEquals(
                PlayerInventoryState.BackpackDeliveryStatus.FULL,
                blocked.status()
        );
        assertSame(full, blocked.state());
        assertTrue(blocked.state().personalStorage().occupied().isEmpty());
        assertTrue(blocked.state().pendingItemRewards().isEmpty());
        assertFalse(blocked.state().deliveryCompleted(
                "openworld_rpg:nessa_purchase/test/0/2"
        ));
    }

    @Test
    void importantDeliveryUsesBackpackFirstAndIsIdempotent() {
        var item = ProjectInventoryItem.ordinary(
                "openworld_rpg:quest_reward",
                1,
                1,
                0
        );

        var delivered = PlayerInventoryState.initial()
                .deliverImportantOnce("openworld_rpg:r01/test_reward", item);
        var retried = delivered.state()
                .deliverImportantOnce("openworld_rpg:r01/test_reward", item);

        assertEquals(PlayerInventoryState.DeliveryStatus.DELIVERED, delivered.status());
        assertTrue(delivered.state().deliveryCompleted("openworld_rpg:r01/test_reward"));
        assertEquals(1, delivered.state().backpack().usedSlots());
        assertEquals(PlayerInventoryState.DeliveryStatus.ALREADY_COMPLETED, retried.status());
        assertSame(delivered.state(), retried.state());
    }

    @Test
    void importantRewardFallsBackToStorageThenPendingClaimWithoutLoss() {
        ProjectBackpackState fullBackpack = fullGrid("openworld_rpg:backpack_");
        ProjectBackpackState fullStorage = fullGrid("openworld_rpg:storage_");
        var full = new PlayerInventoryState(
                1,
                fullBackpack,
                fullStorage,
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Set.of()
        );
        var reward = ProjectInventoryItem.ordinary(
                "openworld_rpg:guaranteed_reward",
                1,
                1,
                0
        );

        var pending = full.deliverImportantOnce(
                "openworld_rpg:r01/guaranteed_reward",
                reward
        );

        assertEquals(PlayerInventoryState.DeliveryStatus.PENDING, pending.status());
        assertTrue(pending.state().pendingReward(
                "openworld_rpg:r01/guaranteed_reward"
        ).isPresent());
        assertFalse(pending.state().deliveryCompleted(
                "openworld_rpg:r01/guaranteed_reward"
        ));

        List<ProjectBackpackState.SlotEntry> freedEntries =
                pending.state().personalStorage().occupied().stream()
                        .filter(entry -> entry.slot() != 0)
                        .toList();
        ProjectBackpackState freedStorage = new ProjectBackpackState(4, freedEntries);
        PlayerInventoryState freed = new PlayerInventoryState(
                1,
                pending.state().backpack(),
                freedStorage,
                pending.state().materialPouch(),
                pending.state().materialVault(),
                pending.state().keyItems(),
                pending.state().pendingItemRewards(),
                pending.state().completedDeliveryIds()
        );

        var claimed = freed.claimPending("openworld_rpg:r01/guaranteed_reward");
        assertEquals(PlayerInventoryState.DeliveryStatus.DELIVERED, claimed.status());
        assertTrue(claimed.state().deliveryCompleted(
                "openworld_rpg:r01/guaranteed_reward"
        ));
        assertTrue(claimed.state().pendingReward(
                "openworld_rpg:r01/guaranteed_reward"
        ).isEmpty());
    }

    @Test
    void materialPouchCapsAt999AndSettlementConsumesPouchBeforeVaultAtomically() {
        var inserted = PlayerInventoryState.initial()
                .addMaterialToPouch("openworld_rpg:iron_ore", 1_050);

        assertEquals(999, inserted.inserted());
        assertEquals(51, inserted.overflow());

        var withVault = new PlayerInventoryState(
                1,
                inserted.state().backpack(),
                inserted.state().personalStorage(),
                Map.of("openworld_rpg:iron_ore", 4),
                Map.of("openworld_rpg:iron_ore", 10),
                Set.of(),
                Map.of(),
                Set.of()
        );

        var fieldFail = withVault.consumeMaterial(
                "openworld_rpg:iron_ore",
                6,
                false
        );
        assertFalse(fieldFail.consumed());
        assertEquals(withVault, fieldFail.state());

        var town = withVault.consumeMaterial(
                "openworld_rpg:iron_ore",
                6,
                true
        );
        assertTrue(town.consumed());
        assertEquals(4, town.fromPouch());
        assertEquals(2, town.fromVault());
        assertFalse(town.state().materialPouch().containsKey("openworld_rpg:iron_ore"));
        assertEquals(8, town.state().materialVault().get("openworld_rpg:iron_ore"));
    }

    @Test
    void inventoryAuthoritySurvivesCodecRoundTrip() {
        var original = PlayerInventoryState.initial()
                .addMaterialToPouch("openworld_rpg:hardwood", 12)
                .state()
                .addKeyItem("openworld_rpg:r01/quarry_relay_evidence")
                .deliverImportantOnce(
                        "openworld_rpg:r01/test_delivery",
                        ProjectInventoryItem.ordinary(
                                "openworld_rpg:test_reward",
                                2,
                                20,
                                5
                        )
                )
                .state();

        var encoded = PlayerInventoryState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerInventoryState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    private static ProjectBackpackState fullGrid(String prefix) {
        ProjectBackpackState state = ProjectBackpackState.startingBackpack();
        for (int i = 0; i < state.capacity(); i++) {
            state = state.insert(ProjectInventoryItem.ordinary(
                    prefix + i,
                    1,
                    1,
                    0
            )).state();
        }
        return state;
    }

    @Test
    void backpackStackableConsumeIsAtomicAndDoesNotUseStorage() {
        var item = ProjectInventoryItem.ordinary(
                "openworld_rpg:trail_skewers",
                2,
                50,
                0
        );
        var inserted = PlayerInventoryState.initial()
                .deliverBackpackOnce(
                        "openworld_rpg:test/meal_delivery",
                        item
                )
                .state();

        var consumed = inserted.consumeBackpackStackable(
                "openworld_rpg:trail_skewers",
                1
        );
        assertTrue(consumed.consumed());
        assertEquals(1, consumed.removed());
        assertEquals(
                1,
                consumed.state().backpack()
                        .itemAt(0).orElseThrow().quantity()
        );

        var failed = consumed.state().consumeBackpackStackable(
                "openworld_rpg:trail_skewers",
                2
        );
        assertFalse(failed.consumed());
        assertSame(consumed.state(), failed.state());
        assertTrue(failed.state().personalStorage().occupied().isEmpty());
    }

    @Test
    void multiMaterialConsumptionIsAtomicPouchFirstAndIdempotent() {
        var initial = new PlayerInventoryState(
                1,
                ProjectBackpackState.startingBackpack(),
                ProjectBackpackState.personalStorage(),
                Map.of(
                        "openworld_rpg:healing_herb", 2,
                        "openworld_rpg:louxia_glow", 1
                ),
                Map.of(
                        "openworld_rpg:healing_herb", 4,
                        "openworld_rpg:louxia_glow", 3
                ),
                Set.of(),
                Map.of(),
                Set.of()
        );
        Map<String, Integer> costs = Map.of(
                "openworld_rpg:healing_herb", 5,
                "openworld_rpg:louxia_glow", 2
        );
        String transactionId = "openworld_rpg:craft_material/test/0";

        assertTrue(initial.canConsumeMaterials(costs, true));
        assertFalse(initial.canConsumeMaterials(costs, false));

        var consumed = initial.consumeMaterialsOnce(
                transactionId,
                costs,
                true
        );
        assertEquals(
                PlayerInventoryState.MaterialsConsumeOnceStatus.CONSUMED,
                consumed.status()
        );
        assertEquals(
                Map.of(
                        "openworld_rpg:healing_herb", 2,
                        "openworld_rpg:louxia_glow", 1
                ),
                consumed.fromPouch()
        );
        assertEquals(
                Map.of(
                        "openworld_rpg:healing_herb", 3,
                        "openworld_rpg:louxia_glow", 1
                ),
                consumed.fromVault()
        );

        var retried = consumed.state().consumeMaterialsOnce(
                transactionId,
                costs,
                true
        );
        assertEquals(
                PlayerInventoryState.MaterialsConsumeOnceStatus.ALREADY_COMPLETED,
                retried.status()
        );
        assertSame(consumed.state(), retried.state());

        var insufficient = initial.consumeMaterialsOnce(
                "openworld_rpg:craft_material/test/1",
                Map.of("openworld_rpg:healing_herb", 7),
                true
        );
        assertEquals(
                PlayerInventoryState.MaterialsConsumeOnceStatus.INSUFFICIENT_MATERIALS,
                insufficient.status()
        );
        assertSame(initial, insufficient.state());
    }


    @Test
    void protectedPouchCountsRemainUntouchedWhileVaultCanCoverCosts() {
        var initial = new PlayerInventoryState(
                1,
                ProjectBackpackState.startingBackpack(),
                ProjectBackpackState.personalStorage(),
                Map.of("openworld_rpg:healing_herb", 5),
                Map.of("openworld_rpg:healing_herb", 4),
                Set.of(),
                Map.of(),
                Set.of()
        );
        Map<String, Integer> protectedCounts =
                Map.of("openworld_rpg:healing_herb", 3);

        assertFalse(initial.canConsumeMaterials(
                Map.of("openworld_rpg:healing_herb", 3),
                false,
                protectedCounts
        ));
        assertTrue(initial.canConsumeMaterials(
                Map.of("openworld_rpg:healing_herb", 3),
                true,
                protectedCounts
        ));

        var consumed = initial.consumeMaterialsOnce(
                "openworld_rpg:test/protected_materials",
                Map.of("openworld_rpg:healing_herb", 3),
                true,
                protectedCounts
        );
        assertTrue(consumed.consumed());
        assertEquals(
                3,
                consumed.state().materialPouch()
                        .get("openworld_rpg:healing_herb")
        );
        assertEquals(
                3,
                consumed.state().materialVault()
                        .get("openworld_rpg:healing_herb")
        );
        assertEquals(
                2,
                consumed.fromPouch().get("openworld_rpg:healing_herb")
        );
        assertEquals(
                1,
                consumed.fromVault().get("openworld_rpg:healing_herb")
        );
    }


}
