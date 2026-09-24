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
}
