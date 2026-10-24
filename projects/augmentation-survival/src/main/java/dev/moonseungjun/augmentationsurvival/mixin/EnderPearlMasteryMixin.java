package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reduces the exact vanilla ender-pearl cooldown invocation for the owning player. */
@Mixin(EnderpearlItem.class)
public abstract class EnderPearlMasteryMixin {
    private static final ThreadLocal<ServerPlayer> AUGMENTATION_SURVIVAL$PLAYER = new ThreadLocal<>();

    @Inject(method = "use", at = @At("HEAD"))
    private void augmentationSurvival$capturePearlUser(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            AUGMENTATION_SURVIVAL$PLAYER.set(serverPlayer);
        } else {
            AUGMENTATION_SURVIVAL$PLAYER.remove();
        }
    }

    @ModifyArg(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/ItemStack;I)V"
            ),
            index = 1
    )
    private int augmentationSurvival$reducePearlCooldown(int originalTicks) {
        ServerPlayer player = AUGMENTATION_SURVIVAL$PLAYER.get();
        return player == null
                ? originalTicks
                : AdvancedUtilityAugmentService.modifyPearlCooldownTicks(player, originalTicks);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void augmentationSurvival$clearPearlUser(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        AUGMENTATION_SURVIVAL$PLAYER.remove();
    }
}
