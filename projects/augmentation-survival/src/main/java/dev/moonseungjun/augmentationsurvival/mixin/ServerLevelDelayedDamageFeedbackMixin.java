package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Delayed-damage installments keep health/death authority but do not retrigger the camera hurt event. */
@Mixin(ServerLevel.class)
abstract class ServerLevelDelayedDamageFeedbackMixin {
    @Inject(method = "broadcastDamageEvent", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressDelayedDamageCameraKick(
            Entity entity,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (entity instanceof ServerPlayer && CombatProvenanceContext.isDelayedDamage()) {
            ci.cancel();
        }
    }
}
