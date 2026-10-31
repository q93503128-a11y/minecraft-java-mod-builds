package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionDurabilityContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the exact post-vanilla durability state for both 26.2 BlocksAttacks overloads. */
@Mixin(BlocksAttacks.class)
abstract class BlocksAttacksShieldCompressionMixin {
    @Inject(
            method = "hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V",
            at = @At("RETURN")
    )
    private void augmentationSurvival$afterBlockingDurability(
            Level level,
            ItemStack item,
            LivingEntity user,
            InteractionHand hand,
            float damage,
            CallbackInfo ci
    ) {
        ShieldCompressionDurabilityContext.afterVanillaDurabilityStep(user, item, damage);
    }

    @Inject(
            method = "hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;FI)V",
            at = @At("RETURN")
    )
    private void augmentationSurvival$afterBlockingDurabilityFixed(
            Level level,
            ItemStack item,
            LivingEntity user,
            InteractionHand hand,
            float damage,
            int fixedDamage,
            CallbackInfo ci
    ) {
        ShieldCompressionDurabilityContext.afterVanillaDurabilityStep(user, item, damage);
    }
}
