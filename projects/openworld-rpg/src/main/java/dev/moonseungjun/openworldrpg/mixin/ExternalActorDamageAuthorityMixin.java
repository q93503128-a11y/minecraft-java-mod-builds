package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDamageApplicationContext;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * External actors with project-owned damage authority fail closed for vanilla/donor HP damage.
 * Exactly one project applicator call receives a one-shot authorization token.
 */
@Mixin(LivingEntity.class)
public abstract class ExternalActorDamageAuthorityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void openworldRpg$enforceExternalActorDamageAuthority(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ExternalActorBindingRuntime.ownsDamageAuthority(self)) {
            return;
        }
        if (!ProjectDamageApplicationContext.consumeIfAuthorized(self)) {
            cir.setReturnValue(false);
        }
    }
}
