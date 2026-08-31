package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Authored Chapter 2 ribbon through canonical Gloamwood, including FT_GLOAM and the exact B02 anchor. */
public final class GloamwoodChapterWorld {
    private static final int MARKER_X = -40;
    private static final int MARKER_Y = 58;
    private static final int MARKER_Z = -300;

    private record Node(double x, int groundY, double z) {}

    public record EncounterPoint(String id, Vec3 fieldPosition, Vec3 battleAnchor, float battleYaw) {}
    public record BuiltChapter(
            Vec3 entry,
            Vec3 fastTravel,
            Vec3 bossAnchor,
            float bossYaw,
            List<Vec3> sporeLanterns,
            List<EncounterPoint> encounters) {
        public BuiltChapter {
            sporeLanterns = List.copyOf(sporeLanterns);
            encounters = List.copyOf(encounters);
        }
    }

    private GloamwoodChapterWorld() {}

    public static BuiltChapter build(ServerLevel level) {
        BuiltChapter chapter = built();
        if (!hasMarker(level)) {
            List<Node> route = route();
            for (int i = 0; i < route.size() - 1; i++) buildSegment(level, route.get(i), route.get(i + 1));
            clearing(level, -20, 67, -185, 15);
            clearing(level, -55, 69, -250, 16);
            clearing(level, -40, 69, -300, 17);
            clearing(level, -38, 69, -320, 15);
            clearing(level, -75, 70, -365, 16);
            clearing(level, -35, 71, -405, 17);
            clearing(level, -35, 71, -440, 22);
            buildFastTravel(level);
            buildSporeLandmarks(level);
            buildDeepGate(level, false);
            buildBossGate(level, false);
            buildForestDetails(level);
            writeMarker(level);
        }
        return chapter;
    }

    public static boolean contains(Vec3 p) {
        if (p == null || p.y < 54 || p.y > 102) return false;
        return p.x >= AsterMarchRegionCatalog.GLOAMWOOD.minX() - 8
                && p.x <= AsterMarchRegionCatalog.GLOAMWOOD.maxX() + 8
                && p.z >= AsterMarchRegionCatalog.GLOAMWOOD.minZ() - 8
                && p.z <= -112;
    }

    public static void setDeepGateOpen(ServerLevel level, boolean open) { buildDeepGate(level, open); }
    public static void setBossGateOpen(ServerLevel level, boolean open) { buildBossGate(level, open); }

