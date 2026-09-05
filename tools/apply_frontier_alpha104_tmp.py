from pathlib import Path

ROOT = Path('projects/frontier-settlement')
SETTLEMENT = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {text.count(old)}')
    return text.replace(old, new, 1)


def replace_method(text, marker, new_method, label):
    start = text.find(marker)
    if start < 0:
        raise SystemExit(f'{label}: marker not found: {marker}')
    brace = text.find('{', start)
    if brace < 0:
        raise SystemExit(f'{label}: opening brace missing')
    depth = 0
    end = None
    for i in range(brace, len(text)):
        c = text[i]
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise SystemExit(f'{label}: closing brace missing')
    return text[:start] + new_method.rstrip() + text[end:]


# Version / lock
gradle = read(ROOT / 'gradle.properties')
gradle = replace_once(gradle, 'mod_version=0.1.0-alpha.103', 'mod_version=0.1.0-alpha.104', 'version')
if '# Alpha.104' not in gradle:
    gradle += ('\n# Alpha.104 civil leveling: explicit selected-area bulldoze flattening, scheduler-aligned work cadence, '
               'and serialized multi-builder crews scaled by completed construction offices.\n')
write(ROOT / 'gradle.properties', gradle)

lock = read(ROOT / 'COMPANION_LOCK.json')
lock = replace_once(lock, '"frontier_settlement": "0.1.0-alpha.103"',
                    '"frontier_settlement": "0.1.0-alpha.104"', 'companion lock version')
write(ROOT / 'COMPANION_LOCK.json', lock)

# CivilWorkState: arbitrary demolition must not mint reusable earth.
p = SETTLEMENT / 'CivilWorkState.java'
s = read(p)
s = replace_method(s, '    public CivilWorkState afterCut() {', '''    public CivilWorkState afterCut() {
        return afterCut(true);
    }

    public CivilWorkState afterCut(boolean reusableEarth) {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_CUT,
                earthBank + (reusableEarth ? 1 : 0), completedSteps + 1,
                initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }''', 'civil afterCut')
if 'withoutRetaining()' not in s:
    marker = '    public CivilWorkState beginRetaining() {'
    idx = s.index(marker)
    s = s[:idx] + '''    /** Save-compatible migration for the retired automatic retaining phase. */
    public CivilWorkState withoutRetaining() {
        if (!active || initialRetainingBlocks <= 0) return this;
        int completedRetaining = completedRetainingBlocks();
        int nextPhase = phase == PHASE_RETAIN ? PHASE_FILL : phase;
        int nextCompleted = Math.max(0, completedSteps - completedRetaining);
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, nextPhase,
                earthBank, nextCompleted, initialCutBlocks, initialFillBlocks, 0);
    }

''' + s[idx:]
write(p, s)

# Civil leveling
p = SETTLEMENT / 'SettlementCivilWorkService.java'
s = read(p)
s = replace_once(s, 'import net.minecraft.world.level.levelgen.Heightmap;\n',
                 'import net.minecraft.world.level.levelgen.Heightmap;\nimport net.minecraft.world.level.pathfinder.Path;\n\nimport java.util.ArrayList;\nimport java.util.Comparator;\nimport java.util.List;\n',
                 'civil imports')
s = replace_once(s, '    public static final int MAX_CUT_DEPTH = 7;\n    public static final int MAX_FILL_DEPTH = 7;',
                 '    public static final int MAX_CUT_DEPTH = 32;\n    public static final int MAX_FILL_DEPTH = 16;',
                 'civil depth bounds')
s = replace_once(s, '    private static final int WORK_INTERVAL_TICKS = 8;',
                 '    // SettlementService already schedules civil work every five ticks. Matching that cadence\n'
                 '    // avoids the old 5-vs-8 LCM bug that reduced real work to one block every 40 ticks.\n'
                 '    private static final int WORK_INTERVAL_TICKS = 5;',
                 'civil cadence')
s = replace_once(s, '    private static final double WORK_REACHED_SQR = 4.0D;',
                 '    private static final double WORK_REACHED_SQR = 4.0D;\n'
                 '    private static final int MAX_CIVIL_APPROACH_PATH_TRIES = 64;',
                 'civil approach bound')
s = replace_once(s,
'''    public record Check(boolean valid, int minX, int maxX, int minZ, int maxZ, int gradeY,
                        int cutBlocks, int fillBlocks, int retainingBlocks, String message) {
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
        public int importedFillBlocks() {
            return SettlementCivilFillSupplyService.importedFillRequired(cutBlocks, fillBlocks);
        }
    }''',
'''    public record Check(boolean valid, int minX, int maxX, int minZ, int maxZ, int gradeY,
                        int cutBlocks, int reusableCutBlocks, int fillBlocks, int retainingBlocks, String message) {
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
        public int importedFillBlocks() {
            return SettlementCivilFillSupplyService.importedFillRequired(reusableCutBlocks, fillBlocks);
        }
    }
    private record ColumnPlan(boolean valid, int cutBlocks, int reusableCutBlocks, int fillBlocks, String message) {
        static ColumnPlan invalid(String message) { return new ColumnPlan(false, 0, 0, 0, message); }
    }''', 'civil Check record')

