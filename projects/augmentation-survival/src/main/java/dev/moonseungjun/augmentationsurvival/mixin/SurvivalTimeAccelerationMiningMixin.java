package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds Time Acceleration to the same additive mining-speed axis as passive mining augments. */
@Mixin(value = SurvivalAugmentService.class, remap = false)
abstract class SurvivalTimeAccelerationMiningMixin {
    @Inject(method = "modifyBlockBreakSpeed", at = @At("RETURN"), cancellable = true)
    private static void augmentationSurvival$addTimeAccelerationMining(
            ServerPlayer player,
            BlockState state,
            float original,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!(original > 0.0F)
                || !Float.isFinite(original)
                || !AdvancedActiveAugmentService.isTimeAccelerationActive(player)) {
            return;
        }
        float returned = cir.getReturnValue();
        double bonus = AdvancedActiveTuning.current().timeAccelerationMiningBonus();
        double combined = returned + original * bonus;
        if (Double.isFinite(combined)) {
            cir.setReturnValue((float) Math.min(Float.MAX_VALUE, combined));
        }
    }
}
