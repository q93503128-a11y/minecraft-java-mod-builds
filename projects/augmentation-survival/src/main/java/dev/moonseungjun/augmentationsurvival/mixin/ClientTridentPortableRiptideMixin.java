package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
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

/** Mirrors the server's dry portable-Riptide eligibility for local prediction and movement. */
@Mixin(TridentItem.class)
public abstract class ClientTridentPortableRiptideMixin {
    private static final String PORTABLE_RIPTIDE_ID =
            AugmentationSurvival.MOD_ID + ":portable_riptide";
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
                player instanceof LocalPlayer
                        && canUsePortableRiptide(player, player.getItemInHand(hand))
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
                entity instanceof LocalPlayer player && canUsePortableRiptide(player, stack)
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

    private static boolean canUsePortableRiptide(Player player, ItemStack trident) {
        if (ClientAugmentProgressState.level(PORTABLE_RIPTIDE_ID) <= 0
                || !player.getOffhandItem().is(Items.WATER_BUCKET)) {
            return false;
        }
        return EnchantmentHelper.getTridentSpinAttackStrength(trident, player) > 0.0F;
    }
}
