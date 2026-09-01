from pathlib import Path

SOURCE = Path('projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java')
s = SOURCE.read_text(encoding='utf-8')

old = '''    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
                                                     BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        BlockPos work = workPositionFor(level, construction, type, placement, supply);
        double workDistance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (workDistance > WORK_POSITION_REACHED_SQR) {
            builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 1.05D);
            return false;
        }
        if (work.getY() <= construction.originY()) return true;
        return builder.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY() + 0.5D,
                placement.pos().getZ() + 0.5D) <= HIGH_WORK_RANGE_SQR;
    }

    private static BlockPos workPositionFor(ServerLevel level, ConstructionState construction, BuildingType type,
                                            BuildingBlueprints.Placement placement, BlockPos supply) {
'''
new = '''    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
                                                     BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        BlockPos work = workPositionFor(level, construction, type, placement, builder, supply);
        double workDistance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (workDistance > WORK_POSITION_REACHED_SQR) {
            builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 1.05D);
            return false;
        }
        if (work.getY() <= construction.originY()) return true;

        double targetDistance = builder.distanceToSqr(
                placement.pos().getX() + 0.5D, placement.pos().getY() + 0.5D,
                placement.pos().getZ() + 0.5D);
        if (targetDistance <= HIGH_WORK_RANGE_SQR) return true;

        // A broad work-position radius must not become a no-navigation dead zone when the
        // actual high target is still out of reach. Keep moving toward the chosen scaffold.
        builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 1.05D);
        return false;
    }

    private static BlockPos workPositionFor(ServerLevel level, ConstructionState construction, BuildingType type,
                                            BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
'''
if old not in s:
    if 'workPositionFor(level, construction, type, placement, builder, supply)' not in s:
        raise SystemExit('move/work-position anchor no longer matches expected Alpha.91 source')
else:
    s = s.replace(old, new, 1)

old2 = '''        BlockPos bestWork = null;
        double bestTargetDistance = Double.MAX_VALUE;
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            ScaffoldTower tower = towers.get(towerIndex);
            if (!towerUsable(level, tower) || tower.steps().isEmpty()) continue;
            int index = Math.min(tower.steps().size() - 1, Math.max(0, relativeY - 3));
            BlockPos candidate = tower.steps().get(index).above();
            double dx = (double) candidate.getX() + 0.5D - ((double) target.getX() + 0.5D);
            double dy = (double) candidate.getY() - ((double) target.getY() + 0.5D);
            double dz = (double) candidate.getZ() + 0.5D - ((double) target.getZ() + 0.5D);
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= HIGH_WORK_RANGE_SQR && distance < bestTargetDistance) {
                bestTargetDistance = distance;
                bestWork = candidate;
            }
        }
'''
new2 = '''        BlockPos bestWork = null;
        double bestBuilderDistance = Double.MAX_VALUE;
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            ScaffoldTower tower = towers.get(towerIndex);
            if (!towerUsable(level, tower) || tower.steps().isEmpty()) continue;
            int index = Math.min(tower.steps().size() - 1, Math.max(0, relativeY - 3));
            BlockPos candidate = tower.steps().get(index).above();
            double dx = (double) candidate.getX() + 0.5D - ((double) target.getX() + 0.5D);
            double dy = (double) candidate.getY() - ((double) target.getY() + 0.5D);
            double dz = (double) candidate.getZ() + 0.5D - ((double) target.getZ() + 0.5D);
            double targetDistance = dx * dx + dy * dy + dz * dz;
            if (targetDistance > HIGH_WORK_RANGE_SQR) continue;

            // If several claimed towers can reach this block, stay near the builder instead of
            // forcing a cross-building scaffold transfer for every alternating roof placement.
            double builderDistance = builder.distanceToSqr(
                    candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            if (builderDistance < bestBuilderDistance) {
                bestBuilderDistance = builderDistance;
                bestWork = candidate;
            }
        }
'''
if old2 not in s:
    if 'double bestBuilderDistance = Double.MAX_VALUE;' not in s:
        raise SystemExit('scaffold-selection anchor no longer matches expected Alpha.91 source')
else:
    s = s.replace(old2, new2, 1)

SOURCE.write_text(s, encoding='utf-8')

patched = SOURCE.read_text(encoding='utf-8')
assert 'workPositionFor(level, construction, type, placement, builder, supply)' in patched
assert 'double bestBuilderDistance = Double.MAX_VALUE;' in patched
assert 'if (targetDistance <= HIGH_WORK_RANGE_SQR) return true;' in patched
assert 'actual high target is still out of reach' in patched
print('Frontier construction 96% reach hotfix applied and source assertions passed')
