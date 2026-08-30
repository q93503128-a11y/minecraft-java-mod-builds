package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * P2 authored ribbon joining Radia South Gate, the canonical FT_MEADOW anchor and B01.
 * It deliberately builds only playable routes/clearings rather than flattening the whole 510x240 region.
 */
public final class SouthgateMeadowExpansion {
    public static final Vec3 SOUTH_GATE = new Vec3(0.5, 65.0, 128.0);
    public static final Vec3 EAST_JUNCTION = new Vec3(31.5, 65.0, 230.5);
    public static final Vec3 M04_CLEARING = new Vec3(248.0, 67.0, 245.0);
    public static final Vec3 M05_CLEARING = new Vec3(236.0, 67.0, 300.0);
    public static final Vec3 BOSS_GATE = new Vec3(330.0, 68.0, 243.0);

    private SouthgateMeadowExpansion() {}

    public static void build(ServerLevel level) {
        Vec3 radia = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_RADIA).position();
        Vec3 meadow = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_MEADOW).position();
        Vec3 boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position();

        buildPlaza(level, radia, 6, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        buildRoad(level, radia, SOUTH_GATE, 4);
        buildRoad(level, EAST_JUNCTION, meadow, 4);
        buildPlaza(level, meadow, 8, Blocks.POLISHED_ANDESITE, Blocks.MOSSY_STONE_BRICKS);
        buildRoad(level, meadow, M04_CLEARING, 5);
        buildClearing(level, M04_CLEARING, 15);
        buildRoad(level, meadow, M05_CLEARING, 4);
        buildClearing(level, M05_CLEARING, 13);
        buildRoad(level, M04_CLEARING, boss, 5);
        buildBossArena(level, boss);
        buildBossGate(level);
    }

    public static void unlockBossGate(ServerLevel level) {
        int x = (int) Math.round(BOSS_GATE.x);
        int z = (int) Math.round(BOSS_GATE.z);
        for (int dz = -5; dz <= 5; dz++) {
            for (int y = 68; y <= 72; y++) set(level, x, y, z + dz, Blocks.AIR);
        }
    }

    public static boolean allowedPosition(Vec3 position, boolean meadowUnlocked, boolean bossUnlocked) {
        Vec3 radia = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_RADIA).position();
        Vec3 meadow = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_MEADOW).position();
        Vec3 boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position();

        if (distanceSq(position, radia) <= 20.0 * 20.0) return true;
        if (distanceSqToSegment(position, radia, SOUTH_GATE) <= 13.0 * 13.0) return true;
        if (FieldCellA01.containsXZ(position.x, position.z)) return true;
        if (!meadowUnlocked) return false;

        if (FieldCellA02.containsXZ(position.x, position.z)) return true;
        if (distanceSqToSegment(position, EAST_JUNCTION, meadow) <= 18.0 * 18.0) return true;
        if (distanceSq(position, meadow) <= 82.0 * 82.0) return true;
        if (distanceSqToSegment(position, meadow, M05_CLEARING) <= 18.0 * 18.0) return true;
        if (distanceSq(position, M05_CLEARING) <= 24.0 * 24.0) return true;
        if (distanceSqToSegment(position, meadow, M04_CLEARING) <= 18.0 * 18.0) return true;
        if (distanceSq(position, M04_CLEARING) <= 25.0 * 25.0) return true;
        if (distanceSqToSegment(position, M04_CLEARING, BOSS_GATE) <= 18.0 * 18.0) return true;
        if (!bossUnlocked) return false;
        return distanceSqToSegment(position, BOSS_GATE, boss) <= 22.0 * 22.0
                || distanceSq(position, boss) <= 35.0 * 35.0;
    }

    static double distanceSqToSegment(Vec3 point, Vec3 a, Vec3 b) {
        double abX = b.x - a.x;
        double abZ = b.z - a.z;
        double apX = point.x - a.x;
        double apZ = point.z - a.z;
        double denom = abX * abX + abZ * abZ;
        if (denom <= 1.0e-9) return distanceSq(point, a);
        double t = Math.max(0.0, Math.min(1.0, (apX * abX + apZ * abZ) / denom));
        double dx = point.x - (a.x + abX * t);
        double dz = point.z - (a.z + abZ * t);
        return dx * dx + dz * dz;
    }

    private static double distanceSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static void buildRoad(ServerLevel level, Vec3 from, Vec3 to, int halfWidth) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(to.x - from.x), Math.abs(to.z - from.z))));
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int cx = (int) Math.round(from.x + (to.x - from.x) * t);
            int cz = (int) Math.round(from.z + (to.z - from.z) * t);
            int standingY = (int) Math.round(from.y + (to.y - from.y) * t);
            int surfaceY = standingY - 1;
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    if (dx * dx + dz * dz > halfWidth * halfWidth + 2) continue;
                    prepareGround(level, cx + dx, surfaceY, cz + dz,
                            (Math.abs(dx) + Math.abs(dz) <= 2 && ((cx + cz) & 3) != 0) ? Blocks.GRAVEL : Blocks.COARSE_DIRT);
                }
            }
        }
    }

    private static void buildPlaza(ServerLevel level, Vec3 anchor, int radius, Block a, Block b) {
        int cx = (int) Math.round(anchor.x);
        int cz = (int) Math.round(anchor.z);
        int surfaceY = (int) Math.round(anchor.y) - 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                prepareGround(level, cx + dx, surfaceY, cz + dz, ((dx + dz) & 1) == 0 ? a : b);
            }
        }
        set(level, cx, surfaceY, cz, Blocks.AMETHYST_BLOCK);
        for (int[] offset : new int[][]{{-radius,0},{radius,0},{0,-radius},{0,radius}}) {
            set(level, cx + offset[0], surfaceY + 1, cz + offset[1], Blocks.STONE_BRICK_WALL);
            set(level, cx + offset[0], surfaceY + 2, cz + offset[1], Blocks.GLOWSTONE);
        }
    }

    private static void buildClearing(ServerLevel level, Vec3 center, int radius) {
        int cx = (int) Math.round(center.x);
        int cz = (int) Math.round(center.z);
        int surfaceY = (int) Math.round(center.y) - 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Block top = ((dx * 13 + dz * 7) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK;
                prepareGround(level, cx + dx, surfaceY, cz + dz, top);
            }
        }
        for (int[] rock : new int[][]{{-radius + 2,3},{radius - 3,-4},{4,radius - 3},{-5,-radius + 4}}) {
            set(level, cx + rock[0], surfaceY + 1, cz + rock[1], Blocks.MOSSY_COBBLESTONE);
        }
    }

    private static void buildBossArena(ServerLevel level, Vec3 center) {
        int cx = (int) Math.round(center.x);
        int cz = (int) Math.round(center.z);
        int surfaceY = (int) Math.round(center.y) - 1;
        int radius = 18;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int r2 = dx * dx + dz * dz;
                if (r2 > radius * radius) continue;
                Block top = r2 < 11 * 11
                        ? (((dx + dz) & 3) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS)
                        : (((dx * 5 + dz * 11) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
                prepareGround(level, cx + dx, surfaceY, cz + dz, top);
            }
        }
        for (int[] p : new int[][]{{-15,-8},{-15,8},{15,-8},{15,8}}) {
            set(level, cx + p[0], surfaceY + 1, cz + p[1], Blocks.STONE_BRICKS);
            set(level, cx + p[0], surfaceY + 2, cz + p[1], Blocks.STONE_BRICKS);
            set(level, cx + p[0], surfaceY + 3, cz + p[1], Blocks.GLOWSTONE);
        }
    }

    private static void buildBossGate(ServerLevel level) {
        int x = (int) Math.round(BOSS_GATE.x);
        int z = (int) Math.round(BOSS_GATE.z);
        for (int dz = -5; dz <= 5; dz++) {
            for (int y = 68; y <= 71; y++) set(level, x, y, z + dz, Blocks.IRON_BARS);
        }
        for (int dz : new int[]{-6,6}) {
            for (int y = 67; y <= 72; y++) set(level, x, y, z + dz, Blocks.STONE_BRICKS);
        }
    }

    private static void prepareGround(ServerLevel level, int x, int surfaceY, int z, Block top) {
        for (int y = surfaceY - 2; y < surfaceY; y++) set(level, x, y, z, Blocks.DIRT);
        set(level, x, surfaceY, z, top);
        for (int y = surfaceY + 1; y <= surfaceY + 6; y++) set(level, x, y, z, Blocks.AIR);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x, y, z), block.defaultBlockState());
    }
}
