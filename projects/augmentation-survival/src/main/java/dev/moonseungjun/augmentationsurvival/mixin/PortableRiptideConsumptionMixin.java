package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One successful dry-land Portable Riptide consumes one water bucket, matching the canon contract. */
@Mixin(value = AdvancedUtilityAugmentService.class, remap = false)
abstract class PortableRiptideConsumptionMixin {
    @Inject(method = "consumePortableRiptideWater", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$consumeOneBucketPerDryRiptide(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        ci.cancel();
        if (player.getAbilities().instabuild) return;
        ItemStack waterBucket = player.getOffhandItem();
        if (!waterBucket.is(Items.WATER_BUCKET)) return;
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
