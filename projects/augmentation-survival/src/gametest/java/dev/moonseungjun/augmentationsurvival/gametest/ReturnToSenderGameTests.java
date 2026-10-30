package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
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

/** Regression coverage for blocked-damage compression Return to Sender. */
public final class ReturnToSenderGameTests implements CustomTestMethodInvoker {
    private static final String RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    private static final double[] EXPECTED_FRACTIONS = {0.60D, 0.80D, 1.00D};

    @GameTest(maxTicks = 40)
    public void blockedDamageThresholdEmitsConeTowardAttacker(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, 3)
        );

        // Passive animals are used deliberately so global hostile threat scaling cannot mutate HP mid-assertion.
        LivingEntity attacker = helper.spawnMob(EntityTypes.COW, 3, 2, 7)
                .withNoFreeWill()
                .spawn();
        LivingEntity insideCone = helper.spawnMob(EntityTypes.COW, 4, 2, 6)
                .withNoFreeWill()
                .spawn();
        LivingEntity outsideCone = helper.spawnMob(EntityTypes.COW, 6, 2, 5)
                .withNoFreeWill()
                .spawn();

        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            attacker.setHealth(attacker.getMaxHealth());
            insideCone.setHealth(insideCone.getMaxHealth());
            outsideCone.setHealth(outsideCone.getMaxHealth());
            attacker.invulnerableTime = 0;
            insideCone.invulnerableTime = 0;
            outsideCone.invulnerableTime = 0;
            float attackerBefore = attacker.getHealth();
            float insideBefore = insideCone.getHealth();
            float outsideBefore = outsideCone.getHealth();
            DamageSource source = player.damageSources().mobAttack(attacker);

            ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(player, source, 6.0F, 0.0F, true);
            require(close(attacker.getHealth(), attackerBefore, 0.001D),
                    "Return to Sender fired before 10 blocked damage was accumulated");

            ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(player, source, 4.0F, 0.0F, true);
            require(attacker.getHealth() < attackerBefore - 9.5F,
                    "10 blocked damage did not return a level-three shockwave to the attacker");
            require(insideCone.getHealth() < insideBefore - 9.5F,
                    "non-allied target inside the forward cone was not hit by the shockwave");
            require(close(outsideCone.getHealth(), outsideBefore, 0.001D),
                    "target outside the 45-degree cone was incorrectly hit");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 80)
    public void compressedWavePreservesLevelDamageFractions(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        LivingEntity attacker = helper.spawnMob(EntityTypes.COW, 2, 2, 6)
                .withNoFreeWill()
                .spawn();

        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            for (int index = 0; index < EXPECTED_FRACTIONS.length; index++) {
                int level = index + 1;
                AugmentationPlayerStateService.replace(
                        player,
                        AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, level)
                );
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                float before = attacker.getHealth();
                DamageSource source = player.damageSources().mobAttack(attacker);
                ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(
                        player,
                        source,
                        10.0F,
                        0.0F,
                        true
                );
                float dealt = before - attacker.getHealth();
                double expected = 10.0D * EXPECTED_FRACTIONS[index];
                require(close(dealt, expected, 0.05D),
                        "level " + level + " shockwave damage mismatch: expected=" + expected + " actual=" + dealt);
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
