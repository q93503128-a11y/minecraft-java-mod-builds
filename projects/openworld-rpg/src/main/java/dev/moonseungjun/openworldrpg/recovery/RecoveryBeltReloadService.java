package dev.moonseungjun.openworldrpg.recovery;

import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryAttachments;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Couples Recovery Belt refill with the real carried Backpack reserve.
 *
 * <p>The plan is calculated without mutating attachments first. The server then commits both
 * player attachments in one synchronous server-thread operation; repeating the operation cannot
 * consume another reserve for an already-filled slot.</p>
 */
public final class RecoveryBeltReloadService {
    private RecoveryBeltReloadService() {
    }

    public static ReloadResult reloadFromCarriedReserves(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerInventoryState inventory = player.getAttachedOrSet(
                PlayerInventoryAttachments.INVENTORY,
                PlayerInventoryState.initial()
        );
        RecoveryBeltState belt = RecoveryBeltService.state(player);
        RecoveryBeltSetupState setup = RecoveryBeltService.setup(player);

        ReloadPlan plan = planReload(belt, setup, inventory);
        if (plan.loadedSlots().isEmpty()) {
            return plan.result();
        }

        player.setAttached(
                PlayerInventoryAttachments.INVENTORY,
                plan.inventory()
        );
        RecoveryBeltService.replace(player, plan.belt());
        return plan.result();
    }

    /**
     * Semantic rest hook. Shrine/inn/camp authority can call this only after its own interaction
     * has been accepted; rest never manufactures consumables.
     */
    public static ReloadResult reloadAtRest(ServerPlayer player) {
        return reloadFromCarriedReserves(player);
    }

    public static ReloadPlan planReload(
            RecoveryBeltState belt,
            RecoveryBeltSetupState setup,
            PlayerInventoryState inventory
    ) {
        Objects.requireNonNull(belt, "belt");
        Objects.requireNonNull(setup, "setup");
        Objects.requireNonNull(inventory, "inventory");

        RecoveryBeltState workingBelt = belt;
        PlayerInventoryState workingInventory = inventory;
        ArrayList<Integer> loadedSlots = new ArrayList<>();
        ArrayList<Integer> missingReserveSlots = new ArrayList<>();

        for (int slot = 0; slot < RecoveryActionRules.BELT_CAPACITY; slot++) {
            if (workingBelt.consumableAt(slot).isPresent()) {
                continue;
            }
            var desired = setup.desiredConsumableAt(slot);
            if (desired.isEmpty()) {
                continue;
            }

            RecoveryConsumable consumable = desired.orElseThrow();
            PlayerInventoryState.BackpackConsumeResult consumed =
                    workingInventory.consumeBackpackStackable(
                            consumable.itemId(),
                            1
                    );
            if (!consumed.consumed()) {
                missingReserveSlots.add(slot);
                continue;
            }

            workingInventory = consumed.state();
            workingBelt = workingBelt.loadReservedDoseIntoSlot(
                    slot,
                    consumable
            );
            loadedSlots.add(slot);
        }

        ReloadStatus status;
        if (setup.isEmpty()) {
            status = ReloadStatus.NO_CONFIGURATION;
        } else if (loadedSlots.isEmpty() && missingReserveSlots.isEmpty()) {
            status = ReloadStatus.NOTHING_TO_RELOAD;
        } else if (!missingReserveSlots.isEmpty()) {
            status = loadedSlots.isEmpty()
                    ? ReloadStatus.INSUFFICIENT_RESERVES
                    : ReloadStatus.PARTIAL;
        } else {
            status = ReloadStatus.RELOADED;
        }

        ReloadResult result = new ReloadResult(
                status,
                loadedSlots,
                missingReserveSlots
        );
        return new ReloadPlan(
                workingBelt,
                workingInventory,
                result
        );
    }

    public enum ReloadStatus {
        RELOADED,
        PARTIAL,
        INSUFFICIENT_RESERVES,
        NOTHING_TO_RELOAD,
        NO_CONFIGURATION
    }

    public record ReloadResult(
            ReloadStatus status,
            List<Integer> loadedSlots,
            List<Integer> missingReserveSlots
    ) {
        public ReloadResult {
            Objects.requireNonNull(status, "status");
            loadedSlots = List.copyOf(
                    Objects.requireNonNull(loadedSlots, "loadedSlots")
            );
            missingReserveSlots = List.copyOf(
                    Objects.requireNonNull(
                            missingReserveSlots,
                            "missingReserveSlots"
                    )
            );
        }

        public int loadedCount() {
            return loadedSlots.size();
        }
    }

    public record ReloadPlan(
            RecoveryBeltState belt,
            PlayerInventoryState inventory,
            ReloadResult result
    ) {
        public ReloadPlan {
            Objects.requireNonNull(belt, "belt");
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(result, "result");
        }

        public List<Integer> loadedSlots() {
            return result.loadedSlots();
        }
    }
}
