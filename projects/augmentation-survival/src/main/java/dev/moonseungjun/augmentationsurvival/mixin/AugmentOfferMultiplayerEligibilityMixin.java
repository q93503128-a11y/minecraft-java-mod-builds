package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defense-in-depth guard for a co-op-only rescue selection that became stale before commit. */
@Mixin(value = AugmentOfferService.class, remap = false)
abstract class AugmentOfferMultiplayerEligibilityMixin {
    @Inject(method = "commitOffer", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$rejectStaleSoloCommit(ServerPlayer player, String token, List<String> selectedIds,
                                                                  CallbackInfoReturnable<AugmentOfferService.CommitResult> cir) {
        if (!PlayerRescueAugmentService.isMultiplayerEligible(player)
                && selectedIds.contains(PlayerRescueAugmentService.RESCUE_ID)) {
            cir.setReturnValue(new AugmentOfferService.CommitResult(
                    AugmentOfferService.CommitStatus.NOT_OFFERED, List.of(), null));
        }
    }
}
