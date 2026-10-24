package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Client input compensation for the canonical food/drink movement-penalty augment. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerConsumptionMixin {
    @ModifyReturnValue(method = "modifyInput", at = @At("RETURN"))
    private Vec2 augmentationSurvival$easeConsumableSlowdown(Vec2 original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!player.isUsingItem()) {
            return original;
        }
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty() || stack.get(DataComponents.CONSUMABLE) == null) {
            return original;
        }
        UseEffects effects = stack.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT);
        float vanillaMultiplier = effects.speedMultiplier();
        if (!(vanillaMultiplier > 0.0F) || vanillaMultiplier >= 1.0F) {
            return original;
        }
        float adjusted = ConsumptionAugmentService.easedUseSpeedMultiplier(player, vanillaMultiplier);
        if (adjusted <= vanillaMultiplier) {
            return original;
        }
        return original.scale(adjusted / vanillaMultiplier);
    }
}
