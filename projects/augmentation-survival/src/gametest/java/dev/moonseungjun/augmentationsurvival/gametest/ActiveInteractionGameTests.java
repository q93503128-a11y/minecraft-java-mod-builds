package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActivePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Interaction regressions for shared active charge, replay, Overtime and special-control contracts. */
public final class ActiveInteractionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 20)
    public void everyRoutedActiveHasPresentationProfile(GameTestHelper helper) {
        require(ActiveAbilityRouter.supportedIds().size() == 22,
                "standard active roster changed without updating the presentation coverage contract");
        for (String abilityId : ActiveAbilityRouter.supportedIds()) {
            if (UtilityAugmentService.INSTANT_MINING_ID.equals(abilityId)) {
                require(!ActivePresentationService.hasProfile(abilityId),
                        "instant mining should use its toggle state without generic world-particle presentation");
                continue;
            }
            require(ActivePresentationService.hasProfile(abilityId),
                    "missing server-confirmed presentation profile for " + abilityId);
        }
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 100)
    public void replayDoesNotConsumeAnotherDoubleChargeOrScheduleItself(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("shield"), 1)
                .withAugmentLevel(id("double_charge"), 1)
                .withAugmentLevel(id("replay"), 1)
                .withActiveSlots(List.of(id("shield"))));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "manual shield cast failed");
        AugmentationPlayerState afterManual = AugmentationPlayerStateService.state(player);
        String charge0 = id("shield") + "#charge0";
        String charge1 = id("shield") + "#charge1";
        long firstReady = afterManual.cooldownReadyTick(charge0);
        long secondReady = afterManual.cooldownReadyTick(charge1);
        require(firstReady > player.level().getGameTime(), "manual cast did not consume the first charge");
        require(secondReady <= player.level().getGameTime(), "manual cast unexpectedly consumed the second charge");
        require(ProtectionAugmentService.poolAmount(
                player, ProtectionAugmentService.PoolKind.SHIELD,
                ProtectionAugmentService.ACTIVE_SHIELD_POOL) > 0.0F,
                "manual shield pool was not created");

        int delay = ExpansionTuning.current().replayDelayTicks();
        GameTestPlayers.runAfterDelay(helper, delay + 2, () -> {
            AugmentationPlayerState afterReplay = AugmentationPlayerStateService.state(player);
            require(afterReplay.cooldownReadyTick(charge0) == firstReady,
                    "replay changed the already-consumed charge cooldown");
            require(afterReplay.cooldownReadyTick(charge1) == secondReady,
                    "replay consumed the spare double charge");
            require(ProtectionAugmentService.poolAmount(
                    player, ProtectionAugmentService.PoolKind.SHIELD,
                    ProtectionAugmentService.ACTIVE_SHIELD_POOL + "_replay") > 0.0F,
                    "replay did not create its isolated shield pool");

            GameTestPlayers.runAfterDelay(helper, delay + 2, () -> {
                AugmentationPlayerState afterSecondWindow = AugmentationPlayerStateService.state(player);
                require(afterSecondWindow.cooldownReadyTick(charge0) == firstReady
                                && afterSecondWindow.cooldownReadyTick(charge1) == secondReady,
                        "replay scheduled another replay or consumed an additional charge");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    @GameTest(maxTicks = 220)
    public void overtimeExtendsManualShieldOnceWithoutExceedingPerKillExtension(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("shield"), 1)
                .withAugmentLevel(id("overtime"), 1)
                .withActiveSlots(List.of(id("shield"))));
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "manual shield cast failed");

        ServerLevel level = (ServerLevel) player.level();
        LivingEntity victim = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).withNoFreeWill().spawn();
        fireKillEvent(level, player, victim, CombatProvenance.ROOT_MELEE);

        int baseDuration = CombatTuning.current().shieldDurationTicks();
        int extension = AdvancedActiveTuning.current().overtimeNormalExtensionTicks();
        require(extension > 1, "overtime normal extension is too small for the runtime boundary test");

        GameTestPlayers.runAfterDelay(helper, baseDuration + 1, () -> {
            require(ProtectionAugmentService.poolAmount(
                    player, ProtectionAugmentService.PoolKind.SHIELD,
                    ProtectionAugmentService.ACTIVE_SHIELD_POOL) > 0.0F,
                    "overtime did not keep the manual shield alive past its base duration");
        });
        GameTestPlayers.runAfterDelay(helper, baseDuration + extension + 2, () -> {
            require(ProtectionAugmentService.poolAmount(
                    player, ProtectionAugmentService.PoolKind.SHIELD,
                    ProtectionAugmentService.ACTIVE_SHIELD_POOL) == 0.0F,
                    "single normal kill extended the manual shield beyond its configured extension");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 260)
    public void timeStopFreezesHostileAndRespectsCompatibility(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String timeStopId = id("time_stop");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(timeStopId, 1)
                .withActiveSlots(List.of(timeStopId)));

        LivingEntity near = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 1).spawn();
        LivingEntity outside = helper.spawnMob(EntityTypes.ZOMBIE, 4, 2, 1).spawn();
        LivingEntity lateEntrant = helper.spawnMob(EntityTypes.ZOMBIE, 5, 2, 1).spawn();
        require(EntityCompatibilityProfileService.allowTimeStop(near),
                "vanilla hostile compatibility baseline unexpectedly denied time stop");
        double radius = SpecialActiveTuning.current().timeStopRadius();
        outside.setPos(player.getX() + radius + 6.0D, player.getY(), player.getZ());
        lateEntrant.setPos(player.getX() + radius + 8.0D, player.getY(), player.getZ());

        long castTick = player.level().getGameTime();
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "time stop route failed");
        require(SpecialActiveAugmentService.isTimeStopped(near), "near hostile was not time-stopped");
        require(!SpecialActiveAugmentService.isTimeStopped(outside), "out-of-range hostile was time-stopped");
        require(!SpecialActiveAugmentService.isTimeStopped(lateEntrant), "future entrant was stopped before entering the field");
        require(near instanceof Mob && ((Mob) near).isNoAi(), "time-stopped hostile AI was not disabled");

        float healthBeforeFrozenAttack = player.getHealth();
        DamageSource frozenAttack = player.damageSources().mobAttack(near);
        boolean frozenAttackApplied = player.hurtServer((ServerLevel) player.level(), frozenAttack, 4.0F);
        require(!frozenAttackApplied, "time-stopped hostile still passed the final server damage boundary");
        require(player.getHealth() == healthBeforeFrozenAttack,
                "time-stopped hostile changed player health through an alternate attack path");

        lateEntrant.setPos(player.getX() + 2.0D, player.getY(), player.getZ());
        float healthBeforeEntryAttack = player.getHealth();
        DamageSource entryAttack = player.damageSources().mobAttack(lateEntrant);
        boolean entryAttackApplied = player.hurtServer((ServerLevel) player.level(), entryAttack, 4.0F);
        require(!entryAttackApplied, "hostile entering an active time-stop field attacked before field reconciliation");
        require(player.getHealth() == healthBeforeEntryAttack,
                "late time-stop entrant changed player health before server reconciliation");

        AugmentationPlayerState afterCast = AugmentationPlayerStateService.state(player);
        require(afterCast.cooldownReadyTick(timeStopId) > castTick, "time stop cooldown was not consumed");
        require(!ActiveAbilityRouter.tryUseSlot(player, 0), "time stop allowed immediate cooldown bypass");

        int duration = SpecialActiveTuning.current().timeStopDurationTicks();
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(SpecialActiveAugmentService.isTimeStopped(lateEntrant),
                    "hostile entering during time stop was not dynamically frozen");
            require(lateEntrant instanceof Mob && ((Mob) lateEntrant).isNoAi(),
                    "late time-stop entrant AI was not disabled");
        });
        GameTestPlayers.runAfterDelay(helper, Math.max(3, duration / 2), () -> {
            require(SpecialActiveAugmentService.isTimeStopped(near), "time stop ended before configured duration");
            require(SpecialActiveAugmentService.isTimeStopped(lateEntrant),
                    "late entrant resumed before the active time-stop window ended");
            require(near instanceof Mob && ((Mob) near).isNoAi(), "hostile AI resumed during time stop");
        });
        GameTestPlayers.runAfterDelay(helper, duration + 3, () -> {
            require(!SpecialActiveAugmentService.isTimeStopped(near), "time stop runtime did not expire");
            require(!SpecialActiveAugmentService.isTimeStopped(lateEntrant), "late entrant time stop did not expire");
            require(!(near instanceof Mob) || !((Mob) near).isNoAi(), "hostile AI did not restore after time stop");
            require(!(lateEntrant instanceof Mob) || !((Mob) lateEntrant).isNoAi(),
                    "late entrant AI did not restore after time stop");
            require(!SpecialActiveAugmentService.isTimeStopped(outside), "out-of-range hostile entered time stop later");

            float healthBeforeReleasedAttack = player.getHealth();
            DamageSource releasedAttack = player.damageSources().mobAttack(near);
            boolean releasedAttackApplied = player.hurtServer((ServerLevel) player.level(), releasedAttack, 4.0F);
            require(releasedAttackApplied, "hostile damage stayed blocked after time stop expired");
            require(player.getHealth() < healthBeforeReleasedAttack,
                    "hostile damage did not resume after time stop expired");
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

    private static void fireKillEvent(
            ServerLevel level,
            ServerPlayer killer,
            LivingEntity killed,
            CombatProvenance provenance
    ) {
        DamageSource source = killer.damageSources().playerAttack(killer);
        CombatProvenanceContext.runAs(provenance, () ->
                ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker()
                        .afterKilledOtherEntity(level, killer, killed, source));
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
