package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveReadabilityService;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks confirmed final-runtime passive events to world-space feedback without changing gameplay authority.
 */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class FinalAugmentReadabilityMixin {
    @Inject(method = "onPotionConsumed", at = @At("TAIL"))
    private static void augmentationSurvival$presentPotionDiffusion(
            ServerPlayer player,
            ItemStack consumedStack,
            CallbackInfo ci,
            @Local(ordinal = 0) List<LivingEntity> allies
    ) {
        PotionContents contents = consumedStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || allies == null || allies.isEmpty()) return;
        boolean hasBeneficialEffect = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                hasBeneficialEffect = true;
                break;
            }
        }
        if (hasBeneficialEffect) {
            PassiveReadabilityService.presentPotionDiffusion(player, allies);
        }
    }

    @Inject(
            method = "tickBallisticBarrier",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;discard()V",
                    remap = true
            ),
            remap = false
    )
    private static void augmentationSurvival$presentBallisticIntercept(
            CallbackInfo ci,
            @Local ServerPlayer player,
            @Local Projectile projectile
    ) {
        PassiveReadabilityService.presentBallisticIntercept(player, projectile);
    }

    @Inject(
            method = "tickWallSlams",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/combat/CombatProvenanceContext;runAs(Ldev/moonseungjun/augmentationsurvival/augment/combat/CombatProvenance;Ljava/lang/Runnable;)V"
            )
    )
    private static void augmentationSurvival$presentWallSlam(
            CallbackInfo ci,
            @Local LivingEntity target,
            @Local ServerPlayer owner
    ) {
        PassiveReadabilityService.presentWallSlam(owner, target);
    }
}
