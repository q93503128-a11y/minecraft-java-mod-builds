package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mirrors the server's dry portable-Riptide eligibility for local prediction and movement. */
@Mixin(TridentItem.class)
public abstract class ClientTridentPortableRiptideMixin {
    private static final String PORTABLE_RIPTIDE_ID =
            AugmentationSurvival.MOD_ID + ":portable_riptide";

    @ModifyExpressionValue(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowClientPortableUse(boolean original, net.minecraft.world.level.Level level, Player player) {
        return original || canUsePortableRiptide(player, player.getMainHandItem())
                || canUsePortableRiptide(player, player.getOffhandItem());
    }

    @ModifyExpressionValue(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowClientPortableRelease(
            boolean original,
            ItemStack stack,
            net.minecraft.world.level.Level level,
            LivingEntity entity,
            int remainingUseTicks
    ) {
        return original || entity instanceof Player player && canUsePortableRiptide(player, stack);
    }

    private static boolean canUsePortableRiptide(Player player, ItemStack trident) {
        if (!(player instanceof LocalPlayer)
                || ClientAugmentProgressState.level(PORTABLE_RIPTIDE_ID) <= 0
                || !player.getOffhandItem().is(Items.WATER_BUCKET)) {
            return false;
        }
        return EnchantmentHelper.getTridentSpinAttackStrength(trident, player) > 0.0F;
    }
}
