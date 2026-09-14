package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Production owner for the first authored Hub -> REGION_01 placement.
 *
 * The block composition still shares the M6 prototype palette while the visual gate is open, but normal gameplay
 * no longer derives placement from an arbitrary operator position. This layer owns terrain selection, ground
 * stitching and production identity so the debug prototype command can remain a separate comparison harness.
 */
public final class AuthoredFirstRegionBuilder {
    public static final String AUTHORED_ENTITY_TAG = "turnbound_re:authored_first_region";
    private static final int UPDATE_FLAGS = 3;
    private static final int FOUNDATION_DEPTH = 12;
    private static final int[] CANDIDATE_OFFSETS = {0, 64, -64, 128, -128};

    private static final int[][] TERRAIN_SAMPLES = {
            {-11,-9},{-11,9},{0,0},{11,-9},{11,9},
            {20,0},{32,0},{42,-12},{42,12},{52,0},{60,15},{62,-6},{72,0},{78,0}
    };

    public record TerrainChoice(BlockPos origin, int relief, int wetSamples, int distanceSquared) {
        public TerrainChoice {
            if (origin == null) throw new IllegalArgumentException("origin required");
            if (relief < 0 || wetSamples < 0 || distanceSquared < 0) {
                throw new IllegalArgumentException("terrain metrics must be >= 0");
            }
        }
    }

    public record Result(
            BlockPos origin,
            BlockPos hubArrival,
            TerrainChoice terrain,
            List<UUID> encounterAnchors,
            List<UUID> travelAnchors
    ) {
        public Result {
            if (origin == null || hubArrival == null || terrain == null
                    || encounterAnchors == null || travelAnchors == null) {
                throw new IllegalArgumentException("authored region result fields required");
            }
            encounterAnchors = List.copyOf(encounterAnchors);
            travelAnchors = List.copyOf(travelAnchors);
        }
    }

    private record Candidate(int x, int z, int minSurfaceY, int maxSurfaceY, int wetSamples, int distanceSquared) {
        int relief() { return maxSurfaceY - minSurfaceY; }
        long score() {
            // Water is a hard preference against; relief dominates distance so a slightly farther flat site wins.
            return (long) wetSamples * 1_000_000L + (long) relief() * 10_000L + distanceSquared;
        }
    }

    private AuthoredFirstRegionBuilder() {}

    public static TerrainChoice chooseTerrain(ServerLevel level, BlockPos worldSpawn) {
        if (level == null || worldSpawn == null) throw new IllegalArgumentException("level/worldSpawn required");
        List<Candidate> candidates = new ArrayList<>();
        for (int dx : CANDIDATE_OFFSETS) {
            for (int dz : CANDIDATE_OFFSETS) {
                candidates.add(sample(level, worldSpawn.getX() + dx, worldSpawn.getZ() + dz, dx * dx + dz * dz));
            }
        }
        Candidate best = candidates.stream().min(Comparator.comparingLong(Candidate::score)).orElseThrow();
        // Heightmap positions are the first free block above the motion-blocking surface; authored floor occupies y-1.
        BlockPos origin = new BlockPos(best.x(), best.maxSurfaceY() - 1, best.z());
        return new TerrainChoice(origin, best.relief(), best.wetSamples(), best.distanceSquared());
    }

