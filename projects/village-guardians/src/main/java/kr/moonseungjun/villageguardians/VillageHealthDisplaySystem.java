package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

public final class VillageHealthDisplaySystem {
    private static int ticks;

    private VillageHealthDisplaySystem() {
    }

    public static void reset() {
        ticks = 0;
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks < 10) {
            return;
        }
        ticks = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int current = Math.max(0, (int) Math.ceil(player.getHealth() / 2.0f));
            int maximum = Math.max(1, (int) Math.ceil(player.getMaxHealth() / 2.0f));
            String teamName = "vghp_" + current + "_" + maximum;
            if (teamName.length() > 16) {
                teamName = "vghp_" + Math.min(99, current) + "_" + Math.min(99, maximum);
            }
            PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
            if (team == null) {
                team = server.getScoreboard().addPlayerTeam(teamName);
                team.setPlayerSuffix(Component.literal(" §c❤" + current + "/" + maximum));
            }
            server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
        }
    }
}
