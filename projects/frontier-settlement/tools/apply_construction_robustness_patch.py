from pathlib import Path

ROOT = Path('projects/frontier-settlement')
CONSTRUCTION = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'
BUILDING_TYPE = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/BuildingType.java'
COMMANDS = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/command/SettlementCommands.java'
CONTEXT = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementContextService.java'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one anchor, found {count}')
    return text.replace(old, new, 1)

# 1) Definite matrix failure: MINE reaches relative Y=10 but declared clearHeight was 9.
s = BUILDING_TYPE.read_text(encoding='utf-8')
s = replace_once(
    s,
    '    MINE("mine", "광산", 68, 44, 11, 11, 9, 0, "채석장 + 전초기지 필요"),',
    '    MINE("mine", "광산", 68, 44, 11, 11, 10, 0, "채석장 + 전초기지 필요"),',
    'mine clearHeight')
BUILDING_TYPE.write_text(s, encoding='utf-8')

s = CONSTRUCTION.read_text(encoding='utf-8')

# 2) New sites must actually have enough external scaffold space for every high placement.
s = replace_once(
    s,
'''        for (GradeCell cell : createGradePlan(level, gradingPreview, type)) {
            if (!canGradeCell(level, gradingPreview, type, cell)) {
                return invalidPlacement("건물 주변 1블록까지 부지 정리가 가능한 공간이 필요합니다. 물·보호된 블록·깊은 절벽·미로드 경계를 피해 다시 지정해 주세요.");
            }
        }
        String message = "배치 가능";
''',
'''        for (GradeCell cell : createGradePlan(level, gradingPreview, type)) {
            if (!canGradeCell(level, gradingPreview, type, cell)) {
                return invalidPlacement("건물 주변 1블록까지 부지 정리가 가능한 공간이 필요합니다. 물·보호된 블록·깊은 절벽·미로드 경계를 피해 다시 지정해 주세요.");
            }
        }
        if (!hasFreshScaffoldCoverage(level, type, site.origin(), rotation)) {
            return invalidPlacement("고층 시공용 작업 발판을 확보할 공간이 부족합니다. 건물 바깥 3~4블록의 나무·바위·기존 구조물을 비우거나 위치를 옮겨 주세요.");
        }
        String message = "배치 가능";
''',
    'placement scaffold coverage')

# 3) A soft/replaceable future target (snow, vegetation, etc.) must not become a permanent blocker.
s = replace_once(
    s,
'''        BlockPos target = placement.pos();
        if (!level.hasChunkAt(target)) return false;
        BlockState current = level.getBlockState(target);
        if (!current.isAir() && !current.is(placement.state().getBlock())) {
            builder.getNavigation().stop();
            return false;
        }

        long woodDelta = costAtStep(type.woodCost(), buildStep + 1, plan.size())
                - costAtStep(type.woodCost(), buildStep, plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), buildStep + 1, plan.size())
                - costAtStep(type.stoneCost(), buildStep, plan.size());
        if (SettlementInventory.countWood(crate) < woodDelta || SettlementInventory.countStone(crate) < stoneDelta) {
            return false;
        }

        boolean placedNow = false;
        if (current.isAir()) {
            if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            placedNow = true;
        }
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {
            if (placedNow) level.setBlock(target, current, NORMAL_BLOCK_UPDATE);
            return false;
        }
''',
'''        BlockPos target = placement.pos();
        if (!level.hasChunkAt(target)) return false;
        BlockState current = level.getBlockState(target);
        if (!current.is(placement.state().getBlock()) && !canReplaceConstructionTarget(level, target, current)) {
            builder.getNavigation().stop();
            return false;
        }

        long woodDelta = costAtStep(type.woodCost(), buildStep + 1, plan.size())
                - costAtStep(type.woodCost(), buildStep, plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), buildStep + 1, plan.size())
                - costAtStep(type.stoneCost(), buildStep, plan.size());
        if (SettlementInventory.countWood(crate) < woodDelta || SettlementInventory.countStone(crate) < stoneDelta) {
            return false;
        }

        boolean placedNow = false;
        if (!current.is(placement.state().getBlock())) {
            if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            placedNow = true;
        }
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {
            if (placedNow) level.setBlock(target, current, NORMAL_BLOCK_UPDATE);
            return false;
        }
''',
    'replaceable active target')

