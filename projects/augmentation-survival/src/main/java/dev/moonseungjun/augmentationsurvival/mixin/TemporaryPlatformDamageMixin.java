package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformLandingProtection;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Final safety boundary so a valid temporary-platform landing cannot leak fall damage to HP. */
@Mixin(value = CombatAugmentService.class, remap = false)
abstract class TemporaryPlatformDamageMixin {
    @Inject(method = "modifyPostMitigationDamage", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$cancelPlatformFallDamageAtCombatBoundary(
            LivingEntity victim,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (victim instanceof ServerPlayer player
                && source.typeHolder().is(DamageTypeTags.IS_FALL)
                && TemporaryPlatformLandingProtection.protects(player)) {
            cir.setReturnValue(0.0F);
        }
    }
}
