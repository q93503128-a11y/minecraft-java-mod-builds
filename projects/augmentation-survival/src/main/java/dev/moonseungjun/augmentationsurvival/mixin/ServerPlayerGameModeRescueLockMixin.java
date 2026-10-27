package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server interaction lock for players in the Downed state. */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeRescueLockMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockDownedBlockBreak(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                                           Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (PlayerRescueAugmentService.isActionLocked(player)) ci.cancel();
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockDownedUseItem(ServerPlayer player, Level level, ItemStack itemStack,
                                                        InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (PlayerRescueAugmentService.isActionLocked(player)) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockDownedUseItemOn(ServerPlayer player, Level level, ItemStack itemStack,
                                                          InteractionHand hand, BlockHitResult hitResult,
                                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (PlayerRescueAugmentService.isActionLocked(player)) cir.setReturnValue(InteractionResult.PASS);
    }
}
