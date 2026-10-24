package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks status application, invulnerable knockback and shield-disable duration. */
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

    @Inject(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$blockInvulnerableKnockback(
            double power,
            double xd,
            double zd,
            DamageSource source,
            float damage,
            CallbackInfo ci
    ) {
        augmentationSurvival$cancelInvulnerableKnockback(ci);
    }

    @Inject(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$blockInvulnerableEffectKnockback(
            double power,
            double xd,
            double zd,
            DamageSource source,
            float damage,
            boolean comesFromEffect,
            CallbackInfo ci
    ) {
        augmentationSurvival$cancelInvulnerableKnockback(ci);
    }

    @ModifyArg(
            method = "blockUsingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BlocksAttacks;disable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/item/ItemStack;)V"
            ),
            index = 2
    )
    private float augmentationSurvival$reduceShieldDisableDuration(float originalSeconds) {
        if ((Object) this instanceof ServerPlayer player) {
            return UtilityAugmentService.modifyShieldDisableSeconds(player, originalSeconds);
        }
        return originalSeconds;
    }

    private void augmentationSurvival$cancelInvulnerableKnockback(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player
                && ReactiveCombatAugmentService.isInvulnerable(player)) {
            ci.cancel();
        }
    }
}
