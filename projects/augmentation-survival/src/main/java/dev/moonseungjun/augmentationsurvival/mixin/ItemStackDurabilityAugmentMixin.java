package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityAugmentMixin {
    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int augmentationSurvival$modifyDurabilityDamage(int amount, int originalAmount,
                                                             net.minecraft.server.level.ServerLevel level,
                                                             ServerPlayer player) {
        if (player == null) {
            return amount;
        }
        return SurvivalAugmentService.modifyDurabilityDamage(
                player,
                (ItemStack) (Object) this,
                amount
        );
    }
}
