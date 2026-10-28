package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Regression for the real Fabric blocked-damage boundary used by Return to Sender. */
public final class ReturnToSenderGameTests implements CustomTestMethodInvoker {
    private static final String RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    private static final double INPUT_SPEED = 2.0D;
    private static final double[] EXPECTED_SCALES = {0.60D, 0.80D, 1.00D};

    @GameTest(maxTicks = 20)
    public void blockedSkeletonArrowUsesBlockTimeAimAndLevelSpeed(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);

        LivingEntity skeleton = helper.spawnMob(EntityTypes.SKELETON, 2, 2, 7)
                .withNoFreeWill()
                .spawn();
        Vec3 blockDirection = player.getLookAngle().normalize();
        Arrow[] arrows = new Arrow[EXPECTED_SCALES.length];

        for (int index = 0; index < EXPECTED_SCALES.length; index++) {
            int level = index + 1;
            AugmentationPlayerStateService.replace(
                    player,
                    AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, level)
            );

            Arrow arrow = new Arrow(
                    player.level(),
                    player.getX(),
                    player.getEyeY(),
                    player.getZ() + 1.5D + index * 0.25D,
                    Items.ARROW.getDefaultInstance(),
                    null
            );
            arrow.setOwner(skeleton);
            arrow.setDeltaMovement(blockDirection.scale(-INPUT_SPEED));
            require(player.level().addFreshEntity(arrow), "test arrow failed to enter the level");
            arrows[index] = arrow;

            DamageSource source = player.damageSources().arrow(arrow, skeleton);
            ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(
                    player,
                    source,
                    INPUT_SPEED,
                    0.0F,
                    true
            );

            double expectedSpeed = INPUT_SPEED * EXPECTED_SCALES[index];
            require(arrow.getOwner() == player,
                    "blocked arrow owner was not transferred to the defending player at level " + level);
            require(ReactiveCombatAugmentService.isReflectedProjectile(arrow),
                    "blocked arrow was not marked reflected at level " + level);
            require(close(arrow.getDeltaMovement().length(), expectedSpeed, 1.0E-6D),
                    "level " + level + " reflected speed drifted from the 60/80/100 percent contract");
            require(arrow.getDeltaMovement().normalize().dot(blockDirection) > 0.999D,
                    "level " + level + " immediate reflection did not follow the block-time look direction");

            // Vanilla AbstractArrow collision logic can still mutate velocity after AFTER_DAMAGE.
            // Recreate that overwrite so the deferred end-of-tick deflection must restore it.
            arrow.setDeltaMovement(blockDirection.scale(-0.25D));
        }

        // Turning after the shield block must not retarget already-blocked arrows.
        player.setYRot(180.0F);

        GameTestPlayers.runAfterDelay(helper, 1, () -> {
            for (int index = 0; index < arrows.length; index++) {
                Arrow arrow = arrows[index];
                int level = index + 1;
                Vec3 velocity = arrow.getDeltaMovement();
                require(!arrow.isRemoved(), "reflected arrow disappeared before deferred deflection at level " + level);
                require(arrow.getOwner() == player,
                        "deferred reflection lost player ownership at level " + level);
                require(velocity.horizontalDistanceSqr() > 1.0E-7D,
                        "deferred reflection produced no horizontal velocity at level " + level);
                Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z).normalize();
                Vec3 expectedHorizontal = new Vec3(blockDirection.x, 0.0D, blockDirection.z).normalize();
                require(horizontal.dot(expectedHorizontal) > 0.98D,
                        "deferred reflection followed the later look direction instead of block-time aim at level " + level);
                double expectedHorizontalSpeed = INPUT_SPEED * EXPECTED_SCALES[index];
                require(Math.abs(Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
                                - expectedHorizontalSpeed) < 0.20D,
                        "deferred reflection did not restore the expected level speed at level " + level);
            }
            GameTestPlayers.succeed(helper);
        });
    }

    private static boolean close(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
