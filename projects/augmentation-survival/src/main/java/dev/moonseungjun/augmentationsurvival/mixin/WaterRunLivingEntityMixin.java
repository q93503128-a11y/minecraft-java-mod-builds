package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Allows a sprinting augmented player to stand on water while the condition remains true. */
@Mixin(LivingEntity.class)
public abstract class WaterRunLivingEntityMixin {
    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$runOnWater(boolean original, FluidState fluidState) {
        if (original) {
            return true;
        }
        return (Object) this instanceof ServerPlayer player
                && fluidState.is(FluidTags.WATER)
                && UtilityAugmentService.canRunOnWater(player);
    }
}
