package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.List;

/** Adds role furniture only into source-air cells selected by the authored-ground plan. */
public final class ErdenUrbanAuthoredGroundMaterializer {
    public static final int MATERIALIZER_REVISION = 1;
    private static boolean ciLogged;

    private ErdenUrbanAuthoredGroundMaterializer() {}

    public static boolean tryMaterialize(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =
                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);
        if (plan == null) {
            throw new IllegalStateException("Missing authored-ground plan role=" + entrance.role()
                    + " entrance=" + entrance.x() + "," + entrance.z());
        }
        if (!chunksReady(level, plan)) return false;

        for (ErdenUrbanAuthoredGroundPlanCatalog.BedPlan bed : plan.beds()) {
            placeBed(level, plan.role(), bed);
        }
        for (ErdenUrbanAuthoredGroundPlanCatalog.FixturePlan fixture : plan.fixtures()) {
            placeFixture(level, fixture);
        }
        verify(level, plan);

        if (!ciLogged && "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) {
            ciLogged = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_AUTHORED_GROUND_MATERIALIZER_PASS role={} entrance={},{} ground_cells={} beds={} fixtures={} resident_targets={} work_target=true source_floor_reused=true source_air_fixtures=true synthetic_room=false source_blocks_cut=0 revision={}",
                    plan.role(), entrance.x(), entrance.z(), plan.groundCells(),
                    plan.beds().size(), plan.fixtures().size(), plan.residentTargets().size(),
                    MATERIALIZER_REVISION);
        }
        return true;
    }

    private static boolean chunksReady(
            ServerLevel level,
            ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan) {
        for (BlockPos pos : positions(plan)) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            if (!level.hasChunk(chunkX, chunkZ)
                    || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) return false;
        }
        return true;
    }

    private static List<BlockPos> positions(
            ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan) {
        ArrayList<BlockPos> result = new ArrayList<>();
        result.addAll(plan.residentTargets());
        result.add(plan.workTarget());
        if (plan.primaryContainer() != null) result.add(plan.primaryContainer());
        for (ErdenUrbanAuthoredGroundPlanCatalog.BedPlan bed : plan.beds()) {
            result.add(bed.foot());
            result.add(bed.head());
        }
        for (ErdenUrbanAuthoredGroundPlanCatalog.FixturePlan fixture : plan.fixtures()) {
            result.add(fixture.pos());
        }
        return List.copyOf(result);
    }

    private static void placeBed(
            ServerLevel level,
            String role,
            ErdenUrbanAuthoredGroundPlanCatalog.BedPlan bed) {
        BedBlock block = switch (role) {
            case "inn" -> bed("red_bed");
            case "guard_post" -> bed("gray_bed");
            default -> bed("white_bed");
        };
        Direction facing = direction(bed.foot(), bed.head());
        requireAirOr(level, bed.foot(), block);
        requireAirOr(level, bed.head(), block);
        requireSupport(level, bed.foot(), "bed-foot");
        requireSupport(level, bed.head(), "bed-head");
        BlockState foot = block.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(HorizontalDirectionalBlock.FACING, facing);
        BlockState head = block.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(HorizontalDirectionalBlock.FACING, facing);
        level.setBlockAndUpdate(bed.foot(), foot);
        level.setBlockAndUpdate(bed.head(), head);
    }

    private static BedBlock bed(String path) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", path));
        if (!(block instanceof BedBlock bed)) {
            throw new IllegalStateException("Missing Minecraft bed block minecraft:" + path);
        }
        return bed;
    }

    private static void placeFixture(
            ServerLevel level,
            ErdenUrbanAuthoredGroundPlanCatalog.FixturePlan fixture) {
        Block block = switch (fixture.kind()) {
            case PRIMARY_BARREL, BARREL -> Blocks.BARREL;
            case CHEST -> Blocks.CHEST;
            case CRAFTING_TABLE -> Blocks.CRAFTING_TABLE;
            case FURNACE -> Blocks.FURNACE;
            case SMOKER -> Blocks.SMOKER;
            case BOOKSHELF -> Blocks.BOOKSHELF;
            case LECTERN -> Blocks.LECTERN;
            case HAY -> Blocks.HAY_BLOCK;
            case WATER_CAULDRON -> Blocks.WATER_CAULDRON;
            case ANVIL -> Blocks.ANVIL;
            case TARGET -> Blocks.TARGET;
            case STONECUTTER -> Blocks.STONECUTTER;
        };
        requireAirOr(level, fixture.pos(), block);
        requireSupport(level, fixture.pos(), fixture.kind().name());
        level.setBlockAndUpdate(fixture.pos(), block.defaultBlockState());
    }

    private static void requireAirOr(ServerLevel level, BlockPos pos, Block desired) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() == desired) return;
        if (!current.isAir()) {
            throw new IllegalStateException("Authored-ground materializer refused source cut pos="
                    + pos + " existing=" + current + " desired=" + desired);
        }
    }

    private static void requireSupport(ServerLevel level, BlockPos pos, String label) {
        BlockState floor = level.getBlockState(pos.below());
        if (floor.isAir() || !floor.getFluidState().isEmpty()) {
            throw new IllegalStateException("Authored-ground fixture lacks source floor label="
                    + label + " pos=" + pos);
        }
    }

    private static void verify(
            ServerLevel level,
            ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan) {
        if (plan.residentTargets().size() != 3 || plan.workTarget() == null) {
            throw new IllegalStateException("Authored-ground movement target count drifted role=" + plan.role());
        }
        for (BlockPos target : plan.residentTargets()) verifyWalkable(level, target, "resident");
        verifyWalkable(level, plan.workTarget(), "work");
        for (ErdenUrbanAuthoredGroundPlanCatalog.BedPlan bed : plan.beds()) {
            if (!(level.getBlockState(bed.foot()).getBlock() instanceof BedBlock)
                    || !(level.getBlockState(bed.head()).getBlock() instanceof BedBlock)) {
                throw new IllegalStateException("Authored-ground bed failed role=" + plan.role());
            }
        }
        for (ErdenUrbanAuthoredGroundPlanCatalog.FixturePlan fixture : plan.fixtures()) {
            if (level.getBlockState(fixture.pos()).isAir()) {
                throw new IllegalStateException("Authored-ground fixture missing kind=" + fixture.kind());
            }
        }
    }

    private static void verifyWalkable(ServerLevel level, BlockPos target, String label) {
        BlockState floor = level.getBlockState(target.below());
        if (floor.isAir() || !floor.getFluidState().isEmpty()
                || !level.getBlockState(target).isAir()
                || !level.getBlockState(target.above()).isAir()) {
            throw new IllegalStateException("Authored-ground target not walkable label="
                    + label + " target=" + target);
        }
    }

    private static Direction direction(BlockPos foot, BlockPos head) {
        int dx = head.getX() - foot.getX();
        int dz = head.getZ() - foot.getZ();
        if (dx == 1 && dz == 0) return Direction.EAST;
        if (dx == -1 && dz == 0) return Direction.WEST;
        if (dx == 0 && dz == 1) return Direction.SOUTH;
        if (dx == 0 && dz == -1) return Direction.NORTH;
        throw new IllegalStateException("Authored-ground bed cells are not adjacent foot="
                + foot + " head=" + head);
    }
}
