package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageBuildingSignatures {
    private VillageBuildingSignatures() {}

    static void buildAll(ServerLevel level, BlockPos villageCenter) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (VillageProgressionSystem.isOperational(building)) build(level, villageCenter, building);
            else remove(level, villageCenter, building);
        }
    }

    static void build(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        BlockPos base = base(villageCenter, building);
        clearAbove(level, base);
        if (building == VillageProgressionSystem.Building.WALLS) {
            buildGateShield(level, base);
            return;
        }
        foundation(level, base, signatureBlock(building));
        mast(level, base, Blocks.POLISHED_DEEPSLATE);
        switch (building) {
            case TOWN_HALL -> buildCrown(level, base);
            case SMITHY -> buildHammer(level, base);
            case SKILL_HALL -> buildRune(level, base);
            case INFIRMARY -> buildHealingCross(level, base);
            case STOREHOUSE -> buildSupplyCrate(level, base);
            case BARRACKS -> buildCrossedBlades(level, base);
            case WALLS -> {}
        }
    }

    static void remove(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        clearAbove(level, base(villageCenter, building));
    }

    private static BlockPos base(BlockPos villageCenter, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return villageCenter.offset(0, 18, -VillageWorldSystem.FORTRESS_RADIUS + 3);
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        int x = villageCenter.getX() + spec.dx() + spec.width() / 2;
        int z = villageCenter.getZ() + spec.dz() + spec.depth() / 2;
        int y = villageCenter.getY() + spec.height();
        return new BlockPos(x, y, z);
    }

    private static void clearAbove(ServerLevel level, BlockPos base) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = 1; y <= 10; y++) set(level, base.offset(x, y, z), Blocks.AIR);
            }
        }
    }

    private static void foundation(ServerLevel level, BlockPos base, Block material) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) set(level, base.offset(x, 0, z), material);
            }
        }
        set(level, base, Blocks.SEA_LANTERN);
    }

    private static void mast(ServerLevel level, BlockPos base, Block material) {
        for (int y = 1; y <= 5; y++) set(level, base.above(y), material);
        set(level, base.above(4).east(), Blocks.END_ROD);
        set(level, base.above(4).west(), Blocks.END_ROD);
        set(level, base.above(4).north(), Blocks.END_ROD);
        set(level, base.above(4).south(), Blocks.END_ROD);
    }

    private static void buildCrown(ServerLevel level, BlockPos base) {
        lineX(level, base, -3, 3, 6, 0, Blocks.GOLD_BLOCK);
        for (int x : new int[]{-3, 0, 3}) set(level, base.offset(x, 7, 0), Blocks.GOLD_BLOCK);
        set(level, base.offset(-3, 8, 0), Blocks.SEA_LANTERN);
        set(level, base.offset(0, 8, 0), Blocks.SEA_LANTERN);
        set(level, base.offset(3, 8, 0), Blocks.SEA_LANTERN);
        lineZ(level, base, -3, 3, 6, 0, Blocks.GOLD_BLOCK);
    }

    private static void buildHammer(ServerLevel level, BlockPos base) {
        for (int y = 5; y <= 8; y++) set(level, base.offset(0, y, 0), Blocks.IRON_BLOCK);
        lineX(level, base, -3, 3, 8, 0, Blocks.COPPER_BLOCK);
        lineZ(level, base, -3, 3, 8, 0, Blocks.COPPER_BLOCK);
        set(level, base.offset(-3, 7, 0), Blocks.ORANGE_CONCRETE);
        set(level, base.offset(3, 7, 0), Blocks.ORANGE_CONCRETE);
    }

    private static void buildRune(ServerLevel level, BlockPos base) {
        set(level, base.offset(0, 8, 0), Blocks.AMETHYST_BLOCK);
        for (int distance = 1; distance <= 3; distance++) {
            set(level, base.offset(distance, 8 - distance, 0), Blocks.PURPLE_CONCRETE);
            set(level, base.offset(-distance, 8 - distance, 0), Blocks.PURPLE_CONCRETE);
            set(level, base.offset(0, 8 - distance, distance), Blocks.PURPLE_CONCRETE);
            set(level, base.offset(0, 8 - distance, -distance), Blocks.PURPLE_CONCRETE);
        }
        set(level, base.offset(0, 7, 0), Blocks.SEA_LANTERN);
        set(level, base.offset(0, 9, 0), Blocks.END_ROD);
    }

    private static void buildHealingCross(ServerLevel level, BlockPos base) {
        for (int y = 5; y <= 9; y++) set(level, base.offset(0, y, 0), Blocks.WHITE_CONCRETE);
        lineX(level, base, -3, 3, 7, 0, Blocks.RED_CONCRETE);
        lineZ(level, base, -3, 3, 7, 0, Blocks.RED_CONCRETE);
        set(level, base.offset(0, 7, 0), Blocks.SEA_LANTERN);
    }

    private static void buildSupplyCrate(ServerLevel level, BlockPos base) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                set(level, base.offset(x, 6, z), Blocks.HAY_BLOCK);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) set(level, base.offset(x, 7, z), Blocks.YELLOW_CONCRETE);
            }
        }
        set(level, base.offset(0, 7, 0), Blocks.SEA_LANTERN);
        set(level, base.offset(0, 8, 0), Blocks.GOLD_BLOCK);
    }

    private static void buildCrossedBlades(ServerLevel level, BlockPos base) {
        for (int i = -3; i <= 3; i++) {
            set(level, base.offset(i, 7 + i / 2, 0), Blocks.IRON_BLOCK);
            set(level, base.offset(i, 7 - i / 2, 0), Blocks.RED_CONCRETE);
            set(level, base.offset(0, 7 + i / 2, i), Blocks.IRON_BLOCK);
            set(level, base.offset(0, 7 - i / 2, i), Blocks.RED_CONCRETE);
        }
        set(level, base.offset(0, 7, 0), Blocks.SEA_LANTERN);
    }

    private static void buildGateShield(ServerLevel level, BlockPos base) {
        for (int x = -4; x <= 4; x++) {
            int lower = Math.abs(x) <= 1 ? 1 : Math.abs(x) <= 3 ? 2 : 3;
            for (int y = lower; y <= 8; y++) {
                Block material = Math.abs(x) == 4 || y == 8 || y == lower
                        ? Blocks.IRON_BLOCK : Blocks.RED_CONCRETE;
                set(level, base.offset(x, y, 0), material);
            }
        }
        lineX(level, base, -2, 2, 5, 0, Blocks.GOLD_BLOCK);
        for (int y = 3; y <= 7; y++) set(level, base.offset(0, y, 0), Blocks.GOLD_BLOCK);
        set(level, base.offset(0, 5, 0), Blocks.SEA_LANTERN);
    }

    private static Block signatureBlock(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> Blocks.GOLD_BLOCK;
            case SMITHY -> Blocks.COPPER_BLOCK;
            case SKILL_HALL -> Blocks.AMETHYST_BLOCK;
            case INFIRMARY -> Blocks.RED_CONCRETE;
            case STOREHOUSE -> Blocks.YELLOW_CONCRETE;
            case BARRACKS -> Blocks.IRON_BLOCK;
            case WALLS -> Blocks.RED_CONCRETE;
        };
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
