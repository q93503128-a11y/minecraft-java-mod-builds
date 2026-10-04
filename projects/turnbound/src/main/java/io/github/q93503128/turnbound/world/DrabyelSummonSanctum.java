package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds a genuinely open patch outside New Drabyel and builds a permanent, non-destructive summon sanctum.
 *
 * <p>The sanctum is placed only into air above solid ground; source-map blocks are never replaced. The distinctive
 * center/runes make the structure rediscoverable after a world reload, so repeated summons do not create duplicates.</p>
 */
final class DrabyelSummonSanctum {
    record Stage(Vec3 position, float actorYaw) {}

    private static final int INNER_RADIUS = 6;
    private static final int ROOF_RADIUS = 7;
    private static final int ROOF_HEIGHT = 7;
    private static final int MIN_DISTANCE = 128;
    private static final int MAX_DISTANCE = 176;
    private static final Map<ServerLevel, BlockPos> CACHE = new IdentityHashMap<>();

    private DrabyelSummonSanctum() {}

    static Stage resolve(ServerLevel level, DrabyelHubServiceCatalog.Service service) {
        if (level == null || service == null || service.runtimePosition() == null) return null;
        Vec3 origin = new Vec3(
                service.runtimePosition().x() + 0.5D,
                service.runtimePosition().y(),
                service.runtimePosition().z() + 0.5D);

        BlockPos cached = CACHE.get(level);
        if (cached != null && sanctumBuilt(level, cached)) return stage(cached, origin);
        CACHE.remove(level);

        List<Candidate> candidates = candidates(level, origin);
        for (Candidate candidate : candidates) {
            BlockPos existing = existingCenter(level, candidate.x(), candidate.z());
            if (existing == null) continue;
            CACHE.put(level, existing);
            return stage(existing, origin);
        }

        for (Candidate candidate : candidates) {
            BlockPos center = safeCenter(level, candidate.x(), candidate.z());
            if (center == null) continue;
            build(level, center);
            CACHE.put(level, center);
            Turnbound.LOGGER.info("TURNBOUND built New Drabyel summon sanctum at {}, {}, {}",
                    center.getX(), center.getY(), center.getZ());
            return stage(center, origin);
        }

        Turnbound.LOGGER.warn("TURNBOUND could not find a safe site for the New Drabyel summon sanctum");
        return null;
    }

    static void clear() {
        CACHE.clear();
    }

    private static Stage stage(BlockPos center, Vec3 origin) {
        Vec3 position = new Vec3(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        double dx = origin.x - position.x;
        double dz = origin.z - position.z;
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        return new Stage(position, wrapYaw(yaw));
    }

    private static List<Candidate> candidates(ServerLevel level, Vec3 origin) {
        int ox = (int)Math.floor(origin.x);
        int oz = (int)Math.floor(origin.z);
        List<Candidate> out = new ArrayList<>();
        for (int dz = -MAX_DISTANCE; dz <= MAX_DISTANCE; dz += 8) {
            for (int dx = -MAX_DISTANCE; dx <= MAX_DISTANCE; dx += 8) {
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq < MIN_DISTANCE * MIN_DISTANCE || distanceSq > MAX_DISTANCE * MAX_DISTANCE) continue;
                int x = ox + dx;
                int z = oz + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int vertical = Math.abs(y - (int)Math.floor(origin.y));
                // Prefer reasonably close, level ground; deterministic tie-break avoids wandering stage placement.
                out.add(new Candidate(x, z, distanceSq + vertical * vertical * 18));
            }
        }
        out.sort(Comparator.comparingInt(Candidate::score)
                .thenComparingInt(Candidate::x).thenComparingInt(Candidate::z));
        return out;
    }