# 4) Completed-state normalization: recover soft drift, still fail closed on fluids/BEs/hard solids.
s = replace_once(
    s,
'''            if (!current.isAir() && !isRecoverableBlueprintDrift(current, placement.state())) {
                return new NormalizeConstructionResult(true, false, repaired,
                        "예상치 못한 고체 블록 보호: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                                + " · 직접 확인 후 다시 실행");
            }
            if (!level.setBlock(pos, placement.state(), NORMAL_BLOCK_UPDATE)) {
''',
'''            if (!isRecoverableBlueprintDrift(current, placement.state())
                    && !canReplaceConstructionTarget(level, pos, current)) {
                return new NormalizeConstructionResult(true, false, repaired,
                        "예상치 못한 고체/유체 블록 보호: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                                + " · 직접 확인 후 다시 실행");
            }
            if (!level.setBlock(pos, placement.state(), NORMAL_BLOCK_UPDATE)) {
''',
    'normalize replaceable target')

# 5) Finalization gets the same safe replaceable handling.
s = replace_once(
    s,
'''            if (!current.isAir()) {
                builder.getNavigation().stop();
                return false;
            }
            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;
            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
''',
'''            if (!canReplaceConstructionTarget(level, placement.pos(), current)) {
                builder.getNavigation().stop();
                return false;
            }
            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;
            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
''',
    'finish replaceable target')

# 6) A foreign Container at the site-crate coordinate must not silently become construction authority.
s = replace_once(
    s,
'''    private static Container ensureSupplyCrate(ServerLevel level, BlockPos supply) {
        if (!level.hasChunkAt(supply)) return null;
        if (level.getBlockEntity(supply) instanceof Container crate) return crate;
        BlockState current = level.getBlockState(supply);
        if (!current.isAir() && !current.canBeReplaced()) return null;
        if (!level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;
        return level.getBlockEntity(supply) instanceof Container crate ? crate : null;
    }
''',
'''    private static Container ensureSupplyCrate(ServerLevel level, BlockPos supply) {
        if (!level.hasChunkAt(supply)) return null;
        BlockState current = level.getBlockState(supply);
        if (current.is(Blocks.BARREL) && level.getBlockEntity(supply) instanceof Container crate) return crate;
        if (level.getBlockEntity(supply) != null || !current.getFluidState().isEmpty()) return null;
        if (!current.isAir() && !current.canBeReplaced()) return null;
        if (!level.setBlock(supply, Blocks.BARREL.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return null;
        return level.getBlockState(supply).is(Blocks.BARREL)
                && level.getBlockEntity(supply) instanceof Container crate ? crate : null;
    }
''',
    'site crate authority')

# 7) High-work navigation: try every reachable claimed tower in builder-distance order; ground stays a
# save-compatible final fallback. This removes the one-selected-tower path failure mode.
s = replace_once(
    s,
'''    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
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
        BlockPos target = placement.pos();
        int relativeY = target.getY() - construction.originY();
        BlockPos ground = new BlockPos(target.getX(), construction.originY(), target.getZ());
        if (relativeY <= 3) return ground;

        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        BlockPos bestWork = null;
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
        return bestWork == null ? ground : bestWork;
    }
''',
'''    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
                                                     BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        BlockPos target = placement.pos();
        int relativeY = target.getY() - construction.originY();
        if (relativeY > 3 && builder.distanceToSqr(
                target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) <= HIGH_WORK_RANGE_SQR) {
            return true;
        }

        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
            double workDistance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
            // Ground work retains the historical wide local envelope as the final compatibility fallback.
            if (work.getY() <= construction.originY() && workDistance <= WORK_POSITION_REACHED_SQR) return true;
            // For a high scaffold, keep walking to the actual work point until the target itself is in range.
            if (builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 1.05D)) return false;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static List<BlockPos> workPositionsFor(ServerLevel level, ConstructionState construction, BuildingType type,
                                                   BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        BlockPos target = placement.pos();
        int relativeY = target.getY() - construction.originY();
        BlockPos ground = new BlockPos(target.getX(), construction.originY(), target.getZ());
        if (relativeY <= 3) return List.of(ground);

        List<BlockPos> result = new ArrayList<>();
        List<ScaffoldTower> towers = scaffoldTowers(construction.origin(), type, construction.buildingRotation(), supply);
        for (int towerIndex = 0; towerIndex < towers.size(); towerIndex++) {
            if (!construction.ownsScaffold(towerIndex)) continue;
            ScaffoldTower tower = towers.get(towerIndex);
            if (!towerUsable(level, tower) || tower.steps().isEmpty()) continue;
            int index = Math.min(tower.steps().size() - 1, Math.max(0, relativeY - 3));
            BlockPos candidate = tower.steps().get(index).above();
            if (targetDistanceSqr(candidate, target) <= HIGH_WORK_RANGE_SQR) result.add(candidate);
        }
        result.sort(Comparator.comparingDouble(pos -> builder.distanceToSqr(
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));
        // If every scaffold route is temporarily unavailable, preserve the old ground fallback so an
        // existing save is never made stricter by this hotfix.
        result.add(ground);
        return List.copyOf(result);
    }
''',
    'alternate scaffold pathing')

