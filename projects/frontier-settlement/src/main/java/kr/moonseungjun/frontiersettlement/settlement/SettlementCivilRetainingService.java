package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Alpha.51 physical retaining-wall planner and exact cobblestone hauling support. */
public final class SettlementCivilRetainingService {
    public static final int MIN_RETAINING_HEIGHT = 3;
    public static final int MAX_RETAINING_HEIGHT = 7;
    public static final int HAUL_BATCH = 16;
    private static final double STORAGE_REACHED_SQR = 9.0D;

    private SettlementCivilRetainingService() {}

    public record Plan(boolean valid, List<BlockPos> positions, String message) {
        public static Plan invalid(String message) { return new Plan(false, List.of(), message); }
        public int requiredBlocks() { return positions.size(); }
        public BlockPos nextMissing(ServerLevel level) {
            for (BlockPos pos : positions) if (!level.getBlockState(pos).is(Blocks.COBBLESTONE)) return pos;
            return null;
        }
    }

    private record BaseCheck(boolean valid, int baseY, String message) {
        static BaseCheck invalid(String message) { return new BaseCheck(false, 0, message); }
    }

    public static Plan checkPlan(ServerLevel level, int minX, int maxX, int minZ, int maxZ, int gradeY) {
        return buildPlan(level, minX, maxX, minZ, maxZ, gradeY, false);
    }

    public static Plan plan(ServerLevel level, CivilWorkState project) {
        if (project == null || !project.active()) return new Plan(true, List.of(), "");
        return buildPlan(level, project.minX(), project.maxX(), project.minZ(), project.maxZ(), project.gradeY(), true);
    }

    private static Plan buildPlan(ServerLevel level, int minX, int maxX, int minZ, int maxZ,
                                  int gradeY, boolean allowPlacedCobblestone) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) continue;
                BlockPos outerTop = new BlockPos(x, gradeY, z);
                int interiorX = Math.max(minX, Math.min(maxX, x));
                int interiorZ = Math.max(minZ, Math.min(maxZ, z));
                BlockPos interiorTop = new BlockPos(interiorX, gradeY, interiorZ);
                if (!level.hasChunkAt(outerTop) || !level.hasChunkAt(interiorTop)) {
                    return Plan.invalid("테라스 외곽 1칸까지 모두 로드된 상태에서 토목을 시작해 주세요.");
                }

                int interiorSurface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, interiorX, interiorZ) - 1;
                if (interiorSurface >= gradeY) continue;

                BaseCheck base = findBase(level, x, z, gradeY, allowPlacedCobblestone);
                if (!base.valid()) return Plan.invalid(base.message());
                int height = gradeY - base.baseY();
                if (height < MIN_RETAINING_HEIGHT) continue;
                if (height > MAX_RETAINING_HEIGHT) {
                    return Plan.invalid("테라스 외곽 옹벽 높이는 최대 " + MAX_RETAINING_HEIGHT + "블록입니다. 더 큰 협곡 공사는 아직 지원하지 않습니다.");
                }
                for (int y = base.baseY() + 1; y <= gradeY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) {
                        return Plan.invalid("옹벽 예정 위치에 컨테이너나 유체가 있어 토목할 수 없습니다.");
                    }
                    if (state.is(Blocks.COBBLESTONE) && allowPlacedCobblestone) {
                        positions.add(pos);
                    } else if (state.isAir() || state.canBeReplaced()) {
                        positions.add(pos);
                    } else {
                        return Plan.invalid("옹벽 예정 위치에 플레이어 블록·구조물이 있어 토목할 수 없습니다.");
                    }
                }
            }
        }
        return new Plan(true, List.copyOf(positions), "");
    }

    private static BaseCheck findBase(ServerLevel level, int x, int z, int gradeY, boolean allowPlacedCobblestone) {
        for (int y = gradeY; y >= gradeY - MAX_RETAINING_HEIGHT; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) {
                return BaseCheck.invalid("테라스 외곽에 컨테이너나 유체가 있어 옹벽을 만들 수 없습니다.");
            }
            if (allowPlacedCobblestone && state.is(Blocks.COBBLESTONE)) continue;
            if (state.isAir() || state.canBeReplaced()) continue;
            if (SettlementCivilWorkService.isNaturalGround(state)) return new BaseCheck(true, y, "");
            return BaseCheck.invalid("테라스 외곽에 플레이어 블록·구조물·비자연 지형이 있어 옹벽을 만들 수 없습니다.");
        }
        return BaseCheck.invalid("테라스 외곽 지지 지면이 너무 낮습니다. 옹벽 높이는 최대 " + MAX_RETAINING_HEIGHT + "블록입니다.");
    }

    public static int availableRetaining(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return -1;
        int total = 0;
        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (isRetainingStack(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    public static int remainingRetaining(ServerLevel level, CivilWorkState project) {
        Plan plan = plan(level, project);
        if (!plan.valid()) return -1;
        int remaining = 0;
        for (BlockPos pos : plan.positions()) if (!level.getBlockState(pos).is(Blocks.COBBLESTONE)) remaining++;
        return remaining;
    }

    public static boolean ensureCarriedRetaining(ServerLevel level, SettlementData data, FrontierWorkerEntity builder,
                                                 CivilWorkState project) {
        ItemStack carried = builder.getMainHandItem();
        if (isRetainingStack(carried)) return true;
        if (!carried.isEmpty()) return false;
        int remaining = remainingRetaining(level, project);
        if (remaining <= 0) return false;
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data,
                SettlementCivilRetainingService::isRetainingStack);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.86D);
            return false;
        }
        ItemStack picked = SettlementStorageService.extract(level, source,
                SettlementCivilRetainingService::isRetainingStack, Math.min(HAUL_BATCH, remaining));
        if (picked.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, picked);
        return true;
    }

    public static void consumeOne(FrontierWorkerEntity builder) {
        ItemStack carried = builder.getMainHandItem();
        if (!isRetainingStack(carried)) return;
        carried.shrink(1);
        if (carried.isEmpty()) builder.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public static boolean isRetainingStack(ItemStack stack) {
        return stack.is(Items.COBBLESTONE);
    }
}
