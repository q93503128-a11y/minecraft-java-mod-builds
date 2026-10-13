package dev.moonseungjun.openworldrpg.mount;

import dev.moonseungjun.openworldrpg.entity.OpenworldRpgEntityTypes;
import dev.moonseungjun.openworldrpg.entity.TrailStagEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Live server binder for the permanent Trail Stag summon. */
public final class TrailStagLiveBinding implements TrailStagMountIntentRuntime.LiveBinding {
    private static final TrailStagLiveBinding INSTANCE = new TrailStagLiveBinding();
    private static final ConcurrentHashMap<UUID, UUID> ACTIVE_BY_OWNER = new ConcurrentHashMap<>();

    private TrailStagLiveBinding() {
    }

    public static void initialize() {
        TrailStagMountIntentRuntime.registerLiveBinding(INSTANCE);
    }

    @Override
    public boolean presentationRuntimeReady() {
        return true;
    }

    @Override
    public TrailStagMountService.SummonEligibility completeSummon(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return TrailStagMountService.SummonEligibility.INVALID_PLAYER;
        }

        Optional<SpawnSolution> solution = firstLegalSummon(level, player);
        TrailStagMountService.SummonEligibility eligibility = TrailStagMountService.evaluateSummon(
                player,
                level.getGameTime(),
                TrailStagMountRules.HOSTILE_COMBAT_QUIET_TICKS,
                false,
                solution.isPresent(),
                true
        );
        if (eligibility != TrailStagMountService.SummonEligibility.ALLOWED) {
            return eligibility;
        }

