package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
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

/** Interaction regressions for protection-pool depletion and invulnerability ordering. */
public final class ProtectionInteractionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";
    private static final String TEST_SHIELD_POOL = "gametest_shield_burst";

    @GameTest
    public void shieldDepletionTriggersBurstExactlyOnce(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("shield_burst"), 1));
        LivingEntity hostile = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 1).withNoFreeWill().spawn();
        float hostileBefore = hostile.getHealth();
        float shieldAmount = 4.0F;

        ProtectionAugmentService.grantShield(player, TEST_SHIELD_POOL, shieldAmount, 100);
        float remaining = ProtectionAugmentService.modifyAfterReductions(
                player,
                player.damageSources().generic(),
                shieldAmount
        );

        requireClose(remaining, 0.0F, 1.0E-5F,
                "depleted shield leaked damage past its owned pool");
        requireClose(ProtectionAugmentService.poolAmount(
                        player, ProtectionAugmentService.PoolKind.SHIELD, TEST_SHIELD_POOL),
                0.0F, 1.0E-5F,
                "fully depleted shield pool was not removed");
        require(hostile.getHealth() < hostileBefore,
                "full shield depletion did not trigger shield burst");

        float afterFirstBurst = hostile.getHealth();
        float unprotected = ProtectionAugmentService.modifyAfterReductions(
                player,
                player.damageSources().generic(),
                1.0F
        );
        requireClose(unprotected, 1.0F, 1.0E-5F,
                "removed shield pool still consumed later damage");
        requireClose(hostile.getHealth(), afterFirstBurst, 1.0E-5F,
                "shield burst fired more than once after the pool was removed");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 500)
    public void invulnerabilityBlocksDelayedDamageReservation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("invincibility"), 1)
                .withAugmentLevel(id("delayed_damage"), 1));
        ServerLevel level = (ServerLevel) player.level();
        int activationDelay = ReactiveCombatTuning.current().invulnerabilityIntervalTicks() + 1;
        int delayedWindow = CombatTuning.current().delayedDamageIntervalTicks()
                * CombatTuning.current().delayedDamageInstallmentCount();
        int finalCheckTick = activationDelay + delayedWindow + 5;
        require(finalCheckTick < 500, "protection tuning exceeded the GameTest timing budget");

        final float[] healthBefore = new float[1];
        GameTestPlayers.runAfterDelay(helper, activationDelay, () -> {
            require(ReactiveCombatAugmentService.isInvulnerable(player),
                    "invincibility window did not open at its configured interval");
            healthBefore[0] = player.getHealth();
            player.hurtServer(level, player.damageSources().generic(), 8.0F);
            requireClose(player.getHealth(), healthBefore[0], 1.0E-5F,
                    "invincibility allowed the initial hit through");

            AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                    .withAugmentLevel(id("delayed_damage"), 1));
            require(!ReactiveCombatAugmentService.isInvulnerable(player),
                    "invincibility runtime remained active after the augment was removed");
        });

        GameTestPlayers.runAfterDelay(helper, finalCheckTick, () -> {
            requireClose(player.getHealth(), healthBefore[0], 1.0E-5F,
                    "blocked invincibility hit reserved delayed-damage installments");
            GameTestPlayers.succeed(helper);
        });
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static String id(String path) {
        return PREFIX + path;
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