s = replace_method(s, '    public static Check check(ServerPlayer player, BlockPos first, BlockPos second) {', r'''    public static Check check(ServerPlayer player, BlockPos first, BlockPos second) {
        MinecraftServer server = player.level().getServer();
        SettlementData settlement = SettlementData.get(server);
        if (!settlement.founded()) return invalid("공동 마을이 없습니다.");
        if (player.level() != server.overworld()) return invalid("선택영역 평탄화는 오버월드에서만 가능합니다.");
        String locked = lockedReason(settlement);
        if (locked != null) return invalid(locked);
        if (SettlementProjectAuthority.anyActive(server, settlement)) {
            return invalid("현재 공동 공사가 끝난 뒤 평탄화를 계획해 주세요.");
        }
        if (first == null || second == null) return invalid("두 모서리를 선택해 주세요.");

        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        long widthLong = (long) maxX - minX + 1L;
        long depthLong = (long) maxZ - minZ + 1L;
        if (widthLong <= 0L || depthLong <= 0L
                || widthLong > MAX_WIDTH || depthLong > MAX_DEPTH || widthLong * depthLong > MAX_AREA) {
            return invalid("평탄화 1회 범위는 최대 " + MAX_WIDTH + "×" + MAX_DEPTH + "입니다.");
        }
        int width = (int) widthLong;
        int depth = (int) depthLong;

        if (!withinHorizontalDistance(player.blockPosition(), first, MAX_PLAYER_DISTANCE)
                || !withinHorizontalDistance(player.blockPosition(), second, MAX_PLAYER_DISTANCE)) {
            return invalid("평탄화 영역은 플레이어 " + MAX_PLAYER_DISTANCE + "블록 안에서 지정해 주세요.");
        }
        int centerX = (int) ((long) minX + ((long) maxX - minX) / 2L);
        int centerZ = (int) ((long) minZ + ((long) maxZ - minZ) / 2L);
        int gradeY = first.getY();
        BlockPos center = new BlockPos(centerX, gradeY, centerZ);
        if (!withinHorizontalDistance(settlement.centerPos(), center, MAX_SETTLEMENT_RADIUS)) {
            return invalid("본진 평탄화 영역은 마을 중심 " + MAX_SETTLEMENT_RADIUS + "블록 안에서 지정해 주세요.");
        }
        if (overlapsInfrastructure(settlement, minX - 1, maxX + 1, minZ - 1, maxZ + 1)) {
            return invalid("선택영역이 기존 건물·도로·전초기지·공동 창고와 겹칩니다. 마을 시설은 불도저 평탄화에서 보호됩니다.");
        }

        ServerLevel level = server.overworld();
        int cut = 0;
        int reusableCut = 0;
        int fill = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!level.hasChunkAt(new BlockPos(x, gradeY, z))) {
                    return invalid("영역 전체가 로드된 상태에서 평탄화를 시작해 주세요.");
                }
                ColumnPlan plan = analyzeColumn(level, x, z, gradeY);
                if (!plan.valid()) return invalid(plan.message());
                cut += plan.cutBlocks();
                reusableCut += plan.reusableCutBlocks();
                fill += plan.fillBlocks();
            }
        }
        if (cut == 0 && fill == 0) return invalid("이미 선택 높이로 완전히 평탄한 영역입니다.");

        int importedFill = SettlementCivilFillSupplyService.importedFillRequired(reusableCut, fill);
        if (importedFill > 0) {
            int available = SettlementCivilFillSupplyService.availableFill(level, settlement);
            if (available < 0) return invalid("공동 창고가 모두 로드된 상태에서 외부 성토 자재를 검사해 주세요.");
            if (available < importedFill) {
                return invalid("외부 성토 흙 부족 · 필요 " + importedFill + " / 공동 창고 " + available
                        + " · 흙/거친 흙 ItemStack을 실제로 넣어 주세요.");
            }
        }
        return new Check(true, minX, maxX, minZ, maxZ, gradeY, cut, reusableCut, fill, 0,
                "완전 평탄화 가능 · " + width + "×" + depth + " · 철거 " + cut + " · 성토 " + fill
                        + (importedFill > 0 ? " · 실제 창고 흙 " + importedFill : " · 현장 토사로 충당")
                        + " · 나무/잎/일반 블록도 기준면 위에서 제거");
    }''', 'civil check')

