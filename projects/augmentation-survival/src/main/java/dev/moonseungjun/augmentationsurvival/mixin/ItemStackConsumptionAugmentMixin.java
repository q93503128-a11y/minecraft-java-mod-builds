package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps server and local-client food use duration on the same augment rule. */
@Mixin(ItemStack.class)
public abstract class ItemStackConsumptionAugmentMixin {
    @ModifyReturnValue(method = "getUseDuration", at = @At("RETURN"))
    private int augmentationSurvival$modifyFoodUseDuration(int original, LivingEntity user) {
        return ConsumptionAugmentService.modifyUseDuration((ItemStack) (Object) this, user, original);
    }
}
