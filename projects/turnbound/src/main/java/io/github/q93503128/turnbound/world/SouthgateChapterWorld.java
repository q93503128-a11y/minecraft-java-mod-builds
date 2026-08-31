package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Authored Southgate Chapter 1 ribbon that connects the starter field to canonical FT_MEADOW and B01.
 * Only the playable route/clearings are authored; the full 1024x1024 Aster March terrain remains a later content pass.
 */
public final class SouthgateChapterWorld {
    private static final int LAYOUT_VERSION = 17;
    private static final int MARKER_X = 190;
    private static final int MARKER_Y = 61;
    private static final int MARKER_Z = 230;

    private record Node(double x, int groundY, double z) {}

    public record BuiltChapter(
            int starterBaseY,
            Vec3 m03Home, Vec3 m03End,
            Vec3 m04Home, Vec3 m04End, Vec3 m04BattleAnchor,
            Vec3 m05Home, Vec3 m05End, Vec3 m05BattleAnchor,
            Vec3 meadowRelay,
            Vec3 bossApproach,
            Vec3 bossAnchor,
            float bossYaw) {}

    private SouthgateChapterWorld() {}

    public static BuiltChapter build(ServerLevel level, int starterBaseY) {
        BuiltChapter chapter = built(starterBaseY);
        if (!hasMarker(level)) {
            List<Node> route = route(starterBaseY);
            for (int i = 0; i < route.size() - 1; i++) buildSegment(level, route.get(i), route.get(i + 1));
            clearing(level, 92, 66, 207, 15);
            clearing(level, 220, 66, 230, 16);
            clearing(level, 286, 66, 240, 16);
            clearing(level, 355, 67, 245, 22);
            buildMeadowRelay(level);
            buildWatchRuins(level);
            buildBossGate(level, false);
            buildEntryGate(level, starterBaseY, false);
            writeMarker(level);
        }
        return chapter;
    }

    public static void setEntryGateOpen(ServerLevel level, int starterBaseY, boolean open) {
        buildEntryGate(level, starterBaseY, open);
    }

    public static void setBossGateOpen(ServerLevel level, boolean open) {
        buildBossGate(level, open);
    }

    public static boolean contains(BuiltChapter chapter, Vec3 pos) {
        if (pos == null || pos.y < Math.min(chapter.starterBaseY(), 58) - 8 || pos.y > 92) return false;
        if (!AsterMarchRegionCatalog.SOUTHGATE.contains(pos.x, pos.z)) return false;
        List<Node> nodes = route(chapter.starterBaseY());
        for (int i = 0; i < nodes.size() - 1; i++) {
            if (distanceSqToSegment(pos.x, pos.z, nodes.get(i), nodes.get(i + 1)) <= 24.0 * 24.0) return true;
        }
        return near(pos, 92, 207, 24) || near(pos, 220, 230, 25) || near(pos, 286, 240, 25) || near(pos, 355, 245, 30);
    }

