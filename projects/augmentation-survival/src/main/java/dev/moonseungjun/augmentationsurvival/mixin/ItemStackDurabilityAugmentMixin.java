package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityAugmentMixin {
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$avoidDurabilityLoss(
            int amount,
            ServerLevel level,
            ServerPlayer player,
            Consumer<Item> onBreak,
            CallbackInfo ci
    ) {
        if (player != null && SurvivalAugmentService.modifyDurabilityDamage(
                player,
                (ItemStack) (Object) this,
                amount
        ) <= 0) {
            ci.cancel();
        }
    }
}
