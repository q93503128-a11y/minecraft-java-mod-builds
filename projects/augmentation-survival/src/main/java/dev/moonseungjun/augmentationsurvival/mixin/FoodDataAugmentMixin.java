package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataAugmentMixin {
    @Shadow private int foodLevel;
    @Shadow private int tickTimer;

    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$accelerateNaturalRegeneration(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        tickTimer += SurvivalAugmentService.consumeExtraNaturalRegenTimerTicks(player, foodLevel);
    }
}
