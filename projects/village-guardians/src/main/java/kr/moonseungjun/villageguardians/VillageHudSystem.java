package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageHudSystem {
    private static final int REFRESH_TICKS = 10;
    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static int ticks;

    private VillageHudSystem() {}

    public static void reset() {
        ticks = 0;
        LAST_TEXT.clear();
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks < REFRESH_TICKS) return;
        ticks = 0;
        LAST_TEXT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String text = VillageDefenseHudFrame.from(player).encode();
            LAST_TEXT.put(player.getUUID(), text);
            VillageNetwork.sendMainHud(player, text);
            VillageNetwork.sendSkillHud(player, VillageRespawnSystem.isDowned(player)
                    ? "" : buildSkillText(player));
        }
    }

    private static String buildSkillText(ServerPlayer player) {
        String base = VillageRoleSkillSystem.hudSlotText(player, 0)
                + " §8│ " + VillageRoleSkillSystem.hudSlotText(player, 1);
        String active = VillageRoleAbilitySystem.activeSkillHud(player);
        return active.isBlank() ? base : base + " §8│ " + active;
    }
}
