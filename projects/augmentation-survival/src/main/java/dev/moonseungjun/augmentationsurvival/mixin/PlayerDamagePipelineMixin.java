package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Owns the final player post-mitigation damage boundary in 26.2.
 *
 * <p>Player implements its own actuallyHurt path. Wrapping the inherited magic-absorption call at
 * that player-owned call site guarantees augment shields and delayed damage modify the exact value
 * that is subsequently consumed by vanilla absorption/HP, while leaving LivingEntity's generic
 * reduction method compatible with other mods.</p>
 */
@Mixin(Player.class)
abstract class PlayerDamagePipelineMixin {
    @WrapOperation(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
            )
    )
    private float augmentationSurvival$applyFinalPlayerDamageLayers(
            Player instance,
            DamageSource source,
            float damage,
            Operation<Float> original
    ) {
        float vanillaPostMagic = original.call(instance, source, damage);
        if (CombatProvenanceContext.isDelayedDamage()) {
            return vanillaPostMagic;
        }
        return CombatAugmentService.modifyPostMitigationDamage(instance, source, vanillaPostMagic);
    }
}
