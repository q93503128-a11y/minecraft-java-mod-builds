package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets dry Portable Riptide enter the exact vanilla Riptide path while keeping its water bucket intact. */
@Mixin(TridentItem.class)
public abstract class TridentPortableRiptideMixin {
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL$PORTABLE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "use", at = @At("HEAD"))
    private void augmentationSurvival$captureUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            AUGMENTATION_SURVIVAL$PORTABLE.set(
                    AdvancedUtilityAugmentService.canUsePortableRiptide(
                            serverPlayer,
                            player.getItemInHand(hand)
                    )
            );
        } else {
            AUGMENTATION_SURVIVAL$PORTABLE.remove();
        }
    }

    @ModifyExpressionValue(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowPortableUse(boolean original) {
        return original || Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void augmentationSurvival$clearUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        AUGMENTATION_SURVIVAL$PORTABLE.remove();
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void augmentationSurvival$captureRelease(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof ServerPlayer player) {
            AUGMENTATION_SURVIVAL$PORTABLE.set(
                    AdvancedUtilityAugmentService.canUsePortableRiptide(player, stack)
            );
        } else {
            AUGMENTATION_SURVIVAL$PORTABLE.remove();
        }
    }

    @ModifyExpressionValue(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowPortableRelease(boolean original) {
        return original || Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
    }

    @Inject(method = "releaseUsing", at = @At("RETURN"))
    private void augmentationSurvival$finishPortableRiptide(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AUGMENTATION_SURVIVAL$PORTABLE.remove();
    }
}
