package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Allows reserve-meal owners to intentionally eat while the visible hunger bar is full. */
@Mixin(Player.class)
public abstract class PlayerConsumptionAugmentMixin {
    @ModifyReturnValue(method = "canEat", at = @At("RETURN"))
    private boolean augmentationSurvival$allowReserveMeal(boolean original, boolean canAlwaysEat) {
        return ConsumptionAugmentService.canEat((Player) (Object) this, original);
    }
}
