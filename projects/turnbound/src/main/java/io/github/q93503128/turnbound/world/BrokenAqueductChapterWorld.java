package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Authored Chapter 3 route through the Broken Aqueduct with two valves, lower channel and exact B03 anchor. */
public final class BrokenAqueductChapterWorld {
    private static final int MARKER_X = -320;
    private static final int MARKER_Y = 56;
    private static final int MARKER_Z = 20;

    private record Node(double x, int groundY, double z) {}
    public record EncounterPoint(String id, Vec3 fieldPosition, Vec3 battleAnchor, float battleYaw) {}
    public record BuiltChapter(Vec3 entry, Vec3 fastTravel, Vec3 bossAnchor, float bossYaw,
                               List<Vec3> valves, List<EncounterPoint> encounters) {
        public BuiltChapter { valves = List.copyOf(valves); encounters = List.copyOf(encounters); }
    }

    private BrokenAqueductChapterWorld() {}

    public static BuiltChapter build(ServerLevel level) {
        BuiltChapter chapter = built();
        if (!hasMarker(level)) {
            List<Node> route = route();
            for (int i = 0; i < route.size() - 1; i++) buildSegment(level, route.get(i), route.get(i + 1));
            clearing(level, -180, 65, 42, 15);
            clearing(level, -240, 65, -18, 16);
            clearing(level, -320, 66, 20, 17);
            clearing(level, -338, 64, 62, 15);
            clearing(level, -380, 64, 15, 16);
            clearing(level, -410, 63, 70, 16);
            clearing(level, -430, 63, 35, 22);
            buildChannel(level);
            buildValves(level);
            buildFastTravel(level);
            buildLowerGate(level, false);
            buildOroGate(level, false);
            buildRuins(level);
            writeMarker(level);
        }
        return chapter;
    }

    public static boolean contains(Vec3 p) {
        if (p == null || p.y < 48 || p.y > 96) return false;
        return p.x >= AsterMarchRegionCatalog.AQUEDUCT.minX() - 8
                && p.x <= -122
                && p.z >= AsterMarchRegionCatalog.AQUEDUCT.minZ() - 8
                && p.z <= AsterMarchRegionCatalog.AQUEDUCT.maxZ() + 8;
    }

    public static void setLowerGateOpen(ServerLevel level, boolean open) { buildLowerGate(level, open); }
    public static void setOroGateOpen(ServerLevel level, boolean open) { buildOroGate(level, open); }

