package kr.moonseungjun.titanbreak.world;

import kr.moonseungjun.titanbreak.entity.RegnantFleshEntity;
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

public final class RegnantEncounterService {
    private static final long FIRST_WARNING_DELAY = 480L;
    private static final long WARNING_TO_ARRIVAL = 260L;
    private static final long RESPAWN_DELAY = 8_400L;
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private RegnantEncounterService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State progression) {
        if (!(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()
                || level.getDifficulty() == Difficulty.PEACEFUL) return;

        if (!progression.hasBossFirstKill("bastion_walker") || progression.hasBossFirstKill("regnant_flesh")) {
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
            player.sendSystemMessage(Component.translatable("message.titanbreak.regnant_warning"));
            return;
        }

        if (!runtime.warningSent || now < runtime.spawnTick) return;
        if (spawnBoss(level, player)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.regnant_arrival"));
            runtime.warningSent = false;
            runtime.warningTick = now + RESPAWN_DELAY;
            runtime.spawnTick = Long.MAX_VALUE;
        } else {
            runtime.spawnTick = now + 100L;
        }
    }

    private static boolean spawnBoss(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 14; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            int range = 104 + player.getRandom().nextInt(33);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * range);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * range);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;

            RegnantFleshEntity boss = ModBossEntities.REGNANT_FLESH.get().create(level, EntitySpawnReason.EVENT);
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
        AABB area = player.getBoundingBox().inflate(360.0D);
        return !level.getEntitiesOfClass(RegnantFleshEntity.class, area, Entity::isAlive).isEmpty();
    }

    public static void clear(UUID playerId) {
        RUNTIME.remove(playerId);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }

    private static final class RuntimeState {
        private long warningTick;
        private long spawnTick = Long.MAX_VALUE;
        private boolean warningSent;

        private RuntimeState(long warningTick) {
            this.warningTick = warningTick;
        }
    }
}
