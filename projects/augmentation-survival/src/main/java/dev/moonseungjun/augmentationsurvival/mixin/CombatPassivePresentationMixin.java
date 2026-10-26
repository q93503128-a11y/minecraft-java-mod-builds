package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassivePresentationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports confirmed survival/combat passive procs without changing their gameplay authority. */
@Mixin(value = CombatAugmentService.class, remap = false)
abstract class CombatPassivePresentationMixin {
    private static final String LAST_STAND = PassivePresentationService.id("last_stand");
    private static final String EMERGENCY_TOTEM = PassivePresentationService.id("emergency_totem");
    private static final String REINCARNATION = PassivePresentationService.id("reincarnation");
    private static final String URGENT_STEPS = PassivePresentationService.id("urgent_steps");
    private static final String KILL_STREAK = PassivePresentationService.id("kill_streak");
    private static final String LIFESTEAL = PassivePresentationService.id("lifesteal");

    @Inject(method = "allowDeath", at = @At("RETURN"))
    private static void augmentationSurvival$presentLastStand(
            LivingEntity entity,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() && entity instanceof ServerPlayer player) {
            PassivePresentationService.proc(player, LAST_STAND);
        }
    }

    @Inject(method = "tryPostVanillaDeathProtection", at = @At("RETURN"))
    private static void augmentationSurvival$presentDeathProtection(
            LivingEntity entity,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() || !(entity instanceof ServerPlayer player)) return;
        PassivePresentationService.proc(
                player,
                CombatAugmentService.isActionLocked(player) ? REINCARNATION : EMERGENCY_TOTEM
        );
    }

    @Inject(method = "afterDamage", at = @At("RETURN"))
    private static void augmentationSurvival$presentUrgentSteps(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        if (entity instanceof ServerPlayer player
                && !blocked
                && damageTaken > 0.0F
                && !CombatProvenanceContext.isDelayedDamage()
                && owns(player, URGENT_STEPS)) {
            PassivePresentationService.proc(player, URGENT_STEPS);
        }
    }

    @Inject(method = "afterKill", at = @At("RETURN"))
    private static void augmentationSurvival$presentKillPassives(
            ServerLevel level,
            Entity killer,
            LivingEntity killedEntity,
            DamageSource damageSource,
            CallbackInfo ci
    ) {
        if (!(killer instanceof ServerPlayer player) || !CompanionAugmentService.isHostileEntity(killedEntity)) {
            return;
        }
        if (owns(player, LIFESTEAL)) PassivePresentationService.proc(player, LIFESTEAL);
        if (owns(player, KILL_STREAK)) PassivePresentationService.proc(player, KILL_STREAK);
    }

    private static boolean owns(ServerPlayer player, String augmentId) {
        return AugmentationPlayerStateService.state(player).level(augmentId) > 0;
    }
}
