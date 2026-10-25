package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.storage.StorageAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sends only vanilla inventory overflow into unlocked Pocket slots. */
@Mixin(Inventory.class)
public abstract class InventoryPocketAugmentMixin {
    @Shadow @Final public Player player;

    @Inject(
            method = "add(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void augmentation_survival$storePocketOverflow(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (stack.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) return;
        if (StorageAugmentService.tryInsertPocketOverflow(serverPlayer, stack)) cir.setReturnValue(true);
    }
}