s = replace_method(s, '    public static StartResult start(ServerPlayer player, BlockPos first, BlockPos second) {', r'''    public static StartResult start(ServerPlayer player, BlockPos first, BlockPos second) {
        MinecraftServer server = player.level().getServer();
        SettlementData settlement = SettlementData.get(server);
        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);
        if (SettlementProjectAuthority.anyActive(server, settlement)) {
            return new StartResult(false, "현재 공동 공사가 끝난 뒤 평탄화를 시작해 주세요.");
        }
        Check check = check(player, first, second);
        if (!check.valid()) return new StartResult(false, check.message());

        data.begin(new CivilWorkState(true, check.minX(), check.maxX(), check.minZ(), check.maxZ(), check.gradeY(),
                CivilWorkState.PHASE_CUT, 0, 0, check.cutBlocks(), check.fillBlocks(), 0));
        List<FrontierWorkerEntity> builders = SettlementConstructionService.ensureProjectBuilders(server.overworld(), settlement);
        if (builders.isEmpty()) {
            data.clear();
            SettlementService.broadcast(server, settlement);
            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 평탄화 착공을 취소했습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자재는 차감되지 않았습니다.");
        }
        for (FrontierWorkerEntity builder : builders) builder.setCustomName(Component.literal("건설 주민 · 평탄화"));
        SettlementService.broadcast(server, settlement);
        return new StartResult(true, "완전 평탄화 착공 · 철거 " + check.cutBlocks() + " / 성토 " + check.fillBlocks()
                + (check.importedFillBlocks() > 0 ? " / 창고 흙 운반 " + check.importedFillBlocks() : "")
                + " · 건설 주민 " + builders.size() + "명이 중앙 공사 권위 아래 순차 분담합니다.");
    }''', 'civil start')

s = replace_method(s, '    public static boolean tick(MinecraftServer server, SettlementData settlement) {', r'''    public static boolean tick(MinecraftServer server, SettlementData settlement) {
        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);
        if (!data.project().active()) return false;
        ServerLevel level = server.overworld();
        List<FrontierWorkerEntity> builders = SettlementConstructionService.ensureProjectBuilders(level, settlement);
        if (builders.isEmpty()) return false;

        for (int i = 0; i < builders.size(); i++) {
            if (!data.project().active()) return true;
            FrontierWorkerEntity builder = builders.get(i);
            if (builder.isNoAi()) builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.setCustomName(Component.literal("건설 주민 · 평탄화"));
            tickBuilder(server, settlement, data, builder, i == 0);
        }
        return !data.project().active();
    }

    private static boolean tickBuilder(MinecraftServer server, SettlementData settlement,
                                       SettlementCivilWorkData data, FrontierWorkerEntity builder,
                                       boolean coordinator) {
        ServerLevel level = server.overworld();
        CivilWorkState project = data.project();
        if (!project.active()) return true;
        if (project.initialRetainingBlocks() > 0) {
            data.replace(project.withoutRetaining());
            project = data.project();
        }

        if (project.phase() == CivilWorkState.PHASE_RETURN) {
            if (!coordinator) return false;
            if (!SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder)) return false;
            return finish(server, settlement, data, builder);
        }
        if (!areaLoaded(level, project)) return false;

        if (project.phase() == CivilWorkState.PHASE_CUT) {
            if (!builder.getMainHandItem().isEmpty()) {
                SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
                return false;
            }
            BlockPos target = findCutTarget(level, project);
            if (target == null) {
                data.replace(project.beginFill());
                return false;
            }
            if (!safeDemolitionTarget(level, target)) return false;
            if (!moveBuilderToCivilSite(level, builder, project)) return false;
            if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;
            BlockState removed = level.getBlockState(target);
            if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE)) return false;
            builder.swing(InteractionHand.MAIN_HAND);
            data.replace(project.afterCut(isReusableCut(removed)));
            return false;
        }

        if (project.phase() == CivilWorkState.PHASE_RETAIN) {
            data.replace(project.withoutRetaining().beginFill());
            return false;
        }

        BlockPos target = findFillTarget(level, project);
        if (target == null) {
            if (!coordinator) return false;
            if (!builder.getMainHandItem().isEmpty()) {
                data.replace(project.beginReturn());
                return false;
            }
            return finish(server, settlement, data, builder);
        }

        boolean importedFill = project.earthBank() <= 0;
        if (importedFill && !coordinator) {
            moveBuilderToCivilSite(level, builder, project);
            return false;
        }
        if (!importedFill && !builder.getMainHandItem().isEmpty()) {
            SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
            return false;
        }
        if (importedFill && !SettlementCivilFillSupplyService.isFillStack(builder.getMainHandItem())
                && !builder.getMainHandItem().isEmpty()) {
            SettlementCivilFillSupplyService.returnCarriedToStorage(level, settlement, builder);
            return false;
        }
        if (importedFill && !SettlementCivilFillSupplyService.ensureCarriedFill(level, settlement, builder, project)) return false;

        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null || !current.getFluidState().isEmpty()
                || (!current.isAir() && !current.canBeReplaced())) return false;
        if (!moveBuilderToCivilSite(level, builder, project)) return false;
        if (server.getTickCount() % WORK_INTERVAL_TICKS != 0) return false;

        BlockState fillState = importedFill
                ? SettlementCivilFillSupplyService.carriedFillState(builder)
                : Blocks.COARSE_DIRT.defaultBlockState();
        if (!level.setBlock(target, fillState, BLOCK_UPDATE)) return false;
        if (importedFill) SettlementCivilFillSupplyService.consumeOne(builder);
        builder.swing(InteractionHand.MAIN_HAND);
        data.replace(project.afterFill());
        return false;
    }''', 'civil tick')

