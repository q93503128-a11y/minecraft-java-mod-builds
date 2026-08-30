package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Command-free staged world bootstrap with a non-dismissible client loading state. */
public final class StarterSliceBootstrap {
    private static final int COLUMN_BUDGET_PER_TICK = 160;
    private static final Map<UUID, StarterSliceWorld.BuildJob> JOBS = new LinkedHashMap<>();

    private StarterSliceBootstrap() {}

    public static void tick(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || player.tickCount < 40) return;
        if (FieldSessionManager.active(player) || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        StarterSliceWorld.BuildJob job = JOBS.get(player.getUUID());
        if (job == null) {
            StarterSliceWorld.BuiltSlice existing = StarterSliceWorld.findExisting(level);
            if (existing != null) {
                player.setNoGravity(false);
                FieldSessionManager.enter(player);
                return;
            }
            job = StarterSliceWorld.begin(level);
            JOBS.put(player.getUUID(), job);
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            FieldNetwork.sync(player, FieldUiSnapshot.loading(job.stageLabel(), 0));
        }

        lock(player);
        clearVanillaMobs(level, job.baseY());
        boolean finished = job.tick(level, COLUMN_BUDGET_PER_TICK);
        if (finished) {
            JOBS.remove(player.getUUID());
            player.setNoGravity(false);
            FieldSessionManager.enter(player);
            return;
        }
        if ((player.tickCount & 1) == 0) {
            FieldNetwork.sync(player, FieldUiSnapshot.loading(job.stageLabel(), job.progressPercent()));
        }
    }

    public static boolean building(ServerPlayer player) { return JOBS.containsKey(player.getUUID()); }

    public static void remove(ServerPlayer player) {
        if (JOBS.remove(player.getUUID()) != null) {
            player.setNoGravity(false);
            FieldNetwork.close(player);
        }
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        JOBS.clear();
    }

    private static void lock(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private static void clearVanillaMobs(ServerLevel level, int baseY) {
        AABB area = new AABB(StarterSliceWorld.ORIGIN_X - 8, baseY - 10, StarterSliceWorld.VILLAGE_Z - 8,
                StarterSliceWorld.ORIGIN_X + StarterSliceWorld.SIZE + 8, baseY + 28,
                StarterSliceWorld.FIELD_Z + StarterSliceWorld.SIZE + 8);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) mob.discard();
    }
}
