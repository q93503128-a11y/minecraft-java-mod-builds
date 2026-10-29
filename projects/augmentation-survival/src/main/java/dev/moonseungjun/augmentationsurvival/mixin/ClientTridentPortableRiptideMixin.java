package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirrors dry Riptide eligibility for local prediction without trusting client augment state.
 * The server remains authoritative and separately checks actual Portable Riptide ownership.
 */
@Mixin(TridentItem.class)
public abstract class ClientTridentPortableRiptideMixin {
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL$PORTABLE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "use", at = @At("HEAD"))
    private void augmentationSurvival$captureClientUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        AUGMENTATION_SURVIVAL$PORTABLE.set(
                player instanceof LocalPlayer && canPredictPortableRiptide(player, player.getItemInHand(hand))
        );
    }

    @ModifyExpressionValue(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowClientPortableUse(boolean original) {
        return original || Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void augmentationSurvival$clearClientUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        AUGMENTATION_SURVIVAL$PORTABLE.remove();
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void augmentationSurvival$captureClientRelease(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AUGMENTATION_SURVIVAL$PORTABLE.set(
                entity instanceof LocalPlayer player && canPredictPortableRiptide(player, stack)
        );
    }

    @ModifyExpressionValue(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowClientPortableRelease(boolean original) {
        return original || Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
    }

    @Inject(method = "releaseUsing", at = @At("RETURN"))
    private void augmentationSurvival$clearClientRelease(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AUGMENTATION_SURVIVAL$PORTABLE.remove();
    }

    private static boolean canPredictPortableRiptide(Player player, ItemStack trident) {
        if (!player.getOffhandItem().is(Items.WATER_BUCKET)) {
            return false;
        }
        return EnchantmentHelper.getTridentSpinAttackStrength(trident, player) > 0.0F;
    }
}
