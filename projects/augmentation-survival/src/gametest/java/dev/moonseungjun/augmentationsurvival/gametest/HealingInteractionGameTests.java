package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.AugmentHealingService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Interaction regressions for amplification, overflow, sharing, and shared-heal recursion. */
public final class HealingInteractionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void amplificationOverflowAndShareUseActualPrimaryHealing(GameTestHelper helper) {
        ServerPlayer source = player(helper, 1.5D);
        ServerPlayer ally = player(helper, 2.5D);
        makeAllies(source, ally);

        AugmentationPlayerStateService.replace(source, AugmentationPlayerState.initial()
                .withAugmentLevel(id("healing_amplification"), 1)
                .withAugmentLevel(id("overflow_healing"), 1)
                .withAugmentLevel(id("healing_share"), 1));

        float sourceMissingHealth = 1.0F;
        source.setHealth(source.getMaxHealth() - sourceMissingHealth);
        ally.setHealth(ally.getMaxHealth() - 10.0F);
        float allyBefore = ally.getHealth();

        float baseHealing = 10.0F;
        CombatTuning.Snapshot tuning = CombatTuning.current();
        float amplified = (float) (baseHealing * (1.0D + tuning.healingAmplificationPerLevel()));
        float expectedOverflow = Math.min(
                (float) tuning.overflowHealingAbsorptionCapPerLevel(),
                amplified - sourceMissingHealth
        );
        float expectedShared = (float) (sourceMissingHealth * tuning.healingSharePerLevel());

        float actualPrimary = AugmentHealingService.healPrimary(source, baseHealing);

        requireClose(actualPrimary, sourceMissingHealth, 1.0E-4F,
                "primary heal did not stop at the missing-health boundary");
        requireClose(source.getHealth(), source.getMaxHealth(), 1.0E-4F,
                "amplified primary heal did not fill the missing health");
        requireClose(
                ProtectionAugmentService.poolAmount(
                        source,
                        ProtectionAugmentService.PoolKind.ABSORPTION,
                        ProtectionAugmentService.OVERFLOW_HEALING_POOL
                ),
                expectedOverflow,
                1.0E-4F,
                "overflow healing did not convert only the amplified excess into its owned pool"
        );
        requireClose(ally.getHealth() - allyBefore, expectedShared, 1.0E-4F,
                "healing share used amplified/overflow healing instead of actual HP restored");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void sharedHealDoesNotCascadeShareOrLifeWave(GameTestHelper helper) {
        ServerPlayer recipient = player(helper, 1.5D);
        ServerPlayer ally = player(helper, 2.5D);
        makeAllies(recipient, ally);

        AugmentationPlayerStateService.replace(recipient, AugmentationPlayerState.initial()
                .withAugmentLevel(id("healing_amplification"), 1)
                .withAugmentLevel(id("healing_share"), 1)
                .withAugmentLevel(id("life_wave"), 1));

        recipient.setHealth(recipient.getMaxHealth() - 10.0F);
        ally.setHealth(ally.getMaxHealth() - 10.0F);
        float allyBefore = ally.getHealth();
        LivingEntity hostile = helper.spawnMob(EntityTypes.SPIDER, 3, 2, 1).withNoFreeWill().spawn();
        float hostileBefore = hostile.getHealth();

        float baseSharedHealing = 4.0F;
        float expectedRecipientHealing = (float) (
                baseSharedHealing * (1.0D + CombatTuning.current().healingAmplificationPerLevel())
        );
        float actualRecipientHealing = AugmentHealingService.healShared(recipient, baseSharedHealing);

        requireClose(actualRecipientHealing, expectedRecipientHealing, 1.0E-4F,
                "shared heal did not apply recipient amplification exactly once");
        requireClose(ally.getHealth(), allyBefore, 1.0E-4F,
                "shared heal recursively triggered the recipient's healing-share augment");
        requireClose(hostile.getHealth(), hostileBefore, 1.0E-4F,
                "shared heal recursively triggered the recipient's life-wave augment");
        GameTestPlayers.succeed(helper);
    }

    private static ServerPlayer player(GameTestHelper helper, double relativeX) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 2, 1));
        player.setPos(anchor.getX() + relativeX, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void makeAllies(ServerPlayer first, ServerPlayer second) {
        Scoreboard scoreboard = first.level().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("heal" + first.getId());
        scoreboard.addPlayerToTeam(first.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(second.getScoreboardName(), team);
    }

    private static String id(String path) {
        return PREFIX + path;
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
