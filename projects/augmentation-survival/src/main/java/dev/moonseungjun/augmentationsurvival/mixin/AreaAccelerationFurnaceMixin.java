package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replays only the local furnace tick under Area Acceleration; world/player/entity time is untouched. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AreaAccelerationFurnaceMixin {
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL_REENTRY =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void augmentationSurvival$accelerateFurnace(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity blockEntity,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(AUGMENTATION_SURVIVAL_REENTRY.get())) {
            return;
        }
        int extra = AreaAccelerationService.extraTicksAt(level, pos);
        if (extra <= 0) {
            return;
        }
        AUGMENTATION_SURVIVAL_REENTRY.set(Boolean.TRUE);
        try {
            for (int index = 0; index < extra; index++) {
                AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), blockEntity);
            }
        } finally {
            AUGMENTATION_SURVIVAL_REENTRY.remove();
        }
    }
}
