from pathlib import Path

ROOT = Path('projects/frontier-settlement')
SERVICE = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'
STORAGE = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementStorageService.java'


def once(text: str, old: str, new: str, label: str) -> str:
    n = text.count(old)
    if n != 1:
        raise SystemExit(f'{label}: expected 1 anchor, got {n}')
    return text.replace(old, new, 1)


s = SERVICE.read_text(encoding='utf-8')

s = once(s,
'''import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
''',
'''import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
''', 'Path import')

s = once(s,
'''import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
''',
'''import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
''', 'collection imports')

s = once(s,
'''    private static boolean moveBuilderTowardGradeCell(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (builder.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.05D)) return true;
        int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for (int[] offset : offsets) {
            int x = target.getX() + offset[0];
            int z = target.getZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (!level.hasChunkAt(candidate) || !level.hasChunkAt(candidate.above()) || !level.hasChunkAt(candidate.below())) continue;
            BlockState feet = level.getBlockState(candidate);
            BlockState head = level.getBlockState(candidate.above());
            BlockState below = level.getBlockState(candidate.below());
            if ((!feet.isAir() && !feet.canBeReplaced()) || (!head.isAir() && !head.canBeReplaced())) continue;
            if (below.isAir() || !below.getFluidState().isEmpty()) continue;
            if (builder.getNavigation().moveTo(x + 0.5D, y, z + 0.5D, 1.05D)) return true;
        }
        builder.getNavigation().stop();
        return false;
    }
''',
'''    private static boolean moveBuilderTowardGradeCell(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (moveToReachable(builder, target, 1.05D)) return true;
        int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for (int[] offset : offsets) {
            int x = target.getX() + offset[0];
            int z = target.getZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isWalkableApproachCell(level, candidate)) continue;
            if (moveToReachable(builder, candidate, 1.05D)) return true;
        }
        builder.getNavigation().stop();
        return false;
    }
''', 'grade reachable path')

# Make ServerLevel available before both supply/source movement branches.
s = once(s,
'''        long missing = Math.max(0L, requiredStone - SettlementInventory.countStone(crate));
        if (missing <= 0L) return true;
        ItemStack carried = builder.getMainHandItem();
''',
'''        long missing = Math.max(0L, requiredStone - SettlementInventory.countStone(crate));
        if (missing <= 0L) return true;
        ServerLevel level = server.overworld();
        ItemStack carried = builder.getMainHandItem();
''', 'terrain level early')
s = once(s, '        ServerLevel level = server.overworld();\n        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone);',
'''        BlockPos source = findReachableExtractionTarget(level, data, builder, SettlementInventory::isStone);''', 'terrain reachable source')
s = s.replace('''                builder.getNavigation().moveTo(supply.getX() + 0.5D, supply.getY(), supply.getZ() + 0.5D, 1.10D);
                return false;
''', '''                moveTowardInteraction(level, builder, supply, 1.10D);
                return false;
''', 1)
s = once(s,
'''            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 1.10D);
            return false;
''',
'''            moveTowardInteraction(level, builder, source, 1.10D);
            return false;
''', 'terrain source movement')

# Build-material staging: level is needed even while carrying toward the site crate.
s = once(s,
'''        int step = data.construction().buildStep();
        long spentWood = costAtStep(type.woodCost(), step, totalSteps);
''',
'''        int step = data.construction().buildStep();
        ServerLevel level = server.overworld();
        long spentWood = costAtStep(type.woodCost(), step, totalSteps);
''', 'build level early')
# The second supply move occurrence belongs to stageRemainingMaterials.
s = s.replace('''                builder.getNavigation().moveTo(supply.getX() + 0.5D, supply.getY(), supply.getZ() + 0.5D, 1.10D);
                return false;
''', '''                moveTowardInteraction(level, builder, supply, 1.10D);
                return false;
''', 1)
s = once(s,
'''        ServerLevel level = server.overworld();
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, wanted);
''',
'''        BlockPos source = findReachableExtractionTarget(level, data, builder, wanted);
''', 'build reachable source')
s = once(s,
'''            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 1.10D);
            return false;
''',
'''            moveTowardInteraction(level, builder, source, 1.10D);
            return false;
''', 'build source movement')

s = once(s,
'''        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {
            builder.getNavigation().stop();
            return false;
        }
''',
'''        BlockPos target = findReachableDepositTarget(level, data, builder, carried);
        if (target == null || !level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {
            builder.getNavigation().stop();
            return false;
        }
''', 'reachable deposit target')
s = once(s,
'''            builder.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.10D);
            return false;
''',
'''            moveTowardInteraction(level, builder, target, 1.10D);
            return false;
''', 'deposit movement')

