package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.DeathReturnAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeStateAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Live-playtest regressions grouped in the 2026-10-30 five-fix batch A. */
public final class LiveFixBatchAGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 220)
    public void lethalFallWithDelayedDamageLeavesNoDebtAfterLastStand(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial()
                        .withAugmentLevel(id("delayed_damage"), 3)
                        .withAugmentLevel(id("last_stand"), 1)
        );
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);
        player.setHealth(8.0F);

        boolean accepted = player.hurtServer(level, player.damageSources().fall(), 40.0F);
        require(accepted, "lethal fall source was rejected by the real damage pipeline");
        require(Math.abs(player.getHealth() - 1.0F) < 0.01F,
                "Last Stand did not leave the player at 1 HP after a lethal fall hit");
        require(delayedQueueEmpty(player),
                "Last Stand left delayed fall-damage debt immediately after activation");

        int wait = CombatTuning.current().lastStandInvulnerableTicks()
                + CombatTuning.current().delayedDamageIntervalTicks() + 5;
        GameTestPlayers.runAfterDelay(helper, wait, () -> {
            require(Math.abs(player.getHealth() - 1.0F) < 0.01F,
                    "stale delayed fall damage damaged or killed the player after Last Stand ended; health="
                            + player.getHealth());
            require(delayedQueueEmpty(player),
                    "delayed fall-damage debt reappeared after Last Stand protection ended");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 30)
    public void delayedInstallmentChangesHealthWithoutKnockbackDirection(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        LivingEntity attacker = helper.spawnMob(EntityTypes.HUSK, 2, 2, 6).withNoFreeWill().spawn();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setDeltaMovement(Vec3.ZERO);
        float beforeHealth = player.getHealth();
        float beforeHurtDir = player.getHurtDir();
        int beforeHurtTime = player.hurtTime;
        int beforeHurtDuration = player.hurtDuration;

        CombatProvenanceContext.callAsDelayed(3.0F,
                () -> player.hurtServer(level, player.damageSources().mobAttack(attacker), 3.0F));

        require(player.getHealth() < beforeHealth,
                "delayed installment did not apply real health damage");
        require(player.getDeltaMovement().lengthSqr() < 1.0E-8D,
                "delayed installment produced knockback velocity: " + player.getDeltaMovement());
        require(Math.abs(player.getHurtDir() - beforeHurtDir) < 1.0E-5F,
                "delayed installment changed the ordinary directional hurt feedback");
        require(player.hurtTime == beforeHurtTime && player.hurtDuration == beforeHurtDuration,
                "delayed installment changed ordinary hurt animation timing");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void deathReturnPreservesOldPlayerDeathPositionAcrossRespawnHandoff(GameTestHelper helper) {
        ServerPlayer oldPlayer = GameTestPlayers.survivalPlayer(helper);
        ServerPlayer newPlayer = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        GlobalPos target = GlobalPos.of(level.dimension(), helper.absolutePos(new BlockPos(6, 2, 6)));
        oldPlayer.setLastDeathLocation(Optional.of(target));
        newPlayer.setLastDeathLocation(Optional.empty());

        // A real death starts with the augment on the dying player, then the copyOnDeath
        // attachment supplies the same ownership to the replacement player before AFTER_RESPAWN.
        AugmentationPlayerState owned = AugmentationPlayerState.initial()
                .withAugmentLevel(id("death_return"), 1);
        AugmentationPlayerStateService.replace(oldPlayer, owned);
        AugmentationPlayerStateService.replace(newPlayer, owned);

        BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
        newPlayer.setPos(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D);

        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(oldPlayer, newPlayer, false);
        require(newPlayer.getLastDeathLocation().filter(target::equals).isPresent(),
                "respawn handoff lost the old player's freshly recorded last-death position");

        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            AugmentationPlayerState state = AugmentationPlayerStateService.state(newPlayer);
            require(state.level(id("death_return")) > 0,
                    "death-return ownership was lost during respawn reconciliation");
            require(state.persistentInt("death_return/available") > 0,
                    "Death Return one-use grant was not armed after respawn reconciliation");
            require(DeathReturnAugmentService.tryUse(newPlayer),
                    "Death Return was armed but the teleport still failed");
            require(newPlayer.position().distanceToSqr(Vec3.atBottomCenterOf(target.pos())) < 2.25D,
                    "Death Return succeeded without moving the player to the recorded death point");
            GameTestPlayers.succeed(helper);
        });
    }

    private static boolean delayedQueueEmpty(ServerPlayer player) {
        Object runtime = ProtectionRuntimeAccessor.augmentationSurvival$getRuntime().get(player.getUUID());
        return runtime == null
                || runtime instanceof ProtectionRuntimeStateAccessor accessor
                && accessor.augmentationSurvival$getDelayed().isEmpty();
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
