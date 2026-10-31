package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.TimeStopBoundaryService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.mixin.CombatExpansionServiceInvoker;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Regressions from the second mythic-focused live play pass. */
public final class MythicLiveFixBatchIGameTests implements CustomTestMethodInvoker {
    private static final String TIME_STOP_ID = id("time_stop");
    private static final String LIGHTNING_ID = id("lightning");
    private static final String RETRACE_ID = id("retrace");

    @GameTest
    public void wardenUsesBossCompatibilityRules(GameTestHelper helper) {
        LivingEntity warden = helper.spawnMob(EntityTypes.WARDEN, 3, 2, 3).withNoFreeWill().spawn();
        require(EntityCompatibilityProfileService.isBoss(warden),
                "warden was still treated as a normal/elite augment target instead of a boss");
        require(!EntityCompatibilityProfileService.allowDominate(warden),
                "boss-classified warden remained dominatable");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void timeStopFreezesNeutralLivingAndWorldEntities(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(TIME_STOP_ID, 1)
                .withActiveSlots(List.of(TIME_STOP_ID)));
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 4, 2, 1).withNoFreeWill().spawn();
        ServerLevel level = player.level();
        ItemEntity item = new ItemEntity(level, player.getX() + 3.0D, player.getY() + 1.0D, player.getZ(),
                new ItemStack(Items.DIAMOND));
        item.setDeltaMovement(0.65D, 0.0D, 0.0D);
        level.addFreshEntity(item);

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "time stop did not activate");
        require(SpecialActiveAugmentService.isTimeStopped(cow),
                "neutral living entity was not included in the time-stop sphere");
        require(TimeStopBoundaryService.freezesWorldEntity(item),
                "ordinary world entity was not included in the time-stop sphere");
        Vec3 frozen = item.position();
        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            require(item.position().distanceToSqr(frozen) < 1.0E-6D,
                    "dropped item continued ticking/moving during time stop");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest
    public void lightningStaysPrimedUntilAChainTargetExists(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerState state = AugmentationPlayerState.initial().withAugmentLevel(LIGHTNING_ID, 1);
        AugmentationPlayerStateService.replace(player, state);
        LivingEntity primary = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 1).withNoFreeWill().spawn();

        for (int i = 0; i < 4; i++) {
            invokeLightning(player, primary, state, 10.0F);
        }
        require(CombatExpansionServiceInvoker.augmentationSurvival$lightningHits()
                        .getOrDefault(player.getUUID(), 0) >= 4,
                "lightning proc was consumed even though no chain target existed");

        LivingEntity secondary = helper.spawnMob(EntityTypes.ZOMBIE, 4, 2, 1).withNoFreeWill().spawn();
        float before = secondary.getHealth();
        invokeLightning(player, primary, state, 10.0F);
        require(secondary.getHealth() < before, "primed lightning did not damage the new chain target");
        require(CombatExpansionServiceInvoker.augmentationSurvival$lightningHits()
                        .getOrDefault(player.getUUID(), -1) == 0,
                "successful lightning chain did not consume its hit counter");
        require(!player.level().getEntitiesOfClass(
                LightningBolt.class,
                secondary.getBoundingBox().inflate(2.0D),
                bolt -> true
        ).isEmpty(), "successful lightning chain did not create a vanilla lightning presentation");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void retraceDamagesTargetsOnTheStoredTwoSecondPath(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerState state = AugmentationPlayerState.initial().withAugmentLevel(RETRACE_ID, 1);
        AugmentationPlayerStateService.replace(player, state);
        ReactiveCombatAugmentService.reconcile(player, state);
        LivingEntity pathTarget = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 1).withNoFreeWill().spawn();
        pathTarget.setHealth(pathTarget.getMaxHealth());

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            BlockPos destination = helper.absolutePos(new BlockPos(5, 2, 1));
            player.setPos(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
        });
        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            List<Vec3> history = retraceHistory(player);
            require(history.size() >= 2,
                    "retrace history never accumulated two samples; count=" + history.size());
            require(CompanionAugmentService.isHostileTarget(player, pathTarget),
                    "path zombie was not considered a hostile retrace target");

            Vec3 targetCenter = pathTarget.getBoundingBox().getCenter();
            double nearest = Double.POSITIVE_INFINITY;
            for (int index = 1; index < history.size(); index++) {
                nearest = Math.min(nearest, distanceToSegmentSquared(
                        targetCenter, history.get(index - 1), history.get(index)
                ));
            }
            double radius = Math.max(0.5D, (player.getBbWidth() + pathTarget.getBbWidth()) * 0.5D);
            require(nearest <= radius * radius,
                    "stored retrace path never crossed target hit radius; samples=" + history.size()
                            + ", nearestSq=" + nearest + ", radiusSq=" + (radius * radius)
                            + ", first=" + history.getFirst() + ", last=" + history.getLast()
                            + ", target=" + targetCenter);

            float before = pathTarget.getHealth();
            long now = player.level().getGameTime();
            invokeRetrace(player, 10.0F);
            require(AugmentationPlayerStateService.state(player).cooldownReadyTick(RETRACE_ID) > now,
                    "retrace had valid history but did not consume its cooldown; samples=" + history.size());
            require(pathTarget.getHealth() < before,
                    "retrace crossed a hostile hitbox and consumed cooldown but dealt no damage; before="
                            + before + ", after=" + pathTarget.getHealth() + ", nearestSq=" + nearest);
            GameTestPlayers.succeed(helper);
        });
    }

    private static List<Vec3> retraceHistory(ServerPlayer player) {
        try {
            Field runtimeField = ReactiveCombatAugmentService.class.getDeclaredField("RUNTIME");
            runtimeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Object> runtime = (Map<UUID, Object>) runtimeField.get(null);
            Object state = runtime.get(player.getUUID());
            if (state == null) return List.of();
            Field positionsField = state.getClass().getDeclaredField("positions");
            positionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Deque<Object> positions = (Deque<Object>) positionsField.get(state);
            List<Vec3> result = new ArrayList<>(positions.size());
            for (Object sample : positions) {
                Method position = sample.getClass().getDeclaredMethod("position");
                position.setAccessible(true);
                result.add((Vec3) position.invoke(sample));
            }
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("failed to inspect retrace history", exception);
        }
    }

    private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared <= 1.0E-9D) return point.distanceToSqr(start);
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSquared));
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private static void invokeLightning(
            ServerPlayer player,
            LivingEntity primary,
            AugmentationPlayerState state,
            float damage
    ) {
        try {
            Method method = CombatExpansionService.class.getDeclaredMethod(
                    "triggerLightning", ServerPlayer.class, LivingEntity.class, AugmentationPlayerState.class, float.class
            );
            method.setAccessible(true);
            method.invoke(null, player, primary, state, damage);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("failed to invoke lightning regression path", exception);
        }
    }

    private static void invokeRetrace(ServerPlayer player, float damage) {
        try {
            Method method = ReactiveCombatAugmentService.class.getDeclaredMethod(
                    "triggerRetrace", ServerPlayer.class, float.class
            );
            method.setAccessible(true);
            method.invoke(null, player, damage);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("failed to invoke retrace regression path", exception);
        }
    }

    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 2, z));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
