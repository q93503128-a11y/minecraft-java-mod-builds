package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageHealthDisplaySystem {
    private static final String PLAYER_TEAM_PREFIX = "vghp_";
    private static final Map<UUID, Component> ENEMY_BASE_NAMES = new HashMap<>();
    private static boolean legacyTeamsCleaned;
    private static int ticks;

    private VillageHealthDisplaySystem() {
    }

    public static void reset() {
        ticks = 0;
        legacyTeamsCleaned = false;
        ENEMY_BASE_NAMES.clear();
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks < 10) {
            return;
        }
        ticks = 0;
        if (!legacyTeamsCleaned) {
            removeLegacyHealthTeams(server);
            legacyTeamsCleaned = true;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerNameHealth(server, player);
        }
        updateRaidEnemyHealth(server);
    }

    public static void forgetEnemy(UUID uuid) {
        ENEMY_BASE_NAMES.remove(uuid);
    }

    private static void updatePlayerNameHealth(MinecraftServer server, ServerPlayer player) {
        int current = Math.max(0, (int) Math.ceil(player.getHealth() / 2.0f));
        int maximum = Math.max(1, (int) Math.ceil(player.getMaxHealth() / 2.0f));
        String teamName = PLAYER_TEAM_PREFIX + player.getUUID().toString().substring(0, 8);
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        if (team == null) {
            team = server.getScoreboard().addPlayerTeam(teamName);
        }
        team.setPlayerSuffix(Component.literal(" §c❤" + current + "/" + maximum));
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
            if (!VillageRaidSystem.isActiveEnemy(mob.getUUID()) || !mob.isAlive()) {
                continue;
            }
            Component baseName = ENEMY_BASE_NAMES.computeIfAbsent(
                    mob.getUUID(),
                    ignored -> mob.getCustomName() != null
                            ? mob.getCustomName().copy()
                            : mob.getType().getDescription().copy());
            int current = Math.max(0, (int) Math.ceil(mob.getHealth() / 2.0f));
            int maximum = Math.max(1, (int) Math.ceil(mob.getMaxHealth() / 2.0f));
            mob.setCustomName(Component.literal("§c❤ " + current + "/" + maximum + " §7").append(baseName.copy()));
            mob.setCustomNameVisible(shouldShowEnemyNameplate(server, mob));
        }
        ENEMY_BASE_NAMES.keySet().removeIf(uuid -> !VillageRaidSystem.isActiveEnemy(uuid));
    }

    private static boolean shouldShowEnemyNameplate(MinecraftServer server, Mob mob) {
        VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);
        if (VillageEnemyArchetypeSystem.alwaysShowNameplate(
                archetype, VillageRaidSystem.isBossEnemy(mob), VillageEnemyArchetypeSystem.isFlying(mob))) {
            return true;
        }
        double nearbySquared = 22.0 * 22.0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            if (player.distanceToSqr(mob) <= nearbySquared) return true;
        }
        return false;
    }

    private static void removeLegacyHealthTeams(MinecraftServer server) {
        for (PlayerTeam team : new ArrayList<>(server.getScoreboard().getPlayerTeams())) {
            if (team.getName().startsWith(PLAYER_TEAM_PREFIX)) {
                server.getScoreboard().removePlayerTeam(team);
            }
        }
    }
}