s = replace_method(s, '    private static BlockPos findCutTarget(ServerLevel level, CivilWorkState state) {', r'''    private static BlockPos findCutTarget(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                for (int y = topY; y > state.gradeY(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) return pos;
                }
            }
        }
        return null;
    }''', 'civil cut target')

s = replace_method(s, '    private static BlockPos findFillTarget(ServerLevel level, CivilWorkState state) {', r'''    private static BlockPos findFillTarget(ServerLevel level, CivilWorkState state) {
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                int supportY = findFillSupportY(level, x, z, state.gradeY());
                if (supportY < state.gradeY()) return new BlockPos(x, supportY + 1, z);
            }
        }
        return null;
    }

    static int remainingFillBlocks(ServerLevel level, CivilWorkState state) {
        if (state == null || !state.active()) return 0;
        int total = 0;
        for (int x = state.minX(); x <= state.maxX(); x++) {
            for (int z = state.minZ(); z <= state.maxZ(); z++) {
                if (!level.hasChunkAt(new BlockPos(x, state.gradeY(), z))) return -1;
                int supportY = findFillSupportY(level, x, z, state.gradeY());
                if (supportY < state.gradeY() - MAX_FILL_DEPTH) return -1;
                total += Math.max(0, state.gradeY() - supportY);
            }
        }
        return total;
    }

    private static int findFillSupportY(ServerLevel level, int x, int z, int gradeY) {
        for (int y = gradeY; y >= gradeY - MAX_FILL_DEPTH; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return gradeY - MAX_FILL_DEPTH - 1;
            if (!state.isAir() && !state.canBeReplaced()) return y;
        }
        return gradeY - MAX_FILL_DEPTH - 1;
    }''', 'civil fill target')

insert_at = s.index('    private static boolean moveBuilder(ServerLevel level, FrontierWorkerEntity builder, BlockPos target, double speed) {')
s = s[:insert_at] + r'''    private static boolean moveBuilderToCivilSite(ServerLevel level, FrontierWorkerEntity builder,
                                                  CivilWorkState project) {
        if (builderInsideCivilEnvelope(builder, project)) {
            builder.getNavigation().stop();
            return true;
        }
        List<BlockPos> approaches = civilApproachPositions(level, builder, project);
        int tried = 0;
        for (BlockPos approach : approaches) {
            if (++tried > MAX_CIVIL_APPROACH_PATH_TRIES) break;
            if (builder.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D) <= WORK_REACHED_SQR) {
                builder.getNavigation().stop();
                return true;
            }
            Path path = builder.getNavigation().createPath(approach, 0);
            if (path == null || !path.canReach() || path.getEndNode() == null) continue;
            BlockPos end = path.getEndNode().asBlockPos();
            if (Math.abs(end.getX() - approach.getX()) > 1 || Math.abs(end.getY() - approach.getY()) > 1
                    || Math.abs(end.getZ() - approach.getZ()) > 1) continue;
            if (builder.getNavigation().moveTo(path, 0.90D)) return false;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static boolean builderInsideCivilEnvelope(FrontierWorkerEntity builder, CivilWorkState project) {
        double margin = 1.5D;
        if (builder.getX() < project.minX() - margin || builder.getX() > project.maxX() + 1.0D + margin
                || builder.getZ() < project.minZ() - margin || builder.getZ() > project.maxZ() + 1.0D + margin) return false;
        return Math.abs(builder.getY() - project.gradeY()) <= MAX_CUT_DEPTH + 4.0D;
    }

    private static List<BlockPos> civilApproachPositions(ServerLevel level, FrontierWorkerEntity builder,
                                                          CivilWorkState project) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = project.minX() - 1; x <= project.maxX() + 1; x++) {
            for (int z = project.minZ() - 1; z <= project.maxZ() + 1; z++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (isCivilWalkable(level, feet)) result.add(feet);
            }
        }
        result.sort(Comparator.comparingDouble(pos -> builder.distanceToSqr(
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));
        return List.copyOf(result);
    }

    private static boolean isCivilWalkable(ServerLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos below = feet.below();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head) || !level.hasChunkAt(below)) return false;
        if (level.getBlockEntity(feet) != null || level.getBlockEntity(head) != null) return false;
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        BlockState belowState = level.getBlockState(below);
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()
                || !belowState.getFluidState().isEmpty()) return false;
        if ((!feetState.isAir() && !feetState.canBeReplaced())
                || (!headState.isAir() && !headState.canBeReplaced())) return false;
        return !belowState.isAir() && !belowState.canBeReplaced();
    }

''' + s[insert_at:]

