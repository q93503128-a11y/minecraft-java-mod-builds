package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageHudSystem {
    private static final int REFRESH_TICKS = 60;
    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static int ticks;

    private VillageHudSystem() {
    }

    public static void reset() {
        ticks = 0;
        LAST_TEXT.clear();
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks < REFRESH_TICKS) {
            return;
        }
        ticks = 0;
        LAST_TEXT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String text = buildText(player);
            LAST_TEXT.put(player.getUUID(), text);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
        }
    }

    private static String buildText(ServerPlayer player) {
        RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
        String xp = progress.level() >= RpgProgress.MAX_LEVEL
                ? "MAX"
                : progress.experience() + "/" + progress.experienceToNextLevel();
        String role = VillageCouncilState.roleOf(player.getUUID())
                .map(VillageRole::shortName)
                .orElse("역할 없음");
        return "§6" + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " §8│ §bLv." + progress.level() + " §7" + xp + " XP"
                + " §8│ §f" + role
                + " §8│ §e주화 " + VillageProgressionSystem.coins(player)
                + " §8│ §6보급품 " + VillageProgressionSystem.supplies();
    }
}
