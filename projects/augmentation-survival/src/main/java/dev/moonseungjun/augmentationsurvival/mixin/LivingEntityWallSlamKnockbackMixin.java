package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Arms Wall Slam only after Minecraft has applied a real knockback impulse. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWallSlamKnockbackMixin {
    @Inject(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("TAIL")
    )
    private void augmentationSurvival$afterKnockback(
            double power,
            double xd,
            double zd,
            DamageSource source,
            float damage,
            boolean comesFromEffect,
            CallbackInfo ci
    ) {
        FinalAugmentRuntimeService.noteWallSlamKnockback((LivingEntity) (Object) this);
    }
}