s = replace_method(s, '    private static String validateColumn(ServerLevel level, int x, int z, int surfaceY, int gradeY) {', r'''    private static ColumnPlan analyzeColumn(ServerLevel level, int x, int z, int gradeY) {
        int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if ((long) topY - gradeY > MAX_CUT_DEPTH) {
            return ColumnPlan.invalid("기준면 위 철거 높이는 최대 " + MAX_CUT_DEPTH + "블록입니다.");
        }
        int cut = 0;
        int reusable = 0;
        for (int y = topY; y > gradeY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (level.getBlockEntity(pos) != null) {
                return ColumnPlan.invalid("상자·기계 등 블록 엔티티가 포함된 영역은 자동 삭제하지 않습니다. 해당 블록을 옮긴 뒤 다시 평탄화해 주세요.");
            }
            if (!state.getFluidState().isEmpty()) {
                return ColumnPlan.invalid("물·용암이 포함된 열은 반복 유입을 막기 위해 평탄화 전에 먼저 배수해 주세요.");
            }
            if (state.getDestroySpeed(level, pos) < 0.0F) {
                return ColumnPlan.invalid("파괴할 수 없는 블록이 포함된 영역은 평탄화할 수 없습니다.");
            }
            cut++;
            if (isReusableCut(state)) reusable++;
        }

        int fill = 0;
        boolean supportFound = false;
        for (int y = gradeY; y >= gradeY - MAX_FILL_DEPTH; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null) return ColumnPlan.invalid("성토 열에 블록 엔티티가 있어 자동 평탄화하지 않습니다.");
            if (!state.getFluidState().isEmpty()) return ColumnPlan.invalid("성토 열에 물·용암이 있어 먼저 배수해야 합니다.");
            if (state.isAir() || state.canBeReplaced()) {
                fill++;
                continue;
            }
            supportFound = true;
            break;
        }
        if (!supportFound) {
            return ColumnPlan.invalid("성토 지지면이 너무 낮습니다. 성토 깊이는 최대 " + MAX_FILL_DEPTH + "블록입니다.");
        }
        return new ColumnPlan(true, cut, reusable, fill, "");
    }

    private static boolean safeDemolitionTarget(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        return !state.isAir() && level.getBlockEntity(target) == null
                && state.getFluidState().isEmpty() && state.getDestroySpeed(level, target) >= 0.0F;
    }

    private static boolean isReusableCut(BlockState state) {
        return isNaturalGround(state);
    }''', 'civil column analysis')
if '    private static boolean safeNaturalTarget(ServerLevel level, BlockPos target) {' in s:
    s = replace_method(s, '    private static boolean safeNaturalTarget(ServerLevel level, BlockPos target) {', '', 'remove safeNaturalTarget')
s = replace_once(s, '        return new Check(false, 0, 0, 0, 0, 0, 0, 0, 0, message);',
                 '        return new Check(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, message);',
                 'civil invalid record')
s = s.replace('if (state.phase() == CivilWorkState.PHASE_CUT) return "선택영역 절토";',
              'if (state.phase() == CivilWorkState.PHASE_CUT) return "선택영역 완전 평탄화 · 철거";')
s = s.replace('return state.earthBank() > 0 ? "선택영역 성토" : "선택영역 성토 · 창고 흙 운반";',
              'return state.earthBank() > 0 ? "선택영역 완전 평탄화 · 성토" : "선택영역 완전 평탄화 · 창고 흙 운반";')
write(p, s)

# Imported fill recomputation
p = SETTLEMENT / 'SettlementCivilFillSupplyService.java'
s = read(p)
s = replace_method(s, '    public static int remainingImportedFill(ServerLevel level, CivilWorkState project) {', r'''    public static int remainingImportedFill(ServerLevel level, CivilWorkState project) {
        if (project == null || !project.active()) return 0;
        int fillRemaining = SettlementCivilWorkService.remainingFillBlocks(level, project);
        if (fillRemaining < 0) return -1;
        return Math.max(0, fillRemaining - project.earthBank());
    }''', 'remaining imported fill')
