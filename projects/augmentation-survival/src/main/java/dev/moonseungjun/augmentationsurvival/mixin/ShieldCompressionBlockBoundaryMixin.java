package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionDurabilityContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens/closes damage-source context around vanilla item blocking; durability is observed in BlocksAttacks itself. */
@Mixin(LivingEntity.class)
abstract class ShieldCompressionBlockBoundaryMixin {
    @Inject(method = "applyItemBlocking", at = @At("HEAD"))
    private void augmentationSurvival$beginShieldCompressionContext(
            ServerLevel level,
            DamageSource source,
            float incomingDamage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        ShieldCompressionDurabilityContext.begin(player, source, stack);
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void augmentationSurvival$endShieldCompressionContext(
            ServerLevel level,
            DamageSource source,
            float incomingDamage,
            CallbackInfoReturnable<Float> cir
    ) {
        if ((Object) this instanceof ServerPlayer player) {
            ShieldCompressionDurabilityContext.end(player);
        }
    }
}
