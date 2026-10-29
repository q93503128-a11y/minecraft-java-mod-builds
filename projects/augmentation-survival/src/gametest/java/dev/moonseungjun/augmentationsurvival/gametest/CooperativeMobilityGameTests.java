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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

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
        long downedAt = target.level().getGameTime();
        target.hurtServer(
                (ServerLevel) target.level(),
                target.damageSources().generic(),
                target.getMaxHealth() * 4.0F
        );
        require(PlayerRescueAugmentService.isDowned(target), "rescue owner died instead of entering Downed");
        require(target.getHealth() > 0.0F, "Downed target did not remain alive");
        require(AugmentationPlayerStateService.state(target).cooldownReadyTick(PlayerRescueAugmentService.RESCUE_ID)
                        == downedAt + 300L * 20L,
                "co-op rescue cooldown is not the canonical 300 seconds");
        require(PlayerRescueAugmentService.tryBeginRescue(rescuer, target), "nearby teammate could not begin rescue");
        GameTestPlayers.runAfterDelay(helper, 45, () -> {
            require(!PlayerRescueAugmentService.isDowned(target), "target remained Downed after the two-second rescue channel");
            requireClose(target.getHealth(), target.getMaxHealth() * 0.30F, 0.05F, "rescue did not restore 30% max health");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 20)
    public void expiredCompanionRescueStateForcesDeathWhenRestored(GameTestHelper helper) {
        ServerPlayer target = player(helper, 1);
        String downedUntilKey = PlayerRescueAugmentService.RESCUE_ID + "#downed_until";
        long expiresAt = target.level().getGameTime() + 1L;
        AugmentationPlayerStateService.replace(
                target,
                AugmentationPlayerState.initial()
                        .withAugmentLevel(PlayerRescueAugmentService.RESCUE_ID, 1)
                        .withCooldownReadyTick(downedUntilKey, expiresAt)
        );

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            try {
                restoreDownedState(target);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("could not invoke stored Downed-state restoration", exception);
            }
            require(target.isDeadOrDying(),
                    "expired stored Downed state was cleared on restore instead of enforcing its timeout death");
            require(AugmentationPlayerStateService.state(target).cooldownReadyTick(downedUntilKey) == 0L,
                    "expired stored Downed marker remained after forced death");
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

    @GameTest
    public void flightImpactBreaksByHardnessAndDealsTenDamageToHostiles(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer player = player(helper, 1);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(FlightAugmentService.FLIGHT_ID, 1)
        );
        require(FlightAugmentService.toggle(player), "owned flight augment could not activate for impact test");
        ServerLevel level = (ServerLevel) player.level();
        BlockPos impactPos = helper.absolutePos(new BlockPos(1, 2, 2));

        level.setBlockAndUpdate(impactPos, Blocks.STONE.defaultBlockState());
        resolveFlightImpact(player, new Vec3(0.0D, 0.0D, 1.30D));
        require(level.getBlockState(impactPos).isAir(), "Cruise impact did not break ordinary stone");

        level.setBlockAndUpdate(impactPos, Blocks.OBSIDIAN.defaultBlockState());
        resolveFlightImpact(player, new Vec3(0.0D, 0.0D, 2.30D));
        require(level.getBlockState(impactPos).is(Blocks.OBSIDIAN), "Boost impact incorrectly broke obsidian-class material");
        level.setBlockAndUpdate(impactPos, Blocks.AIR.defaultBlockState());

        LivingEntity hostile = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 2).withNoFreeWill().spawn();
        hostile.setHealth(20.0F);
        resolveFlightImpact(player, new Vec3(0.0D, 0.0D, 1.30D));
        requireClose(hostile.getHealth(), 10.0F, 0.05F, "flight collision did not deal 10 damage to hostile");
        float afterFirstImpact = hostile.getHealth();
        resolveFlightImpact(player, new Vec3(0.0D, 0.0D, 1.30D));
        requireClose(hostile.getHealth(), afterFirstImpact, 0.05F,
                "same flight overlap dealt repeated per-tick collision damage instead of one impact");
        GameTestPlayers.succeed(helper);
    }

    private static void restoreDownedState(ServerPlayer player) throws ReflectiveOperationException {
        Method method = PlayerRescueAugmentService.class.getDeclaredMethod("restoreDownedState", ServerPlayer.class);
        method.setAccessible(true);
        method.invoke(null, player);
    }

    private static void resolveFlightImpact(ServerPlayer player, Vec3 movement) throws ReflectiveOperationException {
        Method method = FlightAugmentService.class.getDeclaredMethod("resolveImpacts", ServerPlayer.class, Vec3.class);
        method.setAccessible(true);
        method.invoke(null, player, movement);
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