# 8) Insert shared geometry/safe-replacement helpers before scaffold claiming code.
s = replace_once(
    s,
'''    private static void ensureConstructionScaffolds(ServerLevel level, SettlementData data,
                                                    BuildingType type, BlockPos supply) {
''',
'''    private static double targetDistanceSqr(BlockPos work, BlockPos target) {
        double dx = (double) work.getX() + 0.5D - ((double) target.getX() + 0.5D);
        double dy = (double) work.getY() - ((double) target.getY() + 0.5D);
        double dz = (double) work.getZ() + 0.5D - ((double) target.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean hasFreshScaffoldCoverage(ServerLevel level, BuildingType type,
                                                     BlockPos origin, BuildingRotation rotation) {
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, origin, rotation.id());
        BlockPos supply = supplyPosition(origin, type, rotation);
        List<ScaffoldTower> towers = scaffoldTowers(origin, type, rotation, supply);
        boolean[] available = new boolean[towers.size()];
        for (int i = 0; i < towers.size(); i++) available[i] = canClaimFreshTower(level, towers.get(i));

        for (BuildingBlueprints.Placement placement : plan) {
            int relativeY = placement.pos().getY() - origin.getY();
            if (relativeY <= 3) continue;
            boolean covered = false;
            for (int i = 0; i < towers.size(); i++) {
                if (!available[i]) continue;
                ScaffoldTower tower = towers.get(i);
                if (tower.steps().isEmpty()) continue;
                int index = Math.min(tower.steps().size() - 1, Math.max(0, relativeY - 3));
                BlockPos candidate = tower.steps().get(index).above();
                if (targetDistanceSqr(candidate, placement.pos()) <= HIGH_WORK_RANGE_SQR) {
                    covered = true;
                    break;
                }
            }
            if (!covered) return false;
        }
        return true;
    }

    private static boolean canReplaceConstructionTarget(ServerLevel level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) == null
                && state.getFluidState().isEmpty()
                && (state.isAir() || state.canBeReplaced());
    }

    private static void ensureConstructionScaffolds(ServerLevel level, SettlementData data,
                                                    BuildingType type, BlockPos supply) {
''',
    'construction geometry helpers')

# 9) Actionable, side-effect-free blocker diagnosis for /frontier status and context/Jade.
s = replace_once(
    s,
'''    /**
     * Explicit safe repair for /frontier normalize. Only a construction that has already consumed
''',
'''    public static String constructionIssue(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) return "";
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) return "알 수 없는 건물 공사 상태";
        ServerLevel level = server.overworld();
        FrontierWorkerEntity builder = findBuilder(level, data);
        if (builder == null) return "건설 주민 확인 대기 · 마을·창고·현장 주변 청크를 로드하세요";

        if (construction.grading()) {
            List<GradeCell> gradePlan = createGradePlan(level, construction, type);
            int gradeStep = construction.gradeStep();
            if (gradeStep < gradePlan.size() && !canGradeCell(level, construction, type, gradePlan.get(gradeStep))) {
                BlockPos pos = gradePlan.get(gradeStep).floor();
                return "부지 정리 막힘 · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + " 주변의 물·보호 블록·깊은 지형을 확인하세요";
            }
            return "";
        }

        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(
                type, construction.origin(), construction.rotation());
        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        if (!level.hasChunkAt(supply)) {
            return "현장 자재통 청크 미로드 · " + supply.getX() + ", " + supply.getY() + ", " + supply.getZ();
        }
        BlockState supplyState = level.getBlockState(supply);
        if (!(supplyState.is(Blocks.BARREL) && level.getBlockEntity(supply) instanceof Container)
                && (level.getBlockEntity(supply) != null || !supplyState.getFluidState().isEmpty()
                || (!supplyState.isAir() && !supplyState.canBeReplaced()))) {
            return "현장 자재통 위치 막힘 · " + supply.getX() + ", " + supply.getY() + ", " + supply.getZ();
        }

        int step = construction.buildStep();
        if (step < plan.size()) {
            BuildingBlueprints.Placement placement = plan.get(step);
            BlockPos pos = placement.pos();
            if (!level.hasChunkAt(pos)) {
                return "다음 시공 위치 청크 미로드 · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            }
            BlockState current = level.getBlockState(pos);
            if (!current.is(placement.state().getBlock()) && !canReplaceConstructionTarget(level, pos, current)) {
                return "다음 시공 위치 막힘 · " + current.getBlock().getName().getString() + " · "
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            }
            Container crate = level.getBlockState(supply).is(Blocks.BARREL)
                    && level.getBlockEntity(supply) instanceof Container existing ? existing : null;
            if (crate != null && builder.getMainHandItem().isEmpty()) {
                long woodDelta = costAtStep(type.woodCost(), step + 1, plan.size())
                        - costAtStep(type.woodCost(), step, plan.size());
                long stoneDelta = costAtStep(type.stoneCost(), step + 1, plan.size())
                        - costAtStep(type.stoneCost(), step, plan.size());
                if (SettlementInventory.countWood(crate) < woodDelta
                        && SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood) == null) {
                    return "건설 목재 대기 · 공동 저장소에 목재를 보충하세요";
                }
                if (SettlementInventory.countStone(crate) < stoneDelta
                        && SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone) == null) {
                    return "건설 석재 대기 · 공동 저장소에 석재를 보충하세요";
                }
            }
            return "";
        }

        for (BuildingBlueprints.Placement placement : plan) {
            BlockPos pos = placement.pos();
            if (!level.hasChunkAt(pos)) return "마감 위치 청크 미로드 · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            BlockState current = level.getBlockState(pos);
            if (current.is(placement.state().getBlock()) || isRecoverableBlueprintDrift(current, placement.state())
                    || canReplaceConstructionTarget(level, pos, current)) continue;
            return "마감 위치 막힘 · " + current.getBlock().getName().getString() + " · "
                    + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        return "";
    }

    /**
     * Explicit safe repair for /frontier normalize. Only a construction that has already consumed
''',
    'construction issue diagnostics')

