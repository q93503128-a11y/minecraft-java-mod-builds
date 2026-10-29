package dev.moonseungjun.augmentationsurvival.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Regression coverage for the playtest-facing reroll and active-loadout input paths. */
public final class UiInputRegressionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void rerollButtonDoesNotAlsoSelectOrDoubleConsume(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(AugmentCatalog.REROLL_ID, 2)
        );
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE));

        AugmentDeviceMenu menu = new AugmentDeviceMenu(
                61, player.getInventory(), ContainerLevelAccess.NULL, false
        );
        require(menu.clickMenuButton(player, AugmentDeviceMenu.beginButtonId(AugmentTier.COMMON)),
                "reroll regression menu rejected the common offer button");
        require(menu.status() == AugmentDeviceMenu.STATUS_READY,
                "reroll regression offer did not enter ready state");
        require(menu.rerollsRemaining() == 2,
                "level-two reroll did not grant exactly two charges");

        String before = menu.candidate(0).id();
        require(menu.clickMenuButton(player, AugmentDeviceMenu.rerollButtonId(0)),
                "reroll button was not accepted");
        String after = menu.candidate(0).id();
        require(!after.equals(before), "reroll button did not replace the chosen card");
        require(menu.rerollsRemaining() == 1,
                "one reroll click consumed the wrong number of charges");
        require(menu.selectedMask() == 0 && menu.status() == AugmentDeviceMenu.STATUS_READY,
                "reroll click also selected or committed the card");

        require(menu.clickMenuButton(player, AugmentDeviceMenu.rerollButtonId(0)),
                "duplicate same-tick reroll button was not consumed by the guard");
        require(menu.candidate(0).id().equals(after),
                "duplicate same-tick reroll changed the candidate twice");
        require(menu.rerollsRemaining() == 1,
                "duplicate same-tick reroll consumed another charge");

        require(menu.clickMenuButton(player, AugmentDeviceMenu.candidateButtonId(0)),
                "same-tick selection packet was not consumed by the reroll guard");
        require(menu.selectedMask() == 0 && menu.status() == AugmentDeviceMenu.STATUS_READY,
                "reroll and selection were both processed from one input tick");
        require(AugmentationPlayerStateService.state(player).level(after) == 0,
                "same-tick reroll/selection accidentally granted an augment");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void rerollWithoutReplacementKeepsOfferAndCharge(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);

        List<AugmentDefinition> common = AugmentCatalog.definitions(AugmentTier.COMMON);
        require(common.size() >= 3, "common tier no longer has enough candidates for reroll regression");
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(AugmentCatalog.REROLL_ID, 1);
        for (int i = 3; i < common.size(); i++) {
            AugmentDefinition definition = common.get(i);
            state = state.withAugmentLevel(definition.id(), definition.maxLevel());
        }
        AugmentationPlayerStateService.replace(player, state);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE));

        AugmentDeviceMenu menu = new AugmentDeviceMenu(
                62, player.getInventory(), ContainerLevelAccess.NULL, false
        );
        require(menu.clickMenuButton(player, AugmentDeviceMenu.beginButtonId(AugmentTier.COMMON)),
                "no-replacement reroll menu rejected the common offer button");
        require(menu.status() == AugmentDeviceMenu.STATUS_READY && menu.rerollsRemaining() == 1,
                "no-replacement offer did not begin with one reroll charge");

        List<String> before = List.of(
                menu.candidate(0).id(), menu.candidate(1).id(), menu.candidate(2).id()
        );
        require(menu.clickMenuButton(player, AugmentDeviceMenu.rerollButtonId(0)),
                "no-replacement reroll button was not accepted");
        List<String> after = List.of(
                menu.candidate(0).id(), menu.candidate(1).id(), menu.candidate(2).id()
        );
        require(after.equals(before), "no-replacement reroll changed the visible offer");
        require(menu.rerollsRemaining() == 1,
                "no-replacement reroll incorrectly consumed its charge");
        require(menu.status() == AugmentDeviceMenu.STATUS_READY && menu.selectedMask() == 0,
                "no-replacement reroll invalidated or selected the offer");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void expiredOfferSweepReleasesStalePlayerEntry(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(AugmentCoreItems.COMMON_CORE));

        AugmentOfferService.BeginResult first = AugmentOfferService.beginOffer(player, AugmentTier.COMMON);
        require(first.status() == AugmentOfferService.BeginStatus.READY,
                "cleanup regression could not create an offer");
        AugmentOfferService.expireOffers(Integer.MAX_VALUE);
        require(!AugmentOfferService.hasPendingOffer(player),
                "expired offer sweep retained the stale player entry");
        AugmentOfferService.BeginResult replacement = AugmentOfferService.beginOffer(player, AugmentTier.COMMON);
        require(replacement.status() == AugmentOfferService.BeginStatus.READY,
                "cleanup regression could not create a replacement offer");
        require(!first.token().equals(replacement.token()),
                "expired offer remained in the process-wide pending map");
        AugmentOfferService.clearOffer(player);

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void clientMixinConfigRegistersAugmentCardPresentation(GameTestHelper helper) throws Exception {
        try (var stream = AugmentationSurvival.class.getClassLoader()
                .getResourceAsStream("augmentation_survival.mixins.json")) {
            require(stream != null, "mixin configuration is missing from the runtime resources");
            JsonObject config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            boolean registered = config.getAsJsonArray("client").asList().stream()
                    .anyMatch(entry -> entry.getAsString().equals("AugmentDeviceCardPresentationMixin"));
            require(registered, "client card presentation mixin is not registered in the client mixin list");
        }

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void newlyOwnedActiveFillsFirstEmptySlotWithoutOverwritingManualLayout(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        List<String> supported = ActiveAbilityRouter.supportedIds();
        require(supported.size() >= 3, "active router no longer has three standard active augments");
        String first = supported.get(0);
        String second = supported.get(1);
        String newlyOwned = supported.get(2);

        List<String> manual = new ArrayList<>(List.of(
                "", second, "", first, "", "", "", "", "", ""
        ));
        AugmentationPlayerState before = AugmentationPlayerState.initial()
                .withAugmentLevel(first, 1)
                .withAugmentLevel(second, 1)
                .withActiveSlots(manual);
        AugmentationPlayerStateService.replace(player, before);

        List<String> resolvedBefore = ActiveAbilityRouter.resolvedSlots(
                AugmentationPlayerStateService.state(player)
        );
        require(resolvedBefore.get(1).equals(second) && resolvedBefore.get(3).equals(first),
                "manual active layout was not preserved before acquisition");
        require(resolvedBefore.get(0).isBlank(),
                "empty manual slot was unexpectedly occupied before new acquisition");

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerStateService.state(player).withAugmentLevel(newlyOwned, 1)
        );
        List<String> resolvedAfter = ActiveAbilityRouter.resolvedSlots(
                AugmentationPlayerStateService.state(player)
        );
        require(resolvedAfter.get(0).equals(newlyOwned),
                "new active did not auto-fill the first empty HUD slot");
        require(resolvedAfter.get(1).equals(second) && resolvedAfter.get(3).equals(first),
                "new active acquisition overwrote the existing manual slot layout");
        require(resolvedAfter.stream().filter(newlyOwned::equals).count() == 1,
                "new active appeared in more than one resolved slot");

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
