package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the vanilla/server-inferred ground jump without replacing normal jump behavior. */
@Mixin(LivingEntity.class)
public abstract class MovementAugmentMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void augmentationSurvival$afterGroundJump(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            MovementAugmentService.afterGroundJump(player);
        }
    }
}
