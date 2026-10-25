package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies only directly consumed beneficial potion effects to valid nearby allies. */
@Mixin(LivingEntity.class)
public abstract class PotionDiffusionMixin {
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void augmentationSurvival$diffusePotion(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = self.getUseItem();
        if (stack.isEmpty() || stack.get(DataComponents.POTION_CONTENTS) == null) {
            return;
        }
        FinalAugmentRuntimeService.onPotionConsumed(player, stack);
    }
}
