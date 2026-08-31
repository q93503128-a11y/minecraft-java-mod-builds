package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Authored Chapter 4 route through Ember Quarry, including FT_QUARRY, core-fragment recovery sites and B04. */
public final class EmberQuarryChapterWorld {
    private static final int MARKER_X = 20;
    private static final int MARKER_Y = 57;
    private static final int MARKER_Z = 405;

    private record Node(double x, int groundY, double z) {}
    public record EncounterPoint(String id, Vec3 fieldPosition, Vec3 battleAnchor, float battleYaw) {}
    public record BuiltChapter(Vec3 entry, Vec3 fastTravel, Vec3 bossAnchor, float bossYaw,
                               List<Vec3> corePickupPositions, List<EncounterPoint> encounters) {
        public BuiltChapter { corePickupPositions = List.copyOf(corePickupPositions); encounters = List.copyOf(encounters); }
    }

    private EmberQuarryChapterWorld() {}

    public static BuiltChapter build(ServerLevel level) {
        BuiltChapter chapter = built();
        if (!hasMarker(level)) {
            List<Node> route = route();
            for (int i = 0; i < route.size() - 1; i++) buildSegment(level, route.get(i), route.get(i + 1));
            clearing(level, -80, 68, 330, 15);
            clearing(level, -30, 68, 365, 16);
            clearing(level, 20, 69, 405, 18);
            clearing(level, 45, 65, 425, 16);
            clearing(level, -5, 64, 445, 16);
            clearing(level, 48, 63, 470, 16);
            clearing(level, 65, 62, 455, 23);
            buildFastTravel(level);
            buildAshGate(level, false);
            buildBossGate(level, false);
            buildLavaCuts(level);
            buildQuarryMachinery(level);
            buildCoreSites(level);
            writeMarker(level);
        }
        return chapter;
    }

    public static boolean contains(Vec3 p) {
        if (p == null || p.y < 48 || p.y > 100) return false;
        return p.x >= AsterMarchRegionCatalog.QUARRY.minX() - 8
                && p.x <= AsterMarchRegionCatalog.QUARRY.maxX() + 8
                && p.z >= 292 && p.z <= AsterMarchRegionCatalog.QUARRY.maxZ() + 8;
    }

    public static void setAshGateOpen(ServerLevel level, boolean open) { buildAshGate(level, open); }
    public static void setBossGateOpen(ServerLevel level, boolean open) { buildBossGate(level, open); }

