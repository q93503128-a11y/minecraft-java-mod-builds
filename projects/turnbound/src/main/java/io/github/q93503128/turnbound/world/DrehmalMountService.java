package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.RoadhornMountEntity;
import io.github.q93503128.turnbound.presentation.TurnboundMounts;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative rental mounts issued only from physical Drehmal waystations. */
final class DrehmalMountService {
    static final String ROADHORN = "ROADHORN";
    private static final long DISMOUNT_GRACE_TICKS = 20L * 60L;
    private static final double ABANDON_DISTANCE_SQR = 64.0D * 64.0D;
    private static final Map<UUID, Rental> RENTALS = new HashMap<>();

    private record Rental(ServerLevel level, UUID entityId, long dismountedAt, boolean suspended, boolean resumeMounted) {}

    private DrehmalMountService() {}

    static boolean handle(ServerPlayer player, String mountId) {
        if (player == null || mountId == null || !ROADHORN.equals(mountId)) return false;
        if (!ExternalWorldBootstrap.active(player)) return true;
        if (!DrehmalWaystationRuntime.nearWaystation(player)) {
            deny(player, "탈것은 실제 역참지기 곁에서만 부를 수 있습니다.");
            return true;
        }
        if (BattleSessionManager.exists(player) || DrehmalVisibleEncounterService.fastTravelBlocked(player)) {
            deny(player, "주변이 안전해진 뒤 탈것을 부를 수 있습니다.");
            return true;
        }
        if (!(player.level() instanceof ServerLevel level)) return true;

        release(player);
        if (player.getVehicle() != null) player.stopRiding();
        RoadhornMountEntity mount = spawnMount(level, player);
        if (mount == null) {
            deny(player, "역참 주변에서 탈것이 설 안전한 자리를 찾지 못했습니다.");
            return true;
        }
        mount.assignRentalOwner(player.getUUID());
        mount.setCustomName(Component.literal("길뿔 산양").withStyle(ChatFormatting.GOLD));
        mount.setCustomNameVisible(false);
        RENTALS.put(player.getUUID(), new Rental(level, mount.getUUID(), 0L, false, false));
        mount.ride(player);
        player.sendSystemMessage(Component.literal("역참에서 길뿔 산양을 빌렸습니다.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    static void tick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        Rental rental = RENTALS.get(player.getUUID());
        if (rental == null) return;
        if (rental.level() != level) {
            release(player);
            return;
        }

        if (!ExternalWorldBootstrap.active(player)) {
            release(player);
            return;
        }
        if (BattleSessionManager.exists(player)) {
            suspendForBattle(player);
            return;
        }
        if (rental.suspended()) {
            restoreAfterBattle(player, rental);
            return;
        }

        Entity raw = rental.entityId() == null ? null : rental.level().getEntity(rental.entityId());
        if (!(raw instanceof RoadhornMountEntity mount) || mount.isRemoved()) {
            RENTALS.remove(player.getUUID());
            return;
        }

        if (player.getVehicle() == mount) {
            if (rental.dismountedAt() != 0L) {
                RENTALS.put(player.getUUID(), new Rental(rental.level(), rental.entityId(), 0L, false, false));
            }
            return;
        }

        if (player.distanceToSqr(mount) > ABANDON_DISTANCE_SQR) {
            release(player);
            return;
        }

        long now = level.getGameTime();
        if (rental.dismountedAt() == 0L) {
            RENTALS.put(player.getUUID(), new Rental(rental.level(), rental.entityId(), now, false, false));
        } else if (now - rental.dismountedAt() >= DISMOUNT_GRACE_TICKS) {
            release(player);
        }
    }

    static void suspendForBattle(ServerPlayer player) {
        if (player == null) return;
        Rental rental = RENTALS.get(player.getUUID());
        if (rental == null || rental.suspended()) return;
        Entity entity = rental.entityId() == null ? null : rental.level().getEntity(rental.entityId());
        boolean resumeMounted = entity != null && player.getVehicle() == entity;
        if (resumeMounted) player.stopRiding();
        if (entity != null) entity.discard();
        RENTALS.put(player.getUUID(), new Rental(rental.level(), null, 0L, true, resumeMounted));
    }

    private static void restoreAfterBattle(ServerPlayer player, Rental rental) {
        if (!(player.level() instanceof ServerLevel level) || rental.level() != level) {
            release(player);
            return;
        }
        RoadhornMountEntity mount = spawnMount(level, player);
        if (mount == null) return;
        mount.assignRentalOwner(player.getUUID());
        mount.setCustomName(Component.literal("길뿔 산양").withStyle(ChatFormatting.GOLD));
        mount.setCustomNameVisible(false);
        RENTALS.put(player.getUUID(), new Rental(level, mount.getUUID(), 0L, false, false));
        if (rental.resumeMounted()) mount.ride(player);
        player.sendSystemMessage(Component.literal("대여한 길뿔 산양이 전투 뒤 다시 합류했습니다.").withStyle(ChatFormatting.GOLD));
    }

    static void release(ServerPlayer player) {
        if (player == null) return;
        Rental rental = RENTALS.remove(player.getUUID());
        if (rental == null) return;
        if (rental.entityId() != null && player.getVehicle() != null
                && player.getVehicle().getUUID().equals(rental.entityId())) {
            player.stopRiding();
        }
        Entity entity = rental.entityId() == null ? null : rental.level().getEntity(rental.entityId());
        if (entity != null) entity.discard();
    }

    static void clearAll() {
        for (Rental rental : List.copyOf(RENTALS.values())) {
            Entity entity = rental.entityId() == null ? null : rental.level().getEntity(rental.entityId());
            if (entity != null) entity.discard();
        }
        RENTALS.clear();
    }

    private static RoadhornMountEntity spawnMount(ServerLevel level, ServerPlayer player) {
        for (int[] offset : offsets(7)) {
            if (offset[0] * offset[0] + offset[1] * offset[1] < 4) continue;
            int x = player.blockPosition().getX() + offset[0];
            int z = player.blockPosition().getZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!safe(level, feet)) continue;
            RoadhornMountEntity mount = TurnboundMounts.spawn(
                    level, new Vec3(x + 0.5D, y, z + 0.5D), player.getYRot());
            if (mount != null) return mount;
        }
        return null;
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        BlockPos ground = feet.below();
        if (level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()
                || !level.getFluidState(ground).isEmpty()) return false;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = feet.above(dy);
            if (!level.getFluidState(p).isEmpty()
                    || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return false;
        }
        return true;
    }

    private static List<int[]> offsets(int radius) {
        List<int[]> out = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
            if (dx * dx + dz * dz <= radius * radius) out.add(new int[]{dx, dz});
        }
        out.sort(Comparator.comparingInt(v -> v[0] * v[0] + v[1] * v[1]));
        return out;
    }

    private static void deny(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GRAY));
        ExternalWorldBootstrap.refreshFieldContext(player);
    }
}
