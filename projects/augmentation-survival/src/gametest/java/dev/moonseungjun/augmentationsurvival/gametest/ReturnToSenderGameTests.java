package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionDurabilityContext;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

/** Regression coverage for durability-counted Return to Sender at vanilla's shield durability boundary. */
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
        equipShield(player);

        LivingEntity attacker = durableCow(helper, 3, 2, 7);
        LivingEntity insideCone = durableCow(helper, 4, 2, 6);
        LivingEntity outsideCone = durableCow(helper, 6, 2, 5);

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(!player.hasInfiniteMaterials(), "survival test player unexpectedly has infinite materials");
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
        equipShield(player);
        LivingEntity attacker = durableCow(helper, 2, 2, 6);
        Vec3 attackerStart = attacker.position();

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(!player.hasInfiniteMaterials(), "survival test player unexpectedly has infinite materials");
            for (int index = 0; index < EXPECTED_FRACTIONS.length; index++) {
                int augmentLevel = index + 1;
                int requiredBlocks = EXPECTED_BLOCK_COUNTS[index];
                AugmentationPlayerStateService.replace(
                        player,
                        AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, augmentLevel)
                );
                attacker.setPos(attackerStart.x, attackerStart.y, attackerStart.z);
                attacker.setDeltaMovement(Vec3.ZERO);
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

    private static LivingEntity durableCow(GameTestHelper helper, int x, int y, int z) {
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, x, y, z).withNoFreeWill().spawn();
        var maxHealth = cow.getAttribute(Attributes.MAX_HEALTH);
        require(maxHealth != null, "test cow is missing max-health attribute");
        maxHealth.setBaseValue(100.0D);
        cow.setHealth(cow.getMaxHealth());
        return cow;
    }

    private static void equipShield(ServerPlayer player) {
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
    }

    private static void blockHit(ServerLevel level, ServerPlayer player, LivingEntity attacker, float damage) {
        ItemStack shield = player.getOffhandItem();
        BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
        require(blocksAttacks != null, "test shield is missing vanilla blocks-attacks component");
        DamageSource source = player.damageSources().mobAttack(attacker);
        float blocked = blocksAttacks.resolveBlockedDamage(source, damage, 0.0D);
        require(blocked >= 3.0F,
                "shield component did not resolve durability-consuming blocked damage: blocked=" + blocked);

        int durabilityBefore = shield.getDamageValue();
        ShieldCompressionDurabilityContext.begin(player, source, shield);
        try {
            blocksAttacks.hurtBlockingItem(level, shield, player, InteractionHand.OFF_HAND, blocked);
        } finally {
            ShieldCompressionDurabilityContext.end(player);
        }
        require(shield.getDamageValue() > durabilityBefore,
                "vanilla shield durability boundary did not consume durability");
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
