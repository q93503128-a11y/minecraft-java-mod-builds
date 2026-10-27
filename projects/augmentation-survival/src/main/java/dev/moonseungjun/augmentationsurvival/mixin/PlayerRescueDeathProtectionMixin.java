package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends the existing post-vanilla death-protection chain instead of competing with another
 * LivingEntity RETURN injector. Rescue therefore runs only after Emergency Totem and Reincarnation
 * have both declined the lethal event.
 */
@Mixin(value = CombatAugmentService.class, remap = false)
abstract class PlayerRescueDeathProtectionMixin {
    @Inject(method = "tryPostVanillaDeathProtection", at = @At("RETURN"), cancellable = true, remap = false)
    private static void augmentationSurvival$applyCoopRescueLast(
            LivingEntity entity,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()
                && PlayerRescueAugmentService.tryPostVanillaDeathProtection(entity, source)) {
            cir.setReturnValue(true);
        }
    }
}
