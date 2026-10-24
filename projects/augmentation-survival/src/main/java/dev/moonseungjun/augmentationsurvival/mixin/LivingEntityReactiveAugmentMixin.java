package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks status application and knockback at their shared LivingEntity boundaries. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityReactiveAugmentMixin {
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$interceptHarmfulEffect(
            MobEffectInstance effect,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        Boolean handled = ReactiveCombatAugmentService.interceptIncomingEffect(player, effect, source);
        if (handled != null) {
            cir.setReturnValue(handled);
        }
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$blockInvulnerableKnockback(
            double strength,
            double x,
            double z,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof ServerPlayer player
                && ReactiveCombatAugmentService.isInvulnerable(player)) {
            ci.cancel();
        }
    }
}
