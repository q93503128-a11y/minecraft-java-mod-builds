package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Binds Aerial Combo to successful living-entity damage while ROOT_MELEE provenance is live. */
@Mixin(LivingEntity.class)
abstract class LivingEntityAerialComboMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void augmentationSurvival$afterSuccessfulRootMelee(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()
                || CombatProvenanceContext.resolve(source) != CombatProvenance.ROOT_MELEE) {
            return;
        }

        Entity owner = source.getEntity();
        if (!(owner instanceof ServerPlayer)) {
            owner = source.getDirectEntity();
        }
        if (!(owner instanceof ServerPlayer attacker)) {
            return;
        }

        MovementAugmentService.onSuccessfulRootMelee(attacker, (LivingEntity) (Object) this);
    }
}
