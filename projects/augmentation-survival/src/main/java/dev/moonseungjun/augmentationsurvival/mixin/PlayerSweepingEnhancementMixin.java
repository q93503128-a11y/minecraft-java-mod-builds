package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends only vanilla sweep candidates/damage; it never creates a sweep on a non-sweeping attack. */
@Mixin(Player.class)
abstract class PlayerSweepingEnhancementMixin {
    @ModifyExpressionValue(
            method = "doSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"
            )
    )
    private AABB augmentationSurvival$expandSweepBox(AABB original) {
        return SweepingEnhancementService.expandVanillaSweepBox((Player) (Object) this, original);
    }

    @WrapOperation(
            method = "doSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean augmentationSurvival$multiplySweepDamage(
            LivingEntity target,
            ServerLevel level,
            DamageSource source,
            float damage,
            Operation<Boolean> original
    ) {
        return original.call(
                target,
                level,
                source,
                SweepingEnhancementService.modifyVanillaSecondaryDamage((Player) (Object) this, damage)
        );
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void augmentationSurvival$clearBetterCombatSecondaryContext(
            net.minecraft.world.entity.Entity target,
            CallbackInfo ci
    ) {
        SweepingEnhancementService.clearBetterCombatHit((Player) (Object) this);
    }
}
