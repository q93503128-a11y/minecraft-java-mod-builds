from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
STORAGE = JAVA / "settlement/SettlementStorageService.java"
OUTPOST = JAVA / "settlement/SettlementOutpostLogisticsService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

storage = STORAGE.read_text(encoding="utf-8")
outpost = OUTPOST.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

# -----------------------------------------------------------------------------
# Storage selection: logistics needs a null-returning/excluding API so a worker
# can skip a full or unreachable preferred freight bay instead of being forced
# back onto the same stockpile forever.
# -----------------------------------------------------------------------------
old_logistics = '''    /** Outpost deliveries prefer a visible cart-station freight bay before ordinary town storage. */\n    public static BlockPos findLogisticsDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {\n        for (BlockPos pos : cartStationFreightPositions(data)) {\n            if (!level.hasChunkAt(pos)) continue;\n            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;\n            if (hasRoom(container, stack)) return pos;\n        }\n        return findDepositTarget(level, data, stack);\n    }\n'''
new_logistics = '''    /** Outpost deliveries prefer a visible cart-station freight bay before ordinary town storage. */\n    public static BlockPos findLogisticsDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {\n        BlockPos target = findLogisticsDepositTargetExcluding(level, data, stack, Set.of());\n        return target == null ? data.stockpilePos() : target;\n    }\n\n    /**\n     * Reachability-aware transport callers need to exclude a freight/storage target already proved\n     * unusable by this worker. Unlike the legacy convenience wrapper, this method returns null when\n     * no real loaded container with room remains; it never fabricates the stockpile as a fallback.\n     */\n    public static BlockPos findLogisticsDepositTargetExcluding(ServerLevel level, SettlementData data,\n                                                               ItemStack stack, Set<BlockPos> excluded) {\n        for (BlockPos pos : cartStationFreightPositions(data)) {\n            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;\n            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;\n            if (hasRoom(container, stack)) return pos;\n        }\n        return findDepositTargetExcluding(level, data, stack, excluded);\n    }\n'''
if new_logistics not in storage:
    if storage.count(old_logistics) != 1:
        raise SystemExit(f"logistics storage anchor count={storage.count(old_logistics)}")
    storage = storage.replace(old_logistics, new_logistics, 1)

# -----------------------------------------------------------------------------
# Outpost logistics: harden only container interaction. Road waypoint movement
# stays unchanged so existing route pacing/semantics are preserved.
# -----------------------------------------------------------------------------
old_imports = '''import net.minecraft.world.item.Items;\nimport net.minecraft.world.phys.AABB;\n'''
new_imports = '''import net.minecraft.world.item.Items;\nimport net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.level.pathfinder.Path;\nimport net.minecraft.world.phys.AABB;\n'''
if new_imports not in outpost:
    if outpost.count(old_imports) != 1:
        raise SystemExit(f"outpost import anchor count={outpost.count(old_imports)}")
    outpost = outpost.replace(old_imports, new_imports, 1)

# Town-side military and waterfront extraction must skip a storage container that is physically unreachable.
old_military_source = '''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, predicate);\n        if (source == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            move(worker, source, 0.84D);\n            return;\n        }\n'''
new_military_source = '''        BlockPos source = findReachableExtractionTarget(level, data, worker, predicate);\n        if (source == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            moveToStorageInteraction(level, worker, source, 0.84D);\n            return;\n        }\n'''
if new_military_source not in outpost:
    if outpost.count(old_military_source) != 1:
        raise SystemExit(f"military extraction anchor count={outpost.count(old_military_source)}")
    outpost = outpost.replace(old_military_source, new_military_source, 1)

old_waterfront_source = '''        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood);\n        if (source == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            move(worker, source, 0.84D);\n            return;\n        }\n'''
new_waterfront_source = '''        BlockPos source = findReachableExtractionTarget(level, data, worker, SettlementInventory::isWood);\n        if (source == null) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            moveToStorageInteraction(level, worker, source, 0.84D);\n            return;\n        }\n'''
if new_waterfront_source not in outpost:
    if outpost.count(old_waterfront_source) != 1:
        raise SystemExit(f"waterfront extraction anchor count={outpost.count(old_waterfront_source)}")
    outpost = outpost.replace(old_waterfront_source, new_waterfront_source, 1)

