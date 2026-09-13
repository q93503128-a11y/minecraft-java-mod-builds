from pathlib import Path

ROOT = Path("projects/frontier-settlement")
CONSTRUCTION = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java"
WORKERS = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java"
SOURCE_TEST = ROOT / "tools/test_current_source.py"
PROPS = ROOT / "gradle.properties"
README = ROOT / "README.md"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


construction = CONSTRUCTION.read_text(encoding="utf-8")
old_envelope = '''        if (builder.getX() < minX || builder.getX() > maxX || builder.getZ() < minZ || builder.getZ() > maxZ) return false;
        int x = (int) Math.floor(builder.getX());
        int z = (int) Math.floor(builder.getZ());
        BlockPos ground = safeGroundWorkCell(level, x, z);
        return ground != null && Math.abs(builder.getY() - ground.getY()) <= 1.25D;
'''
new_envelope = '''        if (builder.getX() < minX || builder.getX() > maxX || builder.getZ() < minZ || builder.getZ() > maxZ) return false;
        int x = (int) Math.floor(builder.getX());
        int z = (int) Math.floor(builder.getZ());
        // A resident standing inside the blueprint footprint is not a valid finished approach.
        // Treating the interior as "on site" let the next wall/floor placement occupy the resident's
        // own cell, after which the old recovery path jumped them back to the settlement home.
        boolean insideFootprint = x >= construction.originX() && x < construction.originX() + width
                && z >= construction.originZ() && z < construction.originZ() + depth;
        if (insideFootprint) return false;
        BlockPos ground = safeGroundWorkCell(level, x, z);
        return ground != null && Math.abs(builder.getY() - ground.getY()) <= 1.25D;
'''
construction = replace_once(construction, old_envelope, new_envelope, "construction footprint envelope")

old_recovery = '''    private static void recoverBuilderFromBlockedCell(ServerLevel level, SettlementData data,
                                                       FrontierWorkerEntity builder, Set<BlockPos> occupied) {
        BlockPos feet = builder.blockPosition();
        BlockPos head = feet.above();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head)) return;
        boolean physicallyBlocked = blocksCurrentPathCell(level, feet, level.getBlockState(feet))
                || blocksCurrentPathCell(level, head, level.getBlockState(head));
        boolean elevatedCandidate = !physicallyBlocked && builderOnArtificialElevation(level, builder);
        if (!physicallyBlocked && !elevatedCandidate) return;

        BlockPos safe = findSafeBuilderHome(level, data, occupied);
        if (safe == null) return;
        // A connected bridge/balcony is still physical world traversal. Only a genuinely disconnected
        // elevated perch is recovered; ordinary accessible structures never become a teleport shortcut.
        if (elevatedCandidate && !builderStrandedOnArtificialElevation(level, builder, safe)) return;
        builder.getNavigation().stop();
        builder.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
    }

    private static boolean builderOnArtificialElevation(ServerLevel level, FrontierWorkerEntity builder) {
        BlockPos feet = builder.blockPosition();
        int naturalGroundY = nearestNaturalGroundBelow(level, feet, 16);
        if (naturalGroundY == Integer.MIN_VALUE) return false;
        int artificialRise = (feet.getY() - 1) - naturalGroundY;
        return artificialRise >= 3;
    }

    /** Accessible balconies/bridges stay physical; only a disconnected elevated perch is recovered. */
    private static boolean builderStrandedOnArtificialElevation(ServerLevel level, FrontierWorkerEntity builder,
                                                                 BlockPos safe) {
        return builderOnArtificialElevation(level, builder) && createReachablePath(builder, safe) == null;
    }

    private static int nearestNaturalGroundBelow(ServerLevel level, BlockPos feet, int maxDepth) {
        for (int depth = 1; depth <= maxDepth; depth++) {
            BlockPos probe = feet.below(depth);
            if (!level.hasChunkAt(probe)) return Integer.MIN_VALUE;
            if (isNaturalGround(level.getBlockState(probe))) return probe.getY();
        }
        return Integer.MIN_VALUE;
    }
'''
new_recovery = '''    private static void recoverBuilderFromBlockedCell(ServerLevel level, SettlementData data,
                                                       FrontierWorkerEntity builder, Set<BlockPos> occupied) {
        BlockPos feet = builder.blockPosition();
        BlockPos head = feet.above();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head)) return;
        boolean physicallyBlocked = blocksCurrentPathCell(level, feet, level.getBlockState(feet))
                || blocksCurrentPathCell(level, head, level.getBlockState(head));
        if (!physicallyBlocked) return;

        // Automatic recovery must never jump a visible resident across the settlement. The only
        // forced relocation left here is a bounded local unstuck nudge for a body already intersecting
        // collision (for example an old save or a block placed on the exact same tick).
        BlockPos safe = findLocalBuilderEscape(level, builder, occupied);
        if (safe == null) {
            builder.getNavigation().stop();
            return;
        }
        builder.getNavigation().stop();
        builder.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
    }

    private static BlockPos findLocalBuilderEscape(ServerLevel level, FrontierWorkerEntity builder,
                                                   Set<BlockPos> occupied) {
        BlockPos origin = builder.blockPosition();
        int[] dyOrder = {0, 1, -1, 2, -2};
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : dyOrder) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (occupied.contains(candidate)) continue;
                        if (isWalkableApproachCell(level, candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }
'''
construction = replace_once(construction, old_recovery, new_recovery, "builder home teleport recovery")
CONSTRUCTION.write_text(construction, encoding="utf-8")