    private static BuiltChapter built() {
        AsterMarchRegionCatalog.Anchor ft = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_QUARRY);
        AsterMarchRegionCatalog.Anchor boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B04);
        return new BuiltChapter(
                new Vec3(-110.0, 69.0, 315.0),
                new Vec3(ft.x(), ft.y(), ft.z()),
                new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw(),
                List.of(new Vec3(48.0, 66.0, 428.0), new Vec3(50.0, 64.0, 472.0)),
                List.of(
                        point("ENC_Q01", -80, 69, 330, -78, 69, 333, 0),
                        point("ENC_Q02", -30, 69, 365, -28, 69, 368, 0),
                        point("ENC_Q03", 45, 66, 425, 47, 66, 428, 0),
                        point("ENC_Q04", -5, 65, 445, -3, 65, 448, 0),
                        point("ENC_Q05", 48, 64, 470, 50, 64, 473, 0),
                        new EncounterPoint("BATTLE_B04", new Vec3(65, 63, 442), new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw())));
    }

    private static EncounterPoint point(String id, double fx, double fy, double fz, double bx, double by, double bz, float yaw) {
        return new EncounterPoint(id, new Vec3(fx, fy, fz), new Vec3(bx, by, bz), yaw);
    }

    private static List<Node> route() {
        return List.of(
                new Node(-120, 68, 300), new Node(-110, 68, 315), new Node(-80, 68, 330),
                new Node(-30, 68, 365), new Node(20, 69, 405), new Node(45, 65, 425),
                new Node(-5, 64, 445), new Node(48, 63, 470), new Node(65, 62, 455));
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
                        ? (((cx + cz + offset) & 3) == 0 ? Blocks.BLACKSTONE : Blocks.BASALT)
                        : (((cx * 17 + cz * 7 + offset) & 7) == 0 ? Blocks.MAGMA_BLOCK : Blocks.TUFF);
                set(level, x, y, z, ground);
                for (int ay = y + 1; ay <= y + 10; ay++) set(level, x, ay, z, Blocks.AIR);
            }
        }
    }

    private static void clearing(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
            int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
            if (d2 > radius * radius) continue;
            for (int fy = y - 4; fy < y; fy++) set(level, x, fy, z, Blocks.STONE);
            Block ground = ((x * 13 + z * 29) & 7) == 0 ? Blocks.MAGMA_BLOCK : (((x + z) & 3) == 0 ? Blocks.BASALT : Blocks.TUFF);
            set(level, x, y, z, ground);
            for (int ay = y + 1; ay <= y + 12; ay++) set(level, x, ay, z, Blocks.AIR);
        }
    }

    private static void buildFastTravel(ServerLevel level) {
        int cx = 20, y = 69, cz = 405;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (dx * dx + dz * dz <= 18) set(level, cx + dx, y, cz + dz, ((dx + dz) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE : Blocks.POLISHED_BASALT);
        }
        set(level, cx, y + 1, cz, Blocks.AMETHYST_BLOCK);
        set(level, cx, y + 2, cz, Blocks.BEACON);
        post(level, cx - 5, y, cz - 5); post(level, cx + 5, y, cz - 5);
        post(level, cx - 5, y, cz + 5); post(level, cx + 5, y, cz + 5);
    }

    private static void buildAshGate(ServerLevel level, boolean open) {
        int z = 395, y = 68;
        for (int x = -4; x <= 44; x++) {
            boolean center = x >= 14 && x <= 26;
            for (int dy = 1; dy <= 6; dy++) {
                if (center) set(level, x, y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
                else if ((x & 1) == 0) set(level, x, y + dy, z, Blocks.BLACKSTONE);
            }
        }
        for (int x = -5; x <= 45; x++) set(level, x, y + 7, z, Blocks.POLISHED_BLACKSTONE);
    }

    private static void buildBossGate(ServerLevel level, boolean open) {
        int z = 448, y = 62;
        for (int x = 52; x <= 78; x++) {
            boolean center = x >= 59 && x <= 71;
            for (int dy = 1; dy <= 8; dy++) {
                if (center) set(level, x, y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
                else set(level, x, y + dy, z, ((x + dy) & 3) == 0 ? Blocks.MAGMA_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        for (int x = 51; x <= 79; x++) set(level, x, y + 9, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
    }

    private static void buildLavaCuts(ServerLevel level) {
        for (int z = 335; z <= 490; z += 28) {
            int cx = -55 + Math.floorMod(z * 7, 115);
            int y = z < 410 ? 66 : 61;
            for (int x = cx - 10; x <= cx + 10; x++) {
                set(level, x, y - 1, z, Blocks.BLACKSTONE);
                set(level, x, y, z, Blocks.LAVA);
            }
            for (int x = cx - 12; x <= cx + 12; x++) if (Math.abs(x - cx) >= 10) set(level, x, y, z, Blocks.MAGMA_BLOCK);
        }
    }

    private static void buildQuarryMachinery(ServerLevel level) {
        int[][] machines = {{-65,350,68},{-10,385,68},{35,438,64},{-30,455,63},{85,475,62}};
        for (int[] m : machines) {
            int x=m[0], z=m[1], y=m[2];
            for (int dx=-3; dx<=3; dx++) for (int dz=-2; dz<=2; dz++) set(level,x+dx,y,z+dz,Blocks.IRON_BLOCK);
            for (int dy=1; dy<=5; dy++) { set(level,x-3,y+dy,z,Blocks.IRON_BARS); set(level,x+3,y+dy,z,Blocks.IRON_BARS); }
            set(level,x,y+1,z,Blocks.BLAST_FURNACE); set(level,x,y+2,z,Blocks.REDSTONE_LAMP);
        }
    }

    private static void buildCoreSites(ServerLevel level) {
        for (Vec3 p : built().corePickupPositions()) {
            int x=(int)Math.round(p.x), y=(int)Math.round(p.y)-1, z=(int)Math.round(p.z);
            for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) set(level,x+dx,y,z+dz,Blocks.BLACKSTONE);
            set(level,x,y+1,z,Blocks.MAGMA_BLOCK);
            set(level,x,y+2,z,Blocks.IRON_BARS);
        }
    }

    private static void post(ServerLevel level, int x, int groundY, int z) {
        set(level, x, groundY + 1, z, Blocks.BLACKSTONE_WALL);
        set(level, x, groundY + 2, z, Blocks.SOUL_LANTERN);
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(MARKER_X, MARKER_Y, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, MARKER_Y, MARKER_Z)).is(Blocks.MAGMA_BLOCK)
                && level.getBlockState(new BlockPos(MARKER_X + 2, MARKER_Y, MARKER_Z)).is(Blocks.AMETHYST_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, MARKER_X, MARKER_Y, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, MARKER_Y, MARKER_Z, Blocks.MAGMA_BLOCK);
        set(level, MARKER_X + 2, MARKER_Y, MARKER_Z, Blocks.AMETHYST_BLOCK);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static void set(ServerLevel level, int x, int y, int z, Block block) { level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2); }
}
