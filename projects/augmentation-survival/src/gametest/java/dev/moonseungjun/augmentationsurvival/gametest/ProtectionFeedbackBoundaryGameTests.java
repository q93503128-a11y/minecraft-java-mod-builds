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

    @GameTest(maxTicks = 24)
    public void shieldExpirationTriggersBurstExactlyOnce(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("shield_burst"), 1)
        );
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 3).withNoFreeWill().spawn();
        player.setPos(target.getX() - 2.0D, target.getY(), target.getZ());

        // Let global threat-stage scaling finish before taking the health baseline. Otherwise a
        // legitimate max-health refresh can look like a second Shield Burst in this parallel suite.
        GameTestPlayers.runAfterDelay(helper, 5L, () -> {
            target.setHealth(target.getMaxHealth());
            float before = target.getHealth();
            ProtectionAugmentService.grantShield(player, "expiration-regression", 4.0F, 1);

            GameTestPlayers.runAfterDelay(helper, 3L, () -> {
                float afterBurst = target.getHealth();
                require(afterBurst < before,
                        "expiring augment shield did not trigger Shield Burst");
                GameTestPlayers.runAfterDelay(helper, 4L, () -> {
                    require(Math.abs(target.getHealth() - afterBurst) < 1.0E-5F,
                            "one shield expiration triggered Shield Burst more than once");
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
