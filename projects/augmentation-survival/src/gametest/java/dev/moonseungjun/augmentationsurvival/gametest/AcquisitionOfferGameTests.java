package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.acquisition.CoreUpgradeService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
        require(player.getInventory().countItem(AugmentCoreItems.COMMON_CORE) == 1,
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
        require(player.getInventory().countItem(AugmentCoreItems.COMMON_CORE) == 0,
                "committed offer did not consume exactly one common core");
        require(!AugmentOfferService.hasPendingOffer(player),
                "committed offer remained pending");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void rerollAndSecondChoiceStayInsideOneServerTransaction(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial()
                        .withAugmentLevel(AugmentCatalog.SECOND_CHOICE_ID, 1)
                        .withAugmentLevel(AugmentCatalog.REROLL_ID, 1)
        );
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE));

        AugmentOfferService.BeginResult initial = AugmentOfferService.beginOffer(player, AugmentTier.COMMON);
        require(initial.status() == AugmentOfferService.BeginStatus.READY,
                "offer modifiers could not begin a common offer: " + initial.status());
        require(initial.selectionCount() == 2,
                "second choice did not raise the selection count to two: " + initial.selectionCount());
        require(initial.rerollsRemaining() == 1,
                "candidate reroll did not grant one reroll: " + initial.rerollsRemaining());
        require(initial.candidateIds().size() == 3,
                "modifier offer did not preserve three candidate slots: " + initial.candidateIds());

        String replaced = initial.candidateIds().getFirst();
        AugmentOfferService.RerollResult reroll = AugmentOfferService.rerollCandidate(player, initial.token(), 0);
        require(reroll.status() == AugmentOfferService.RerollStatus.SUCCESS,
                "valid candidate reroll failed: " + reroll.status());
        require(reroll.rerollsRemaining() == 0,
                "reroll charge was not consumed exactly once: " + reroll.rerollsRemaining());
        require(reroll.replacementId() != null && !reroll.replacementId().equals(replaced),
                "reroll returned the same candidate it replaced");
        require(!initial.candidateIds().contains(reroll.replacementId()),
                "reroll duplicated another currently offered candidate: " + reroll.replacementId());

        AugmentOfferService.BeginResult resumed = AugmentOfferService.beginOffer(player, AugmentTier.COMMON);
        require(resumed.status() == AugmentOfferService.BeginStatus.READY,
                "pending rerolled offer could not be resumed: " + resumed.status());
        require(resumed.token().equals(initial.token()),
                "resuming a pending offer replaced its transaction token");
        require(resumed.selectionCount() == 2 && resumed.rerollsRemaining() == 0,
                "resumed offer lost modifier state");
        require(resumed.candidateIds().getFirst().equals(reroll.replacementId()),
                "resumed offer did not retain the rerolled candidate");

        List<String> selected = List.of(resumed.candidateIds().get(0), resumed.candidateIds().get(1));
        AugmentationPlayerState before = AugmentationPlayerStateService.state(player);
        AugmentOfferService.CommitResult commit = AugmentOfferService.commitOffer(player, resumed.token(), selected);
        require(commit.status() == AugmentOfferService.CommitStatus.SUCCESS,
                "two-card second-choice commit failed: " + commit.status());
        AugmentationPlayerState after = AugmentationPlayerStateService.state(player);
        for (String selectedId : selected) {
            require(after.level(selectedId) == before.level(selectedId) + 1,
                    "second-choice commit did not grant exactly one level: " + selectedId);
        }
        require(player.getInventory().countItem(AugmentCoreItems.COMMON_CORE) == 0,
                "two-card selection consumed more or less than one core");
        require(!AugmentOfferService.hasPendingOffer(player),
                "completed modifier offer remained pending");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void coreUpgradeAllPreservesRemainderAndTopTierIsImmutable(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE, 7));
        player.getInventory().add(new ItemStack(AugmentCoreItems.MYTHIC_CORE, 3));

        CoreUpgradeService.UpgradeResult upgrade = CoreUpgradeService.upgradeAll(player, AugmentTier.COMMON);
        require(upgrade.status() == CoreUpgradeService.UpgradeStatus.SUCCESS,
                "valid 3:1 bulk core upgrade failed: " + upgrade.status());
        require(upgrade.outputCount() == 2,
                "seven common cores did not produce exactly two uncommon cores: " + upgrade.outputCount());
        require(player.getInventory().countItem(AugmentCoreItems.COMMON_CORE) == 1,
                "bulk core upgrade did not preserve the one-core remainder");
        require(player.getInventory().countItem(AugmentCoreItems.UNCOMMON_CORE) == 2,
                "bulk core upgrade did not grant exactly two uncommon cores");

        int mythicBefore = player.getInventory().countItem(AugmentCoreItems.MYTHIC_CORE);
        CoreUpgradeService.UpgradeResult topTier = CoreUpgradeService.upgradeOne(player, AugmentTier.MYTHIC);
        require(topTier.status() == CoreUpgradeService.UpgradeStatus.TOP_TIER,
                "mythic core incorrectly allowed an upward conversion: " + topTier.status());
        require(player.getInventory().countItem(AugmentCoreItems.MYTHIC_CORE) == mythicBefore,
                "top-tier rejection consumed mythic cores");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void deviceMenuButtonsRouteOfferAndUpgradeTransactions(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE));

        AugmentDeviceMenu basic = new AugmentDeviceMenu(
                41, player.getInventory(), ContainerLevelAccess.NULL, false
        );
        require(!basic.clickMenuButton(player, AugmentDeviceMenu.upgradeOneButtonId(AugmentTier.COMMON)),
                "basic augment device accepted an advanced-only core upgrade button");
        require(basic.clickMenuButton(player, AugmentDeviceMenu.beginButtonId(AugmentTier.COMMON)),
                "basic augment device did not accept the common-offer button");
        require(basic.status() == AugmentDeviceMenu.STATUS_READY,
                "menu did not enter ready state after beginning a valid offer: " + basic.status());
        require(basic.selectionCount() == 1,
                "basic offer exposed the wrong selection count: " + basic.selectionCount());
        require(basic.candidate(0) != null && basic.candidate(1) != null && basic.candidate(2) != null,
                "menu did not expose all three server offer candidates");

        String selected = basic.candidate(0).id();
        require(basic.clickMenuButton(player, AugmentDeviceMenu.candidateButtonId(0)),
                "basic augment device rejected a valid candidate button");
        require(basic.status() == AugmentDeviceMenu.STATUS_ACQUIRED,
                "menu did not enter acquired state after committing selection: " + basic.status());
        require(AugmentationPlayerStateService.state(player).level(selected) == 1,
                "menu candidate button did not grant the selected augment");
        require(player.getInventory().countItem(AugmentCoreItems.COMMON_CORE) == 0,
                "menu selection did not consume exactly one common core");

        AugmentOfferService.clearOffer(player);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE, 3));
        AugmentDeviceMenu advanced = new AugmentDeviceMenu(
                42, player.getInventory(), ContainerLevelAccess.NULL, true
        );
        require(advanced.clickMenuButton(player, AugmentDeviceMenu.upgradeOneButtonId(AugmentTier.COMMON)),
                "advanced augment device rejected a valid upgrade button");
        require(advanced.status() == AugmentDeviceMenu.STATUS_UPGRADED,
                "advanced menu did not enter upgraded state: " + advanced.status());
        require(advanced.lastOutputCount() == 1,
                "advanced menu reported the wrong core output count: " + advanced.lastOutputCount());
        require(player.getInventory().countItem(AugmentCoreItems.COMMON_CORE) == 0,
                "advanced menu did not consume exactly three common cores");
        require(player.getInventory().countItem(AugmentCoreItems.UNCOMMON_CORE) == 1,
                "advanced menu did not grant one uncommon core");

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
