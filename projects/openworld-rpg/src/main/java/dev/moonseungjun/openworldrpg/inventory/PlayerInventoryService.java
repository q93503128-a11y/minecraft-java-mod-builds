package dev.moonseungjun.openworldrpg.inventory;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-only mutation/read boundary for project Backpack/Storage/Material inventory. */
public final class PlayerInventoryService {
    private PlayerInventoryService() {
    }

    public static PlayerInventoryState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerInventoryAttachments.INVENTORY,
                PlayerInventoryState.initial()
        );
    }

    public static PlayerInventoryState.DeliveryResult deliverImportantOnce(
            ServerPlayer player,
            String transactionId,
            ProjectInventoryItem item
    ) {
        PlayerInventoryState.DeliveryResult result =
                state(player).deliverImportantOnce(transactionId, item);
        replace(player, result.state());
        return result;
    }

    public static PlayerInventoryState.DeliveryResult claimPending(
            ServerPlayer player,
            String transactionId
    ) {
        PlayerInventoryState.DeliveryResult result =
                state(player).claimPending(transactionId);
        replace(player, result.state());
        return result;
    }

    public static PlayerInventoryState.MaterialInsertResult addMaterialToPouch(
            ServerPlayer player,
            String materialId,
            int amount
    ) {
        PlayerInventoryState.MaterialInsertResult result =
                state(player).addMaterialToPouch(materialId, amount);
        replace(player, result.state());
        return result;
    }

    public static PlayerInventoryState.MaterialConsumeResult consumeMaterial(
            ServerPlayer player,
            String materialId,
            int amount,
            boolean settlementMayUseVault
    ) {
        PlayerInventoryState.MaterialConsumeResult result =
                state(player).consumeMaterial(materialId, amount, settlementMayUseVault);
        replace(player, result.state());
        return result;
    }

    public static PlayerInventoryState.MaterialTransferResult depositMaterialToVault(
            ServerPlayer player,
            String materialId,
            int requestedAmount
    ) {
        PlayerInventoryState.MaterialTransferResult result =
                state(player).depositMaterialToVault(materialId, requestedAmount);
        replace(player, result.state());
        return result;
    }

    public static PlayerInventoryState addKeyItem(
            ServerPlayer player,
            String keyItemId
    ) {
        return replace(player, state(player).addKeyItem(keyItemId));
    }

    private static PlayerInventoryState replace(
            ServerPlayer player,
            PlayerInventoryState next
    ) {
        PlayerInventoryState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(PlayerInventoryAttachments.INVENTORY, next);
        return next;
    }
}
