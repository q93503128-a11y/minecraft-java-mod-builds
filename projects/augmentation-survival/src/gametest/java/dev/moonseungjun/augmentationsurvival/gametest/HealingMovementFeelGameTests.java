package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementTuning;
import dev.moonseungjun.augmentationsurvival.mixin.LivingEntityJumpPowerAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Bundle-seven regressions for live heal entry and Double Jump movement feel invariants. */
public final class HealingMovementFeelGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void lifeWaveTriggersFromVanillaHealBoundaryUsingActualHpRestored(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("life_wave"), 1)
        );
        player.setHealth(player.getMaxHealth() - 4.0F);
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 3).withNoFreeWill().spawn();
        player.setPos(target.getX() - 3.0D, target.getY(), target.getZ());
        float healthBefore = player.getHealth();
        float targetBefore = target.getHealth();

        // Enter through LivingEntity.heal, not AugmentHealingService.healPrimary.
        player.heal(4.0F);

        require(player.getHealth() > healthBefore,
                "vanilla heal boundary did not restore player HP");
        require(target.getHealth() < targetBefore,
                "Life Wave did not react to a positive non-natural heal entering through LivingEntity.heal");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void stationaryDoubleJumpUsesConfiguredVerticalImpulseWithoutForcedForwardMotion(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("double_jump"), 1)
        );
        BlockPos air = helper.absolutePos(new BlockPos(4, 7, 4));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.12D, 0.0D);
        player.setYRot(137.0F);

        double expectedVertical = ((LivingEntityJumpPowerAccessor) player)
                .augmentationSurvival$invokeGetJumpPower()
                * MovementTuning.current().doubleJumpVerticalMultiplier();
        require(MovementAugmentService.tryAirJump(player),
                "owned Double Jump rejected its stationary airborne use");

        Vec3 after = player.getDeltaMovement();
        require(Math.abs(after.x) < 1.0E-8D && Math.abs(after.z) < 1.0E-8D,
                "stationary Double Jump injected forced horizontal/look-direction movement");
        require(Math.abs(after.y - expectedVertical) < 1.0E-6D,
                "stationary Double Jump did not use actual jump power times the configured vertical multiplier");
        GameTestPlayers.succeed(helper);
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
