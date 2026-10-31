package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Regression coverage for durability-counted Return to Sender using the real raised-shield path. */
public final class ReturnToSenderGameTests implements CustomTestMethodInvoker {
    private static final String RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    private static final int[] EXPECTED_BLOCK_COUNTS = {5, 4, 3};
    private static final double[] EXPECTED_FRACTIONS = {0.60D, 0.80D, 1.00D};
    private static final float DURABILITY_CONSUMING_HIT = 6.0F;

    @GameTest(maxTicks = 80)
    public void realShieldDurabilityBlocksEmitConeAtLevelThreeThreshold(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, 3)
        );
        equipRaisedShield(player);

        LivingEntity attacker = helper.spawnMob(EntityTypes.COW, 3, 2, 7).withNoFreeWill().spawn();
        LivingEntity insideCone = helper.spawnMob(EntityTypes.COW, 4, 2, 6).withNoFreeWill().spawn();
        LivingEntity outsideCone = helper.spawnMob(EntityTypes.COW, 6, 2, 5).withNoFreeWill().spawn();

        GameTestPlayers.runAfterDelay(helper, 8, () -> {
            float attackerBefore = attacker.getHealth();
            float insideBefore = insideCone.getHealth();
            float outsideBefore = outsideCone.getHealth();

            blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
            blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
            require(close(attacker.getHealth(), attackerBefore, 0.001D),
                    "level-three Return to Sender fired before three durability-consuming blocks");

            blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
            require(close(attackerBefore - attacker.getHealth(), 18.0D, 0.05D),
                    "three durability-consuming blocks did not return the accumulated blocked damage");
            require(close(insideBefore - insideCone.getHealth(), 18.0D, 0.05D),
                    "target inside the 45-degree cone was not hit by the shockwave");
            require(close(outsideCone.getHealth(), outsideBefore, 0.001D),
                    "target outside the 45-degree cone was incorrectly hit");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 120)
    public void realShieldReturnPreservesFiveFourThreeThresholdsAndDamageFractions(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        LivingEntity attacker = helper.spawnMob(EntityTypes.COW, 2, 2, 6).withNoFreeWill().spawn();

        GameTestPlayers.runAfterDelay(helper, 8, () -> {
            for (int index = 0; index < EXPECTED_FRACTIONS.length; index++) {
                int augmentLevel = index + 1;
                int requiredBlocks = EXPECTED_BLOCK_COUNTS[index];
                AugmentationPlayerStateService.replace(
                        player,
                        AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, augmentLevel)
                );
                equipRaisedShield(player);
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                float before = attacker.getHealth();

                for (int block = 1; block <= requiredBlocks; block++) {
                    blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
                    if (block < requiredBlocks) {
                        require(close(attacker.getHealth(), before, 0.001D),
                                "level " + augmentLevel + " fired before block " + requiredBlocks);
                    }
                }

                double expected = requiredBlocks * DURABILITY_CONSUMING_HIT * EXPECTED_FRACTIONS[index];
                double dealt = before - attacker.getHealth();
                require(close(dealt, expected, 0.05D),
                        "level " + augmentLevel + " reflected damage mismatch: expected="
                                + expected + " actual=" + dealt);
            }
            GameTestPlayers.succeed(helper);
        });
    }

    private static void equipRaisedShield(ServerPlayer player) {
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        player.startUsingItem(InteractionHand.OFF_HAND);
    }

    private static void blockHit(ServerLevel level, ServerPlayer player, LivingEntity attacker, float damage) {
        int durabilityBefore = player.getOffhandItem().getDamageValue();
        player.invulnerableTime = 0;
        float healthBefore = player.getHealth();
        player.hurtServer(level, player.damageSources().mobAttack(attacker), damage);
        require(close(player.getHealth(), healthBefore, 0.001D),
                "test hit was not fully blocked by the raised shield");
        require(player.getOffhandItem().getDamageValue() > durabilityBefore,
                "test shield block did not consume durability");
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