# Completion leftover -> site crate interaction.
s = once(s,
'''    private static boolean returnCrateExtrasPhysically(MinecraftServer server, SettlementData data,
                                                       FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        if (!builder.getMainHandItem().isEmpty()) return returnCarriedToTownStorage(server, data, builder);
''',
'''    private static boolean returnCrateExtrasPhysically(MinecraftServer server, SettlementData data,
                                                       FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        if (!builder.getMainHandItem().isEmpty()) return returnCarriedToTownStorage(server, data, builder);
        ServerLevel level = server.overworld();
''', 'crate extras level')
s = once(s,
'''            builder.getNavigation().moveTo(supply.getX() + 0.5D, supply.getY(), supply.getZ() + 0.5D, 1.10D);
            return false;
''',
'''            moveTowardInteraction(level, builder, supply, 1.10D);
            return false;
''', 'crate extras movement')

# High-work candidate must be exactly reachable; a partial path must not suppress the next candidate.
s = once(s,
'''            // For a high scaffold, keep walking to the actual work point until the target itself is in range.
            if (builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 1.05D)) return false;
''',
'''            // A partial path is not authority: try the next scaffold if this exact work point cannot be reached.
            if (moveToReachable(builder, work, 1.05D)) return false;
''', 'high work reachable path')

# Scaffold reservation now requires a physical walk-in cell at its first tread.
s = once(s,
'''    private static boolean canClaimFreshTower(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos())) return false;
            BlockState current = level.getBlockState(piece.pos());
            if (level.getBlockEntity(piece.pos()) != null || !current.getFluidState().isEmpty()) return false;
            if (!current.isAir() && !current.canBeReplaced()) return false;
        }
        return true;
    }
''',
'''    private static boolean canClaimFreshTower(ServerLevel level, ScaffoldTower tower) {
        for (ScaffoldPiece piece : tower.pieces()) {
            if (!level.hasChunkAt(piece.pos())) return false;
            BlockState current = level.getBlockState(piece.pos());
            if (level.getBlockEntity(piece.pos()) != null || !current.getFluidState().isEmpty()) return false;
            if (!current.isAir() && !current.canBeReplaced()) return false;
        }
        return hasWalkableScaffoldEntry(level, tower);
    }

    private static boolean hasWalkableScaffoldEntry(ServerLevel level, ScaffoldTower tower) {
        if (tower.steps().isEmpty()) return false;
        BlockPos firstTread = tower.steps().getFirst();
        int[][] offsets = { {0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1} };
        for (int[] offset : offsets) {
            if (isWalkableApproachCell(level, firstTread.offset(offset[0], 0, offset[1]))) return true;
        }
        return false;
    }
''', 'walkable scaffold entry')

# Home return is non-authoritative, but do not repeatedly accept a partial path.
s = once(s,
'''        builder.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.10D);
        return false;
''',
'''        if (!moveToReachable(builder, home, 1.10D)) builder.getNavigation().stop();
        return false;
''', 'home reachable path')

# Navigation helpers live beside construction geometry helpers.
anchor = '''    private static double targetDistanceSqr(BlockPos work, BlockPos target) {
'''
helpers = '''    private static Path createReachablePath(FrontierWorkerEntity builder, BlockPos target) {
        Path path = builder.getNavigation().createPath(target, 0);
        return path != null && path.canReach() ? path : null;
    }

    private static boolean moveToReachable(FrontierWorkerEntity builder, BlockPos target, double speed) {
        Path path = createReachablePath(builder, target);
        return path != null && builder.getNavigation().moveTo(path, speed);
    }

    private static boolean isWalkableApproachCell(ServerLevel level, BlockPos feet) {
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

    private static List<BlockPos> interactionApproachPositions(ServerLevel level, FrontierWorkerEntity builder,
                                                               BlockPos target) {
        int[][] offsets = { {0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1} };
        List<BlockPos> candidates = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int[] offset : offsets) {
                BlockPos candidate = target.offset(offset[0], dy, offset[1]);
                if (isWalkableApproachCell(level, candidate)) candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> builder.distanceToSqr(
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)));
        return List.copyOf(candidates);
    }

    private static boolean canReachInteraction(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= SUPPLY_INTERACTION_RANGE_SQR) return true;
        for (BlockPos candidate : interactionApproachPositions(level, builder, target)) {
            if (createReachablePath(builder, candidate) != null) return true;
        }
        return false;
    }

    private static boolean moveTowardInteraction(ServerLevel level, FrontierWorkerEntity builder,
                                                 BlockPos target, double speed) {
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                <= SUPPLY_INTERACTION_RANGE_SQR) return true;
        for (BlockPos candidate : interactionApproachPositions(level, builder, target)) {
            if (moveToReachable(builder, candidate, speed)) return true;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static BlockPos findReachableExtractionTarget(ServerLevel level, SettlementData data,
                                                          FrontierWorkerEntity builder, Predicate<ItemStack> predicate) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos source = SettlementStorageService.findExtractionTargetExcluding(level, data, predicate, excluded);
            if (source == null) return null;
            if (canReachInteraction(level, builder, source)) return source;
            excluded.add(source);
        }
    }

    private static BlockPos findReachableDepositTarget(ServerLevel level, SettlementData data,
                                                       FrontierWorkerEntity builder, ItemStack stack) {
        Set<BlockPos> excluded = new HashSet<>();
        while (true) {
            BlockPos target = SettlementStorageService.findDepositTargetExcluding(level, data, stack, excluded);
            if (target == null) return null;
            if (canReachInteraction(level, builder, target)) return target;
            excluded.add(target);
        }
    }

'''
s = once(s, anchor, helpers + anchor, 'navigation helper insertion')