write(p, s)

# Multi-builder construction crew
p = SETTLEMENT / 'SettlementConstructionService.java'
s = read(p)
s = replace_once(s, '    private static final int BUILDER_ROUTE_MARGIN = 32;',
                 '    private static final int BUILDER_ROUTE_MARGIN = 32;\n    private static final int MAX_BUILDER_CREW = 3;',
                 'builder crew cap')

s = replace_method(s, '    public static boolean tick(MinecraftServer server, SettlementData data) {', r'''    public static boolean tick(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) return false;
        ServerLevel level = server.overworld();
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) {
            FrontierWorkerEntity strandedBuilder = findBuilder(level, data);
            if (strandedBuilder != null) strandedBuilder.setInvulnerable(false);
            data.clearConstruction();
            return true;
        }

        List<FrontierWorkerEntity> builders = ensureProjectBuilders(level, data);
        if (builders.isEmpty()) return false;
        for (int i = 0; i < builders.size(); i++) {
            if (!data.construction().active()) return true;
            tickConstructionBuilder(server, data, type, builders.get(i), i == 0);
        }
        return !data.construction().active();
    }

    private static boolean tickConstructionBuilder(MinecraftServer server, SettlementData data,
                                                   BuildingType type, FrontierWorkerEntity builder,
                                                   boolean coordinator) {
        ServerLevel level = server.overworld();
        ConstructionState construction = data.construction();
        if (!construction.active()) return true;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(false);

        if (construction.grading()) return tickGrading(server, data, type, builder, coordinator);

        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
        int buildStep = construction.buildStep();
        if (buildStep >= plan.size()) return coordinator && finishIfValid(server, data, type, plan, builder, supply);

        Container crate = ensureSupplyCrate(level, supply);
        if (crate == null) return false;
        if (coordinator) {
            retireLegacyConstructionScaffolds(level, data, type, builder, supply);
            if (!stageRemainingMaterials(server, data, type, plan.size(), builder, crate, supply)) return false;
        } else if (!builder.getMainHandItem().isEmpty()) {
            return false;
        }
        if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;

        construction = data.construction();
        buildStep = construction.buildStep();
        if (buildStep >= plan.size()) return coordinator && finishIfValid(server, data, type, plan, builder, supply);
        BuildingBlueprints.Placement placement = plan.get(buildStep);
        if (!moveBuilderToWorkPosition(level, construction, type, placement, builder, supply)) return false;

        BlockPos target = placement.pos();
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
        if (SettlementInventory.countWood(crate) < woodDelta || SettlementInventory.countStone(crate) < stoneDelta) return false;

        boolean placedNow = false;
        if (!current.is(placement.state().getBlock())) {
            if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            placedNow = true;
        }
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {
            if (placedNow) level.setBlock(target, current, NORMAL_BLOCK_UPDATE);
            return false;
        }
        if (placedNow) builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();
        if (data.construction().buildStep() >= plan.size()) {
            return coordinator && finishIfValid(server, data, type, plan, builder, supply);
        }
        return false;
    }''', 'construction tick')

s = replace_method(s, '    private static boolean tickGrading(MinecraftServer server, SettlementData data,', r'''    private static boolean tickGrading(MinecraftServer server, SettlementData data,
                                       BuildingType type, FrontierWorkerEntity builder,
                                       boolean coordinator) {
        ServerLevel level = server.overworld();
        ConstructionState construction = data.construction();
        List<GradeCell> plan = createGradePlan(level, construction, type);
        int gradeStep = construction.gradeStep();
        if (gradeStep >= plan.size()) {
            data.replaceConstructionStep(ConstructionState.BUILD_STEP_OFFSET);
            return false;
        }
        GradeCell cell = plan.get(gradeStep);
        if (!canGradeCell(level, construction, type, cell)) {
            builder.getNavigation().stop();
            return false;
        }

        Container terrainCrate = null;
        if (cell.retainingStone() > 0) {
            BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
            terrainCrate = ensureSupplyCrate(level, supply);
            if (terrainCrate == null) return false;
            if (coordinator) {
                if (!stageTerrainStone(server, data, builder, terrainCrate, supply, cell.retainingStone())) return false;
            } else if (SettlementInventory.countStone(terrainCrate) < cell.retainingStone()) {
                return false;
            }
        }
        if (server.getTickCount() % GRADE_INTERVAL_TICKS != 0) return false;

        BlockPos work = gradeWorkPosition(level, cell.floor());
        if (builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > GRADE_WORK_RANGE_SQR) {
            moveBuilderTowardGradeCell(level, builder, work);
            return false;
        }
        if (cell.retainingStone() > 0 && (terrainCrate == null
                || SettlementInventory.countStone(terrainCrate) < cell.retainingStone())) return false;

        List<BlockSnapshot> gradeMutation = applyGradeCellTransactional(level, construction, type, cell);
        if (gradeMutation == null) return false;
        if (cell.retainingStone() > 0 && !SettlementInventory.consume(terrainCrate, 0L, cell.retainingStone(), 0L)) {
            rollbackGradeMutation(level, gradeMutation);
            return false;
        }
        if (cell.retainingStone() > 0) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();
        if (data.construction().gradeStep() >= plan.size()) data.replaceConstructionStep(ConstructionState.BUILD_STEP_OFFSET);
        return false;
    }''', 'construction grading')