workers = WORKERS.read_text(encoding="utf-8")
workers = replace_once(
    workers,
    '''    // Deep quarry/mine routes can become physically valid but impractically long after excavation.\n    // Keep ordinary walking first, then recover only the cargo-return leg after twelve seconds.\n    private static final long DEEP_WORK_RETURN_TELEPORT_TICKS = 240L;\n''',
    '''    // Deep quarry/mine returns may need a fresh path after excavation, but visible residents must\n    // remain physical. After twelve seconds, clear stale path/blocked-target state and repath in place.\n    private static final long DEEP_WORK_RETURN_REPATH_TICKS = 240L;\n''',
    "deep return timeout constant",
)
workers = replace_once(
    workers,
    "            recoverBlockedWorker(level, worker, building.workCenter());\n",
    "            recoverBlockedWorker(level, worker);\n",
    "worker blocked recovery call",
)
old_worker_recovery = '''    private static void recoverBlockedWorker(ServerLevel level, FrontierWorkerEntity worker, BlockPos workplace) {
        BlockPos feet = worker.blockPosition();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.above())) return;
        boolean blocked = !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
        if (!blocked) return;
        BlockPos safe = safeWorkerSpawn(level, workplace);
        if (safe == null) return;
        worker.getNavigation().stop();
        worker.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        clearTransientWorkerState(worker);
    }
'''
new_worker_recovery = '''    private static void recoverBlockedWorker(ServerLevel level, FrontierWorkerEntity worker) {
        BlockPos feet = worker.blockPosition();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.above())) return;
        boolean blocked = !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
        if (!blocked) return;
        BlockPos safe = safeLocalWorkerEscape(level, worker);
        if (safe == null) {
            worker.getNavigation().stop();
            return;
        }
        worker.getNavigation().stop();
        worker.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        clearTransientWorkerState(worker);
    }

    private static BlockPos safeLocalWorkerEscape(ServerLevel level, FrontierWorkerEntity worker) {
        BlockPos origin = worker.blockPosition();
        int[] dyOrder = {0, 1, -1, 2, -2};
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : dyOrder) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (isWalkableApproach(level, candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }
'''
workers = replace_once(workers, old_worker_recovery, new_worker_recovery, "production worker long-distance recovery")
workers = replace_once(
    workers,
    "            if (rescueLongDeepWorkReturn(level, worker, building)) {\n",
    "            if (restartLongDeepWorkReturn(level, worker, building)) {\n",
    "deep return caller",
)
old_deep = '''    private static boolean rescueLongDeepWorkReturn(ServerLevel level, FrontierWorkerEntity worker,
                                                    BuildingRecord building) {
        BuildingType type = building.buildingType();
        java.util.UUID workerId = worker.getUUID();
        if (type != BuildingType.QUARRY && type != BuildingType.MINE) {
            CARGO_RETURN_STARTED_AT.remove(workerId);
            return false;
        }
        long now = level.getGameTime();
        long started = CARGO_RETURN_STARTED_AT.computeIfAbsent(workerId, ignored -> now);
        if (now - started < DEEP_WORK_RETURN_TELEPORT_TICKS) return false;
        BlockPos safe = safeWorkerSpawn(level, building.workCenter());
        if (safe == null) return false;
        worker.getNavigation().stop();
        worker.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        clearResourceTarget(worker);
        MOVEMENT_WATCHES.remove(workerId);
        BLOCKED_TARGETS.remove(workerId);
        CARGO_RETURN_STARTED_AT.remove(workerId);
        return true;
    }
'''
new_deep = '''    private static boolean restartLongDeepWorkReturn(ServerLevel level, FrontierWorkerEntity worker,
                                                     BuildingRecord building) {
        BuildingType type = building.buildingType();
        java.util.UUID workerId = worker.getUUID();
        if (type != BuildingType.QUARRY && type != BuildingType.MINE) {
            CARGO_RETURN_STARTED_AT.remove(workerId);
            return false;
        }
        long now = level.getGameTime();
        long started = CARGO_RETURN_STARTED_AT.computeIfAbsent(workerId, ignored -> now);
        if (now - started < DEEP_WORK_RETURN_REPATH_TICKS) return false;
        // Do not teleport the resident or its real carried ItemStack. Reset only transient routing
        // evidence so the next work tick computes a fresh physical return path from the same position.
        worker.getNavigation().stop();
        clearResourceTarget(worker);
        MOVEMENT_WATCHES.remove(workerId);
        BLOCKED_TARGETS.remove(workerId);
        CARGO_RETURN_STARTED_AT.put(workerId, now);
        return true;
    }
'''
workers = replace_once(workers, old_deep, new_deep, "deep work teleport fallback")
WORKERS.write_text(workers, encoding="utf-8")

