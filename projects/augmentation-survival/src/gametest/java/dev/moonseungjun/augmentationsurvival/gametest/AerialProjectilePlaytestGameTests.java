package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementTuning;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileTuning;
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
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Real runtime regressions for the 4-A aerial-movement and tracking-arrow playtest bundle. */
public final class AerialProjectilePlaytestGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void doubleJumpUsesStrengthenedVanillaJumpImpulseAndConsumesOnce(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("double_jump"), 1)
        );

        BlockPos air = helper.absolutePos(new BlockPos(2, 6, 2));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(0.12D, -0.10D, 0.08D);

        float vanillaJumpPower = ((LivingEntityJumpPowerAccessor) player)
                .augmentationSurvival$invokeGetJumpPower();
        double expectedVertical = vanillaJumpPower * MovementTuning.current().doubleJumpVerticalMultiplier();

        require(MovementAugmentService.tryAirJump(player),
                "owned double jump rejected its first airborne use");
        requireClose(player.getDeltaMovement().y, expectedVertical, 1.0E-6D,
                "double jump did not use the strengthened configured vertical impulse");
        requireClose(player.getDeltaMovement().x, 0.12D, 1.0E-9D,
                "double jump unexpectedly changed horizontal X momentum");
        requireClose(player.getDeltaMovement().z, 0.08D, 1.0E-9D,
                "double jump unexpectedly changed horizontal Z momentum");
        require(!MovementAugmentService.tryAirJump(player),
                "double jump allowed a second airborne use without a recharge");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void aerialComboWorksWithoutDoubleJumpAndConsumesHeldRecharge(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("aerial_combo"), 1)
        );
        require(AugmentationPlayerStateService.state(player).level(id("double_jump")) == 0,
                "aerial combo standalone test accidentally owns double jump");

        BlockPos air = helper.absolutePos(new BlockPos(2, 5, 2));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        MovementAugmentService.setJumpHeld(player, true);

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 2, 5, 3)
                .withNoFreeWill()
                .spawn();
        zombie.setHealth(zombie.getMaxHealth());
        float before = zombie.getHealth();

        player.attack(zombie);

        require(zombie.getHealth() < before,
                "aerial combo test melee attack did not damage its hostile target");
        require(player.getDeltaMovement().y > 0.0D,
                "airborne ROOT melee hit did not immediately consume held aerial-combo recharge");
        double expectedMinimum = ((LivingEntityJumpPowerAccessor) player)
                .augmentationSurvival$invokeGetJumpPower()
                * MovementTuning.current().doubleJumpVerticalMultiplier() - 1.0E-6D;
        require(player.getDeltaMovement().y >= expectedMinimum,
                "standalone aerial combo used a weaker-than-air-jump vertical impulse");

        MovementAugmentService.setJumpHeld(player, false);
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void aerialComboStoresRechargeUntilLaterJumpWhenNotHeld(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("aerial_combo"), 1)
        );

        BlockPos air = helper.absolutePos(new BlockPos(4, 6, 4));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.05D, 0.0D);
        MovementAugmentService.setJumpHeld(player, false);

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 4, 6, 5)
                .withNoFreeWill()
                .spawn();
        player.attack(zombie);
        require(player.getDeltaMovement().y <= 0.0D,
                "aerial combo auto-jumped even though jump was not held");
        require(MovementAugmentService.tryAirJump(player),
                "airborne aerial-combo recharge was erased before a later jump press");
        require(!MovementAugmentService.tryAirJump(player),
                "one aerial-combo hit granted more than one stored air jump");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void trackingArrowCorrectsTowardMovingTargetWithoutSnapTurn(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("tracking_arrow"), 3)
        );

        BlockPos origin = helper.absolutePos(new BlockPos(2, 5, 2));
        Arrow arrow = new Arrow(
                player.level(),
                origin.getX() + 0.5D,
                origin.getY() + 0.5D,
                origin.getZ() + 0.5D,
                Items.ARROW.getDefaultInstance(),
                null
        );
        arrow.setOwner(player);
        arrow.setDeltaMovement(0.0D, 0.0D, 2.0D);
        require(player.level().addFreshEntity(arrow), "tracking test arrow failed to enter the level");

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 6, 5, 10)
                .withNoFreeWill()
                .spawn();
        Vec3 before = arrow.getDeltaMovement().normalize();
        Vec3 desiredBefore = zombie.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double dotBefore = before.dot(desiredBefore);

        ProjectileAugmentService.beforeArrowTick(arrow);
        Vec3 first = arrow.getDeltaMovement().normalize();
        double firstTurn = angleDegrees(before, first);
        double maxTurn = ProjectileTuning.current().trackingTurnDegrees(3);
        require(firstTurn > 0.05D,
                "tracking arrow did not correct toward an eligible off-axis target");
        require(firstTurn <= maxTurn + 1.0E-4D,
                "tracking arrow snapped farther than its per-tick turn limit: " + firstTurn);
        require(first.dot(desiredBefore) > dotBefore,
                "tracking arrow correction did not improve aim toward the target");
        requireClose(arrow.getDeltaMovement().length(), 2.0D, 1.0E-6D,
                "tracking correction changed projectile speed");

        zombie.setPos(zombie.getX() + 1.5D, zombie.getY(), zombie.getZ());
        Vec3 desiredMoved = zombie.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double movedBefore = first.dot(desiredMoved);
        ProjectileAugmentService.beforeArrowTick(arrow);
        Vec3 second = arrow.getDeltaMovement().normalize();
        double secondTurn = angleDegrees(first, second);
        require(secondTurn <= maxTurn + 1.0E-4D,
                "tracking arrow exceeded turn cap while following a moved target: " + secondTurn);
        require(second.dot(desiredMoved) > movedBefore,
                "tracking arrow did not continue correcting toward the moved target");

        zombie.setPos(arrow.getX(), arrow.getY(), arrow.getZ() - 6.0D);
        Vec3 beforeBehind = arrow.getDeltaMovement().normalize();
        ProjectileAugmentService.beforeArrowTick(arrow);
        Vec3 afterBehind = arrow.getDeltaMovement().normalize();
        require(angleDegrees(beforeBehind, afterBehind) < 1.0E-4D,
                "tracking arrow performed a snap turn toward a target behind its acquisition cone");

        GameTestPlayers.succeed(helper);
    }

    private static double angleDegrees(Vec3 first, Vec3 second) {
        double dot = Math.max(-1.0D, Math.min(1.0D, first.normalize().dot(second.normalize())));
        return Math.toDegrees(Math.acos(dot));
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireClose(double actual, double expected, double tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
