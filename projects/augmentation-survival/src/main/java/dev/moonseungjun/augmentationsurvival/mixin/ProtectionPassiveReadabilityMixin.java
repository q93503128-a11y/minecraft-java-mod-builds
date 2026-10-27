package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.ConditionalPassiveReadabilityService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes confirmed Damage Adaptation runtime branches without changing protection math. */
@Mixin(value = ProtectionAugmentService.class, remap = false)
public abstract class ProtectionPassiveReadabilityMixin {
    @Inject(method = "afterDamage", at = @At("TAIL"))
    private static void augmentationSurvival$presentDamageAdaptationArmed(
            ServerPlayer player,
            DamageSource source,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        ConditionalPassiveReadabilityService.presentDamageAdaptationArmed(player, source);
    }

    @Inject(
            method = "modifyAfterReductions",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/combat/ProtectionAugmentService;safeScale(FD)F",
                    ordinal = 0
            )
    )
    private static void augmentationSurvival$presentDamageAdaptationProc(
            ServerPlayer player,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        ConditionalPassiveReadabilityService.presentDamageAdaptationProc(player);
    }
}
