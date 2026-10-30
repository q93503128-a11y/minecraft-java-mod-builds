package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Regression coverage for replacement-projectile Return to Sender at the blocked-damage boundary. */
public final class ReturnToSenderGameTests implements CustomTestMethodInvoker {
    private static final String RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    private static final double INPUT_SPEED = 2.0D;
    private static final double[] EXPECTED_SCALES = {0.60D, 0.80D, 1.00D};

    @GameTest(maxTicks = 40)
    public void activeShieldBlockedDamageEventReplacesIncomingArrowAndRelaunchesClone(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, 3)
        );
        player.startUsingItem(InteractionHand.OFF_HAND);

        LivingEntity skeleton = helper.spawnMob(EntityTypes.SKELETON, 3, 2, 8)
                .withNoFreeWill()
                .spawn();
        Vec3 blockDirection = player.getLookAngle().normalize();

        GameTestPlayers.runAfterDelay(helper, 6, () -> {
            require(player.isUsingItem() && player.getUseItem().is(Items.SHIELD),
                    "shield-use state was not active at the blocked-damage integration boundary");
            Arrow original = new Arrow(
                    player.level(),
                    player.getX(),
                    player.getEyeY(),
                    player.getZ() + 2.0D,
                    Items.ARROW.getDefaultInstance(),
                    null
            );
            original.setOwner(skeleton);
            original.setDeltaMovement(blockDirection.scale(-INPUT_SPEED));
            require(player.level().addFreshEntity(original), "incoming shield-test arrow failed to spawn");

            DamageSource source = player.damageSources().arrow(original, skeleton);
            ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(
                    player,
                    source,
                    (float) INPUT_SPEED,
                    0.0F,
                    true
            );

            require(original.isRemoved(),
                    "blocked projectile object was not discarded by Return to Sender");
            List<Arrow> reflected = player.level().getEntitiesOfClass(
                    Arrow.class,
                    player.getBoundingBox().inflate(20.0D),
                    candidate -> candidate.getOwner() == player
            );
            require(!reflected.isEmpty(),
                    "blocked-damage event did not spawn a player-owned replacement arrow");
            Arrow replacement = reflected.getFirst();
            require(replacement != original,
                    "Return to Sender reused the collision object instead of replacing it");
            require(replacement.getDeltaMovement().length() > 1.5D,
                    "replacement arrow lost the level-three reflection speed");
            require(replacement.getDeltaMovement().normalize().dot(blockDirection) > 0.95D,
                    "replacement arrow did not travel along the block-time look direction");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 20)
    public void reflectedClonePreservesLevelSpeedScaling(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        LivingEntity skeleton = helper.spawnMob(EntityTypes.SKELETON, 2, 2, 7)
                .withNoFreeWill()
                .spawn();
        Vec3 blockDirection = player.getLookAngle().normalize();

        for (int index = 0; index < EXPECTED_SCALES.length; index++) {
            int level = index + 1;
            AugmentationPlayerStateService.replace(
                    player,
                    AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, level)
            );
            Arrow original = new Arrow(
                    player.level(),
                    player.getX(),
                    player.getEyeY(),
                    player.getZ() + 1.5D,
                    Items.ARROW.getDefaultInstance(),
                    null
            );
            original.setOwner(skeleton);
            original.setDeltaMovement(blockDirection.scale(-INPUT_SPEED));
            require(player.level().addFreshEntity(original), "scale-test arrow failed to enter the level");

            DamageSource source = player.damageSources().arrow(original, skeleton);
            ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(
                    player,
                    source,
                    (float) INPUT_SPEED,
                    0.0F,
                    true
            );
            require(original.isRemoved(),
                    "blocked projectile object survived clone replacement at level " + level);

            double expectedSpeed = INPUT_SPEED * EXPECTED_SCALES[index];
            Arrow replacement = player.level().getEntitiesOfClass(
                            Arrow.class,
                            player.getBoundingBox().inflate(6.0D),
                            candidate -> candidate.getOwner() == player
                                    && close(candidate.getDeltaMovement().length(), expectedSpeed, 1.0E-6D)
                    ).stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "no reflected clone with expected speed at level " + level));
            require(replacement.getDeltaMovement().normalize().dot(blockDirection) > 0.999D,
                    "level " + level + " reflected clone did not follow block-time aim");
            replacement.discard();
        }
        GameTestPlayers.succeed(helper);
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
