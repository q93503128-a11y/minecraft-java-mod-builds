package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes only successful Tool Care / Armor Care durability cancellations. */
@Mixin(value = SurvivalAugmentService.class, remap = false)
abstract class DurabilityPassiveReadabilityMixin {
    @Inject(method = "modifyDurabilityDamage", at = @At("RETURN"))
    private static void augmentationSurvival$presentDurabilitySave(
            ServerPlayer player,
            ItemStack stack,
            int amount,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (amount > 0 && cir.getReturnValueI() == 0) {
            PassiveProcReadabilityService.presentDurabilitySaved(player, stack);
        }
    }
}
