package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Server-authoritative water walking: stand, walk or run on water unless crouching or diving from 5+ blocks. */
@Mixin(LivingEntity.class)
public abstract class WaterRunLivingEntityMixin {
    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$walkOnWater(boolean original, FluidState fluidState) {
        if (original) return true;
        if (!((Object) this instanceof ServerPlayer player) || !fluidState.is(FluidTags.WATER)) return false;
        return UtilityAugmentService.canRunOnWater(player)
                && !player.isShiftKeyDown()
                && !player.isDeadOrDying()
                && !player.isPassenger()
                && player.fallDistance < 5.0F;
    }
}
