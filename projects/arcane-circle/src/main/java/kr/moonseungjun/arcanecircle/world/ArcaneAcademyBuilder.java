
package kr.moonseungjun.arcanecircle.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Builds a deterministic academy hub using only vanilla blocks and public-domain Gothic/sigil motifs. */
public final class ArcaneAcademyBuilder {
    private ArcaneAcademyBuilder() {}

    public static BlockPos build(ServerLevel level, BlockPos near) {
        int cx = near.getX();
        int cz = near.getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        y = Math.max(level.getMinY() + 8, Math.min(level.getMaxY() - 28, y + 1));
        BlockPos origin = new BlockPos(cx, y, cz);

        // Flatten and clear a 61x61 campus. The footprint is deliberately compact to avoid a long freeze.
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                Block floor = (Math.abs(x) <= 3 || Math.abs(z) <= 3) ? Blocks.SMOOTH_QUARTZ : Blocks.STONE_BRICKS;
                set(level, origin, x, -2, z, Blocks.DEEPSLATE_BRICKS);
                set(level, origin, x, -1, z, floor);
                for (int h = 0; h <= 16; h++) set(level, origin, x, h, z, Blocks.AIR);
            }
        }

        // Outer cloister wall and arched corner towers.
        walls(level, origin, -30, -30, 30, 30, 0, 7, Blocks.POLISHED_BLACKSTONE_BRICKS);
        gate(level, origin, 0, -30, true);
        gate(level, origin, 0, 30, true);
        gate(level, origin, -30, 0, false);
        gate(level, origin, 30, 0, false);
        tower(level, origin, -25, -25, Blocks.PURPUR_BLOCK, Blocks.GLASS);
        tower(level, origin, 25, -25, Blocks.QUARTZ_BRICKS, Blocks.GLOWSTONE);
        tower(level, origin, -25, 25, Blocks.MOSSY_STONE_BRICKS, Blocks.GLASS);
        tower(level, origin, 25, 25, Blocks.WARPED_PLANKS, Blocks.SEA_LANTERN);

        // Central ninefold rotunda and celestial floor seal.
        disc(level, origin, 0, -1, 0, 12, Blocks.POLISHED_DEEPSLATE);
        for (int radius : new int[]{3, 6, 9, 12}) ring(level, origin, 0, 0, 0, radius, Blocks.AMETHYST_BLOCK);
        for (int arm = -10; arm <= 10; arm++) {
            set(level, origin, arm, 0, 0, Blocks.GOLD_BLOCK);
            set(level, origin, 0, 0, arm, Blocks.GOLD_BLOCK);
            if (Math.abs(arm) <= 7) {
                set(level, origin, arm, 0, arm, Blocks.SEA_LANTERN);
                set(level, origin, arm, 0, -arm, Blocks.SEA_LANTERN);
            }
        }
        for (int x = -13; x <= 13; x++) for (int z = -13; z <= 13; z++) {
            int d2 = x * x + z * z;
            if (d2 >= 150 && d2 <= 180) {
                for (int h = 1; h <= 8; h++) if ((x + z + h) % 4 == 0)
                    set(level, origin, x, h, z, Blocks.QUARTZ_PILLAR);
            }
        }
        set(level, origin, 0, 1, 0, Blocks.ENCHANTING_TABLE);
        set(level, origin, 0, 2, 0, Blocks.END_ROD);

        // Four faculty halls: Arcane, Divine, Occult, Primal.
        hall(level, origin, -4, -27, 4, -14, Blocks.PURPUR_BLOCK, Blocks.SEA_LANTERN);
        hall(level, origin, -4, 14, 4, 27, Blocks.QUARTZ_BRICKS, Blocks.GLOWSTONE);
        hall(level, origin, -27, -4, -14, 4, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GLASS);
        hall(level, origin, 14, -4, 27, 4, Blocks.MOSSY_STONE_BRICKS, Blocks.GLASS);
        // Open each faculty toward the central rotunda.
        for (int a = -1; a <= 1; a++) for (int h = 1; h <= 4; h++) {
            set(level, origin, a, h, -14, Blocks.AIR);
            set(level, origin, a, h, 14, Blocks.AIR);
            set(level, origin, -14, h, a, Blocks.AIR);
            set(level, origin, 14, h, a, Blocks.AIR);
        }

        // Library and lecture furnishings.
        for (int z = -24; z <= 24; z += 4) {
            if (Math.abs(z) < 12) continue;
            set(level, origin, -18, 1, z, Blocks.BOOKSHELF);
            set(level, origin, 18, 1, z, Blocks.BOOKSHELF);
            set(level, origin, -17, 1, z, Blocks.LECTERN);
            set(level, origin, 17, 1, z, Blocks.LECTERN);
        }

        // Southern training arena and safe arrival platform.
        for (int x = -11; x <= 11; x++) for (int z = 16; z <= 27; z++) {
            if (x * x + (z - 21) * (z - 21) <= 120) set(level, origin, x, 0, z, Blocks.SMOOTH_STONE);
        }
        ring(level, origin, 0, 1, 21, 10, Blocks.REDSTONE_BLOCK);
        set(level, origin, 0, 1, -8, Blocks.LODESTONE);
        set(level, origin, 0, 2, -8, Blocks.END_ROD);

        return origin;
    }

    private static void set(ServerLevel level, BlockPos origin, int x, int y, int z, Block block) {
        level.setBlock(origin.offset(x, y, z), block.defaultBlockState(), 2);
    }

    private static void walls(ServerLevel level, BlockPos o, int x1, int z1, int x2, int z2,
                              int y, int height, Block block) {
        for (int x = x1; x <= x2; x++) for (int h = y; h <= y + height; h++) {
            set(level, o, x, h, z1, block); set(level, o, x, h, z2, block);
        }
        for (int z = z1; z <= z2; z++) for (int h = y; h <= y + height; h++) {
            set(level, o, x1, h, z, block); set(level, o, x2, h, z, block);
        }
    }

    private static void gate(ServerLevel level, BlockPos o, int x, int z, boolean alongX) {
        for (int a = -3; a <= 3; a++) for (int h = 0; h <= 5; h++) {
            int gx = alongX ? x + a : x;
            int gz = alongX ? z : z + a;
            set(level, o, gx, h, gz, Blocks.AIR);
        }
        for (int a = -4; a <= 4; a++) {
            int gx = alongX ? x + a : x;
            int gz = alongX ? z : z + a;
            set(level, o, gx, 6 + Math.abs(a) / 2, gz, Blocks.CHISELED_STONE_BRICKS);
        }
    }

    private static void tower(ServerLevel level, BlockPos o, int cx, int cz, Block wall, Block glass) {
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int d2 = x * x + z * z;
            if (d2 < 12 || d2 > 24) continue;
            for (int h = 0; h <= 13; h++) set(level, o, cx + x, h, cz + z,
                    h == 6 && (x == 0 || z == 0) ? glass : wall);
        }
        for (int r = 5; r >= 1; r--) ring(level, o, cx, 14 + (5 - r), cz, r, wall);
        set(level, o, cx, 20, cz, Blocks.END_ROD);
    }

    private static void hall(ServerLevel level, BlockPos o, int x1, int z1, int x2, int z2,
                             Block wall, Block glass) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            set(level, o, x, 0, z, Blocks.POLISHED_DEEPSLATE);
            boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
            if (!edge) continue;
            for (int h = 1; h <= 7; h++) set(level, o, x, h, z,
                    h == 4 && ((x + z) & 2) == 0 ? glass : wall);
        }
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if ((x + z) % 3 == 0) set(level, o, x, 8, z, Blocks.DARK_PRISMARINE);
        }
    }

    private static void disc(ServerLevel level, BlockPos o, int cx, int y, int cz, int radius, Block block) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (x * x + z * z <= radius * radius) set(level, o, cx + x, y, cz + z, block);
        }
    }

    private static void ring(ServerLevel level, BlockPos o, int cx, int y, int cz, int radius, Block block) {
        int points = Math.max(32, radius * 12);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            set(level, o, cx + (int) Math.round(Math.cos(angle) * radius), y,
                    cz + (int) Math.round(Math.sin(angle) * radius), block);
        }
    }
}
