package kr.moonseungjun.survivalascension.world;

/*
 * Boss-driven difficulty-stage progression is adapted from Hostiles Are Too Easy (CC0-1.0),
 * which advances world difficulty through milestone states including Wither and Ender Dragon kills.
 * Survival Ascension uses a two-step post-boss world stage tied to its elite and warband systems.
 */

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class WorldAscensionProgression {
    private WorldAscensionProgression() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) return;
        int targetStage;
        String bossName;
        if (event.getEntity() instanceof EnderDragon) {
            targetStage = 2;
            bossName = "엔더 드래곤";
        } else if (event.getEntity() instanceof WitherBoss) {
            targetStage = 1;
            bossName = "위더";
        } else {
            return;
        }

        MinecraftServer server = level.getServer();
        WorldAscensionData data = WorldAscensionData.get(server);
        if (!data.advanceTo(targetStage)) return;

        String detail = targetStage == 1
                ? "엘리트와 전술 분대가 더 자주, 더 큰 체급으로 출현합니다."
                : "세계가 최종 종말 단계에 진입했습니다. 상위 엘리트와 대형 전술 분대가 크게 증가합니다.";
        Component message = Component.literal("§5[월드 승천] §f" + bossName + " 격파 → §d" + data.stageName() + "§f\n§7" + detail);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) online.sendSystemMessage(message);
    }
}
