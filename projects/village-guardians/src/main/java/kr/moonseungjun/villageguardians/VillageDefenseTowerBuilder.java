package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageDefenseTowerBuilder {
    private VillageDefenseTowerBuilder() {}

    static void build(ServerLevel level, BlockPos center) {
        build(level, center, VillageProgressionSystem.wallLevel());
    }

    static void build(ServerLevel level, BlockPos center, int installedStage) {
        int radius = VillageWorldSystem.FORTRESS_RADIUS - 4;
        BlockPos ballista = center.offset(radius, 13, -radius);
        BlockPos flame = center.offset(-radius, 13, -radius);
        BlockPos frost = center.offset(radius, 13, radius);
        BlockPos arcane = center.offset(-radius, 13, radius);

        clearInstallationPad(level, ballista);
        clearInstallationPad(level, flame);
        clearInstallationPad(level, frost);
        clearInstallationPad(level, arcane);

        if (installedStage >= 1) buildBallista(level, ballista);
        if (installedStage >= 2) buildFlame(level, flame);
        if (installedStage >= 3) buildFrost(level, frost);
        if (installedStage >= 4) buildArcane(level, arcane);
    }

    private static void clearInstallationPad(ServerLevel level, BlockPos base) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                set(level, base.offset(x, 0, z), Blocks.STONE_BRICKS);
                for (int y = 1; y <= 8; y++) set(level, base.offset(x, y, z), Blocks.AIR);
            }
        }
        for (int x = -3; x <= 3; x++) {
            set(level, base.offset(x, 1, -3), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(x, 1, 3), Blocks.STONE_BRICK_WALL);
        }
        for (int z = -2; z <= 2; z++) {
            set(level, base.offset(-3, 1, z), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(3, 1, z), Blocks.STONE_BRICK_WALL);
        }
    }

    private static void buildBallista(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.DARK_OAK_PLANKS);
        column(level, base, 0, 0, 1, 3, Blocks.STRIPPED_DARK_OAK_WOOD);
        lineX(level, base, -3, 3, 4, 0, Blocks.DARK_OAK_FENCE);
        lineZ(level, base, -2, 2, 4, 0, Blocks.IRON_BARS);
        set(level, base.offset(0, 5, -2), Blocks.END_ROD);
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.BALLISTA);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.BALLISTA);
        if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_TITAN) {
            lineZ(level, base, -3, 2 + rank, 5, 0, Blocks.IRON_BLOCK);
            set(level, base.offset(0, 5, -3), Blocks.ANVIL);
        } else if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_PIERCE) {
            for (int z = -3; z <= 2 + rank; z++) set(level, base.offset(0, 5, z), Blocks.END_ROD);
        } else if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_SPLIT) {
            for (int x = -1; x <= 1; x++) {
                lineZ(level, base, -3, 1 + rank, 5, x, Blocks.END_ROD);
            }
        }
    }

    private static void buildFlame(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.BRICKS);
        column(level, base, 0, 0, 1, 4, Blocks.POLISHED_BLACKSTONE);
        set(level, base.offset(0, 5, 0), Blocks.CAMPFIRE);
        set(level, base.offset(1, 4, 0), Blocks.IRON_BARS);
        set(level, base.offset(-1, 4, 0), Blocks.IRON_BARS);
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.FLAME);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.FLAME);
        if (branch == VillageTowerSpecializationSystem.Branch.FLAME_INFERNO) {
            for (int x = -rank; x <= rank; x++) {
                set(level, base.offset(x, 1, -2), Blocks.MAGMA_BLOCK);
                set(level, base.offset(x, 1, 2), Blocks.MAGMA_BLOCK);
            }
            set(level, base.offset(0, 6, 0), Blocks.SOUL_CAMPFIRE);
        } else if (branch == VillageTowerSpecializationSystem.Branch.FLAME_BLAST) {
            set(level, base.offset(0, 6, 0), Blocks.REDSTONE_LAMP);
            set(level, base.offset(1, 5, 0), Blocks.BRICKS);
            set(level, base.offset(-1, 5, 0), Blocks.BRICKS);
        } else if (branch == VillageTowerSpecializationSystem.Branch.FLAME_MELT) {
            column(level, base, 0, 0, 5, 5 + rank, Blocks.MAGMA_BLOCK);
            set(level, base.offset(0, 6 + rank, 0), Blocks.SEA_LANTERN);
        }
    }

    private static void buildFrost(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.PACKED_ICE);
        column(level, base, 0, 0, 1, 3, Blocks.BLUE_ICE);
        set(level, base.offset(0, 4, 0), Blocks.SEA_LANTERN);
        set(level, base.offset(1, 3, 0), Blocks.AMETHYST_CLUSTER);
        set(level, base.offset(-1, 3, 0), Blocks.AMETHYST_CLUSTER);
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.FROST);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.FROST);
        if (branch == VillageTowerSpecializationSystem.Branch.FROST_DEEP) {
            column(level, base, 0, 0, 5, 5 + rank, Blocks.BLUE_ICE);
            set(level, base.offset(0, 6 + rank, 0), Blocks.SEA_LANTERN);
        } else if (branch == VillageTowerSpecializationSystem.Branch.FROST_SHATTER) {
            for (int x = -2; x <= 2; x += 2) {
                set(level, base.offset(x, 2 + rank, 0), Blocks.AMETHYST_CLUSTER);
                set(level, base.offset(0, 2 + rank, x), Blocks.AMETHYST_CLUSTER);
            }
        } else if (branch == VillageTowerSpecializationSystem.Branch.FROST_BLIZZARD) {
            for (int x = -2; x <= 2; x += 2) {
                set(level, base.offset(x, 4, 0), Blocks.SEA_LANTERN);
                set(level, base.offset(0, 4, x), Blocks.SEA_LANTERN);
            }
            set(level, base.offset(0, 5 + rank, 0), Blocks.PACKED_ICE);
        }
    }

    private static void buildArcane(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.POLISHED_DEEPSLATE);
        column(level, base, 0, 0, 1, 3, Blocks.AMETHYST_BLOCK);
        set(level, base.offset(0, 4, 0), Blocks.END_ROD);
        set(level, base.offset(2, 2, 0), Blocks.CRYING_OBSIDIAN);
        set(level, base.offset(-2, 2, 0), Blocks.CRYING_OBSIDIAN);
        set(level, base.offset(0, 2, 2), Blocks.CRYING_OBSIDIAN);
        set(level, base.offset(0, 2, -2), Blocks.CRYING_OBSIDIAN);
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.ARCANE);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.ARCANE);
        if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_CHAIN) {
            for (int x = -2; x <= 2; x += 2) {
                set(level, base.offset(x, 5, 0), Blocks.END_ROD);
                set(level, base.offset(0, 5, x), Blocks.END_ROD);
            }
        } else if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_NULL) {
            ring(level, base, 3, 4 + rank, Blocks.CRYING_OBSIDIAN);
            set(level, base.offset(0, 5 + rank, 0), Blocks.SCULK_CATALYST);
        } else if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_OVERCHARGE) {
            column(level, base, 0, 0, 4, 4 + rank, Blocks.AMETHYST_BLOCK);
            set(level, base.offset(0, 5 + rank, 0), Blocks.BEACON);
        }
    }

    private static void ring(ServerLevel level, BlockPos base, int radius, int y, Block material) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) == radius || Math.abs(z) == radius) set(level, base.offset(x, y, z), material);
            }
        }
    }

    private static void platform(ServerLevel level, BlockPos base, Block material) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) set(level, base.offset(x, 0, z), material);
        }
        for (int x = -3; x <= 3; x++) {
            set(level, base.offset(x, 1, -3), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(x, 1, 3), Blocks.STONE_BRICK_WALL);
        }
        for (int z = -2; z <= 2; z++) {
            set(level, base.offset(-3, 1, z), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(3, 1, z), Blocks.STONE_BRICK_WALL);
        }
    }

    private static void column(ServerLevel level, BlockPos base, int x, int z, int fromY, int toY, Block block) {
        for (int y = fromY; y <= toY; y++) set(level, base.offset(x, y, z), block);
    }

    private static void lineX(ServerLevel level, BlockPos base, int from, int to, int y, int z, Block block) {
        for (int x = from; x <= to; x++) set(level, base.offset(x, y, z), block);
    }

    private static void lineZ(ServerLevel level, BlockPos base, int from, int to, int y, int x, Block block) {
        for (int z = from; z <= to; z++) set(level, base.offset(x, y, z), block);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }
}
