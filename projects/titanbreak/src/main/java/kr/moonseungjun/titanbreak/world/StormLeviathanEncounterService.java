package kr.moonseungjun.titanbreak.world;

import kr.moonseungjun.titanbreak.entity.StormLeviathanEntity;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StormLeviathanEncounterService {
    private static final long FIRST_WARNING_DELAY = 680L;
    private static final long WARNING_TO_ARRIVAL = 340L;
    private static final long RESPAWN_DELAY = 11_600L;
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private StormLeviathanEncounterService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State progression) {
        if (!(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()
                || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!progression.hasBossFirstKill("chronophage") || progression.hasBossFirstKill("storm_leviathan")) {
            RUNTIME.remove(player.getUUID());
            return;
        }

        long now = level.getGameTime();
        if (hasNearbyBoss(level, player)) return;
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(),
                ignored -> new RuntimeState(now + FIRST_WARNING_DELAY));

        if (!runtime.warningSent && now >= runtime.warningTick) {
            runtime.warningSent = true;
            runtime.spawnTick = now + WARNING_TO_ARRIVAL;
            player.sendSystemMessage(Component.translatable("message.titanbreak.storm_leviathan_warning"));
            return;
        }
        if (!runtime.warningSent || now < runtime.spawnTick) return;

        if (spawnBoss(level, player)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.storm_leviathan_arrival"));
            runtime.warningSent = false;
            runtime.warningTick = now + RESPAWN_DELAY;
            runtime.spawnTick = Long.MAX_VALUE;
        } else {
            runtime.spawnTick = now + 140L;
        }
    }

    private static boolean spawnBoss(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 18; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            int range = 168 + player.getRandom().nextInt(61);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * range);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * range);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int y = groundY + 48 + player.getRandom().nextInt(18);
            BlockPos pos = new BlockPos(x, groundY, z);
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;

            StormLeviathanEntity boss = ModBossEntities.STORM_LEVIATHAN.get().create(level, EntitySpawnReason.EVENT);
            if (boss == null) return false;
            boss.setPos(x + 0.5D, y, z + 0.5D);
            boss.setYRot(player.getRandom().nextFloat() * 360.0F);
            if (!level.noCollision(boss)) continue;
            boss.setTarget(player);
            return level.addFreshEntity(boss);
        }
        return false;
    }

    private static boolean hasNearbyBoss(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(560.0D);
        return !level.getEntitiesOfClass(StormLeviathanEntity.class, area, Entity::isAlive).isEmpty();
    }

    public static void clear(UUID playerId) { RUNTIME.remove(playerId); }
    public static void clearAll() { RUNTIME.clear(); }

    private static final class RuntimeState {
        private long warningTick;
        private long spawnTick = Long.MAX_VALUE;
        private boolean warningSent;

        private RuntimeState(long warningTick) {
            this.warningTick = warningTick;
        }
    }
}
