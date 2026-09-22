package dev.moonseungjun.openworldrpg.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Openworld RPG does not use vanilla Hunger/Saturation as a survival or healing system.
 *
 * <p>Canceling FoodData's server tick prevents vanilla exhaustion, starvation and food-driven
 * natural regeneration from competing with project Stamina/recovery rules. Project food remains
 * an authored recovery/nourishment item system rather than a hidden second survival meter.</p>
 */
@Mixin(FoodData.class)
public abstract class PlayerFoodSystemSuppressionMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void openworldRpg$suppressVanillaFoodLoop(ServerPlayer player, CallbackInfo ci) {
        ci.cancel();
    }
}
