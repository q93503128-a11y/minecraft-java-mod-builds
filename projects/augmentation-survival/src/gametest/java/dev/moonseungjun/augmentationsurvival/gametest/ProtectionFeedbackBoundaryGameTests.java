package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

/** Protection regressions that complement the existing real-hit shield/delayed-damage tests. */
public final class ProtectionFeedbackBoundaryGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";
    private static final String EXPIRATION_POOL = "expiration-regression";

    @GameTest(maxTicks = 28)
    public void shieldExpirationTriggersBurstExactlyOnce(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("shield_burst"), 1)
        );
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 3).withNoFreeWill().spawn();
        player.setPos(target.getX() - 2.0D, target.getY(), target.getZ());

        // Let pack-wide threat scaling settle before taking the damage snapshot. The pool itself is
        // the authoritative exactly-once boundary; raw hostile HP can legitimately be refreshed by
        // parallel threat-stage tests after the burst has already fired.
        GameTestPlayers.runAfterDelay(helper, 5L, () -> {
            target.setHealth(target.getMaxHealth());
            float before = target.getHealth();
            ProtectionAugmentService.grantShield(player, EXPIRATION_POOL, 4.0F, 1);

            GameTestPlayers.runAfterDelay(helper, 5L, () -> {
                float afterBurst = target.getHealth();
                require(afterBurst < before,
                        "expiring augment shield did not trigger Shield Burst");
                require(ProtectionAugmentService.poolAmount(
                                player, ProtectionAugmentService.PoolKind.SHIELD, EXPIRATION_POOL
                        ) <= 1.0E-5F,
                        "expired shield pool remained after Shield Burst");

                target.invulnerableTime = 0;
                GameTestPlayers.runAfterDelay(helper, 4L, () -> {
                    require(ProtectionAugmentService.poolAmount(
                                    player, ProtectionAugmentService.PoolKind.SHIELD, EXPIRATION_POOL
                            ) <= 1.0E-5F,
                            "expired shield pool was recreated after its burst");
                    require(target.getHealth() + 1.0E-5F >= afterBurst,
                            "one shield expiration applied additional Shield Burst damage later");
                    GameTestPlayers.succeed(helper);
                });
            });
        });
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