# Diagnostics: explain unreachable site crate or inaccessible physical stock instead of silent waiting.
s = once(s,
'''            Container crate = level.getBlockState(supply).is(Blocks.BARREL)
                    && level.getBlockEntity(supply) instanceof Container existing ? existing : null;
            if (crate != null && builder.getMainHandItem().isEmpty()) {
''',
'''            Container crate = level.getBlockState(supply).is(Blocks.BARREL)
                    && level.getBlockEntity(supply) instanceof Container existing ? existing : null;
            if (!builder.getMainHandItem().isEmpty()
                    && builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR
                    && !canReachInteraction(level, builder, supply)) {
                return "현장 자재통 접근 불가 · 주변 통로 또는 발판을 확인하세요";
            }
            if (crate != null && builder.getMainHandItem().isEmpty()) {
''', 'supply reach diagnostic')

s = once(s,
'''                if (SettlementInventory.countWood(crate) < woodDelta
                        && SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood) == null) {
                    return "건설 목재 대기 · 공동 저장소에 목재를 보충하세요";
                }
                if (SettlementInventory.countStone(crate) < stoneDelta
                        && SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone) == null) {
                    return "건설 석재 대기 · 공동 저장소에 석재를 보충하세요";
                }
''',
'''                if (SettlementInventory.countWood(crate) < woodDelta) {
                    BlockPos woodSource = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood);
                    if (woodSource == null) return "건설 목재 대기 · 공동 저장소에 목재를 보충하세요";
                    if (findReachableExtractionTarget(level, data, builder, SettlementInventory::isWood) == null) {
                        return "건설 목재 접근 불가 · 자재가 든 저장소까지 통로를 확보하세요";
                    }
                }
                if (SettlementInventory.countStone(crate) < stoneDelta) {
                    BlockPos stoneSource = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone);
                    if (stoneSource == null) return "건설 석재 대기 · 공동 저장소에 석재를 보충하세요";
                    if (findReachableExtractionTarget(level, data, builder, SettlementInventory::isStone) == null) {
                        return "건설 석재 접근 불가 · 자재가 든 저장소까지 통로를 확보하세요";
                    }
                }
''', 'resource reach diagnostics')

# No direct coordinate moveTo may remain outside the one exact-Path helper.
if s.count('getNavigation().moveTo(') != 1:
    raise SystemExit(f'unexpected navigation.moveTo count after patch: {s.count("getNavigation().moveTo(")}')
if 'moveTo(path, speed)' not in s:
    raise SystemExit('exact Path move missing')
SERVICE.write_text(s, encoding='utf-8')


st = STORAGE.read_text(encoding='utf-8')
st = once(st,
'''    public static BlockPos findDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        for (BlockPos pos : depositPositions(level, data, stack)) {
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        return data.stockpilePos();
    }
''',
'''    public static BlockPos findDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        BlockPos target = findDepositTargetExcluding(level, data, stack, Set.of());
        return target == null ? data.stockpilePos() : target;
    }

    public static BlockPos findDepositTargetExcluding(ServerLevel level, SettlementData data,
                                                      ItemStack stack, Set<BlockPos> excluded) {
        for (BlockPos pos : depositPositions(level, data, stack)) {
            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        BlockPos stockpile = data.stockpilePos();
        if (!excluded.contains(stockpile) && hasRoomAt(level, stockpile, stack)) return stockpile;
        return null;
    }
''', 'deposit excluding')
STORAGE.write_text(st, encoding='utf-8')

print('construction navigation hardening patch applied')
