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

/**
 * Keeps delayed installments audible while removing the ordinary directional damage tilt.
 * ServerPlayer applies its own post-Player damage feedback, so the final tilt restore belongs here.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerDelayedDamageSoundMixin {
    @Unique
    private boolean augmentationSurvival$trackingDelayedDamage;
    @Unique
    private float augmentationSurvival$delayedHealthBefore;
    @Unique
    private float augmentationSurvival$delayedHurtDirBefore;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void augmentationSurvival$captureDelayedHealth(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        augmentationSurvival$trackingDelayedDamage = CombatProvenanceContext.isDelayedDamage();
        if (augmentationSurvival$trackingDelayedDamage) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            augmentationSurvival$delayedHealthBefore = player.getHealth();
            augmentationSurvival$delayedHurtDirBefore = player.getHurtDir();
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void augmentationSurvival$restoreQuietFeedbackAndPlayDelayedSound(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!augmentationSurvival$trackingDelayedDamage) return;
        augmentationSurvival$trackingDelayedDamage = false;

        ServerPlayer player = (ServerPlayer) (Object) this;
        ((PlayerHurtDirAccessor) player).augmentationSurvival$setHurtDir(
                augmentationSurvival$delayedHurtDirBefore
        );

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
