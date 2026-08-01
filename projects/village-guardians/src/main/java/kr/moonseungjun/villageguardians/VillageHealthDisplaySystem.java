package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
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
            updatePlayerNameHealth(server, player);
        }
        updateRaidEnemyHealth(server);
    }

    private static void updatePlayerNameHealth(MinecraftServer server, ServerPlayer player) {
        int current = Math.max(0, (int) Math.ceil(player.getHealth() / 2.0f));
        int maximum = Math.max(1, (int) Math.ceil(player.getMaxHealth() / 2.0f));
        String teamName = "vghp_" + Math.min(99, current) + "_" + Math.min(99, maximum);
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        if (team == null) {
            team = server.getScoreboard().addPlayerTeam(teamName);
            team.setPlayerSuffix(Component.literal(" §c❤" + current + "/" + maximum));
        }
        server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
    }

    private static void updateRaidEnemyHealth(MinecraftServer server) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return;
        }
        ServerLevel level = server.overworld();
        AABB area = new AABB(center).inflate(VillageWorldSystem.ENEMY_SPAWN_DISTANCE + 48, 64,
                VillageWorldSystem.ENEMY_SPAWN_DISTANCE + 48);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!VillageWorldSystem.isAllowedGameMob(mob) || !mob.isAlive()) {
                continue;
            }
            int current = Math.max(0, (int) Math.ceil(mob.getHealth() / 2.0f));
            int maximum = Math.max(1, (int) Math.ceil(mob.getMaxHealth() / 2.0f));
            mob.setCustomName(Component.literal("§c❤ " + current + "/" + maximum));
            mob.setCustomNameVisible(true);
        }
    }
}
