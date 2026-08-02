package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Prevents legacy facility handlers from reopening obsolete local management screens. */
public final class VillageLocalActionSystem {
    private VillageLocalActionSystem() {}

    public static boolean handle(ServerPlayer player, String action) {
        if (player == null || action == null) return false;
        switch (action) {
            case "use_infirmary" -> {
                player.sendSystemMessage(Component.literal("§a" + VillageProgressionSystem.useInfirmary(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.INFIRMARY);
                return true;
            }
            case "train" -> {
                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.train(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.BARRACKS);
                return true;
            }
            default -> { return false; }
        }
    }
}