        discardPreviousOwnedSummon(player);
        SpawnSolution spawn = solution.orElseThrow();
        TrailStagEntity stag = new TrailStagEntity(OpenworldRpgEntityTypes.TRAIL_STAG, level);
        stag.bindOwner(player);
        stag.snapTo(spawn.position().x, spawn.position().y, spawn.position().z, player.getYRot(), 0.0F);
        if (!level.addFreshEntity(stag)) {
            return TrailStagMountService.SummonEligibility.NEED_OPEN_GROUND;
        }
        if (!player.startRiding(stag, true)) {
            stag.discard();
            return TrailStagMountService.SummonEligibility.NEED_OPEN_GROUND;
        }
        ACTIVE_BY_OWNER.put(player.getUUID(), stag.getUUID());
        return TrailStagMountService.SummonEligibility.ALLOWED;
    }

    /** Normal U-key/manual dismount. A failed safety search leaves the player mounted. */
    public static boolean requestSafeDismount(ServerPlayer player, TrailStagEntity stag) {
        if (!stag.ownedBy(player) || player.getVehicle() != stag || !(stag.level() instanceof ServerLevel level)) {
            return false;
        }
        Optional<Vec3> target = firstLegalDismount(level, player, stag);
        if (target.isEmpty()) {
            return false;
        }
        Vec3 position = target.get();
        boolean separated = TrailStagDismountGuard.runAuthorized(() -> {
            player.dismountTo(position.x, position.y, position.z);
            return player.getVehicle() != stag;
        });
        if (separated) {
            stag.armCleanDespawn();
        }
        return separated;
    }

    /** Resolve break must end the mounted state; prefer the same safety search, then nearest open column. */
    public static void forceResolveBreakDismount(ServerPlayer player, TrailStagEntity stag) {
        if (player.getVehicle() != stag) {
            stag.armCleanDespawn();
            return;
        }
        Optional<Vec3> safe = firstLegalDismount((ServerLevel) stag.level(), player, stag);
        Vec3 target = safe.orElseGet(() -> emergencyDismount((ServerLevel) stag.level(), player, stag));
        TrailStagDismountGuard.runAuthorized(() -> {
            player.dismountTo(target.x, target.y, target.z);
            return true;
        });
        stag.armCleanDespawn();
    }

    public static boolean hasSafeDismount(ServerPlayer player, TrailStagEntity stag) {
        return stag.level() instanceof ServerLevel level
                && firstLegalDismount(level, player, stag).isPresent();
    }

    public static void tickUnridden(TrailStagEntity stag) {
        if (stag.cleanDespawnTicksRemaining() < 0) {
            return;
        }
        int remaining = stag.tickCleanDespawnCountdown();
        if (remaining <= 0) {
            UUID owner = stag.ownerId();
            if (owner != null) {
                ACTIVE_BY_OWNER.remove(owner, stag.getUUID());
            }
            stag.discard();
        }
    }

    private static void discardPreviousOwnedSummon(ServerPlayer owner) {
        UUID entityId = ACTIVE_BY_OWNER.remove(owner.getUUID());
        if (entityId == null || owner.getServer() == null) {
            return;
        }
        for (ServerLevel level : owner.getServer().getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof TrailStagEntity stag) {
                stag.ejectPassengers();
                stag.discard();
                return;
            }
        }
    }

    private static Optional<SpawnSolution> firstLegalSummon(ServerLevel level, ServerPlayer player) {
        List<TrailStagPlacementAuthority.SummonProbe> probes = new ArrayList<>();
        List<SpawnSolution> solutions = new ArrayList<>();
        for (TrailStagPlacementAuthority.SummonCandidate candidate : TrailStagPlacementAuthority.SUMMON_ORDER) {
            Vec3 horizontal = offset(player.getYRot(), candidate.direction(), candidate.distanceBlocks());
            double x = player.getX() + horizontal.x;
            double z = player.getZ() + horizontal.z;
            Support support = support(level, x, player.getY(), z, TrailStagEntity.REVIEW_WIDTH);
            double y = support.highestY();
            AABB box = boxAt(x, y, z, TrailStagEntity.REVIEW_WIDTH, TrailStagEntity.REVIEW_HEIGHT);
            boolean collisionClear = support.present() && level.noCollision(box);
            boolean entitiesClear = collisionClear && level.getEntities((Entity) null, box.inflate(0.05)).isEmpty();
            boolean legalVolume = TrailStagMountVolumeAuthority.isAllowed(level, box);
            TrailStagPlacementAuthority.SummonProbe probe = new TrailStagPlacementAuthority.SummonProbe(
                    candidate,
                    collisionClear,
                    support.present() && support.nonLiquid(),
                    support.variance(),
                    legalVolume,
                    entitiesClear
            );
            probes.add(probe);
            solutions.add(new SpawnSolution(candidate, new Vec3(x, y, z)));
        }
        Optional<TrailStagPlacementAuthority.SummonCandidate> chosen =
                TrailStagPlacementAuthority.firstLegalSummon(probes);
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        for (SpawnSolution solution : solutions) {
            if (solution.candidate().equals(chosen.get())) {
                return Optional.of(solution);
            }
        }
        throw new IllegalStateException("Trail Stag placement authority selected an unknown candidate.");
    }

    private static Optional<Vec3> firstLegalDismount(
            ServerLevel level,
            ServerPlayer player,
            TrailStagEntity stag
    ) {
        double distance = stag.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + 0.20;
        List<TrailStagPlacementAuthority.DismountProbe> probes = new ArrayList<>();
        List<Vec3> positions = new ArrayList<>();
        for (TrailStagPlacementAuthority.RelativeDirection direction : TrailStagPlacementAuthority.DISMOUNT_ORDER) {
            Vec3 horizontal = offset(stag.getYRot(), direction, distance);
            double x = stag.getX() + horizontal.x;
            double z = stag.getZ() + horizontal.z;
            Support support = support(level, x, stag.getY(), z, player.getBbWidth());
            double y = support.highestY();
            AABB box = boxAt(x, y, z, player.getBbWidth(), player.getBbHeight());
            boolean clear = support.present()
                    && support.nonLiquid()
                    && support.variance() <= TrailStagPlacementAuthority.MAX_SUPPORT_HEIGHT_VARIANCE
                    && level.noCollision(player, box)
                    && level.getEntities(player, box.inflate(0.02)).stream().allMatch(entity -> entity == stag);
            probes.add(new TrailStagPlacementAuthority.DismountProbe(direction, clear, support.present() && support.nonLiquid()));
            positions.add(new Vec3(x, y, z));
        }
        Optional<TrailStagPlacementAuthority.RelativeDirection> chosen =
                TrailStagPlacementAuthority.firstLegalDismount(probes);
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        int index = TrailStagPlacementAuthority.DISMOUNT_ORDER.indexOf(chosen.get());
        return Optional.of(positions.get(index));
    }

    private static Vec3 emergencyDismount(ServerLevel level, ServerPlayer player, TrailStagEntity stag) {
        List<Vec3> candidates = new ArrayList<>();
        for (int vertical = 0; vertical <= 3; vertical++) {
            candidates.add(new Vec3(stag.getX(), stag.getY() + vertical, stag.getZ()));
        }
        return candidates.stream()
                .filter(pos -> level.noCollision(player, boxAt(
                        pos.x, pos.y, pos.z, player.getBbWidth(), player.getBbHeight())))
                .min(Comparator.comparingDouble(pos -> pos.distanceToSqr(player.position())))
                .orElse(new Vec3(stag.getX(), stag.getY() + Math.max(1.0, stag.getBbHeight()), stag.getZ()));
    }

    private static Support support(ServerLevel level, double x, double aroundY, double z, double footprintWidth) {
        double half = Math.max(0.05, footprintWidth * 0.5 - 0.08);
        double[][] samples = {
                {x, z},
                {x - half, z - half},
                {x - half, z + half},
                {x + half, z - half},
                {x + half, z + half}
        };
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        boolean nonLiquid = true;
        for (double[] sample : samples) {
            Optional<Surface> surface = findSurface(level, sample[0], aroundY, sample[1]);
            if (surface.isEmpty()) {
                return Support.missing();
            }
            min = Math.min(min, surface.get().y());
            max = Math.max(max, surface.get().y());
            nonLiquid &= surface.get().nonLiquid();
        }
        return new Support(true, nonLiquid, max, max - min);
    }

    private static Optional<Surface> findSurface(ServerLevel level, double x, double aroundY, double z) {
        int base = Mth.floor(aroundY);
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        for (int y = base + 1; y >= base - 3; y--) {
            BlockPos pos = new BlockPos(bx, y, bz);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double top = y + shape.max(Direction.Axis.Y);
            return Optional.of(new Surface(top, state.getFluidState().isEmpty()));
        }
        return Optional.empty();
    }

    private static Vec3 offset(
            float yawDegrees,
            TrailStagPlacementAuthority.RelativeDirection direction,
            double distance
    ) {
        double radians = Math.toRadians(yawDegrees);
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        Vec3 right = new Vec3(Math.cos(radians), 0.0, Math.sin(radians));
        Vec3 vector = switch (direction) {
            case LEFT -> right.scale(-1.0);
            case RIGHT -> right;
            case BEHIND -> forward.scale(-1.0);
            case BEHIND_LEFT -> forward.scale(-1.0).add(right.scale(-1.0));
            case BEHIND_RIGHT -> forward.scale(-1.0).add(right);
        };
        if (vector.lengthSqr() > 1.0001) {
            vector = vector.normalize();
        }
        return vector.scale(distance);
    }

    private static AABB boxAt(double x, double y, double z, double width, double height) {
        double half = width * 0.5;
        return new AABB(x - half, y, z - half, x + half, y + height, z + half);
    }

    private record SpawnSolution(TrailStagPlacementAuthority.SummonCandidate candidate, Vec3 position) {
    }

    private record Surface(double y, boolean nonLiquid) {
    }

    private record Support(boolean present, boolean nonLiquid, double highestY, double variance) {
        private static Support missing() {
            return new Support(
                    false,
                    false,
                    0.0,
                    TrailStagPlacementAuthority.MAX_SUPPORT_HEIGHT_VARIANCE + 1.0
            );
        }
    }
}
