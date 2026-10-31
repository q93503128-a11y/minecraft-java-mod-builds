package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationGrowthSampler;
import dev.moonseungjun.augmentationsurvival.augment.active.RewindSafetyService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Regressions discovered by the 2026-10-31 live-play pass. */
public final class LegendaryLiveFixBatchEGameTests implements CustomTestMethodInvoker {
    private static final String PHASE_SHIFT_ID = AugmentationSurvival.MOD_ID + ":phase_shift";
    private static final String TIME_ACCELERATION_ID = AugmentationSurvival.MOD_ID + ":time_acceleration";

    @GameTest(maxTicks = 60)
    public void phaseShiftSuppressesOnlyInWallDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(PHASE_SHIFT_ID, 1)
        );
        require(AdvancedWorldAugmentService.tryUse(player, PHASE_SHIFT_ID), "phase shift did not activate");

        float before = player.getHealth();
        player.invulnerableTime = 0;
        player.hurtServer(level, player.damageSources().inWall(), 4.0F);
        require(close(player.getHealth(), before), "phase shift still took in-wall suffocation damage");

        player.invulnerableTime = 0;
        player.hurtServer(level, player.damageSources().generic(), 1.0F);
        require(player.getHealth() < before,
                "phase shift accidentally became general invulnerability instead of only suppressing in-wall damage");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 60)
    public void rewindSafetyMovesSolidHistoricalDestinationToNearbyFreeSpace(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos blocked = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(blocked, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(blocked.above(), Blocks.STONE.defaultBlockState());
        Vec3 desired = Vec3.atBottomCenterOf(blocked);
        TeleportTransition transition = new TeleportTransition(
                level, desired, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING
        );
        require(!RewindSafetyService.isSafe(player, level, desired), "rewind safety setup destination was not solid");

        TeleportTransition adjusted = RewindSafetyService.adjust(player, transition);
        require(adjusted.position().distanceToSqr(desired) > 1.0E-7D,
                "rewind kept a historical destination inside solid blocks");
        require(RewindSafetyService.isSafe(player, level, adjusted.position()),
                "rewind fallback destination is still colliding");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 100)
    public void timeAccelerationAppliesCanonicalRuntimeModifiersAfterFocus(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(TIME_ACCELERATION_ID, 1)
        );

        double movementBefore = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double attackBefore = player.getAttributeValue(Attributes.ATTACK_SPEED);
        float miningBefore = player.getDestroySpeed(Blocks.STONE.defaultBlockState());

        require(AdvancedActiveAugmentService.tryUse(player, TIME_ACCELERATION_ID),
                "time acceleration focus did not start");
        require(!AdvancedActiveAugmentService.isTimeAccelerationActive(player),
                "time acceleration became active before the three-second focus completed");

        GameTestPlayers.runAfterDelay(helper, 65, () -> {
            AdvancedActiveTuning.Snapshot tuning = AdvancedActiveTuning.current();
            require(AdvancedActiveAugmentService.isTimeAccelerationActive(player),
                    "time acceleration never entered its sustained state after focus");
            require(close(tuning.timeAccelerationMovementBonus(), 0.60D), "movement tuning drifted");
            require(close(tuning.timeAccelerationAttackSpeedBonus(), 0.60D), "attack-speed tuning drifted");
            require(close(tuning.timeAccelerationMiningBonus(), 0.60D), "mining tuning drifted");
            require(close(tuning.timeAccelerationItemUseSpeedMultiplier(), 1.40D),
                    "item-use tuning drifted from canonical x1.40");
            require(close(tuning.timeAccelerationProjectileSpeedMultiplier(), 1.40D),
                    "projectile tuning drifted from canonical x1.40");

            require(close(player.getAttributeValue(Attributes.MOVEMENT_SPEED), movementBefore * 1.60D),
                    "live movement-speed attribute did not receive the +60% modifier");
            require(close(player.getAttributeValue(Attributes.ATTACK_SPEED), attackBefore * 1.60D),
                    "live attack-speed attribute did not receive the +60% modifier");
            require(close(player.getDestroySpeed(Blocks.STONE.defaultBlockState()), miningBefore * 1.60D),
                    "live final destroy speed did not receive the +60% mining modifier");
            require(AdvancedActiveAugmentService.modifyUseDuration(player, 100) == 72,
                    "live item-use duration was not shortened by the canonical x1.40 multiplier");
            require(close(AdvancedActiveAugmentService.modifyOwnedProjectileSpeed(player, 10.0F), 14.0D),
                    "live owned-projectile speed was not multiplied by the canonical x1.40 multiplier");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest
    public void areaAccelerationGrowthSamplerPreservesThreeAndSevenTimesLongRunBudgets(GameTestHelper helper) {
        double x3 = AreaAccelerationGrowthSampler.exactExtraSamplesPerTick(3, 3);
        double x7 = AreaAccelerationGrowthSampler.exactExtraSamplesPerTick(7, 3);
        require(AreaAccelerationGrowthSampler.spherePointCount() > 0, "area acceleration sphere sampler is empty");
        require(x3 > 0.0D, "level-one area acceleration has no extra crop samples");
        require(close(x7, x3 * 3.0D), "x7 extra crop budget is not three times the x3 extra budget");
        require(close(AreaAccelerationGrowthSampler.exactExtraSamplesPerTick(1, 3), 0.0D),
                "inactive area acceleration still schedules crop samples");
        require(close(AreaAccelerationGrowthSampler.exactExtraSamplesPerTick(7, 0), 0.0D),
                "randomTickSpeed=0 was not respected");
        GameTestPlayers.succeed(helper);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= 1.0E-6D;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
