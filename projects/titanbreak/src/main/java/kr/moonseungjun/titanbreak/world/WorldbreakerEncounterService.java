package kr.moonseungjun.titanbreak.world;

import kr.moonseungjun.titanbreak.entity.WorldbreakerEntity;
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

public final class WorldbreakerEncounterService {
    private static final long FIRST_WARNING_DELAY = 1_000L;
    private static final long WARNING_TO_ARRIVAL = 420L;
    private static final long RESPAWN_DELAY = 18_000L;
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private WorldbreakerEncounterService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State progression) {
        if (!(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()
                || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!progression.hasBossFirstKill("null_seraph") || progression.hasBossFirstKill("worldbreaker")) {
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
            player.sendSystemMessage(Component.translatable("message.titanbreak.worldbreaker_warning"));
            return;
        }
        if (!runtime.warningSent || now < runtime.spawnTick) return;

        if (spawnBoss(level, player)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.worldbreaker_arrival"));
            runtime.warningSent = false;
            runtime.warningTick = now + RESPAWN_DELAY;
            runtime.spawnTick = Long.MAX_VALUE;
        } else {
            runtime.spawnTick = now + 200L;
        }
    }

    private static boolean spawnBoss(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            int range = 230 + player.getRandom().nextInt(111);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * range);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * range);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;

            WorldbreakerEntity boss = ModBossEntities.WORLDBREAKER.get().create(level, EntitySpawnReason.EVENT);
            if (boss == null) return false;
            boss.setPos(x + 0.5D, y, z + 0.5D);

            double throughX = player.getX() - (x + 0.5D);
            double throughZ = player.getZ() - (z + 0.5D);
            double length = Math.sqrt(throughX * throughX + throughZ * throughZ);
            if (length < 1.0D) {
                throughX = Math.cos(angle + Math.PI);
                throughZ = Math.sin(angle + Math.PI);
                length = 1.0D;
            }
            throughX /= length;
            throughZ /= length;
            boss.setYRot((float) Math.toDegrees(Math.atan2(-throughX, throughZ)));
            boss.setMarchDestination(player.getX() + throughX * 1_800.0D,
                    player.getZ() + throughZ * 1_800.0D);
            if (!level.noCollision(boss)) continue;
            return level.addFreshEntity(boss);
        }
        return false;
    }

    private static boolean hasNearbyBoss(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(900.0D);
        return !level.getEntitiesOfClass(WorldbreakerEntity.class, area, Entity::isAlive).isEmpty();
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
