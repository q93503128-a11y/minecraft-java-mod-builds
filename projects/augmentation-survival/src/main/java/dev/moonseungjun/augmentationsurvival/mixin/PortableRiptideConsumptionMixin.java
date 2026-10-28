package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Portable Riptide water is ability fuel: one filled bucket always provides exactly ten uses. */
@Mixin(value = AdvancedUtilityAugmentService.class, remap = false)
abstract class PortableRiptideConsumptionMixin {
    private static final String AUGMENTATION_SURVIVAL$USE_KEY =
            "augmentation_survival.portable_riptide_uses";
    private static final int AUGMENTATION_SURVIVAL$USES_PER_BUCKET = 10;

    @Inject(method = "consumePortableRiptideWater", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$consumeTenUseWaterFuel(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        ci.cancel();
        ItemStack waterBucket = player.getOffhandItem();
        if (!waterBucket.is(Items.WATER_BUCKET)) return;

        int remaining = AdvancedUtilityAugmentService.portableRiptideUsesRemaining(waterBucket);
        if (remaining <= 1) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
            return;
        }
        int nextUsed = AUGMENTATION_SURVIVAL$USES_PER_BUCKET - remaining + 1;
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                waterBucket,
                tag -> tag.putInt(AUGMENTATION_SURVIVAL$USE_KEY, nextUsed)
        );
    }
}
