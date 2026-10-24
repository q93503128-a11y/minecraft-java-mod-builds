package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
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

/** Locks mining and item/block use during reincarnation recovery stasis. */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeActionLockMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockBlockBreak(
            BlockPos pos,
            ServerboundPlayerActionPacket.Action action,
            Direction direction,
            int maxY,
            int sequence,
            CallbackInfo ci
    ) {
        if (CombatAugmentService.isActionLocked(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockUseItem(
            ServerPlayer player,
            Level level,
            ItemStack itemStack,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (CombatAugmentService.isActionLocked(player)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockUseItemOn(
            ServerPlayer player,
            Level level,
            ItemStack itemStack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (CombatAugmentService.isActionLocked(player)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