# Every outpost-local stockpile interaction should path to a real adjacent standing cell rather than
# repeatedly asking ground navigation to enter the solid container block itself.
old_stock_moves = [
'''            if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)\n                    > STORAGE_INTERACTION_RANGE_SQR) {\n                move(worker, stock, 0.82D);\n                return;\n            }\n''',
'''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            move(worker, stock, 0.84D);\n            return;\n        }\n'''
]
new_stock_moves = [
'''            if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)\n                    > STORAGE_INTERACTION_RANGE_SQR) {\n                moveToStorageInteraction(level, worker, stock, 0.82D);\n                return;\n            }\n''',
'''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            moveToStorageInteraction(level, worker, stock, 0.84D);\n            return;\n        }\n'''
]
# First pattern occurs once (normal pickup); second occurs twice (military + waterfront delivery).
if new_stock_moves[0] not in outpost:
    if outpost.count(old_stock_moves[0]) != 1:
        raise SystemExit(f"normal stock move anchor count={outpost.count(old_stock_moves[0])}")
    outpost = outpost.replace(old_stock_moves[0], new_stock_moves[0], 1)
if outpost.count(new_stock_moves[1]) < 2:
    count = outpost.count(old_stock_moves[1])
    if count != 2:
        raise SystemExit(f"supply stock move anchor count={count}")
    outpost = outpost.replace(old_stock_moves[1], new_stock_moves[1])

old_town_delivery = '''    private static void deliverToTownStorage(ServerLevel level, SettlementData data,\n                                             FrontierWorkerEntity worker, ItemStack carried) {\n        BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);\n        if (!level.hasChunkAt(target)) {\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            move(worker, target, 0.85D);\n            return;\n        }\n        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementStorageService.insertAt(level, target, carried));\n    }\n'''
new_town_delivery = '''    private static void deliverToTownStorage(ServerLevel level, SettlementData data,\n                                             FrontierWorkerEntity worker, ItemStack carried) {\n        BlockPos target = findReachableLogisticsDepositTarget(level, data, worker, carried);\n        if (target == null || !level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {\n            // The exact physical cargo remains in MAINHAND until a real reachable container has room.\n            worker.getNavigation().stop();\n            return;\n        }\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                > STORAGE_INTERACTION_RANGE_SQR) {\n            moveToStorageInteraction(level, worker, target, 0.85D);\n            return;\n        }\n        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementStorageService.insertAt(level, target, carried));\n    }\n\n    private static BlockPos findReachableExtractionTarget(ServerLevel level, SettlementData data,\n                                                          FrontierWorkerEntity worker, Predicate<ItemStack> predicate) {\n        Set<BlockPos> excluded = new HashSet<>();\n        while (true) {\n            BlockPos source = SettlementStorageService.findExtractionTargetExcluding(level, data, predicate, excluded);\n            if (source == null) return null;\n            if (canReachStorageInteraction(level, worker, source)) return source;\n            excluded.add(source);\n        }\n    }\n\n    private static BlockPos findReachableLogisticsDepositTarget(ServerLevel level, SettlementData data,\n                                                                FrontierWorkerEntity worker, ItemStack stack) {\n        Set<BlockPos> excluded = new HashSet<>();\n        while (true) {\n            BlockPos target = SettlementStorageService.findLogisticsDepositTargetExcluding(\n                    level, data, stack, excluded);\n            if (target == null) return null;\n            if (canReachStorageInteraction(level, worker, target)) return target;\n            excluded.add(target);\n        }\n    }\n\n    private static boolean canReachStorageInteraction(ServerLevel level, FrontierWorkerEntity worker, BlockPos target) {\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                <= STORAGE_INTERACTION_RANGE_SQR) return true;\n        for (BlockPos approach : storageApproachPositions(level, worker, target)) {\n            if (createStoragePath(worker, approach) != null) return true;\n        }\n        return false;\n    }\n\n    private static boolean moveToStorageInteraction(ServerLevel level, FrontierWorkerEntity worker,\n                                                    BlockPos target, double speed) {\n        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)\n                <= STORAGE_INTERACTION_RANGE_SQR) return true;\n        for (BlockPos approach : storageApproachPositions(level, worker, target)) {\n            Path path = createStoragePath(worker, approach);\n            if (path != null && worker.getNavigation().moveTo(path, speed)) return true;\n        }\n        worker.getNavigation().stop();\n        return false;\n    }\n\n    private static List<BlockPos> storageApproachPositions(ServerLevel level, FrontierWorkerEntity worker,\n                                                           BlockPos target) {\n        int[][] offsets = { {0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1} };\n        List<BlockPos> result = new ArrayList<>();\n        for (int dy = -1; dy <= 1; dy++) {\n            for (int[] offset : offsets) {\n                BlockPos approach = target.offset(offset[0], dy, offset[1]);\n                if (isWalkableStorageApproach(level, approach)) result.add(approach);\n            }\n        }\n        result.sort(Comparator.comparingDouble(pos -> worker.distanceToSqr(\n                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));\n        return List.copyOf(result);\n    }\n\n    private static Path createStoragePath(FrontierWorkerEntity worker, BlockPos target) {\n        Path path = worker.getNavigation().createPath(target, 0);\n        if (path == null || !path.canReach() || path.getEndNode() == null\n                || !path.getEndNode().asBlockPos().equals(target)) return null;\n        return path;\n    }\n\n    private static boolean isWalkableStorageApproach(ServerLevel level, BlockPos feet) {\n        BlockPos head = feet.above();\n        BlockPos below = feet.below();\n        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head) || !level.hasChunkAt(below)) return false;\n        if (level.getBlockEntity(feet) != null || level.getBlockEntity(head) != null) return false;\n        BlockState feetState = level.getBlockState(feet);\n        BlockState headState = level.getBlockState(head);\n        BlockState belowState = level.getBlockState(below);\n        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()\n                || !belowState.getFluidState().isEmpty()) return false;\n        if ((!feetState.isAir() && !feetState.canBeReplaced())\n                || (!headState.isAir() && !headState.canBeReplaced())) return false;\n        return !belowState.isAir() && !belowState.canBeReplaced();\n    }\n'''
if new_town_delivery not in outpost:
    if outpost.count(old_town_delivery) != 1:
        raise SystemExit(f"town delivery anchor count={outpost.count(old_town_delivery)}")
    outpost = outpost.replace(old_town_delivery, new_town_delivery, 1)

