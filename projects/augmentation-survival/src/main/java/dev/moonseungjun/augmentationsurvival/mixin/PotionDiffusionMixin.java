package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Copies only directly drunk beneficial potion effects to valid nearby allies. */
@Mixin(PotionItem.class)
public abstract class PotionDiffusionMixin {
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void augmentationSurvival$diffusePotion(
            ItemStack stack,
            Level level,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            FinalAugmentRuntimeService.onPotionConsumed(player, stack);
        }
    }
}
