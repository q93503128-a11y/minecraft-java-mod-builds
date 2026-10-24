package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.AbundanceService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server-owned fishing timing, treasure bias and abundance scheduling. */
@Mixin(FishingHook.class)
public abstract class FishingHookAugmentMixin {
    @Shadow private int timeUntilLured;
    @Shadow private int nibble;
    @Shadow @Final @Mutable private int luck;

    @Unique private int augmentationSurvival$beforeLured;
    @Unique private double augmentationSurvival$lureCredit;
    @Unique private boolean augmentationSurvival$hadNibble;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At("TAIL"))
    private void augmentationSurvival$augmentTreasureLuck(
            Player player,
            net.minecraft.world.level.Level level,
            int luck,
            int lureSpeed,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            this.luck += ConsumptionAugmentService.treasureFishingLuckBonus(serverPlayer);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$captureLureTimer(CallbackInfo ci) {
        augmentationSurvival$beforeLured = timeUntilLured;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$accelerateLureTimer(CallbackInfo ci) {
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        if (!(owner instanceof ServerPlayer player)
                || augmentationSurvival$beforeLured <= 0
                || timeUntilLured < 0
                || timeUntilLured >= augmentationSurvival$beforeLured) {
            return;
        }
        double reduction = ConsumptionAugmentService.fishingWaitReduction(player);
        if (!(reduction > 0.0D) || reduction >= 0.95D) {
            return;
        }
        int vanillaProgress = augmentationSurvival$beforeLured - timeUntilLured;
        augmentationSurvival$lureCredit += vanillaProgress * reduction / (1.0D - reduction);
        int extra = (int) Math.floor(augmentationSurvival$lureCredit);
        if (extra <= 0) {
            return;
        }
        int applied = Math.min(extra, timeUntilLured);
        timeUntilLured -= applied;
        augmentationSurvival$lureCredit -= applied;
    }

    @Inject(method = "retrieve", at = @At("HEAD"))
    private void augmentationSurvival$captureFishingResult(
            ItemStack rod,
            CallbackInfoReturnable<Integer> cir
    ) {
        augmentationSurvival$hadNibble = nibble > 0;
    }

    @Inject(method = "retrieve", at = @At("TAIL"))
    private void augmentationSurvival$scheduleFishingAbundance(
            ItemStack rod,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!augmentationSurvival$hadNibble) {
            return;
        }
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        if (owner instanceof ServerPlayer player) {
            AbundanceService.scheduleFishing(player, ((FishingHook) (Object) this).position());
        }
        augmentationSurvival$hadNibble = false;
    }
}
