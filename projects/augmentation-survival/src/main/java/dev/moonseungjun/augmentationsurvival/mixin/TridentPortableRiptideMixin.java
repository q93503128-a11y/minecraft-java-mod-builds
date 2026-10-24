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

/** Supplies the water condition only for a valid dry portable-Riptide activation. */
@Mixin(TridentItem.class)
public abstract class TridentPortableRiptideMixin {
    private static final ThreadLocal<ServerPlayer> AUGMENTATION_SURVIVAL$PLAYER = new ThreadLocal<>();
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
            AUGMENTATION_SURVIVAL$PLAYER.set(serverPlayer);
            AUGMENTATION_SURVIVAL$PORTABLE.set(
                    AdvancedUtilityAugmentService.canUsePortableRiptide(
                            serverPlayer,
                            player.getItemInHand(hand)
                    )
            );
        } else {
            clearContext();
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
        clearContext();
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
            AUGMENTATION_SURVIVAL$PLAYER.set(player);
            AUGMENTATION_SURVIVAL$PORTABLE.set(
                    AdvancedUtilityAugmentService.canUsePortableRiptide(player, stack)
            );
        } else {
            clearContext();
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
    private void augmentationSurvival$consumePortableWater(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        try {
            ServerPlayer player = AUGMENTATION_SURVIVAL$PLAYER.get();
            if (player != null
                    && Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get())
                    && Boolean.TRUE.equals(cir.getReturnValue())) {
                AdvancedUtilityAugmentService.consumePortableRiptideWater(player);
            }
        } finally {
            clearContext();
        }
    }

    private static void clearContext() {
        AUGMENTATION_SURVIVAL$PLAYER.remove();
        AUGMENTATION_SURVIVAL$PORTABLE.remove();
    }
}
