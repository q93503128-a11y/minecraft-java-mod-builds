package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks only the two damage boundaries that the public Fabric events cannot modify precisely. */
@Mixin(LivingEntity.class)
abstract class CombatAugmentMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float augmentationSurvival$modifyRootOutgoingDamage(
            float damage,
            ServerLevel level,
            DamageSource source,
            float originalDamage
    ) {
        return CombatAugmentService.modifyOutgoingDamage(
                (LivingEntity) (Object) this,
                source,
                damage
        );
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$modifyPostMitigationDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(CombatAugmentService.modifyPostMitigationDamage(
                (LivingEntity) (Object) this,
                source,
                cir.getReturnValueF()
        ));
    }
}
