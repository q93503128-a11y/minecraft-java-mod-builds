package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Broadens Aerial Combo to non-allies and gives a clear confirmation when an air jump is recharged. */
@Mixin(value = MovementAugmentService.class, remap = false)
abstract class MovementOffensiveTargetContextMixin {
    private static final String AUGMENTATION_SURVIVAL$AERIAL_COMBO_ID = AugmentationSurvival.MOD_ID + ":aerial_combo";

    @Inject(method = "afterDamage", at = @At("HEAD"))
    private static void augmentationSurvival$pushOffensiveContext(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        OffensiveTargetContext.push();
    }

    @Inject(method = "afterDamage", at = @At("RETURN"))
    private static void augmentationSurvival$presentAndPopOffensiveContext(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        try {
            if (blocked || !(damageTaken > 0.0F) || !(source.getEntity() instanceof ServerPlayer attacker)) return;
            if (CombatProvenanceContext.resolve(source) != CombatProvenance.ROOT_MELEE) return;
            if (!OffensiveTargetContext.isValid(attacker, entity)) return;
            if (AugmentationPlayerStateService.state(attacker).level(AUGMENTATION_SURVIVAL$AERIAL_COMBO_ID) <= 0) return;
            if (attacker.onGround() || attacker.isInWater() || attacker.onClimbable()) return;

            attacker.level().sendParticles(
                    ParticleTypes.END_ROD,
                    attacker.getX(), attacker.getY() + 0.18D, attacker.getZ(),
                    7, 0.28D, 0.05D, 0.28D, 0.025D
            );
            attacker.level().playSound(
                    null, attacker.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.35F, 1.65F
            );
            attacker.sendSystemMessage(Component.literal("공중 연계 · 공중 점프 충전"), true);
        } finally {
            OffensiveTargetContext.pop();
        }
    }
}
