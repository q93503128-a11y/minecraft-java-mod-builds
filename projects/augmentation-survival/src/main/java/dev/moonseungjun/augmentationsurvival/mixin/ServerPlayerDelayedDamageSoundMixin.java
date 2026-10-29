package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores audible delayed-damage feedback without reintroducing vanilla hurt camera/knockback feedback. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerDelayedDamageSoundMixin {
    @Unique
    private boolean augmentationSurvival$trackingDelayedDamage;
    @Unique
    private float augmentationSurvival$delayedHealthBefore;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void augmentationSurvival$captureDelayedHealth(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        augmentationSurvival$trackingDelayedDamage = CombatProvenanceContext.isDelayedDamage();
        if (augmentationSurvival$trackingDelayedDamage) {
            augmentationSurvival$delayedHealthBefore = ((ServerPlayer) (Object) this).getHealth();
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void augmentationSurvival$playDelayedDamageSound(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!augmentationSurvival$trackingDelayedDamage) return;
        augmentationSurvival$trackingDelayedDamage = false;

        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!cir.getReturnValue() || !(player.getHealth() < augmentationSurvival$delayedHealthBefore - 1.0E-6F)) {
            return;
        }
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS,
                0.65F,
                1.0F
        );
    }
}
