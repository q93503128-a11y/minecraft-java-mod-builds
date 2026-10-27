package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the co-op-only rescue augment out of solo offers, rerolls and stale commits. */
@Mixin(value = AugmentOfferService.class, remap = false)
abstract class AugmentOfferMultiplayerEligibilityMixin {
    @Unique private static final ThreadLocal<ServerPlayer> augmentationSurvival$offerPlayer = new ThreadLocal<>();

    @Inject(method = "beginOffer", at = @At("HEAD"))
    private static void augmentationSurvival$beginContext(ServerPlayer player, AugmentTier tier,
                                                          CallbackInfoReturnable<AugmentOfferService.BeginResult> cir) {
        augmentationSurvival$offerPlayer.set(player);
    }

    @Inject(method = "beginOffer", at = @At("RETURN"), cancellable = true)
    private static void augmentationSurvival$repairStaleSoloOffer(ServerPlayer player, AugmentTier tier,
                                                                 CallbackInfoReturnable<AugmentOfferService.BeginResult> cir) {
        try {
            AugmentOfferService.BeginResult result = cir.getReturnValue();
            if (!PlayerRescueAugmentService.isMultiplayerEligible(player)
                    && result != null
                    && result.candidateIds().contains(PlayerRescueAugmentService.RESCUE_ID)) {
                AugmentOfferService.clearOffer(player);
                cir.setReturnValue(AugmentOfferService.beginOffer(player, tier));
            }
        } finally {
            augmentationSurvival$offerPlayer.remove();
        }
    }

    @Inject(method = "rerollCandidate", at = @At("HEAD"))
    private static void augmentationSurvival$rerollContext(ServerPlayer player, String token, int slot,
                                                           CallbackInfoReturnable<AugmentOfferService.RerollResult> cir) {
        augmentationSurvival$offerPlayer.set(player);
    }

    @Inject(method = "rerollCandidate", at = @At("RETURN"))
    private static void augmentationSurvival$clearRerollContext(ServerPlayer player, String token, int slot,
                                                                CallbackInfoReturnable<AugmentOfferService.RerollResult> cir) {
        augmentationSurvival$offerPlayer.remove();
    }

    @Inject(method = "commitOffer", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$rejectStaleSoloCommit(ServerPlayer player, String token, List<String> selectedIds,
                                                                  CallbackInfoReturnable<AugmentOfferService.CommitResult> cir) {
        if (!PlayerRescueAugmentService.isMultiplayerEligible(player)
                && selectedIds.contains(PlayerRescueAugmentService.RESCUE_ID)) {
            cir.setReturnValue(new AugmentOfferService.CommitResult(
                    AugmentOfferService.CommitStatus.NOT_OFFERED, List.of(), null));
        }
    }

    @ModifyReturnValue(method = {"eligibleCandidates", "rerollPool"}, at = @At("RETURN"))
    private static List<AugmentDefinition> augmentationSurvival$filterSoloRescue(List<AugmentDefinition> original) {
        ServerPlayer player = augmentationSurvival$offerPlayer.get();
        if (player == null || PlayerRescueAugmentService.isMultiplayerEligible(player)) return original;
        return original.stream().filter(definition -> !definition.id().equals(PlayerRescueAugmentService.RESCUE_ID)).toList();
    }
}
