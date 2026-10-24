package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the vanilla nutrition transaction so augment additions run after normal food behavior. */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesAugmentMixin {
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void augmentationSurvival$beforeFoodConsume(
            Level level,
            LivingEntity consumer,
            ItemStack stack,
            Consumable consumable,
            CallbackInfo ci
    ) {
        if (consumer instanceof ServerPlayer player) {
            ConsumptionAugmentService.beforeFoodConsumed(player, (FoodProperties) (Object) this);
        }
    }

    @Inject(method = "onConsume", at = @At("TAIL"))
    private void augmentationSurvival$afterFoodConsume(
            Level level,
            LivingEntity consumer,
            ItemStack stack,
            Consumable consumable,
            CallbackInfo ci
    ) {
        if (consumer instanceof ServerPlayer player) {
            ConsumptionAugmentService.afterFoodConsumed(player, (FoodProperties) (Object) this);
        }
    }
}