    public static Result build(
            ServerPlayer bootstrapPlayer,
            DefinitionRegistry definitions,
            BlockPos worldSpawn
    ) {
        if (bootstrapPlayer == null || definitions == null || worldSpawn == null) {
            throw new IllegalArgumentException("bootstrapPlayer/definitions/worldSpawn required");
        }
        ServerLevel level = bootstrapPlayer.level();
        String dimension = level.dimension().identifier().toString();
        if (!FunctionalWorldSliceLayout.DIMENSION.equals(dimension)) {
            throw new IllegalStateException("authored first region requires " + FunctionalWorldSliceLayout.DIMENSION);
        }

        TerrainChoice terrain = chooseTerrain(level, worldSpawn);
        BlockPos origin = terrain.origin();
        level.getChunkAt(origin);

        bootstrapPlayer.stopRiding();
        bootstrapPlayer.teleportTo(
                level,
                origin.getX() + 0.5D,
                origin.getY() + 1.0D,
                origin.getZ() + 0.5D,
                Set.of(),
                bootstrapPlayer.getYRot(),
                bootstrapPlayer.getXRot(),
                false);

        ProductionWorldSlicePrototypeBuilder.Result blocks =
                ProductionWorldSlicePrototypeBuilder.build(bootstrapPlayer, definitions);
        WorldFastTravelPrototype.Result travel = WorldFastTravelPrototype.install(bootstrapPlayer, definitions, blocks.origin());

        stitchFoundations(level, blocks.origin());
        retag(level, blocks.encounterAnchors());
        retag(level, travel.anchorEntityIds());

        FastTravelSavedData.AnchorLocation hub = FastTravelSavedData.get(level.getServer())
                .anchor(WorldFastTravelPrototype.HUB_LOCATOR)
                .orElseThrow(() -> new IllegalStateException("authored Hub waypoint was not registered"));
        return new Result(
                blocks.origin(),
                hub.blockPos(),
                terrain,
                blocks.encounterAnchors(),
                travel.anchorEntityIds());
    }

    private static Candidate sample(ServerLevel level, int originX, int originZ, int distanceSquared) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int wet = 0;
        for (int[] offset : TERRAIN_SAMPLES) {
            BlockPos probe = new BlockPos(originX + offset[0], 0, originZ + offset[1]);
            level.getChunkAt(probe);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            minY = Math.min(minY, surface.getY());
            maxY = Math.max(maxY, surface.getY());
            if (!level.getFluidState(surface.below()).isEmpty()) wet++;
        }
        return new Candidate(originX, originZ, minY, maxY, wet, distanceSquared);
    }

    private static void retag(ServerLevel level, List<UUID> ids) {
        for (UUID id : ids) {
            Entity entity = level.getEntity(id);
            if (entity == null) continue;
            entity.entityTags().remove(ProductionWorldSlicePrototypeBuilder.PROTOTYPE_ENTITY_TAG);
            entity.entityTags().add(AUTHORED_ENTITY_TAG);
        }
    }

    private static void stitchFoundations(ServerLevel level, BlockPos origin) {
        supportArea(level, origin, -11, 11, -9, 9, Blocks.STONE_BRICKS.defaultBlockState());
        supportArea(level, origin, 11, ProductionWorldSlicePlan.ROUTE_END_X, -4, 4, Blocks.DIRT.defaultBlockState());
        supportFootprint(level, origin, ProductionWorldSlicePlan.MINE, Blocks.STONE.defaultBlockState());
        supportFootprint(level, origin, ProductionWorldSlicePlan.FARM, Blocks.DIRT.defaultBlockState());
        supportFootprint(level, origin, ProductionWorldSlicePlan.RIVER, Blocks.GRAVEL.defaultBlockState());
        supportFootprint(level, origin, ProductionWorldSlicePlan.PATROL, Blocks.STONE.defaultBlockState());
        supportFootprint(level, origin, ProductionWorldSlicePlan.ELITE, Blocks.DEEPSLATE.defaultBlockState());
    }

    private static void supportFootprint(
            ServerLevel level,
            BlockPos origin,
            ProductionWorldSlicePlan.Footprint footprint,
            BlockState support
    ) {
        supportArea(level, origin, footprint.minX(), footprint.maxX(), footprint.minZ(), footprint.maxZ(), support);
    }

    private static void supportArea(
            ServerLevel level,
            BlockPos origin,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            BlockState support
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos floor = origin.offset(x, 0, z);
                if (level.getBlockState(floor).isAir()) continue;
                for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
                    BlockPos below = floor.below(depth);
                    BlockState current = level.getBlockState(below);
                    if (!current.isAir() && level.getFluidState(below).isEmpty()) break;
                    level.setBlock(below, support, UPDATE_FLAGS);
                }
            }
        }
    }
}