# -----------------------------------------------------------------------------
# Persistent source audit: this hardening must remain visible after the one-shot
# patch workflow is deleted.
# -----------------------------------------------------------------------------
old_audit_var = '''worker = text(JAVA / "settlement/SettlementWorkerService.java")\nstorage = text(JAVA / "settlement/SettlementStorageService.java")\nentity = text(JAVA / "content/FrontierWorkerEntity.java")\n'''
new_audit_var = '''worker = text(JAVA / "settlement/SettlementWorkerService.java")\nstorage = text(JAVA / "settlement/SettlementStorageService.java")\noutpost = text(JAVA / "settlement/SettlementOutpostLogisticsService.java")\nentity = text(JAVA / "content/FrontierWorkerEntity.java")\n'''
if new_audit_var not in audit:
    if audit.count(old_audit_var) != 1:
        raise SystemExit(f"audit var anchor count={audit.count(old_audit_var)}")
    audit = audit.replace(old_audit_var, new_audit_var, 1)

old_audit_storage_tail = '''    "ensureStarterSupplyDepot(level, stockpile);"\n), "shared storage hardening")\nmust(service, (\n'''
new_audit_storage_tail = '''    "ensureStarterSupplyDepot(level, stockpile);",\n    "findLogisticsDepositTargetExcluding"\n), "shared storage hardening")\nmust(outpost, (\n    "findReachableExtractionTarget", "findReachableLogisticsDepositTarget",\n    "findLogisticsDepositTargetExcluding", "canReachStorageInteraction",\n    "moveToStorageInteraction", "createStoragePath", "path.canReach()"\n), "outpost storage reachability")\nforbid(outpost, (\n    "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);",\n), "outpost forced storage fallback")\nmust(service, (\n'''
if new_audit_storage_tail not in audit:
    if audit.count(old_audit_storage_tail) != 1:
        raise SystemExit(f"audit storage tail anchor count={audit.count(old_audit_storage_tail)}")
    audit = audit.replace(old_audit_storage_tail, new_audit_storage_tail, 1)

STORAGE.write_text(storage, encoding="utf-8")
OUTPOST.write_text(outpost, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

# One-shot script invariants.
final_storage = STORAGE.read_text(encoding="utf-8")
final_outpost = OUTPOST.read_text(encoding="utf-8")
final_audit = AUDIT.read_text(encoding="utf-8")
for token in (
    "findLogisticsDepositTargetExcluding",
    "return findDepositTargetExcluding(level, data, stack, excluded);",
):
    if token not in final_storage:
        raise SystemExit(f"storage invariant missing: {token}")
for token in (
    "findReachableExtractionTarget",
    "findReachableLogisticsDepositTarget",
    "moveToStorageInteraction",
    "createStoragePath",
    "path.canReach()",
    "The exact physical cargo remains in MAINHAND",
):
    if token not in final_outpost:
        raise SystemExit(f"outpost invariant missing: {token}")
if "BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);" in final_outpost:
    raise SystemExit("legacy forced logistics target remains")
if "outpost storage reachability" not in final_audit:
    raise SystemExit("persistent outpost audit missing")

print("OUTPOST STORAGE REACHABILITY PATCH PASS")
