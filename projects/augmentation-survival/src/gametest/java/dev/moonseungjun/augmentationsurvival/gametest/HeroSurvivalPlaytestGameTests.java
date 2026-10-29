package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.AugmentHealingService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
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

/** Real runtime regressions for the 4-B survival and six-block pulse playtest bundle. */
public final class HeroSurvivalPlaytestGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 100)
    public void lastStandSurvivesLethalHitForFullWindowAndDoesNotRetriggerOnCooldown(GameTestHelper helper) {
        ServerPlayer player = player(helper, 2, 2);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("last_stand"), 1)
        );
        ServerLevel level = (ServerLevel) player.level();
        long triggeredAt = level.getGameTime();

        lethalDamage(player);

        require(!player.isDeadOrDying(), "last stand allowed the triggering lethal hit to kill the player");
        requireClose(player.getHealth(), 1.0F, 1.0E-6F,
                "last stand did not leave the player at exactly 1 HP");
        long readyTick = AugmentationPlayerStateService.state(player).cooldownReadyTick(id("last_stand"));
        require(readyTick >= triggeredAt + CombatTuning.current().lastStandCooldownTicks(),
                "last stand did not consume its full cooldown");

        player.hurtServer(level, player.damageSources().generic(), 2.0F);
        requireClose(player.getHealth(), 1.0F, 1.0E-6F,
                "last stand did not block ordinary damage immediately after triggering");

        int invulnerableTicks = CombatTuning.current().lastStandInvulnerableTicks();
        require(invulnerableTicks == 60,
                "last stand playtest contract drifted from the intended 3-second window");

        GameTestPlayers.runAfterDelay(helper, invulnerableTicks - 1L, () -> {
            player.setHealth(5.0F);
            player.hurtServer(level, player.damageSources().generic(), 1.0F);
            requireClose(player.getHealth(), 5.0F, 1.0E-6F,
                    "last stand invulnerability ended before the 3-second window completed");

            GameTestPlayers.runAfterDelay(helper, 2L, () -> {
                player.setHealth(5.0F);
                player.hurtServer(level, player.damageSources().generic(), 1.0F);
                require(player.getHealth() < 5.0F,
                        "last stand invulnerability remained active after its 3-second window");

                player.setHealth(2.0F);
                lethalDamage(player);
                require(player.isDeadOrDying(),
                        "last stand retriggered while its 180-second cooldown was still active");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    @GameTest
    public void shieldBurstActualPoolDepletionUsesSixBlockDamageBoundary(GameTestHelper helper) {
        ServerPlayer player = player(helper, 3, 3);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("shield_burst"), 1)
        );
        double radius = ReactiveCombatTuning.current().shieldBurstRadius();
        requireClose((float) radius, 6.0F, 1.0E-6F,
                "shield burst playtest contract drifted from six blocks");

        LivingEntity inside = zombieAt(helper, player.getX() + radius - 0.25D, player.getY(), player.getZ());
        LivingEntity outside = zombieAt(helper, player.getX() + radius + 0.25D, player.getY(), player.getZ());
        float insideBefore = inside.getHealth();
        float outsideBefore = outside.getHealth();
        float playerBefore = player.getHealth();

        ProtectionAugmentService.grantShield(
                player,
                "gametest_shield_burst",
                4.0F,
                200
        );
        player.hurtServer((ServerLevel) player.level(), player.damageSources().generic(), 4.0F);

        requireClose(player.getHealth(), playerBefore, 1.0E-6F,
                "shield depletion leaked the absorbed hit into player HP");
        requireClose(
                ProtectionAugmentService.poolAmount(
                        player,
                        ProtectionAugmentService.PoolKind.SHIELD,
                        "gametest_shield_burst"
                ),
                0.0F,
                1.0E-6F,
                "depleted shield pool remained after the burst"
        );
        require(inside.getHealth() < insideBefore,
                "shield burst failed to damage a hostile just inside its six-block radius");
        requireClose(outside.getHealth(), outsideBefore, 1.0E-6F,
                "shield burst damaged a hostile just outside its six-block radius");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void lifeWaveActualPrimaryHealingUsesSixBlockDamageBoundary(GameTestHelper helper) {
        ServerPlayer player = player(helper, 3, 3);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("life_wave"), 1)
        );
        double radius = ExpansionTuning.current().lifeWaveRadius();
        requireClose((float) radius, 6.0F, 1.0E-6F,
                "life wave playtest contract drifted from six blocks");

        LivingEntity inside = zombieAt(helper, player.getX(), player.getY(), player.getZ() + radius - 0.25D);
        LivingEntity outside = zombieAt(helper, player.getX(), player.getY(), player.getZ() + radius + 0.25D);
        float insideBefore = inside.getHealth();
        float outsideBefore = outside.getHealth();
        player.setHealth(player.getMaxHealth() - 5.0F);
        float beforeHealing = player.getHealth();

        float actual = AugmentHealingService.healPrimary(player, 4.0F);

        requireClose(actual, 4.0F, 1.0E-6F,
                "life wave test did not produce the expected real primary healing amount");
        requireClose(player.getHealth(), beforeHealing + 4.0F, 1.0E-6F,
                "primary healing did not reach player HP before life wave resolution");
        require(inside.getHealth() < insideBefore,
                "life wave failed to damage a hostile just inside its six-block radius");
        requireClose(outside.getHealth(), outsideBefore, 1.0E-6F,
                "life wave damaged a hostile just outside its six-block radius");

        GameTestPlayers.succeed(helper);
    }

    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 3, z));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static LivingEntity zombieAt(GameTestHelper helper, double x, double y, double z) {
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 1, 3, 1).withNoFreeWill().spawn();
        zombie.setPos(x, y, z);
        zombie.setHealth(zombie.getMaxHealth());
        return zombie;
    }

    private static void lethalDamage(ServerPlayer player) {
        player.hurtServer(
                (ServerLevel) player.level(),
                player.damageSources().generic(),
                player.getMaxHealth() * 20.0F
        );
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