start = s.index('    public static FrontierWorkerEntity ensureProjectBuilder(ServerLevel level, SettlementData data) {')
end = s.index('    private static void recoverBuilderFromBlockedCell', start)
s = s[:start] + r'''    public static int desiredBuilderCount(SettlementData data) {
        return Math.min(MAX_BUILDER_CREW, 1 + Math.max(0, data.buildingCount(BuildingType.CONSTRUCTION_OFFICE)));
    }

    public static List<FrontierWorkerEntity> ensureProjectBuilders(ServerLevel level, SettlementData data) {
        reconcileBuilderDuplicates(level, data);
        List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));
        int desired = desiredBuilderCount(data);
        for (FrontierWorkerEntity builder : existing) {
            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            recoverBuilderFromBlockedCell(level, data, builder);
        }
        if (existing.size() >= desired || !builderAssignmentEvidenceLoaded(level, data)) return List.copyOf(existing);

        Set<BlockPos> occupied = new HashSet<>();
        for (FrontierWorkerEntity builder : existing) occupied.add(builder.blockPosition());
        while (existing.size() < desired) {
            BlockPos spawn = findSafeBuilderHome(level, data, occupied);
            if (spawn == null) break;
            FrontierWorkerEntity builder = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
            builder.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            builder.setYRot(0.0F);
            builder.setXRot(0.0F);
            builder.setCustomName(Component.literal(BUILDER_NAME));
            builder.setCustomNameVisible(true);
            builder.setPersistenceRequired();
            builder.setNoAi(false);
            builder.addTag(BUILDER_TAG);
            if (!level.addFreshEntity(builder)) break;
            existing.add(builder);
            occupied.add(spawn);
        }
        existing.sort(Comparator.comparing(builder -> builder.getUUID().toString()));
        return List.copyOf(existing);
    }

    public static FrontierWorkerEntity ensureProjectBuilder(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = ensureProjectBuilders(level, data);
        return builders.isEmpty() ? null : builders.getFirst();
    }

    public static FrontierWorkerEntity ensureBuilder(ServerLevel level, SettlementData data) {
        return ensureProjectBuilder(level, data);
    }

''' + s[end:]

s = replace_method(s, '    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data) {', r'''    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data) {
        return findSafeBuilderHome(level, data, Set.of());
    }

    private static BlockPos findSafeBuilderHome(ServerLevel level, SettlementData data, Set<BlockPos> occupied) {
        BlockPos center = data.centerPos();
        BlockPos preferred = safeSurfaceCell(level, center.getX() + 1, center.getZ() + 1);
        if (preferred != null && !occupied.contains(preferred)) return preferred;
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = safeSurfaceCell(level, center.getX() + dx, center.getZ() + dz);
                    if (candidate != null && !occupied.contains(candidate)) return candidate;
                }
            }
        }
        BlockPos fallback = safeSurfaceCell(level, center.getX(), center.getZ());
        return fallback != null && !occupied.contains(fallback) ? fallback : null;
    }''', 'safe builder homes')

s = replace_method(s, '    public static int reconcileBuilderDuplicates(ServerLevel level, SettlementData data) {', r'''    public static int reconcileBuilderDuplicates(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = findBuilders(level, data);
        if (builders.isEmpty()) return 0;
        int allowed = desiredBuilderCount(data);
        int keep = Math.min(allowed, builders.size());
        for (int i = 0; i < keep; i++) {
            FrontierWorkerEntity builder = builders.get(i);
            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);
            builder.setNoAi(false);
            builder.setInvulnerable(false);
        }
        int removed = 0;
        for (int i = keep; i < builders.size(); i++) {
            if (removeDuplicateBuilderPreservingCargo(level, builders.get(i))) removed++;
        }
        return removed;
    }''', 'builder duplicate reconciliation')

