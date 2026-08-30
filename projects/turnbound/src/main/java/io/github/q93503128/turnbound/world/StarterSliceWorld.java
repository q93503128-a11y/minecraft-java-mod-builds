package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Authored starter slice bootstrap. alpha.16 builds terrain in bounded column batches and writes a hidden
 * layout-version marker so later logins/server restarts reuse the completed world instead of flattening it again.
 */
public final class StarterSliceWorld {
    public static final int ORIGIN_X = -32;
    public static final int VILLAGE_Z = 64;
    public static final int FIELD_Z = 128;
    public static final int SIZE = 64;
    public static final int LAYOUT_VERSION = 16;

    private static final int MARKER_X = ORIGIN_X + 2;
    private static final int MARKER_Z = VILLAGE_Z + 2;
    private static final int COLUMN_COUNT = SIZE * SIZE;

    public record BuiltSlice(
            int baseY,
            Vec3 spawn,
            Vec3 npc,
            Vec3 relay,
            Vec3 m01Home,
            Vec3 m01End,
            Vec3 m02Home,
            Vec3 m02End
    ) {}

    public enum BuildStage {
        VILLAGE_TERRAIN,
        FIELD_TERRAIN,
        VILLAGE_DETAIL,
        FIELD_DETAIL,
        FINALIZE,
        DONE
    }

    /** Mutable server-thread build job; one job is advanced once per player tick by StarterSliceBootstrap. */
    public static final class BuildJob {
        private final int baseY;
        private BuildStage stage = BuildStage.VILLAGE_TERRAIN;
        private int columnCursor;

        private BuildJob(int baseY) { this.baseY = baseY; }

        public int baseY() { return baseY; }
        public BuildStage stage() { return stage; }
        public boolean done() { return stage == BuildStage.DONE; }

        public String stageLabel() {
            return switch (stage) {
                case VILLAGE_TERRAIN -> "남문 마을 지형 준비";
                case FIELD_TERRAIN -> "남문 초원 지형 준비";
                case VILLAGE_DETAIL -> "마을 구조 배치";
                case FIELD_DETAIL -> "첫 필드 구조 배치";
                case FINALIZE -> "월드 정합성 확인";
                case DONE -> "완료";
            };
        }

        public int progressPercent() {
            return switch (stage) {
                case VILLAGE_TERRAIN -> (int)Math.floor(34.0 * columnCursor / COLUMN_COUNT);
                case FIELD_TERRAIN -> 34 + (int)Math.floor(34.0 * columnCursor / COLUMN_COUNT);
                case VILLAGE_DETAIL -> 72;
                case FIELD_DETAIL -> 84;
                case FINALIZE -> 96;
                case DONE -> 100;
            };
        }

        /** Returns true on the tick that the complete layout becomes ready for play. */
        public boolean tick(ServerLevel level, int columnBudget) {
            int budget = Math.max(1, columnBudget);
            while (budget > 0 && (stage == BuildStage.VILLAGE_TERRAIN || stage == BuildStage.FIELD_TERRAIN)) {
                int originZ = stage == BuildStage.VILLAGE_TERRAIN ? VILLAGE_Z : FIELD_Z;
                levelColumn(level, originZ, baseY, columnCursor++);
                budget--;
                if (columnCursor >= COLUMN_COUNT) {
                    columnCursor = 0;
                    stage = stage == BuildStage.VILLAGE_TERRAIN ? BuildStage.FIELD_TERRAIN : BuildStage.VILLAGE_DETAIL;
                }
            }
            if (stage == BuildStage.VILLAGE_DETAIL) {
                buildVillage(level, baseY);
                stage = BuildStage.FIELD_DETAIL;
                return false;
            }
            if (stage == BuildStage.FIELD_DETAIL) {
                buildField(level, baseY);
                stage = BuildStage.FINALIZE;
                return false;
            }
            if (stage == BuildStage.FINALIZE) {
                writeLayoutMarker(level, baseY);
                stage = BuildStage.DONE;
                return true;
            }
            return stage == BuildStage.DONE;
        }

        public BuiltSlice result() {
            if (!done()) throw new IllegalStateException("Starter slice build is not complete");
            return built(baseY);
        }
    }

