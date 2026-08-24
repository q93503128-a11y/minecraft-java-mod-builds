package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Alpha.50 physical imported-fill support for civil works.
 *
 * The project-local earthBank still represents only earth actually cut from the current site.
 * When a larger fill project needs more material than it cuts, the shared construction worker must
 * walk to loaded settlement storage, extract real dirt/coarse-dirt ItemStacks and carry them back.
 * No virtual soil balance, direct storage-to-world teleport, or free block minting is allowed.
 */
public final class SettlementCivilFillSupplyService {
    public static final int HAUL_BATCH = 16;
    private static final double STORAGE_REACHED_SQR = 9.0D;

    private SettlementCivilFillSupplyService() {}

    public static int importedFillRequired(int cutBlocks, int fillBlocks) {
        return Math.max(0, fillBlocks - cutBlocks);
    }

    public static int availableFill(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return -1;
        int total = 0;
        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (isFillStack(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Recomputes the still-missing imported volume from the current physical site instead of trusting
     * the initial plan. This keeps the final haul exact if an admin/mod changes already-planned cells.
     * Returns -1 while any selected-area chunk is unloaded.
     */
    public static int remainingImportedFill(ServerLevel level, CivilWorkState project) {
        if (project == null || !project.active()) return 0;
        int fillRemaining = 0;
        for (int x = project.minX(); x <= project.maxX(); x++) {
            for (int z = project.minZ(); z <= project.maxZ(); z++) {
                BlockPos column = new BlockPos(x, project.gradeY(), z);
                if (!level.hasChunkAt(column)) return -1;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (surfaceY < project.gradeY()) fillRemaining += project.gradeY() - surfaceY;
            }
        }
        return Math.max(0, fillRemaining - project.earthBank());
    }

    /**
     * Ensures the builder physically carries imported fill. Returns true only when an eligible
     * carried stack is already in hand and can be consumed at the work cell.
     */
    public static boolean ensureCarriedFill(ServerLevel level, SettlementData data, Villager builder,
                                            CivilWorkState project) {
        ItemStack carried = builder.getMainHandItem();
        if (isFillStack(carried)) return true;
        if (!carried.isEmpty()) return false;

        int remaining = remainingImportedFill(level, project);
        if (remaining <= 0) return false;
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data,
                SettlementCivilFillSupplyService::isFillStack);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D, 0.86D);
            return false;
        }

        ItemStack picked = SettlementStorageService.extract(level, source,
                SettlementCivilFillSupplyService::isFillStack, Math.min(HAUL_BATCH, remaining));
        if (picked.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, picked);
        return true;
    }

    /**
     * Physically returns any carried construction/civil cargo to one concrete loaded storage block.
     * The worker must reach that exact container before insertion, so cleanup cannot become an
     * inventory teleport. If storage is unloaded/full the project simply remains paused with the
     * real ItemStack still in the worker's hand.
     */
    public static boolean returnCarriedToStorage(ServerLevel level, SettlementData data, Villager builder) {
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty()) return true;
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) return false;
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_REACHED_SQR) {
            builder.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 0.86D);
            return false;
        }
        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);
        builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        return remaining.isEmpty();
    }

    public static BlockState carriedFillState(Villager builder) {
        ItemStack carried = builder.getMainHandItem();
        if (!isFillStack(carried)) return Blocks.COARSE_DIRT.defaultBlockState();
        Block block = Block.byItem(carried.getItem());
        return block.defaultBlockState();
    }

    public static void consumeOne(Villager builder) {
        ItemStack carried = builder.getMainHandItem();
        if (!isFillStack(carried)) return;
        carried.shrink(1);
        if (carried.isEmpty()) builder.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public static boolean isFillStack(ItemStack stack) {
        return stack.is(Items.DIRT) || stack.is(Items.COARSE_DIRT);
    }
}
