from pathlib import Path

plan_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/IncrementalWorldEditPlan.java")
plan = plan_path.read_text(encoding="utf-8")

constant = """    private static final int CONSTRUCTION_UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
"""
constant_new = constant + """    private static final long MAX_APPLY_NANOS = 40_000_000L;
"""
if "MAX_APPLY_NANOS" not in plan:
    if plan.count(constant) != 1:
        raise SystemExit("world edit time-slice constant insertion point missing")
    plan = plan.replace(constant, constant_new, 1)

old_apply = """    public int apply(ServerLevel level, int budget) {
        flushPendingTerrainColumn();
        int used = 0;
        while (operationIndex < operations.size() && used < budget) {
            Operation operation = operations.get(operationIndex);
            int consumed = operation.apply(level, budget - used);
            used += consumed;
            appliedWrites += consumed;
            if (operation.done()) operationIndex++;
            else break;
        }
        return used;
    }
"""
new_apply = """    public int apply(ServerLevel level, int budget) {
        flushPendingTerrainColumn();
        int used = 0;
        long deadline = System.nanoTime() + MAX_APPLY_NANOS;
        while (operationIndex < operations.size() && used < budget) {
            if (used > 0 && System.nanoTime() >= deadline) break;
            Operation operation = operations.get(operationIndex);
            int consumed = operation.apply(level, budget - used, deadline);
            if (consumed <= 0) break;
            used += consumed;
            appliedWrites += consumed;
            if (operation.done()) operationIndex++;
            else break;
        }
        return used;
    }
"""
if old_apply in plan:
    plan = plan.replace(old_apply, new_apply, 1)
elif new_apply not in plan:
    raise SystemExit("world edit apply time-slice pattern missing")

plan = plan.replace(
    "int apply(ServerLevel level, int budget);",
    "int apply(ServerLevel level, int budget, long deadline);")
plan = plan.replace(
    "@Override public int apply(ServerLevel level, int budget) {\n            if (done || budget <= 0) return 0;",
    "@Override public int apply(ServerLevel level, int budget, long deadline) {\n            if (done || budget <= 0 || System.nanoTime() >= deadline) return 0;")
plan = plan.replace(
    "@Override public int apply(ServerLevel level, int budget) {\n            int used = 0;\n            while (!done && used < budget) {",
    "@Override public int apply(ServerLevel level, int budget, long deadline) {\n            int used = 0;\n            while (!done && used < budget\n                    && (used == 0 || System.nanoTime() < deadline)) {")
if plan.count("int apply(ServerLevel level, int budget, long deadline)") != 3:
    raise SystemExit("world edit operation time-slice signature count mismatch")
plan_path.write_text(plan, encoding="utf-8")

residence_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceBuilder.java")
residence = residence_path.read_text(encoding="utf-8")
old_validation = """        BlockPos door = doorPosition(plot);
        BlockPos doorUpper = door.above();
        if (!level.hasChunkAt(door)
                || !level.getBlockState(door).is(Blocks.SPRUCE_DOOR)
                || !level.getBlockState(doorUpper).is(Blocks.SPRUCE_DOOR)) return false;
        for (BlockPos bed : bedFootPositions(plot)) {
            if (!level.getBlockState(bed).is(bedBlock())) return false;
        }
        if (!level.getBlockState(storagePosition(plot)).is(Blocks.BARREL)
                || !level.getBlockState(hearthPosition(plot)).is(Blocks.FURNACE)
                || !level.getBlockState(workPosition(plot)).is(Blocks.CRAFTING_TABLE)
                || !level.getBlockState(lightPosition(plot)).is(Blocks.LANTERN)) return false;
"""
new_validation = """        BlockPos door = doorPosition(plot);
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
"""
if old_validation in residence:
    residence = residence.replace(old_validation, new_validation, 1)
elif new_validation not in residence:
    raise SystemExit("complete residence validation pattern missing")
residence_path.write_text(residence, encoding="utf-8")
