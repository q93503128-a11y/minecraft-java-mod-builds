package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-runtime coverage for the core-to-offer acquisition transaction. */
public final class AcquisitionOfferGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void commonCoreBeginsThreeCandidateOfferAndCommitsSelection(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE));

        AugmentOfferService.BeginResult offer = AugmentOfferService.beginOffer(player, AugmentTier.COMMON);
        require(offer.status() == AugmentOfferService.BeginStatus.READY,
                "common core did not begin a ready augment offer: " + offer.status());
        require(offer.candidateIds().size() == 3,
                "common offer did not produce three candidates: " + offer.candidateIds());
        require(new HashSet<>(offer.candidateIds()).size() == offer.candidateIds().size(),
                "common offer produced duplicate candidates: " + offer.candidateIds());
        require(AugmentOfferService.countCores(player, AugmentTier.COMMON) == 1,
                "beginning an offer consumed its core before selection");

        String selected = offer.candidateIds().getFirst();
        AugmentOfferService.CommitResult commit = AugmentOfferService.commitOffer(
                player,
                offer.token(),
                List.of(selected)
        );
        require(commit.status() == AugmentOfferService.CommitStatus.SUCCESS,
                "valid common offer selection did not commit: " + commit.status());
        require(AugmentationPlayerStateService.state(player).level(selected) == 1,
                "committed augment did not reach level one: " + selected);
        require(AugmentOfferService.countCores(player, AugmentTier.COMMON) == 0,
                "committed offer did not consume exactly one common core");
        require(!AugmentOfferService.hasPendingOffer(player),
                "committed offer remained pending");

        GameTestPlayers.succeed(helper);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
