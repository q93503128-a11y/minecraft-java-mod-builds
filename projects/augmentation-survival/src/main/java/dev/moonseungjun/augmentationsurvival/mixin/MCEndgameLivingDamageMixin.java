package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.augment.combat.MCEndgameDamageCompatibility;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hooks the exact final amount produced by pinned MCEndgame 1.7.0+26.2 before its injected
 * LivingEntity damage loop applies invulnerability cooldown and calls actuallyHurt.
 */
@Mixin(value = LivingEntity.class, priority = 900)
abstract class MCEndgameLivingDamageMixin {
    @ModifyExpressionValue(
            method = "callHurtServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/fuballer/mcendgame/main/component/damage/DamageCalculationResult;getAmount()F",
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private float augmentationSurvival$modifyMCEndgameCalculatedDamage(
            float calculatedDamage,
            ServerLevel level,
            DamageSource source,
            float incomingDamage
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return calculatedDamage;
        }
        return MCEndgameDamageCompatibility.modifyCalculatedDamage(
                player,
                level,
                source,
                Optional.of(calculatedDamage)
        ).orElse(calculatedDamage);
    }
}