    private static BuiltChapter built(int starterBaseY) {
        AsterMarchRegionCatalog.Anchor boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01);
        AsterMarchRegionCatalog.Anchor relay = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_MEADOW);
        return new BuiltChapter(
                starterBaseY,
                new Vec3(88.0, 67.0, 205.0), new Vec3(104.0, 67.0, 211.0),
                new Vec3(218.0, 67.0, 228.0), new Vec3(232.0, 67.0, 235.0), new Vec3(220.0, 67.0, 230.0),
                new Vec3(282.0, 67.0, 237.0), new Vec3(298.0, 67.0, 244.0), new Vec3(286.0, 67.0, 240.0),
                new Vec3(relay.x(), relay.y(), relay.z()),
                new Vec3(342.0, 68.0, 245.0),
                new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw());
    }

    private static List<Node> route(int starterBaseY) {
        return List.of(
                new Node(0, starterBaseY, 190),
                new Node(88, 66, 205),
                new Node(190, 66, 230),
                new Node(220, 66, 230),
                new Node(286, 66, 240),
                new Node(330, 67, 245),
                new Node(355, 67, 245));
    }

    private static void buildSegment(ServerLevel level, Node a, Node b) {
        int steps = Math.max(1, (int)Math.ceil(Math.max(Math.abs(b.x - a.x), Math.abs(b.z - a.z))));
        for (int step = 0; step <= steps; step++) {
            double t = step / (double)steps;
            int cx = (int)Math.round(lerp(a.x, b.x, t));
            int cz = (int)Math.round(lerp(a.z, b.z, t));
            int y = (int)Math.round(lerp(a.groundY, b.groundY, t));
            double dx = b.x - a.x;
            double dz = b.z - a.z;
            double length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            double rx = -dz / length;
            double rz = dx / length;
            for (int offset = -7; offset <= 7; offset++) {
                int x = (int)Math.round(cx + rx * offset);
                int z = (int)Math.round(cz + rz * offset);
                for (int fy = y - 3; fy < y; fy++) set(level, x, fy, z, Blocks.DIRT);
                Block ground = Math.abs(offset) <= 2
                        ? (((cx + cz + offset) & 3) == 0 ? Blocks.COBBLESTONE : Blocks.GRAVEL)
                        : (((cx * 13 + cz * 7 + offset) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
                set(level, x, y, z, ground);
                for (int ay = y + 1; ay <= y + 6; ay++) set(level, x, ay, z, Blocks.AIR);
            }
            if (step % 28 == 0 && step > 8 && step < steps - 8) lanternPost(level, cx + (int)Math.round(rx * 6), y, cz + (int)Math.round(rz * 6));
        }
    }

    private static void clearing(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                if (d2 > radius * radius) continue;
                for (int fy = y - 3; fy < y; fy++) set(level, x, fy, z, Blocks.DIRT);
                set(level, x, y, z, ((x * 31 + z * 17) & 9) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
                for (int ay = y + 1; ay <= y + 8; ay++) set(level, x, ay, z, Blocks.AIR);
            }
        }
    }

    private static void buildMeadowRelay(ServerLevel level) {
        int cx = 190, y = 66, cz = 230;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx * dx + dz * dz > 10) continue;
            set(level, cx + dx, y, cz + dz, ((dx + dz) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
        }
        set(level, cx, y + 1, cz, Blocks.AMETHYST_BLOCK);
        set(level, cx, y + 2, cz, Blocks.LIGHTNING_ROD);
        for (int[] p : new int[][]{{-4,-4},{4,-4},{-4,4},{4,4}}) lanternPost(level, cx + p[0], y, cz + p[1]);
    }

    private static void buildWatchRuins(ServerLevel level) {
        for (int z = 224; z <= 248; z += 4) {
            int h = 2 + Math.floorMod(z, 3);
            for (int dy = 1; dy <= h; dy++) set(level, 312, 67 + dy, z, dy == h ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
        }
        for (int x = 303; x <= 314; x++) set(level, x, 68, 220, (x & 1) == 0 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE);
    }

    private static void buildEntryGate(ServerLevel level, int starterBaseY, boolean open) {
        int x = 20;
        int y = (int)Math.round(lerp(starterBaseY, 66, 20.0 / 88.0));
        for (int z = 187; z <= 201; z++) {
            for (int dy = 1; dy <= 4; dy++) set(level, x, y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
        }
        for (int z : new int[]{186, 202}) {
            for (int dy = 1; dy <= 5; dy++) set(level, x, y + dy, z, Blocks.STONE_BRICKS);
            set(level, x, y + 6, z, Blocks.LANTERN);
        }
    }

    private static void buildBossGate(ServerLevel level, boolean open) {
        int x = 330, y = 67;
        for (int z = 237; z <= 253; z++) {
            for (int dy = 1; dy <= 5; dy++) set(level, x, y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
        }
        for (int z : new int[]{236, 254}) {
            for (int dy = 1; dy <= 7; dy++) set(level, x, y + dy, z, dy >= 6 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
            set(level, x, y + 8, z, Blocks.SOUL_LANTERN);
        }
        for (int z = 236; z <= 254; z++) set(level, x, y + 7, z, Blocks.STONE_BRICKS);
    }

    private static void lanternPost(ServerLevel level, int x, int groundY, int z) {
        set(level, x, groundY + 1, z, Blocks.COBBLESTONE_WALL);
        set(level, x, groundY + 2, z, Blocks.LANTERN);
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(MARKER_X, MARKER_Y, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, MARKER_Y, MARKER_Z)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(new BlockPos(MARKER_X + 2, MARKER_Y, MARKER_Z)).is(Blocks.CRYING_OBSIDIAN)
                && level.getBlockState(new BlockPos(MARKER_X + 3, MARKER_Y, MARKER_Z)).is(Blocks.COPPER_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, MARKER_X, MARKER_Y, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, MARKER_Y, MARKER_Z, Blocks.AMETHYST_BLOCK);
        set(level, MARKER_X + 2, MARKER_Y, MARKER_Z, Blocks.CRYING_OBSIDIAN);
        set(level, MARKER_X + 3, MARKER_Y, MARKER_Z, Blocks.COPPER_BLOCK);
    }

    private static boolean near(Vec3 pos, double x, double z, double radius) {
        double dx = pos.x - x, dz = pos.z - z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static double distanceSqToSegment(double px, double pz, Node a, Node b) {
        double vx = b.x - a.x, vz = b.z - a.z;
        double wx = px - a.x, wz = pz - a.z;
        double vv = vx * vx + vz * vz;
        double t = vv <= 0.000001 ? 0.0 : Math.max(0.0, Math.min(1.0, (wx * vx + wz * vz) / vv));
        double dx = px - (a.x + vx * t), dz = pz - (a.z + vz * t);
        return dx * dx + dz * dz;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