    private static BuiltChapter built() {
        AsterMarchRegionCatalog.Anchor ft = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_AQUEDUCT);
        AsterMarchRegionCatalog.Anchor boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B03);
        return new BuiltChapter(
                new Vec3(-150.0, 67.0, 20.0),
                new Vec3(ft.x(), ft.y(), ft.z()),
                new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw(),
                List.of(new Vec3(-205.0, 66.0, 8.0), new Vec3(-274.0, 66.0, 46.0)),
                List.of(
                        point("ENC_A01", -180, 66, 42, -180, 66, 45, -90),
                        point("ENC_A02", -240, 66, -18, -243, 66, -18, -90),
                        point("ENC_A03", -338, 65, 62, -341, 65, 62, -90),
                        point("ENC_A04", -380, 65, 15, -383, 65, 15, -90),
                        point("ENC_A05", -410, 64, 70, -413, 64, 70, -90),
                        new EncounterPoint("BATTLE_B03", new Vec3(-418, 64, 35), new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw())));
    }

    private static EncounterPoint point(String id, double fx, double fy, double fz, double bx, double by, double bz, float yaw) {
        return new EncounterPoint(id, new Vec3(fx, fy, fz), new Vec3(bx, by, bz), yaw);
    }

    private static List<Node> route() {
        return List.of(
                new Node(-122, 65, 20), new Node(-150, 65, 20), new Node(-180, 65, 42),
                new Node(-205, 65, 8), new Node(-240, 65, -18), new Node(-274, 65, 46),
                new Node(-300, 65, 20), new Node(-320, 66, 20), new Node(-338, 64, 62),
                new Node(-380, 64, 15), new Node(-410, 63, 70), new Node(-430, 63, 35));
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
                int x = (int)Math.round(cx + rx * offset), z = (int)Math.round(cz + rz * offset);
                for (int fy = y - 4; fy < y; fy++) set(level, x, fy, z, Blocks.STONE);
                Block ground = Math.abs(offset) <= 2
                        ? (((cx + cz + offset) & 3) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS)
                        : (((cx * 11 + cz * 17 + offset) & 7) == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.ANDESITE);
                set(level, x, y, z, ground);
                for (int ay = y + 1; ay <= y + 9; ay++) set(level, x, ay, z, Blocks.AIR);
            }
        }
    }

    private static void clearing(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
            int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
            if (d2 > radius * radius) continue;
            for (int fy = y - 4; fy < y; fy++) set(level, x, fy, z, Blocks.STONE);
            set(level, x, y, z, ((x * 23 + z * 19) & 7) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
            for (int ay = y + 1; ay <= y + 10; ay++) set(level, x, ay, z, Blocks.AIR);
        }
    }

    private static void buildChannel(ServerLevel level) {
        for (int x = -460; x <= -145; x++) {
            int centerZ = 92 + (int)Math.round(Math.sin(x / 22.0) * 8.0);
            int y = x < -390 ? 61 : x < -310 ? 62 : 63;
            for (int dz = -3; dz <= 3; dz++) {
                set(level, x, y - 1, centerZ + dz, Blocks.STONE_BRICKS);
                set(level, x, y, centerZ + dz, Blocks.WATER);
                for (int ay = y + 1; ay <= y + 4; ay++) set(level, x, ay, centerZ + dz, Blocks.AIR);
            }
            if (x % 18 == 0) {
                for (int dz = -5; dz <= 5; dz++) set(level, x, y + 1, centerZ + dz, Blocks.IRON_BARS);
            }
        }
    }

    private static void buildValves(ServerLevel level) {
        for (Vec3 p : built().valves()) {
            int x = (int)Math.round(p.x), y = (int)Math.round(p.y) - 1, z = (int)Math.round(p.z);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(level, x + dx, y, z + dz, Blocks.POLISHED_ANDESITE);
            set(level, x, y + 1, z, Blocks.IRON_BLOCK);
            set(level, x, y + 2, z, Blocks.REDSTONE_LAMP);
            set(level, x - 1, y + 1, z, Blocks.IRON_BARS);
            set(level, x + 1, y + 1, z, Blocks.IRON_BARS);
        }
    }

    private static void buildFastTravel(ServerLevel level) {
        int cx = -320, y = 66, cz = 20;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (dx * dx + dz * dz <= 18) set(level, cx + dx, y, cz + dz,
                    ((dx + dz) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
        }
        set(level, cx, y + 1, cz, Blocks.AMETHYST_BLOCK);
        set(level, cx, y + 2, cz, Blocks.BEACON);
        post(level, cx - 5, y, cz - 5); post(level, cx + 5, y, cz - 5);
        post(level, cx - 5, y, cz + 5); post(level, cx + 5, y, cz + 5);
    }

    private static void buildLowerGate(ServerLevel level, boolean open) {
        int x = -300, y = 65;
        for (int z = 8; z <= 32; z++) {
            boolean center = z >= 15 && z <= 25;
            for (int dy = 1; dy <= 6; dy++) {
                if (center) set(level, x, y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
                else set(level, x, y + dy, z, ((z + dy) & 3) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        for (int z = 7; z <= 33; z++) set(level, x, y + 7, z, Blocks.STONE_BRICKS);
    }

    private static void buildOroGate(ServerLevel level, boolean open) {
        int x = -416, y = 63;
        for (int z = 23; z <= 47; z++) {
            boolean center = z >= 30 && z <= 40;
            for (int dy = 1; dy <= 7; dy++) {
                if (center) set(level, x, y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
                else set(level, x, y + dy, z, ((z + dy) & 2) == 0 ? Blocks.IRON_BLOCK : Blocks.DEEPSLATE_TILES);
            }
        }
        for (int z = 22; z <= 48; z++) set(level, x, y + 8, z, Blocks.DEEPSLATE_TILES);
    }

    private static void buildRuins(ServerLevel level) {
        int[][] pillars = {{-165,-30},{-215,65},{-260,-55},{-345,-20},{-365,90},{-445,85},{-455,-15}};
        for (int i = 0; i < pillars.length; i++) {
            int x = pillars[i][0], z = pillars[i][1], y = x < -400 ? 63 : x < -320 ? 64 : 65;
            int h = 3 + (i % 4);
            for (int dy = 1; dy <= h; dy++) set(level, x, y + dy, z, dy == h ? Blocks.CHISELED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS);
            set(level, x, y + h + 1, z, Blocks.SOUL_LANTERN);
        }
    }

    private static void post(ServerLevel level, int x, int groundY, int z) {
        set(level, x, groundY + 1, z, Blocks.COBBLESTONE_WALL);
        set(level, x, groundY + 2, z, Blocks.LANTERN);
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(MARKER_X, MARKER_Y, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, MARKER_Y, MARKER_Z)).is(Blocks.IRON_BLOCK)
                && level.getBlockState(new BlockPos(MARKER_X + 2, MARKER_Y, MARKER_Z)).is(Blocks.AMETHYST_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, MARKER_X, MARKER_Y, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, MARKER_Y, MARKER_Z, Blocks.IRON_BLOCK);
        set(level, MARKER_X + 2, MARKER_Y, MARKER_Z, Blocks.AMETHYST_BLOCK);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static void set(ServerLevel level, int x, int y, int z, Block block) { level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2); }
}
