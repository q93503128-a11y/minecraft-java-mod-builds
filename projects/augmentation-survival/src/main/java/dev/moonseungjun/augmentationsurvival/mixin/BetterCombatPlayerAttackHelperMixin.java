package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Optional Better Combat server range adapter. */
@Pseudo
@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
abstract class BetterCombatPlayerAttackHelperMixin {
    @ModifyReturnValue(method = "getRangeForItem", at = @At("RETURN"))
    private static double augmentationSurvival$extendServerValidationRange(
            double original,
            Player player,
            ItemStack stack
    ) {
        if (player.level().isClientSide()) return original;
        return SweepingEnhancementService.modifyBetterCombatServerRange(player, stack, original);
    }
}