    private StarterSliceWorld() {}

    public static BuildJob begin(ServerLevel level) {
        return new BuildJob(sampleBaseY(level));
    }

    /** Returns the persisted authored layout when its hidden version marker matches this build. */
    public static BuiltSlice findExisting(ServerLevel level) {
        int baseY = sampleBaseY(level);
        if (!hasLayoutMarker(level, baseY)) return null;
        return built(baseY);
    }

    /** Synchronous developer fallback. Normal gameplay uses begin()+BuildJob.tick() through StarterSliceBootstrap. */
    public static BuiltSlice build(ServerLevel level) {
        BuiltSlice existing = findExisting(level);
        if (existing != null) return existing;
        BuildJob job = begin(level);
        while (!job.done()) job.tick(level, COLUMN_COUNT * 2);
        return job.result();
    }

    public static boolean contains(BuiltSlice slice, Vec3 position) {
        return position.x >= ORIGIN_X - 2 && position.x <= ORIGIN_X + SIZE + 2
                && position.z >= VILLAGE_Z - 2 && position.z <= FIELD_Z + SIZE + 2
                && position.y >= slice.baseY() - 8 && position.y <= slice.baseY() + 24;
    }

    private static int sampleBaseY(ServerLevel level) {
        int sampled = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, VILLAGE_Z + 28) - 1;
        return Math.max(level.getMinY() + 4, sampled);
    }

    private static BuiltSlice built(int baseY) {
        return new BuiltSlice(
                baseY,
                pos(0.5, baseY + 1, VILLAGE_Z + 18.5),
                pos(-6.5, baseY + 1, VILLAGE_Z + 27.5),
                pos(8.5, baseY + 1, VILLAGE_Z + 27.5),
                pos(-12.0, baseY + 1, FIELD_Z + 24.0),
                pos(8.0, baseY + 1, FIELD_Z + 27.0),
                pos(13.0, baseY + 1, FIELD_Z + 44.0),
                pos(-5.0, baseY + 1, FIELD_Z + 48.0));
    }

    private static void levelColumn(ServerLevel level, int originZ, int baseY, int cursor) {
        int lx = cursor / SIZE;
        int lz = cursor % SIZE;
        int x = ORIGIN_X + lx;
        int z = originZ + lz;
        for (int y = baseY - 3; y < baseY; y++) set(level, x, y, z, Blocks.DIRT);
        set(level, x, baseY, z, Blocks.GRASS_BLOCK);
        for (int y = baseY + 1; y <= baseY + 12; y++) set(level, x, y, z, Blocks.AIR);
    }

    private static void writeLayoutMarker(ServerLevel level, int baseY) {
        set(level, MARKER_X, baseY - 2, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, baseY - 2, MARKER_Z, Blocks.AMETHYST_BLOCK);
        set(level, MARKER_X + 2, baseY - 2, MARKER_Z, Blocks.CRYING_OBSIDIAN);
    }

    private static boolean hasLayoutMarker(ServerLevel level, int baseY) {
        return level.getBlockState(new BlockPos(MARKER_X, baseY - 2, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, baseY - 2, MARKER_Z)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(new BlockPos(MARKER_X + 2, baseY - 2, MARKER_Z)).is(Blocks.CRYING_OBSIDIAN);
    }

    private static void buildVillage(ServerLevel level, int y) {
        for (int z = VILLAGE_Z; z < VILLAGE_Z + SIZE; z++) {
            for (int dx = -3; dx <= 3; dx++) {
                Block road = Math.abs(dx) <= 1 ? Blocks.GRAVEL : Blocks.DIRT_PATH;
                set(level, dx, y, z, road);
            }
        }
        for (int x = -10; x <= 10; x++) for (int z = VILLAGE_Z + 22; z <= VILLAGE_Z + 39; z++) {
            if ((x + z) % 7 == 0) set(level, x, y, z, Blocks.MOSSY_STONE_BRICKS);
            else set(level, x, y, z, Blocks.STONE_BRICKS);
        }
        house(level, -25, y, VILLAGE_Z + 12, 10, 9);
        house(level, 15, y, VILLAGE_Z + 12, 10, 9);
        house(level, -25, y, VILLAGE_Z + 42, 11, 10);
        house(level, 14, y, VILLAGE_Z + 43, 11, 9);
        int gateZ = VILLAGE_Z + SIZE - 1;
        for (int x = -9; x <= 9; x++) {
            if (Math.abs(x) <= 4) continue;
            for (int h = 1; h <= 4; h++) set(level, x, y + h, gateZ, h == 4 ? Blocks.STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS);
        }
        for (int x : new int[]{-6, 6}) {
            set(level, x, y + 1, gateZ - 2, Blocks.COBBLESTONE_WALL);
            set(level, x, y + 2, gateZ - 2, Blocks.LANTERN);
        }
        for (int z = VILLAGE_Z + 4; z < VILLAGE_Z + SIZE - 4; z += 4) {
            set(level, ORIGIN_X + 1, y + 1, z, Blocks.OAK_LEAVES);
            set(level, ORIGIN_X + SIZE - 2, y + 1, z, Blocks.OAK_LEAVES);
        }
    }

    private static void house(ServerLevel level, int x0, int y, int z0, int w, int d) {
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) set(level, x, y, z, Blocks.OAK_PLANKS);
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) {
            boolean wall = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + d - 1;
            if (!wall) continue;
            for (int h = 1; h <= 4; h++) set(level, x, y + h, z, h == 4 ? Blocks.OAK_LOG : Blocks.OAK_PLANKS);
        }
        int doorX = x0 + w / 2;
        set(level, doorX, y + 1, z0 + d - 1, Blocks.AIR);
        set(level, doorX, y + 2, z0 + d - 1, Blocks.AIR);
        for (int x = x0 - 1; x <= x0 + w; x++) for (int z = z0 - 1; z <= z0 + d; z++) set(level, x, y + 5, z, Blocks.SPRUCE_SLAB);
    }

    private static void buildField(ServerLevel level, int y) {
        for (int lz = 0; lz < SIZE; lz++) {
            int center = (int)Math.round(Math.sin(lz / 10.0) * 4.0);
            for (int dx = -3; dx <= 3; dx++) set(level, center + dx, y, FIELD_Z + lz,
                    Math.abs(dx) <= 1 ? Blocks.GRAVEL : Blocks.DIRT_PATH);
        }
        for (int x = ORIGIN_X + 3; x < ORIGIN_X + SIZE - 3; x++) {
            int z = FIELD_Z + 34 + (int)Math.round(Math.sin(x / 8.0));
            for (int dz = -1; dz <= 1; dz++) {
                set(level, x, y, z + dz, Blocks.WATER);
                set(level, x, y - 1, z + dz, Blocks.CLAY);
            }
        }
        for (int x = -5; x <= 5; x++) for (int z = FIELD_Z + 32; z <= FIELD_Z + 37; z++) set(level, x, y + 1, z, Blocks.OAK_PLANKS);
        clearing(level, -12, y, FIELD_Z + 24, 10);
        clearing(level, 13, y, FIELD_Z + 44, 11);
        for (int i = 0; i < 10; i++) tree(level, ORIGIN_X + 5 + (i * 13) % 54, y, FIELD_Z + 7 + (i * 19) % 50, i % 3 == 0);
        for (int x = -26; x <= -17; x++) {
            int h = 1 + Math.floorMod(x, 4);
            for (int dy = 1; dy <= h; dy++) set(level, x, y + dy, FIELD_Z + 55, dy % 2 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE_BRICKS);
        }
    }

    private static void clearing(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
            if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > radius * radius) continue;
            set(level, x, y, z, ((x * 17 + z * 31) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
        }
    }

    private static void tree(ServerLevel level, int x, int y, int z, boolean spruce) {
        Block log = spruce ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG;
        Block leaves = spruce ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES;
        for (int h = 1; h <= 5; h++) set(level, x, y + h, z, log);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 4; dy <= 6; dy++) {
            if (Math.abs(dx) + Math.abs(dz) > 3) continue;
            set(level, x + dx, y + dy, z + dz, leaves);
        }
    }

    private static Vec3 pos(double x, double y, double z) { return new Vec3(x, y, z); }
    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