CONSTRUCTION.write_text(s, encoding='utf-8')

# 10) /frontier status must not use the raw 1M/2M encoded step or ignore rotation.
s = COMMANDS.read_text(encoding='utf-8')
s = replace_once(
    s,
'''        ConstructionState c=data.construction(); if(c.active()){BuildingType t=BuildingType.fromId(c.type()); if(t!=null){int total=SettlementConstructionService.totalSteps(t,c.origin());player.sendSystemMessage(Component.literal("공사 중 | "+t.displayName()+" "+(total<=0?0:Math.min(100,c.step()*100/total))+"%"));}}
''',
'''        ConstructionState c=data.construction();
        if(c.active()){
            BuildingType t=BuildingType.fromId(c.type());
            if(t!=null){
                int buildTotal=SettlementConstructionService.totalSteps(t,c.origin(),c.rotation());
                int gradeTotal=SettlementConstructionService.gradingSteps(server.overworld(),c,t);
                int worked=c.grading()?Math.min(gradeTotal,Math.max(0,c.gradeStep())):gradeTotal+Math.max(0,c.buildStep());
                int total=gradeTotal+buildTotal;
                int progress=total<=0?0:Math.max(0,Math.min(100,worked*100/total));
                String issue=SettlementConstructionService.constructionIssue(server,data);
                player.sendSystemMessage(Component.literal("공사 중 | "+t.displayName()+" "+progress+"% | "+SettlementConstructionService.phaseLabel(c)
                        +(issue.isBlank()?"":" | "+issue)));
            }
        }
''',
    'status encoded progress')
COMMANDS.write_text(s, encoding='utf-8')

# 11) Surface an actionable construction blocker through the existing context/Jade channel without a schema change.
s = CONTEXT.read_text(encoding='utf-8')
s = replace_once(
    s,
'''                projectProgress = percent(worked, gradeTotal + buildTotal);
                projectLabel = type.displayName() + " 공사";
                targets.add(new SettlementContextTarget(
''',
'''                projectProgress = percent(worked, gradeTotal + buildTotal);
                projectLabel = type.displayName() + " 공사";
                String constructionDetail = construction.grading()
                        ? "부지 정리 중 · 건물 자재는 정리 완료 후 실물 운반"
                        : "자재 운반·시공 중";
                String constructionIssue = SettlementConstructionService.constructionIssue(server, data);
                if (!constructionIssue.isBlank()) constructionDetail += " · " + constructionIssue;
                targets.add(new SettlementContextTarget(
''',
    'context construction detail setup')
s = replace_once(
    s,
'''                        construction.originX() + width / 2, construction.originY() + 1, construction.originZ() + depth / 2,
                        type.displayName(), construction.grading() ? "부지 정리 중 · 건물 자재는 정리 완료 후 실물 운반" : "자재 운반·시공 중", projectProgress));
''',
'''                        construction.originX() + width / 2, construction.originY() + 1, construction.originZ() + depth / 2,
                        type.displayName(), constructionDetail, projectProgress));
''',
    'context construction detail use')
CONTEXT.write_text(s, encoding='utf-8')

print('Frontier construction robustness patch applied')
