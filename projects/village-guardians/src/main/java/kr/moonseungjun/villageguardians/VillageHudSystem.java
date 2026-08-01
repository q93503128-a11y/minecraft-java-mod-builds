package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class VillageHudSystem {
    private static int ticks;

    private VillageHudSystem() {
    }

    public static void reset() {
        ticks = 0;
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks < 20) {
            return;
        }
        ticks = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
            String xp = progress.level() >= RpgProgress.MAX_LEVEL
                    ? "MAX"
                    : progress.experience() + "/" + progress.experienceToNextLevel();
            String role = VillageCouncilState.roleOf(player.getUUID())
                    .map(VillageRole::shortName)
                    .orElse("역할 없음");
            String text = "§6" + VillageCouncilState.currentDay() + "일 "
                    + VillageCouncilState.currentPhase().koreanName()
                    + " §8│ §bLv." + progress.level() + " §7" + xp + " XP"
                    + " §8│ §f" + role
                    + " §8│ §e주화 " + VillageProgressionSystem.coins(player)
                    + " §8│ §6보급품 " + VillageProgressionSystem.supplies();
            player.displayClientMessage(Component.literal(text), true);
        }
    }
}
