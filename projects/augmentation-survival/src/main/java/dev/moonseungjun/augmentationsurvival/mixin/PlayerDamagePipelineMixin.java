package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Owns the exact post-magic/pre-vanilla-absorption player damage value in 26.2. */
@Mixin(Player.class)
abstract class PlayerDamagePipelineMixin {
    @ModifyExpressionValue(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
            )
    )
    private float augmentationSurvival$applyFinalPlayerDamageLayers(
            float vanillaPostMagic,
            ServerLevel level,
            DamageSource source,
            float incomingDamage
    ) {
        if (CombatProvenanceContext.isDelayedDamage()) {
            return vanillaPostMagic;
        }
        return CombatAugmentService.modifyPostMitigationDamage(
                (Player) (Object) this,
                source,
                vanillaPostMagic
        );
    }
}
