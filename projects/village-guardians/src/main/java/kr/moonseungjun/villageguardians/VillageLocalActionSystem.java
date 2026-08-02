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
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.INFIRMARY)) {
                    player.sendSystemMessage(Component.literal("§c치료는 의무소 단말기 근처에서만 가능합니다."));
                    return true;
                }
                player.sendSystemMessage(Component.literal("§a" + VillageProgressionSystem.useInfirmary(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.INFIRMARY);
                return true;
            }
            case "train" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
                    player.sendSystemMessage(Component.literal("§c전투 훈련은 병영 단말기 근처에서만 가능합니다."));
                    return true;
                }
                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.train(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.BARRACKS);
                return true;
            }
            default -> { return false; }
        }
    }
}
