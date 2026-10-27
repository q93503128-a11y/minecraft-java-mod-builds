package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Regression coverage for co-op-only rescue eligibility and custom superhero flight authority. */
public final class CooperativeMobilityGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void companionRescueIsEligibleWhenTwoPlayersArePresent(GameTestHelper helper) {
        ServerPlayer first = player(helper, 1);
        ServerPlayer second = player(helper, 3);
        require(PlayerRescueAugmentService.isMultiplayerEligible(first), "co-op rescue did not become eligible with two players");
        require(PlayerRescueAugmentService.isMultiplayerEligible(second), "second player did not see co-op rescue eligibility");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 80)
    public void companionRescueDownsAndAnotherPlayerRestoresThirtyPercentHealth(GameTestHelper helper) {
        ServerPlayer target = player(helper, 1);
        ServerPlayer rescuer = player(helper, 2);
        AugmentationPlayerStateService.replace(
                target,
                AugmentationPlayerState.initial().withAugmentLevel(PlayerRescueAugmentService.RESCUE_ID, 1)
        );
        target.setHealth(target.getMaxHealth());
        target.hurtServer(
                (ServerLevel) target.level(),
                target.damageSources().generic(),
                target.getMaxHealth() * 4.0F
        );
        require(PlayerRescueAugmentService.isDowned(target), "rescue owner died instead of entering Downed");
        require(target.getHealth() > 0.0F, "Downed target did not remain alive");
        require(PlayerRescueAugmentService.tryBeginRescue(rescuer, target), "nearby teammate could not begin rescue");
        GameTestPlayers.runAfterDelay(helper, 45, () -> {
            require(!PlayerRescueAugmentService.isDowned(target), "target remained Downed after the two-second rescue channel");
            requireClose(target.getHealth(), target.getMaxHealth() * 0.30F, 0.05F, "rescue did not restore 30% max health");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest
    public void flightUsesCustomAuthorityAndBrakesInsteadOfCreativeStop(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(FlightAugmentService.FLIGHT_ID, 1)
        );
        require(FlightAugmentService.toggle(player), "owned flight augment could not activate");
        require(FlightAugmentService.isActive(player), "flight runtime did not become active");
        require(player.isNoGravity(), "custom flight did not suspend gravity");
        require(player.getAbilities().mayfly, "custom flight did not grant temporary server flight permission");
        require(!player.getAbilities().flying, "custom flight incorrectly enabled vanilla Creative flying");

        require(FlightAugmentService.toggle(player), "flight could not begin shutdown");
        require(FlightAugmentService.phase(player) == FlightAugmentService.Phase.BRAKE_LANDING,
                "flight toggle-off skipped the intended brake/landing phase");
        require(!player.getAbilities().flying, "vanilla Creative flying was enabled during powered-flight shutdown");

        GameTestPlayers.runAfterDelay(helper, 6, () -> {
            require(!FlightAugmentService.isActive(player), "flight remained active after grounded brake/landing completion");
            require(!player.getAbilities().flying, "vanilla Creative flying remained enabled after powered flight ended");
            GameTestPlayers.succeed(helper);
        });
    }

    private static ServerPlayer player(GameTestHelper helper, int x) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireClose(float actual, float expected, float tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
