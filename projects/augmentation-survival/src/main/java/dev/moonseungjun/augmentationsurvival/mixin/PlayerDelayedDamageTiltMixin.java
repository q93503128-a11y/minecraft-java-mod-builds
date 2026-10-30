package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores the player-owned damage-tilt yaw after a quiet delayed-damage installment. */
@Mixin(Player.class)
abstract class PlayerDelayedDamageTiltMixin {
    @Shadow protected float hurtDir;

    @Unique private boolean augmentationSurvival$restoreDelayedTilt;
    @Unique private float augmentationSurvival$hurtDirBeforeDelayed;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void augmentationSurvival$captureDelayedTilt(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        augmentationSurvival$restoreDelayedTilt =
                (Object) this instanceof ServerPlayer && CombatProvenanceContext.isDelayedDamage();
        if (augmentationSurvival$restoreDelayedTilt) {
            augmentationSurvival$hurtDirBeforeDelayed = hurtDir;
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void augmentationSurvival$restoreDelayedTilt(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!augmentationSurvival$restoreDelayedTilt) return;
        augmentationSurvival$restoreDelayedTilt = false;
        hurtDir = augmentationSurvival$hurtDirBeforeDelayed;
    }
}
