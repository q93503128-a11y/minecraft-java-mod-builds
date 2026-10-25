package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.AugmentHealingService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataAugmentMixin {
    @Shadow private int foodLevel;
    @Shadow private int tickTimer;

    @Unique private int augmentationSurvival$foodBeforeTick;

    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$beforeFoodTick(ServerPlayer player, CallbackInfo ci) {
        AugmentHealingService.beginNaturalHealingContext();
        augmentationSurvival$foodBeforeTick = foodLevel;
        tickTimer += SurvivalAugmentService.consumeExtraNaturalRegenTimerTicks(player, foodLevel);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$consumeReserveBeforeVisibleHunger(ServerPlayer player, CallbackInfo ci) {
        AugmentHealingService.endNaturalHealingContext();
        foodLevel = ConsumptionAugmentService.restoreFoodFromReserve(
                player,
                augmentationSurvival$foodBeforeTick,
                foodLevel
        );
    }
}
