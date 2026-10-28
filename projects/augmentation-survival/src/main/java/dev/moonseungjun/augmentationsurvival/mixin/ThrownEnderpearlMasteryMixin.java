package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies Pearl Mastery exactly where vanilla ender-pearl teleport self-damage is dealt. */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMasteryMixin {
    @WrapOperation(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean augmentationSurvival$applyPearlMasteryDamage(
            ServerPlayer player,
            ServerLevel level,
            DamageSource source,
            float damage,
            Operation<Boolean> original
    ) {
        return original.call(
                player,
                level,
                source,
                AdvancedUtilityAugmentService.modifyPearlSelfDamage(player, damage)
        );
    }
}
