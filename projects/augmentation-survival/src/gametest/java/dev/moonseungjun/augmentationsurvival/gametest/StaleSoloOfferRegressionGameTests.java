package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Regression for the live StackOverflow caused by recursively repairing a stale co-op-only offer. */
public final class StaleSoloOfferRegressionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void staleCoopOnlyPendingOfferRefreshesOnceThroughSoloEligibilityPath(GameTestHelper helper)
            throws ReflectiveOperationException {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentOfferService.clearOffer(player);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
        player.getInventory().clearContent();

        AugmentDefinition rescue = AugmentCatalog.require(PlayerRescueAugmentService.RESCUE_ID);
        AugmentTier tier = rescue.tier();
        player.getInventory().add(new ItemStack(AugmentCoreItems.item(tier)));

        String staleToken = "stale-coop-rescue-regression";
        seedPendingRescueOffer(player, tier, staleToken);

        AugmentOfferService.BeginResult refreshed = beginOfferWithEligibility(player, tier, false);
        require(refreshed.status() != AugmentOfferService.BeginStatus.NO_CORE,
                "solo stale-offer repair unexpectedly lost the owned core");
        require(!refreshed.candidateIds().contains(PlayerRescueAugmentService.RESCUE_ID),
                "solo stale-offer repair retained the co-op-only rescue candidate: " + refreshed.candidateIds());
        if (refreshed.status() == AugmentOfferService.BeginStatus.READY) {
            require(refreshed.token() != null && !staleToken.equals(refreshed.token()),
                    "solo stale-offer repair reused the invalid co-op transaction token");
        } else {
            require(refreshed.status() == AugmentOfferService.BeginStatus.FULLY_MAXED,
                    "solo stale-offer repair returned an unexpected status: " + refreshed.status());
        }

        AugmentOfferService.clearOffer(player);
        GameTestPlayers.succeed(helper);
    }

    private static AugmentOfferService.BeginResult beginOfferWithEligibility(
            ServerPlayer player,
            AugmentTier tier,
            boolean multiplayerEligible
    ) throws ReflectiveOperationException {
        Method method = AugmentOfferService.class.getDeclaredMethod(
                "beginOfferForEligibility", ServerPlayer.class, AugmentTier.class, boolean.class
        );
        method.setAccessible(true);
        return (AugmentOfferService.BeginResult) method.invoke(null, player, tier, multiplayerEligible);
    }

    @SuppressWarnings("unchecked")
    private static void seedPendingRescueOffer(ServerPlayer player, AugmentTier tier, String token)
            throws ReflectiveOperationException {
        Class<?> pendingType = Class.forName(AugmentOfferService.class.getName() + "$PendingOffer");
        Constructor<?> constructor = pendingType.getDeclaredConstructor(
                String.class, AugmentTier.class, List.class, int.class, int.class, int.class, Set.class
        );
        constructor.setAccessible(true);
        Object pending = constructor.newInstance(
                token,
                tier,
                List.of(PlayerRescueAugmentService.RESCUE_ID),
                1,
                0,
                player.level().getServer().getTickCount() + 200,
                Set.of(PlayerRescueAugmentService.RESCUE_ID)
        );

        Field field = AugmentOfferService.class.getDeclaredField("PENDING");
        field.setAccessible(true);
        Map<UUID, Object> offers = (Map<UUID, Object>) field.get(null);
        offers.put(player.getUUID(), pending);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
