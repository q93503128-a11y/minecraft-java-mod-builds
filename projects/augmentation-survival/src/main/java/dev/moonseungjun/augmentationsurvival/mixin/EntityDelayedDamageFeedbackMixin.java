package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Delayed-damage installments change health without producing the ordinary impact impulse marker.
 * The player still receives the dedicated delayed-damage sound from ServerPlayerDelayedDamageSoundMixin.
 */
@Mixin(Entity.class)
abstract class EntityDelayedDamageFeedbackMixin {
    @Inject(method = "markHurt", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressDelayedImpactMarker(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer && CombatProvenanceContext.isDelayedDamage()) {
            ci.cancel();
        }
    }
}
