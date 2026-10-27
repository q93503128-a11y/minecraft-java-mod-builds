package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects actual extra item spawns from harvest procs without touching their RNG or drop logic. */
@Mixin(value = ResourceHarvestService.class, remap = false)
abstract class ResourceProcReadabilityMixin {
    private static final ThreadLocal<ArrayDeque<ItemSnapshot>> AUGMENTATION_SURVIVAL_CROP_SNAPSHOTS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ArrayDeque<ItemSnapshot>> AUGMENTATION_SURVIVAL_ORE_SNAPSHOTS =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "applyCropBonusDrops", at = @At("HEAD"))
    private static void augmentationSurvival$captureCropBonusBefore(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments,
            CallbackInfo ci
    ) {
        AUGMENTATION_SURVIVAL_CROP_SNAPSHOTS.get().push(ItemSnapshot.capture(level, pos));
    }

    @Inject(method = "applyCropBonusDrops", at = @At("RETURN"))
    private static void augmentationSurvival$presentCropBonusAfter(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments,
            CallbackInfo ci
    ) {
        int spawned = popAndCount(AUGMENTATION_SURVIVAL_CROP_SNAPSHOTS, level, pos);
        if (spawned > 0) {
            PassiveProcReadabilityService.presentResourceBonus(player, pos, false, spawned);
        }
    }

    @Inject(method = "applyOreBonus", at = @At("HEAD"))
    private static void augmentationSurvival$captureOreBonusBefore(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments,
            CallbackInfo ci
    ) {
        AUGMENTATION_SURVIVAL_ORE_SNAPSHOTS.get().push(ItemSnapshot.capture(level, pos));
    }

    @Inject(method = "applyOreBonus", at = @At("RETURN"))
    private static void augmentationSurvival$presentOreBonusAfter(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments,
            CallbackInfo ci
    ) {
        int spawned = popAndCount(AUGMENTATION_SURVIVAL_ORE_SNAPSHOTS, level, pos);
        if (spawned > 0) {
            PassiveProcReadabilityService.presentResourceBonus(player, pos, true, spawned);
        }
    }

    private static int popAndCount(
            ThreadLocal<ArrayDeque<ItemSnapshot>> holder,
            ServerLevel level,
            BlockPos pos
    ) {
        ArrayDeque<ItemSnapshot> stack = holder.get();
        ItemSnapshot before = stack.isEmpty() ? ItemSnapshot.capture(level, pos) : stack.pop();
        if (stack.isEmpty()) {
            holder.remove();
        }
        return before.countNew(level, pos);
    }

    private record ItemSnapshot(Set<Integer> entityIds) {
        private static ItemSnapshot capture(ServerLevel level, BlockPos pos) {
            Set<Integer> ids = new HashSet<>();
            for (ItemEntity item : nearbyItems(level, pos)) {
                ids.add(item.getId());
            }
            return new ItemSnapshot(ids);
        }

        private int countNew(ServerLevel level, BlockPos pos) {
            int count = 0;
            for (ItemEntity item : nearbyItems(level, pos)) {
                if (!entityIds.contains(item.getId())) {
                    count++;
                }
            }
            return count;
        }
    }

    private static List<ItemEntity> nearbyItems(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(
                pos.getX() - 0.35D,
                pos.getY() - 0.35D,
                pos.getZ() - 0.35D,
                pos.getX() + 1.35D,
                pos.getY() + 1.35D,
                pos.getZ() + 1.35D
        );
        return level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive);
    }
}
