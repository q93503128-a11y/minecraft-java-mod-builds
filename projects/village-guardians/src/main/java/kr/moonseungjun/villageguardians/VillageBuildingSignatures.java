package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Compact front-facing crests. Old crest space is rebuilt as facade instead of air. */
final class VillageBuildingSignatures {
    private static final Block BACKDROP = Blocks.POLISHED_DEEPSLATE;

    private VillageBuildingSignatures() {}

    static void buildAll(ServerLevel level, BlockPos villageCenter) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.INFIRMARY) continue;
            if (VillageProgressionSystem.isOperational(building)) build(level, villageCenter, building);
            else remove(level, villageCenter, building);
        }
    }

    static void build(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.INFIRMARY) return;
        if (building == VillageProgressionSystem.Building.WALLS) {
            buildGateShield(level, villageCenter);
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        BlockPos anchor = entrance.relative(spec.entranceFacing().getOpposite()).above(4);
        Direction sideways = spec.entranceFacing().getClockWise();

        // The previous migration cleared a 5x4 plane to air and then filled only 3x3,
        // which created the large hole visible above the entrance. Rebuild the full
        // former mark as the original facade material before placing the small crest.
        fillPlane(level, anchor, sideways, 2, -1, 2, spec.panel());
        buildBackdrop(level, anchor, sideways, 1, 0, 2);
        switch (building) {
            case TOWN_HALL -> buildCrown(level, anchor, sideways);
            case SMITHY -> buildHammer(level, anchor, sideways);
            case SKILL_HALL -> buildRune(level, anchor, sideways);
            case STOREHOUSE -> buildSupplyCrate(level, anchor, sideways);
            case BARRACKS -> buildCrossedBlades(level, anchor, sideways);
            case INFIRMARY, WALLS -> { }
        }
    }

    static void remove(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.INFIRMARY) return;
        if (building == VillageProgressionSystem.Building.WALLS) {
            clearPlane(level, gateAnchor(villageCenter), Direction.EAST, 3, -2, 2);
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = villageCenter.offset(spec.dx(), 0, spec.dz());
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        BlockPos anchor = entrance.relative(spec.entranceFacing().getOpposite()).above(4);
        fillPlane(level, anchor, spec.entranceFacing().getClockWise(), 2, -1, 2, spec.panel());
    }

    private static void buildCrown(ServerLevel level, BlockPos anchor, Direction side) {
        mark(level, anchor, side, -1, 0, Blocks.GOLD_BLOCK);
        mark(level, anchor, side, 0, 0, Blocks.GOLD_BLOCK);
        mark(level, anchor, side, 1, 0, Blocks.GOLD_BLOCK);
        mark(level, anchor, side, -1, 2, Blocks.GOLD_BLOCK);
        mark(level, anchor, side, 0, 1, Blocks.SEA_LANTERN);
        mark(level, anchor, side, 1, 2, Blocks.GOLD_BLOCK);
    }

    private static void buildHammer(ServerLevel level, BlockPos anchor, Direction side) {
        mark(level, anchor, side, 0, 0, Blocks.IRON_BLOCK);
        mark(level, anchor, side, 0, 1, Blocks.IRON_BLOCK);
        mark(level, anchor, side, -1, 2, Blocks.BRICKS);
        mark(level, anchor, side, 0, 2, Blocks.BRICKS);
        mark(level, anchor, side, 1, 2, Blocks.MAGMA_BLOCK);
    }

    private static void buildRune(ServerLevel level, BlockPos anchor, Direction side) {
        mark(level, anchor, side, 0, 0, Blocks.PURPUR_BLOCK);
        mark(level, anchor, side, -1, 1, Blocks.PURPUR_BLOCK);
        mark(level, anchor, side, 0, 1, Blocks.AMETHYST_BLOCK);
        mark(level, anchor, side, 1, 1, Blocks.PURPUR_BLOCK);
        mark(level, anchor, side, 0, 2, Blocks.SEA_LANTERN);
    }

    private static void buildSupplyCrate(ServerLevel level, BlockPos anchor, Direction side) {
        for (int lateral = -1; lateral <= 1; lateral++) {
            mark(level, anchor, side, lateral, 0, Blocks.GOLD_BLOCK);
            mark(level, anchor, side, lateral, 2, Blocks.GOLD_BLOCK);
        }
        mark(level, anchor, side, -1, 1, Blocks.GOLD_BLOCK);
        mark(level, anchor, side, 0, 1, Blocks.HAY_BLOCK);
        mark(level, anchor, side, 1, 1, Blocks.GOLD_BLOCK);
    }

    private static void buildCrossedBlades(ServerLevel level, BlockPos anchor, Direction side) {
        mark(level, anchor, side, -1, 0, Blocks.IRON_BLOCK);
        mark(level, anchor, side, 0, 1, Blocks.SEA_LANTERN);
        mark(level, anchor, side, 1, 2, Blocks.IRON_BLOCK);
        mark(level, anchor, side, 1, 0, Blocks.NETHER_BRICKS);
        mark(level, anchor, side, -1, 2, Blocks.NETHER_BRICKS);
    }

    private static void buildGateShield(ServerLevel level, BlockPos villageCenter) {
        BlockPos anchor = gateAnchor(villageCenter);
        Direction side = Direction.EAST;
        buildBackdrop(level, anchor, side, 2, -1, 2);
        for (int lateral = -2; lateral <= 2; lateral++) {
            int low = Math.abs(lateral) == 2 ? 0 : -1;
            for (int vertical = low; vertical <= 2; vertical++) {
                mark(level, anchor, side, lateral, vertical,
                        Math.abs(lateral) == 2 || vertical == low ? Blocks.IRON_BLOCK : Blocks.NETHER_WART_BLOCK);
            }
        }
        mark(level, anchor, side, 0, 0, Blocks.SEA_LANTERN);
        mark(level, anchor, side, 0, 1, Blocks.GOLD_BLOCK);
    }

    private static BlockPos gateAnchor(BlockPos center) {
        return center.offset(0, 9, -VillageWorldSystem.FORTRESS_RADIUS + 3);
    }

    private static void buildBackdrop(ServerLevel level, BlockPos anchor, Direction sideways,
                                      int halfWidth, int minVertical, int maxVertical) {
        fillPlane(level, anchor, sideways, halfWidth, minVertical, maxVertical, BACKDROP);
    }

    private static void fillPlane(ServerLevel level, BlockPos anchor, Direction sideways,
                                  int halfWidth, int minVertical, int maxVertical, Block block) {
        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            for (int vertical = minVertical; vertical <= maxVertical; vertical++) {
                mark(level, anchor, sideways, lateral, vertical, block);
            }
        }
    }

    private static void clearPlane(ServerLevel level, BlockPos anchor, Direction sideways,
                                   int halfWidth, int minVertical, int maxVertical) {
        fillPlane(level, anchor, sideways, halfWidth, minVertical, maxVertical, Blocks.AIR);
    }

    private static void mark(ServerLevel level, BlockPos anchor, Direction sideways,
                             int lateral, int vertical, Block block) {
        level.setBlockAndUpdate(anchor.relative(sideways, lateral).above(vertical), block.defaultBlockState());
    }
}
