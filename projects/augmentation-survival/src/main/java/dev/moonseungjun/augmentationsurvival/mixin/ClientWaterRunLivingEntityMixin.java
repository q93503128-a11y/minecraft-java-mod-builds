package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Client prediction companion to the server water-walking boundary. */
@Mixin(LivingEntity.class)
abstract class ClientWaterRunLivingEntityMixin {
    @Unique private static final String AUGMENTATION_SURVIVAL$WATER_RUN_ID =
            AugmentationSurvival.MOD_ID + ":water_run";

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$predictWaterRun(boolean original, FluidState fluidState) {
        if (original) return true;
        if (!((Object) this instanceof LocalPlayer player) || !fluidState.is(FluidTags.WATER)) return false;
        return ClientAugmentProgressState.level(AUGMENTATION_SURVIVAL$WATER_RUN_ID) > 0
                && !player.isShiftKeyDown()
                && !player.isDeadOrDying()
                && !player.isPassenger()
                && player.fallDistance < 5.0F;
    }
}
