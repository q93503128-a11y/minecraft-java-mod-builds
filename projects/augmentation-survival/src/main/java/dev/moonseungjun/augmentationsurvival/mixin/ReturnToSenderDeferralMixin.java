package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disables the retired projectile-reflection implementation; compression is bound at item blocking. */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
public abstract class ReturnToSenderDeferralMixin {
    @Inject(method = "tryReturnProjectile", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$disableLegacyProjectileReflection(
            ServerPlayer player,
            DamageSource source,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