    private static BuiltChapter built() {
        AsterMarchRegionCatalog.Anchor ft = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_GLOAM);
        AsterMarchRegionCatalog.Anchor boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B02);
        return new BuiltChapter(
                new Vec3(-3.0, 68.0, -145.0),
                new Vec3(ft.x(), ft.y(), ft.z()),
                new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw(),
                List.of(
                        new Vec3(-7.0, 68.0, -170.0),
                        new Vec3(-43.0, 69.0, -222.0),
                        new Vec3(-27.0, 70.0, -270.0)),
                List.of(
                        point("ENC_G01", -20, 68, -185, -18, 68, -188, 180),
                        point("ENC_G02", -55, 70, -250, -55, 70, -253, 180),
                        point("ENC_G03", -38, 70, -320, -38, 70, -323, 180),
                        point("ENC_G04", -75, 71, -365, -75, 71, -368, 180),
                        point("ENC_G05", -35, 72, -405, -35, 72, -408, 180),
                        new EncounterPoint("BATTLE_B02", new Vec3(-35, 72, -426), new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw())));
    }

    private static EncounterPoint point(String id, double fx, double fy, double fz, double bx, double by, double bz, float yaw) {
        return new EncounterPoint(id, new Vec3(fx, fy, fz), new Vec3(bx, by, bz), yaw);
    }

    private static List<Node> route() {
        return List.of(
                new Node(0, 65, -112),
                new Node(-3, 67, -145),
                new Node(-20, 67, -185),
                new Node(-43, 68, -222),
                new Node(-55, 69, -250),
                new Node(-27, 69, -270),
                new Node(-40, 69, -300),
                new Node(-38, 69, -320),
                new Node(-75, 70, -365),
                new Node(-35, 71, -405),
                new Node(-35, 71, -440));
    }

    private static void buildSegment(ServerLevel level, Node a, Node b) {
        int steps = Math.max(1, (int)Math.ceil(Math.max(Math.abs(b.x - a.x), Math.abs(b.z - a.z))));
        for (int step = 0; step <= steps; step++) {
            double t = step / (double)steps;
            int cx = (int)Math.round(lerp(a.x, b.x, t));
            int cz = (int)Math.round(lerp(a.z, b.z, t));
            int y = (int)Math.round(lerp(a.groundY, b.groundY, t));
            double dx = b.x - a.x, dz = b.z - a.z;
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            double rx = -dz / len, rz = dx / len;
            for (int offset = -7; offset <= 7; offset++) {
                int x = (int)Math.round(cx + rx * offset);
                int z = (int)Math.round(cz + rz * offset);
                for (int fy = y - 3; fy < y; fy++) set(level, x, fy, z, Blocks.DIRT);
                Block ground;
                if (Math.abs(offset) <= 2) ground = ((cx + cz + offset) & 3) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.PODZOL;
                else ground = ((cx * 17 + cz * 11 + offset) & 7) == 0 ? Blocks.MOSS_BLOCK : Blocks.GRASS_BLOCK;
                set(level, x, y, z, ground);
                for (int ay = y + 1; ay <= y + 8; ay++) set(level, x, ay, z, Blocks.AIR);
            }
        }
    }

    private static void clearing(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                if (d2 > radius * radius) continue;
                for (int fy = y - 3; fy < y; fy++) set(level, x, fy, z, Blocks.DIRT);
                Block ground = ((x * 31 + z * 13) & 7) == 0 ? Blocks.MOSS_BLOCK : Blocks.PODZOL;
                set(level, x, y, z, ground);
                for (int ay = y + 1; ay <= y + 10; ay++) set(level, x, ay, z, Blocks.AIR);
            }
        }
    }

    private static void buildFastTravel(ServerLevel level) {
        int cx = -40, y = 69, cz = -300;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (dx * dx + dz * dz > 18) continue;
            set(level, cx + dx, y, cz + dz, ((dx + dz) & 1) == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.POLISHED_ANDESITE);
        }
        set(level, cx, y + 1, cz, Blocks.AMETHYST_BLOCK);
        set(level, cx, y + 2, cz, Blocks.BEACON);
        for (int[] p : new int[][]{{-5,-5},{5,-5},{-5,5},{5,5}}) post(level, cx + p[0], y, cz + p[1]);
    }

    private static void buildSporeLandmarks(ServerLevel level) {
        for (Vec3 p : built().sporeLanterns()) {
            int x = (int)Math.round(p.x), z = (int)Math.round(p.z), y = (int)Math.round(p.y) - 1;
            set(level, x, y, z, Blocks.MOSS_BLOCK);
            set(level, x, y + 1, z, Blocks.GLOWSTONE);
            set(level, x, y + 2, z, Blocks.OAK_FENCE);
            set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) == 3) set(level, x + dx, y, z + dz, Blocks.MOSS_BLOCK);
            }
        }
    }

    private static void buildDeepGate(ServerLevel level, boolean open) {
        int cz = -286, cy = 69;
        for (int x = -52; x <= -28; x++) {
            boolean center = x >= -44 && x <= -36;
            for (int y = cy + 1; y <= cy + 5; y++) {
                if (center) set(level, x, y, cz, open ? Blocks.AIR : Blocks.DARK_OAK_LOG);
                else if ((x & 1) == 0) set(level, x, y, cz, Blocks.MOSSY_STONE_BRICKS);
            }
        }
        post(level, -50, cy + 4, cz); post(level, -30, cy + 4, cz);
    }

    private static void buildBossGate(ServerLevel level, boolean open) {
        int cz = -420, cy = 71;
        for (int x = -47; x <= -23; x++) {
            boolean center = x >= -40 && x <= -30;
            for (int y = cy + 1; y <= cy + 6; y++) {
                if (center) set(level, x, y, cz, open ? Blocks.AIR : Blocks.IRON_BARS);
                else set(level, x, y, cz, ((x + y) & 3) == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        for (int x = -48; x <= -22; x++) set(level, x, cy + 7, cz, Blocks.MOSSY_STONE_BRICKS);
        post(level, -46, cy + 6, cz); post(level, -24, cy + 6, cz);
    }

    private static void buildForestDetails(ServerLevel level) {
        int[][] trees = {
                {-26,-155},{18,-165},{-52,-190},{5,-205},{-78,-230},{-18,-238},{-82,-270},{5,-278},
                {-72,-305},{2,-315},{-102,-342},{-45,-350},{-110,-380},{-5,-380},{-74,-410},{5,-425},
                {-92,-452},{18,-455},{-125,-290},{35,-245},{-130,-360},{42,-335},{-120,-430},{30,-400}
        };
        for (int i = 0; i < trees.length; i++) tree(level, trees[i][0], groundAt(trees[i][1]), trees[i][1], i % 3 == 0);
        for (int i = 0; i < 36; i++) {
            int x = -120 + Math.floorMod(i * 37, 170);
            int z = -150 - Math.floorMod(i * 53, 300);
            int y = groundAt(z);
            set(level, x, y + 1, z, i % 5 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
        }
    }

    private static int groundAt(int z) {
        if (z > -200) return 67;
        if (z > -285) return 69;
        if (z > -380) return 70;
        return 71;
    }

    private static void tree(ServerLevel level, int x, int groundY, int z, boolean spruce) {
        Block log = spruce ? Blocks.SPRUCE_LOG : Blocks.DARK_OAK_LOG;
        Block leaves = spruce ? Blocks.SPRUCE_LEAVES : Blocks.DARK_OAK_LEAVES;
        int h = spruce ? 7 : 6;
        for (int y = 1; y <= h; y++) set(level, x, groundY + y, z, log);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = h - 2; dy <= h + 1; dy++) {
            if (Math.abs(dx) + Math.abs(dz) > 4) continue;
            set(level, x + dx, groundY + dy, z + dz, leaves);
        }
    }

    private static void post(ServerLevel level, int x, int groundY, int z) {
        set(level, x, groundY + 1, z, Blocks.MOSSY_COBBLESTONE_WALL);
        set(level, x, groundY + 2, z, Blocks.SOUL_LANTERN);
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(MARKER_X, MARKER_Y, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, MARKER_Y, MARKER_Z)).is(Blocks.MOSS_BLOCK)
                && level.getBlockState(new BlockPos(MARKER_X + 2, MARKER_Y, MARKER_Z)).is(Blocks.AMETHYST_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, MARKER_X, MARKER_Y, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, MARKER_Y, MARKER_Z, Blocks.MOSS_BLOCK);
        set(level, MARKER_X + 2, MARKER_Y, MARKER_Z, Blocks.AMETHYST_BLOCK);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static void set(ServerLevel level, int x, int y, int z, Block block) { level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2); }
}
