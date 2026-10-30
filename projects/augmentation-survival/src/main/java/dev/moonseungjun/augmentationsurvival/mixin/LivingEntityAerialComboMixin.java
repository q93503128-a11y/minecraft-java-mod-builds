package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Binds Aerial Combo to the successful living-entity server damage boundary while ROOT_MELEE provenance is live. */
@Mixin(LivingEntity.class)
abstract class LivingEntityAerialComboMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void augmentationSurvival$afterSuccessfulRootMelee(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        CombatProvenance provenance = CombatProvenanceContext.resolve(source);
        if (!cir.getReturnValueZ() || provenance != CombatProvenance.ROOT_MELEE) return;

        LivingEntity target = (LivingEntity) (Object) this;
        AugmentationSurvival.LOGGER.info(
                "AERIAL_DIAG before target={} valid={} ground={} climbBlock={} water={} motionY={}",
                target.getType(), OffensiveTargetContext.isValid(attacker, target), attacker.onGround(),
                attacker.level().getBlockState(attacker.blockPosition()).is(BlockTags.CLIMBABLE),
                attacker.level().getFluidState(attacker.blockPosition()).is(FluidTags.WATER),
                attacker.getDeltaMovement().y
        );
        MovementAugmentService.onSuccessfulRootMelee(attacker, target);
        AugmentationSurvival.LOGGER.info(
                "AERIAL_DIAG after target={} ground={} motionY={}",
                target.getType(), attacker.onGround(), attacker.getDeltaMovement().y
        );
    }
}