s = replace_method(s, '    public static int normalizeLoadedBuilders(ServerLevel level, SettlementData data) {', r'''    public static int normalizeLoadedBuilders(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> builders = new ArrayList<>(findBuilders(level, data));
        BlockPos center = data.centerPos();
        AABB maintenance = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        for (FrontierWorkerEntity candidate : level.getEntitiesOfClass(FrontierWorkerEntity.class, maintenance, worker ->
                worker.entityTags().contains(BUILDER_TAG)
                        || (worker.getCustomName() != null && BUILDER_NAME.equals(worker.getCustomName().getString())))) {
            if (!builders.contains(candidate)) builders.add(candidate);
        }
        builders.sort(Comparator.comparing(worker -> worker.getUUID().toString()));
        if (builders.isEmpty()) return 0;

        int allowed = desiredBuilderCount(data);
        int keep = Math.min(allowed, builders.size());
        for (int i = 0; i < keep; i++) {
            FrontierWorkerEntity builder = builders.get(i);
            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.getNavigation().stop();
        }
        int removed = 0;
        for (int i = keep; i < builders.size(); i++) {
            if (removeDuplicateBuilderPreservingCargo(level, builders.get(i))) removed++;
        }
        if (!SettlementProjectAuthority.anyActive(level.getServer(), data)) {
            for (int i = 0; i < keep; i++) returnBuilderHome(level, data, builders.get(i));
        }
        return removed;
    }

    public static void settleIdleBuilders(MinecraftServer server, SettlementData data) {
        if (SettlementProjectAuthority.anyActive(server, data)) return;
        ServerLevel level = server.overworld();
        for (FrontierWorkerEntity builder : findBuilders(level, data)) {
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.setCustomName(Component.literal(BUILDER_NAME));
            if (!builder.getMainHandItem().isEmpty()) {
                returnCarriedToTownStorage(server, data, builder);
                continue;
            }
            returnBuilderHome(level, data, builder);
        }
    }''', 'normalize builders')
write(p, s)

# Idle crew homing
p = SETTLEMENT / 'SettlementService.java'
s = read(p)
s = replace_once(s, '        SettlementConstructionOfficeService.tick(server, data);',
                 '        if (tick % 10 == 0) SettlementConstructionService.settleIdleBuilders(server, data);\n        SettlementConstructionOfficeService.tick(server, data);',
                 'idle builder settle')
write(p, s)

# Construction office docs
p = SETTLEMENT / 'SettlementConstructionOfficeService.java'
s = read(p)
s = s.replace(' * nearby office material bays stocked from real loaded settlement storage, while the existing\n * construction builder remains the only authority that grades and places project blocks.\n',
              ' * nearby office material bays stocked from real loaded settlement storage. Completed offices also\n * authorize additional physical builders (up to the centralized crew cap), while SettlementConstructionService\n * remains the sole serialized mutation/resource scheduler so multiple bodies cannot double-spend a project step.\n')
write(p, s)

# Current-source verifier
p = ROOT / 'tools/test_current_source.py'
t = read(p)
t = replace_once(t, 'mod_version=0.1.0-alpha.103', 'mod_version=0.1.0-alpha.104', 'verifier version')
t = t.replace('require("ensureProjectBuilder(" in construction, "shared project-builder authority missing")',
'''require("ensureProjectBuilder(" in construction, "shared project-builder authority missing")
require("MAX_BUILDER_CREW = 3" in construction, "bounded construction crew cap missing")
require("ensureProjectBuilders" in construction and "desiredBuilderCount" in construction, "multi-builder crew authority missing")
require("i == 0" in construction and "tickConstructionBuilder" in construction, "builder crew is not serialized through one scheduler")''')
t = t.replace('require("ensureProjectBuilder" in civil and "data.clear();" in civil, "civil start is not transactional")',
'''require("ensureProjectBuilders" in civil and "data.clear();" in civil, "civil start/crew acquisition is not transactional")
require("WORK_INTERVAL_TICKS = 5" in civil, "civil scheduler cadence drifted from five-tick service cadence")
require("Heightmap.Types.WORLD_SURFACE" in civil, "full flatten does not clear leaves/ordinary surface blocks")
require("safeDemolitionTarget" in civil and "isReusableCut" in civil, "bulldoze demolition/reusable-earth split missing")
require("MAX_CUT_DEPTH = 32" in civil and "MAX_FILL_DEPTH = 16" in civil, "bounded full-flatten vertical envelope missing")
require("builderInsideCivilEnvelope" in civil, "civil work still depends on exact per-cell pathing")
require("SettlementCivilRetainingService.checkPlan" not in civil, "retired automatic retaining gate still blocks full flatten planning")''')
t = t.replace('print("CURRENT SOURCE CHECK PASS: alpha103 production reach plus alpha102 authority invariants")',
              'print("CURRENT SOURCE CHECK PASS: alpha104 full flatten + serialized builder crew + prior authority invariants")')
write(p, t)

print('Alpha104 patch applied')