source_test = SOURCE_TEST.read_text(encoding="utf-8")
source_test = replace_once(
    source_test,
    'require("mod_version=0.1.0-alpha.132" in gradle, "current verifier/version drift")',
    'require("mod_version=0.1.0-alpha.133" in gradle, "current verifier/version drift")',
    "source test version",
)
old_builder_test = '''require("builderStrandedOnArtificialElevation" in construction and "builderOnArtificialElevation" in construction
        and "nearestNaturalGroundBelow" in construction and "return artificialRise >= 3;" in construction,
        "disconnected elevated builder recovery missing")
'''
new_builder_test = '''builder_recovery = construction.split("private static void recoverBuilderFromBlockedCell", 1)[1].split(
        "private static BlockPos findSafeBuilderHome", 1)[0]
require("findLocalBuilderEscape" in builder_recovery and "findSafeBuilderHome" not in builder_recovery
        and "radius <= 3" in builder_recovery,
        "active construction recovery can again jump a resident back to settlement home")
require("boolean insideFootprint" in construction and "if (insideFootprint) return false;" in construction,
        "building workers can again stop inside the blueprint footprint and be built into the structure")
'''
source_test = replace_once(source_test, old_builder_test, new_builder_test, "builder recovery source test")
old_worker_test = '''require("DEEP_WORK_RETURN_TELEPORT_TICKS = 240L" in worker
        and "CARGO_RETURN_STARTED_AT" in worker and "rescueLongDeepWorkReturn" in worker,
        "deep quarry/mine cargo-return recovery missing")
'''
new_worker_test = '''require("DEEP_WORK_RETURN_REPATH_TICKS = 240L" in worker
        and "CARGO_RETURN_STARTED_AT" in worker and "restartLongDeepWorkReturn" in worker
        and "DEEP_WORK_RETURN_TELEPORT_TICKS" not in worker and "rescueLongDeepWorkReturn" not in worker,
        "deep quarry/mine cargo return can again teleport a visible resident")
require("safeLocalWorkerEscape" in worker and "safeWorkerSpawn(level, workplace)" not in worker,
        "blocked production-worker recovery can again jump across the settlement")
'''
source_test = replace_once(source_test, old_worker_test, new_worker_test, "worker teleport source test")
SOURCE_TEST.write_text(source_test, encoding="utf-8")

props = PROPS.read_text(encoding="utf-8")
props = replace_once(props, "mod_version=0.1.0-alpha.132", "mod_version=0.1.0-alpha.133", "mod version")
PROPS.write_text(props, encoding="utf-8")

readme = README.read_text(encoding="utf-8")
readme = replace_once(readme, "## Current version: 0.1.0-alpha.132", "## Current version: 0.1.0-alpha.133", "README current version")
anchor = "## Alpha.132 construction-route commitment\n"
alpha133 = '''## Alpha.133 resident movement: no settlement-home teleport fallback

Real play after Alpha.132 exposed a second movement bug: an active construction resident that intersected a newly placed block, or was judged stranded on artificial elevation, could be recovered with `setPos` all the way back to the settlement safe-home cell. That is why a resident could visibly vanish and reappear at the same house. Alpha.133 removes that long-range automatic recovery. Ordinary builders may no longer count the inside of the blueprint footprint as a finished approach, collision recovery is limited to a three-block local unstuck nudge, and elevated residents must recover through real navigation rather than a town-center jump. The same rule now applies to production residents: blocked-worker recovery is local only, while the old twelve-second quarry/mine cargo-return teleport has been replaced by an in-place path reset and physical repath. Newly created residents still spawn normally at a safe initial cell; this change targets already-existing visible residents.

'''
if alpha133.strip() not in readme:
    if anchor not in readme:
        raise SystemExit("README Alpha.132 anchor missing")
    readme = readme.replace(anchor, alpha133 + anchor, 1)
README.write_text(readme, encoding="utf-8")

print("Alpha.133 no-home-teleport patch applied")
