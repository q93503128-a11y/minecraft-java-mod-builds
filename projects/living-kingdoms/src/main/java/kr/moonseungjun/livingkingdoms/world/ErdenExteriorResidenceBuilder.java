package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/**
 * Metre-scale physical homes for the 74 exterior households. Logical estate parcels remain stable,
 * while physical homes occupy collision-free worker hamlets outside production geometry. Every unit has
 * a real door, three beds, storage, a hearth/work surface, lighting and a short access path.
 */
public final class ErdenExteriorResidenceBuilder {
    public static final int RESIDENCE_REVISION = 2;
    public static final int WIDTH = 9;
    public static final int DEPTH = 9;
    public static final int BEDS_PER_RESIDENCE = 3;

    private static final Identifier BED_ID =
            Identifier.fromNamespaceAndPath("minecraft", "red_bed");

    private ErdenExteriorResidenceBuilder() {
    }

    public static void addChunk(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        if (footprint.chunkX() != (chunk.getMinBlockX() >> 4) || footprint.chunkZ() != (chunk.getMinBlockZ() >> 4)) return;

        Block wall = wallFor(plot.nodeRole());
        Block floor = floorFor(plot.nodeRole());
        int baseY = footprint.baseY();
        int minX = footprint.minX();
        int minZ = footprint.minZ();
        int maxX = minX + WIDTH - 1;
        int maxZ = minZ + DEPTH - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int original = plan.originalSurfaceY(level, x, z);
                if (original < baseY) {
                    plan.addFill(x, original + 1, z, x, baseY - 1, z, Blocks.DIRT);
                } else if (original > baseY) {
                    plan.addFill(x, baseY + 1, z, x, original + 4, z, Blocks.AIR);
                }
                plan.addSet(x, baseY, z, Blocks.STONE_BRICKS);
                plan.addSet(x, baseY + 1, z, floor);
                plan.setPlannedSurfaceY(x, z, baseY + 1);
            }
        }

        plan.addFill(minX, baseY + 2, minZ, maxX, baseY + 5, maxZ, wall);
        plan.addFill(minX + 1, baseY + 2, minZ + 1,
                maxX - 1, baseY + 5, maxZ - 1, Blocks.AIR);
        addFrame(plan, footprint, baseY);
        addWindows(plan, footprint, baseY);
        addDoor(plan, footprint, baseY);
        addRoof(plan, footprint, baseY);
        addBeds(plan, footprint, baseY);
        addFixtures(plan, footprint, baseY);
        addAccessPath(plan, footprint, plot);
    }

    public static boolean validateLoadedResidence(
            ServerLevel level,
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        BlockPos door = doorPosition(plot);
        BlockPos doorUpper = door.above();
        if (!level.hasChunkAt(door)) return false;
        BlockState lowerDoor = level.getBlockState(door);
        BlockState upperDoor = level.getBlockState(doorUpper);
        if (!lowerDoor.is(Blocks.SPRUCE_DOOR)
                || lowerDoor.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                != DoubleBlockHalf.LOWER
                || !upperDoor.is(Blocks.SPRUCE_DOOR)
                || upperDoor.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                != DoubleBlockHalf.UPPER) return false;
        for (BlockPos footPosition : bedFootPositions(plot)) {
            BlockState foot = level.getBlockState(footPosition);
            BlockState head = level.getBlockState(footPosition.east());
            if (!foot.is(bedBlock())
                    || foot.getValue(BlockStateProperties.BED_PART) != BedPart.FOOT
                    || !head.is(bedBlock())
                    || head.getValue(BlockStateProperties.BED_PART) != BedPart.HEAD) return false;
        }
        BlockState lantern = level.getBlockState(lightPosition(plot));
        if (!level.getBlockState(storagePosition(plot)).is(Blocks.BARREL)
                || !level.getBlockState(hearthPosition(plot)).is(Blocks.FURNACE)
                || !level.getBlockState(workPosition(plot)).is(Blocks.CRAFTING_TABLE)
                || !lantern.is(Blocks.LANTERN)
                || !lantern.getValue(BlockStateProperties.HANGING)) return false;
        for (BlockPos path : accessPathPositions(plot)) {
            BlockState state = level.getBlockState(path);
            if (!state.is(Blocks.DIRT_PATH) && !state.is(Blocks.COBBLESTONE)) return false;
            if (collidesWithStorage(path)) return false;
        }
        return true;
    }

    public static BlockPos residentSpawnPosition(String householdId, int slot) {
        ErdenExteriorResidenceCatalog.ResidencePlot plot =
                ErdenExteriorResidenceCatalog.plot(householdId);
        if (plot == null) return BlockPos.ZERO;
        Footprint footprint = footprint(plot);
        int normalized = Math.floorMod(slot, 4);
        int x = footprint.minX() + 4 + (normalized & 1);
        int z = footprint.minZ() + 4 + (normalized >> 1);
        return new BlockPos(x, footprint.baseY() + 2, z);
    }

    public static BlockPos homeTarget(String householdId) {
        ErdenExteriorResidenceCatalog.ResidencePlot plot =
                ErdenExteriorResidenceCatalog.plot(householdId);
        if (plot == null) return BlockPos.ZERO;
        Footprint footprint = footprint(plot);
        BlockPos door = doorPosition(plot);
        Direction inward = footprint.doorFacing().getOpposite();
        return door.relative(inward);
    }

    public static BlockPos doorPosition(ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        int centerX = footprint.minX() + WIDTH / 2;
        int centerZ = footprint.minZ() + DEPTH / 2;
        return switch (footprint.doorFacing()) {
            case NORTH -> new BlockPos(centerX, footprint.baseY() + 2, footprint.minZ());
            case SOUTH -> new BlockPos(centerX, footprint.baseY() + 2,
                    footprint.minZ() + DEPTH - 1);
            case WEST -> new BlockPos(footprint.minX(), footprint.baseY() + 2, centerZ);
            case EAST -> new BlockPos(footprint.minX() + WIDTH - 1,
                    footprint.baseY() + 2, centerZ);
            default -> throw new IllegalStateException("Vertical residence door direction");
        };
    }

    public static List<BlockPos> bedFootPositions(
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        int y = footprint.baseY() + 2;
        return List.of(
                new BlockPos(footprint.minX() + 2, y, footprint.minZ() + 2),
                new BlockPos(footprint.minX() + 2, y, footprint.minZ() + 4),
                new BlockPos(footprint.minX() + 2, y, footprint.minZ() + 6)
        );
    }

    public static BlockPos storagePosition(ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        return new BlockPos(footprint.minX() + 6, footprint.baseY() + 2,
                footprint.minZ() + 2);
    }

    public static BlockPos hearthPosition(ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        return new BlockPos(footprint.minX() + 6, footprint.baseY() + 2,
                footprint.minZ() + 4);
    }

    public static BlockPos workPosition(ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        return new BlockPos(footprint.minX() + 6, footprint.baseY() + 2,
                footprint.minZ() + 6);
    }

    public static BlockPos lightPosition(ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        return new BlockPos(footprint.minX() + 4, footprint.baseY() + 5,
                footprint.minZ() + 4);
    }

    private static void addFrame(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            int baseY) {
        int minX = footprint.minX();
        int minZ = footprint.minZ();
        int maxX = minX + WIDTH - 1;
        int maxZ = minZ + DEPTH - 1;
        for (int y = baseY + 2; y <= baseY + 6; y++) {
            plan.addSet(minX, y, minZ, Blocks.STRIPPED_SPRUCE_LOG);
            plan.addSet(maxX, y, minZ, Blocks.STRIPPED_SPRUCE_LOG);
            plan.addSet(minX, y, maxZ, Blocks.STRIPPED_SPRUCE_LOG);
            plan.addSet(maxX, y, maxZ, Blocks.STRIPPED_SPRUCE_LOG);
        }
    }

    private static void addWindows(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            int baseY) {
        int minX = footprint.minX();
        int minZ = footprint.minZ();
        int maxX = minX + WIDTH - 1;
        int maxZ = minZ + DEPTH - 1;
        plan.addSet(minX + 2, baseY + 3, minZ, Blocks.GLASS_PANE);
        plan.addSet(minX + 6, baseY + 3, minZ, Blocks.GLASS_PANE);
        plan.addSet(minX + 2, baseY + 3, maxZ, Blocks.GLASS_PANE);
        plan.addSet(minX + 6, baseY + 3, maxZ, Blocks.GLASS_PANE);
        plan.addSet(minX, baseY + 3, minZ + 2, Blocks.GLASS_PANE);
        plan.addSet(minX, baseY + 3, minZ + 6, Blocks.GLASS_PANE);
        plan.addSet(maxX, baseY + 3, minZ + 2, Blocks.GLASS_PANE);
        plan.addSet(maxX, baseY + 3, minZ + 6, Blocks.GLASS_PANE);
    }

    private static void addDoor(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            int baseY) {
        BlockPos lower = doorPosition(footprint.plot());
        BlockState lowerState = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, footprint.doorFacing())
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upperState = lowerState
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        addSet(plan, lower, lowerState);
        addSet(plan, lower.above(), upperState);
    }

    private static void addRoof(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            int baseY) {
        int minX = footprint.minX();
        int minZ = footprint.minZ();
        int maxX = minX + WIDTH - 1;
        int maxZ = minZ + DEPTH - 1;
        plan.addFill(minX, baseY + 6, minZ, maxX, baseY + 6, maxZ,
                Blocks.DARK_OAK_PLANKS);
        plan.addFill(minX + 1, baseY + 7, minZ + 1,
                maxX - 1, baseY + 7, maxZ - 1, Blocks.DARK_OAK_PLANKS);
        plan.addSet(maxX - 1, baseY + 8, maxZ - 1, Blocks.BRICKS);
    }

    private static void addBeds(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            int baseY) {
        BlockState foot = bedBlock().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        BlockState head = foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
        for (BlockPos footPos : bedFootPositions(footprint.plot())) {
            addSet(plan, footPos, foot);
            addSet(plan, footPos.east(), head);
        }
    }

    private static void addFixtures(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            int baseY) {
        ErdenExteriorResidenceCatalog.ResidencePlot plot = footprint.plot();
        addSet(plan, storagePosition(plot), Blocks.BARREL);
        addSet(plan, hearthPosition(plot), Blocks.FURNACE);
        addSet(plan, workPosition(plot), Blocks.CRAFTING_TABLE);
        addSet(plan, lightPosition(plot), Blocks.LANTERN.defaultBlockState()
                .setValue(BlockStateProperties.HANGING, true));
        plan.addSet(footprint.minX() + 5, baseY + 2,
                footprint.minZ() + 6, Blocks.OAK_FENCE);
        plan.addSet(footprint.minX() + 5, baseY + 3,
                footprint.minZ() + 6, Blocks.OAK_PRESSURE_PLATE);
    }

    private static void addAccessPath(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        List<BlockPos> positions = accessPathPositions(plot);
        for (BlockPos position : positions) {
            addSet(plan, position, Blocks.DIRT_PATH);
            plan.addFill(position.getX(), position.getY() + 1, position.getZ(),
                    position.getX(), position.getY() + 3, position.getZ(), Blocks.AIR);
        }
        if (!positions.isEmpty()) addSet(plan, positions.getFirst(), Blocks.COBBLESTONE);
    }

    private static List<BlockPos> accessPathPositions(
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        BlockPos door = doorPosition(plot);
        Direction outward = footprint.doorFacing();
        List<BlockPos> straight = straightAccessPath(footprint, door, outward, null);
        boolean blocked = straight.stream().anyMatch(ErdenExteriorResidenceBuilder::collidesWithStorage);
        if (!blocked) return straight;

        List<BlockPos> clockwise = detouredAccessPath(
                footprint, door, outward, outward.getClockWise());
        if (clockwise.stream().noneMatch(ErdenExteriorResidenceBuilder::collidesWithStorage)) {
            return clockwise;
        }
        List<BlockPos> counterClockwise = detouredAccessPath(
                footprint, door, outward, outward.getCounterClockWise());
        if (counterClockwise.stream().noneMatch(ErdenExteriorResidenceBuilder::collidesWithStorage)) {
            return counterClockwise;
        }
        throw new IllegalStateException(
                "No storage-safe Erden residence access path for " + plot.householdId());
    }

    private static List<BlockPos> straightAccessPath(
            Footprint footprint,
            BlockPos door,
            Direction outward,
            Direction lateral) {
        List<BlockPos> positions = new ArrayList<>();
        for (int distance = 1; distance <= 5; distance++) {
            BlockPos position = door.relative(outward, distance).below();
            if (lateral != null && distance >= 2) position = position.relative(lateral);
            if (!insideResidenceChunk(footprint, position)) break;
            positions.add(position);
        }
        return positions;
    }

    private static List<BlockPos> detouredAccessPath(
            Footprint footprint,
            BlockPos door,
            Direction outward,
            Direction lateral) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos first = door.relative(outward).below();
        if (!insideResidenceChunk(footprint, first)) return positions;
        positions.add(first);
        BlockPos bend = first.relative(lateral);
        if (!insideResidenceChunk(footprint, bend)) return List.of();
        positions.add(bend);
        for (int distance = 2; distance <= 5; distance++) {
            BlockPos position = door.relative(outward, distance).below().relative(lateral);
            if (!insideResidenceChunk(footprint, position)) break;
            positions.add(position);
        }
        return positions;
    }

    private static boolean insideResidenceChunk(Footprint footprint, BlockPos position) {
        return (position.getX() >> 4) == footprint.chunkX()
                && (position.getZ() >> 4) == footprint.chunkZ();
    }

    private static boolean collidesWithStorage(BlockPos position) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            BlockPos storage = ErdenKingdomExteriorBuilder.storagePosition(null, node);
            if (storage.getY() != position.getY() || storage.getZ() != position.getZ()) continue;
            int dx = position.getX() - storage.getX();
            if (dx == 0 || dx == -2 || dx == 2) return true;
        }
        return false;
    }

    private static Footprint footprint(
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        int chunkX = plot.physicalX() >> 4;
        int chunkZ = plot.physicalZ() >> 4;
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int desiredX = plot.physicalX() - WIDTH / 2;
        int desiredZ = plot.physicalZ() - DEPTH / 2;
        int minX = Math.clamp(desiredX, chunkMinX + 1, chunkMinX + 6);
        int minZ = Math.clamp(desiredZ, chunkMinZ + 1, chunkMinZ + 6);
        int centerX = minX + WIDTH / 2;
        int centerZ = minZ + DEPTH / 2;
        ErdenKingdomSupplyCatalog.SupplyNode node =
                ErdenKingdomSupplyCatalog.node(plot.nodeId());
        if (node == null) {
            throw new IllegalStateException("Missing residence node " + plot.nodeId());
        }
        int dx = node.x - centerX;
        int dz = node.z - centerZ;
        Direction doorFacing = Math.abs(dx) >= Math.abs(dz)
                ? (dx >= 0 ? Direction.EAST : Direction.WEST)
                : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
        int baseY = (int) Math.round(
                AuthoredContinentDensity.surfaceHeight(centerX, centerZ));
        return new Footprint(plot, chunkX, chunkZ, minX, minZ, baseY, doorFacing);
    }

    private static Block bedBlock() {
        return BuiltInRegistries.BLOCK.getOptional(BED_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing registered Erden residence bed " + BED_ID));
    }

    private static void addSet(
            IncrementalWorldEditPlan plan,
            BlockPos position,
            Block block) {
        plan.addSet(position.getX(), position.getY(), position.getZ(), block);
    }

    private static void addSet(
            IncrementalWorldEditPlan plan,
            BlockPos position,
            BlockState state) {
        plan.addSet(position.getX(), position.getY(), position.getZ(), state);
    }

    private static Block wallFor(String role) {
        return switch (role) {
            case "grain_estate" -> Blocks.OAK_PLANKS;
            case "ranch" -> Blocks.SPRUCE_PLANKS;
            case "colliery" -> Blocks.COBBLESTONE;
            case "iron_mine" -> Blocks.DEEPSLATE_BRICKS;
            case "paper_mill" -> Blocks.BIRCH_PLANKS;
            case "river_wharf" -> Blocks.OAK_PLANKS;
            default -> Blocks.SPRUCE_PLANKS;
        };
    }

    private static Block floorFor(String role) {
        return switch (role) {
            case "colliery", "iron_mine" -> Blocks.POLISHED_ANDESITE;
            case "paper_mill" -> Blocks.BIRCH_PLANKS;
            default -> Blocks.SPRUCE_PLANKS;
        };
    }

    private record Footprint(
            ErdenExteriorResidenceCatalog.ResidencePlot plot,
            int chunkX,
            int chunkZ,
            int minX,
            int minZ,
            int baseY,
            Direction doorFacing) {
    }
}