    private static BlockPos existingCenter(ServerLevel level, int x, int z) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int lowestCandidateY = surface - (ROOF_HEIGHT + 3);
        for (int y = surface - 1; y >= lowestCandidateY; y--) {
            BlockPos center = new BlockPos(x, y, z);
            if (sanctumBuilt(level, center)) return center;
        }
        return null;
    }

    private static boolean sanctumBuilt(ServerLevel level, BlockPos center) {
        if (!level.getBlockState(center).is(Blocks.CHISELED_DEEPSLATE)) return false;
        boolean floorSignature = level.getBlockState(center.offset(3, 0, 0)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(center.offset(-3, 0, 0)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(center.offset(0, 0, 3)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(center.offset(0, 0, -3)).is(Blocks.AMETHYST_BLOCK);
        if (!floorSignature) return false;

        // Roof blocks are the current structure-version signature. Old outdoor circles intentionally fail this check.
        return level.getBlockState(center.offset(0, ROOF_HEIGHT, 0)).is(Blocks.TINTED_GLASS)
                && level.getBlockState(center.offset(4, ROOF_HEIGHT, 0)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(center.offset(-4, ROOF_HEIGHT, 0)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(center.offset(0, ROOF_HEIGHT, 4)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(center.offset(0, ROOF_HEIGHT, -4)).is(Blocks.AMETHYST_BLOCK);
    }

    private static BlockPos safeCenter(ServerLevel level, int x, int z) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int dz = -ROOF_RADIUS; dz <= ROOF_RADIUS; dz++) {
            for (int dx = -ROOF_RADIUS; dx <= ROOF_RADIUS; dx++) {
                if (dx * dx + dz * dz > ROOF_RADIUS * ROOF_RADIUS) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                BlockPos below = new BlockPos(x + dx, y - 1, z + dz);
                if (level.getBlockState(below).isAir() || level.getBlockState(below).is(BlockTags.LEAVES)
                        || !level.getFluidState(below).isEmpty()) return null;
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxY - minY > 1) return null;

        int floorY = maxY;
        for (int dz = -ROOF_RADIUS; dz <= ROOF_RADIUS; dz++) {
            for (int dx = -ROOF_RADIUS; dx <= ROOF_RADIUS; dx++) {
                if (dx * dx + dz * dz > ROOF_RADIUS * ROOF_RADIUS) continue;
                BlockPos floor = new BlockPos(x + dx, floorY, z + dz);
                for (int dy = 0; dy <= ROOF_HEIGHT + 1; dy++) {
                    BlockPos pos = floor.above(dy);
                    if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) return null;
                }
            }
        }
        return new BlockPos(x, floorY, z);
    }

    private static void build(ServerLevel level, BlockPos center) {
        for (int dz = -INNER_RADIUS; dz <= INNER_RADIUS; dz++) {
            for (int dx = -INNER_RADIUS; dx <= INNER_RADIUS; dx++) {
                int radiusSq = dx * dx + dz * dz;
                if (radiusSq > INNER_RADIUS * INNER_RADIUS) continue;
                BlockPos floor = center.offset(dx, 0, dz);
                BlockPos support = floor.below();
                if (level.getBlockState(support).isAir()) {
                    level.setBlock(support, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                }

                var state = radiusSq >= 25
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.DEEPSLATE_TILES.defaultBlockState();
                if (dx == 0 && dz == 0) state = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                else if ((Math.abs(dx) == 3 && dz == 0) || (Math.abs(dz) == 3 && dx == 0)) {
                    state = Blocks.AMETHYST_BLOCK.defaultBlockState();
                } else if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                }
                level.setBlock(floor, state, 3);
            }
        }

        int[][] pillars = {
                {5,0},{-5,0},{0,5},{0,-5},
                {4,4},{-4,4},{4,-4},{-4,-4}
        };
        for (int[] pillar : pillars) {
            BlockPos base = center.offset(pillar[0], 1, pillar[1]);
            for (int y = 0; y < ROOF_HEIGHT - 1; y++) {
                level.setBlock(base.above(y),
                        (y == 2 || y == 5)
                                ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
                                : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                        3);
            }
        }

        // A real ceiling keeps the reveal composition independent from weather/sky and reads as a dedicated chamber.
        for (int dz = -ROOF_RADIUS; dz <= ROOF_RADIUS; dz++) {
            for (int dx = -ROOF_RADIUS; dx <= ROOF_RADIUS; dx++) {
                int radiusSq = dx * dx + dz * dz;
                if (radiusSq > ROOF_RADIUS * ROOF_RADIUS) continue;
                BlockPos roof = center.offset(dx, ROOF_HEIGHT, dz);
                var state = radiusSq >= 40
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    state = Blocks.TINTED_GLASS.defaultBlockState();
                } else if ((Math.abs(dx) == 4 && dz == 0) || (Math.abs(dz) == 4 && dx == 0)) {
                    state = Blocks.AMETHYST_BLOCK.defaultBlockState();
                }
                level.setBlock(roof, state, 3);
            }
        }

        int[][] lights = {{3,3},{-3,3},{3,-3},{-3,-3}};
        for (int[] light : lights) {
            level.setBlock(center.offset(light[0], ROOF_HEIGHT - 1, light[1]),
                    Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }
    }

    private static float wrapYaw(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private record Candidate(int x, int z, int score) {}
}
