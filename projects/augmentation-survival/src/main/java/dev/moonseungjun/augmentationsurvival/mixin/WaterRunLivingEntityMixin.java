package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server-authoritative Water Run: sprint intent plus real horizontal travel is enough to stand on water. */
@Mixin(LivingEntity.class)
public abstract class WaterRunLivingEntityMixin {
    @Unique private static final double AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED = 0.0064D;
    @Unique private static final long AUGMENTATION_SURVIVAL$SPRINT_EDGE_GRACE_TICKS = 2L;
    @Unique private long augmentationSurvival$lastSprintTick = Long.MIN_VALUE;

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$rememberSprintIntent(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && player.isSprinting()) {
            augmentationSurvival$lastSprintTick = player.level().getGameTime();
        }
    }

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$runOnWater(boolean original, FluidState fluidState) {
        if (original) return true;
        if (!((Object) this instanceof ServerPlayer player) || !fluidState.is(FluidTags.WATER)) return false;
        if (!UtilityAugmentService.canRunOnWater(player)
                || player.isShiftKeyDown()
                || player.isDeadOrDying()
                || player.isPassenger()
                || player.getDeltaMovement().horizontalDistanceSqr() < AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED) {
            return false;
        }
        long now = player.level().getGameTime();
        return player.isSprinting()
                || (augmentationSurvival$lastSprintTick != Long.MIN_VALUE
                && now >= augmentationSurvival$lastSprintTick
                && now - augmentationSurvival$lastSprintTick <= AUGMENTATION_SURVIVAL$SPRINT_EDGE_GRACE_TICKS);
    }
}
