package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-only portable Riptide environment mirror. While the augment, Riptide trident and water
 * bucket are equipped, vanilla client code sees the local player as wet for Riptide prediction and
 * animation. No actual fluid state, swimming state or server environment is changed.
 */
@Mixin(Entity.class)
abstract class ClientPortableRiptideWetnessMixin {
    private static final String PORTABLE_RIPTIDE_ID = AugmentationSurvival.MOD_ID + ":portable_riptide";

    @Inject(method = "isInWaterOrRain", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$portableRiptideWetness(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        if (!((Object) this instanceof LocalPlayer player)
                || ClientAugmentProgressState.level(PORTABLE_RIPTIDE_ID) <= 0
                || !player.getOffhandItem().is(Items.WATER_BUCKET)
                || !(player.getMainHandItem().getItem() instanceof TridentItem)) {
            return;
        }
        if (EnchantmentHelper.getTridentSpinAttackStrength(player.getMainHandItem(), player) > 0.0F) {
            cir.setReturnValue(true);
        }
    }
}
