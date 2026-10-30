package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveRangeTelegraphService;
import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.TimeStopBoundaryService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
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

/** Range/readability regressions for the third live-play repair bundle. */
public final class LegendaryLiveFixBatchCGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 30)
    public void areaAccelerationTelegraphUsesTheGameplayRadius(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String area = id("area_acceleration");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(area, 1)
                .withActiveSlots(List.of(area)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "area acceleration route failed");
        require(ActiveRangeTelegraphService.trackedRadius(player, area) == AreaAccelerationService.radius(),
                "area acceleration telegraph radius diverged from gameplay radius");
        require(ActiveRangeTelegraphService.remainingTicks(player, area) > 0,
                "area acceleration sustained boundary did not remain active");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void timeStopTelegraphUsesTheDedicatedGameplaySphere(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String timeStop = id("time_stop");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(timeStop, 1)
                .withActiveSlots(List.of(timeStop)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "time stop route failed");
        double gameplayRadius = SpecialActiveTuning.current().timeStopRadius();
        require(Math.abs(TimeStopBoundaryService.activeRadius(player) - gameplayRadius) <= 1.0E-6D,
                "time stop sphere boundary diverged from gameplay radius");
        require(ActiveRangeTelegraphService.trackedRadius(player, timeStop) == 0.0F,
                "generic range ring duplicated the dedicated time stop sphere");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void cancelledChannelRemovesItsPersistentBoundary(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String revive = id("revive");
        ActiveRangeTelegraphService.onState(player, revive, ActivePresentationPayload.CHANNEL, 60, 20.0F);
        require(ActiveRangeTelegraphService.trackedRadius(player, revive) == 20.0F,
                "channel range was not registered");
        ActiveRangeTelegraphService.onState(player, revive, ActivePresentationPayload.CANCEL, 0, 0.0F);
        require(ActiveRangeTelegraphService.trackedRadius(player, revive) == 0.0F,
                "cancelled channel left a stale range telegraph");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 50)
    public void overtimeExtensionAlsoExtendsAreaTelegraph(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String area = id("area_acceleration");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(area, 1)
                .withAugmentLevel(id("overtime"), 1)
                .withActiveSlots(List.of(area)));
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "area acceleration route failed");

        int gameplayBefore = AreaAccelerationService.remainingTicks(player);
        int telegraphBefore = ActiveRangeTelegraphService.remainingTicks(player, area);
        LivingEntity victim = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 1).withNoFreeWill().spawn();
        fireKillEvent((ServerLevel) player.level(), player, victim);
        int gameplayAfter = AreaAccelerationService.remainingTicks(player);
        int telegraphAfter = ActiveRangeTelegraphService.remainingTicks(player, area);

        require(gameplayAfter > gameplayBefore,
                "overtime did not extend area acceleration gameplay duration");
        require(telegraphAfter > telegraphBefore,
                "overtime gameplay extension did not extend the visible range window");
        require(Math.abs(gameplayAfter - telegraphAfter) <= 1,
                "area acceleration gameplay and telegraph end ticks diverged");
        GameTestPlayers.succeed(helper);
    }

    private static void fireKillEvent(ServerLevel level, ServerPlayer killer, LivingEntity killed) {
        DamageSource source = killer.damageSources().playerAttack(killer);
        CombatProvenanceContext.runAs(CombatProvenance.ROOT_MELEE, () ->
                ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker()
                        .afterKilledOtherEntity(level, killer, killed, source));
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

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
