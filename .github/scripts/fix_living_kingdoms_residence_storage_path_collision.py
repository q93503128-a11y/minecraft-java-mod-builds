from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceBuilder.java')
text = path.read_text(encoding='utf-8')

text = text.replace('import java.util.List;\n', 'import java.util.ArrayList;\nimport java.util.List;\n', 1)

old_validate = '''        BlockPos path = accessPathSample(plot);
        return level.getBlockState(path).is(Blocks.DIRT_PATH)
                || level.getBlockState(path).is(Blocks.COBBLESTONE);
'''
new_validate = '''        for (BlockPos path : accessPathPositions(plot)) {
            BlockState state = level.getBlockState(path);
            if (!state.is(Blocks.DIRT_PATH) && !state.is(Blocks.COBBLESTONE)) return false;
            if (collidesWithStorage(path)) return false;
        }
        return true;
'''
if old_validate not in text:
    if new_validate not in text:
        raise SystemExit('residence path validation pattern missing')
else:
    text = text.replace(old_validate, new_validate, 1)

old_path = '''    private static void addAccessPath(
            IncrementalWorldEditPlan plan,
            Footprint footprint,
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        BlockPos door = doorPosition(plot);
        Direction outward = footprint.doorFacing();
        for (int distance = 1; distance <= 5; distance++) {
            BlockPos position = door.relative(outward, distance).below();
            if ((position.getX() >> 4) != footprint.chunkX()
                    || (position.getZ() >> 4) != footprint.chunkZ()) break;
            addSet(plan, position, Blocks.DIRT_PATH);
            plan.addFill(position.getX(), position.getY() + 1, position.getZ(),
                    position.getX(), position.getY() + 3, position.getZ(), Blocks.AIR);
        }
        BlockPos step = door.relative(outward).below();
        if ((step.getX() >> 4) == footprint.chunkX()
                && (step.getZ() >> 4) == footprint.chunkZ()) {
            addSet(plan, step, Blocks.COBBLESTONE);
        }
    }

    private static BlockPos accessPathSample(
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        Footprint footprint = footprint(plot);
        BlockPos door = doorPosition(plot);
        BlockPos first = door.relative(footprint.doorFacing()).below();
        if ((first.getX() >> 4) == footprint.chunkX()
                && (first.getZ() >> 4) == footprint.chunkZ()) return first;
        return door.relative(footprint.doorFacing()).below();
    }
'''
new_path = '''    private static void addAccessPath(
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
            if (storage.getX() == position.getX()
                    && storage.getY() == position.getY()
                    && storage.getZ() == position.getZ()) return true;
        }
        return false;
    }
'''
if old_path not in text:
    if new_path not in text:
        raise SystemExit('residence access path implementation pattern missing')
else:
    text = text.replace(old_path, new_path, 1)

path.write_text(text, encoding='utf-8')
