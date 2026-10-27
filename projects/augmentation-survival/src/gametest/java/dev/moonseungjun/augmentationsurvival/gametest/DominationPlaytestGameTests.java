package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityAttachments;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityState;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/** Manual-play regressions for permanent domination companion targeting. */
public final class DominationPlaytestGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void dominatedMobImmediatelyAcquiresNearbyHostile(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 1, 1);
        Mob dominated = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 2).withNoFreeWill().spawn();
        dominated.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(dominated);
        Mob hostile = helper.spawnMob(EntityTypes.SKELETON, 1, 2, 4).withNoFreeWill().spawn();
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(dominated.getTarget() == hostile,
                    "dominated mob did not automatically acquire a nearby hostile after conversion");
            require(dominated.isCurrentlyGlowing(), "dominated mob lost its visible domination marker");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 50)
    public void dominatedWardenKeepsOwnerHitWardenAsCommandTarget(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 1, 1);
        Mob dominated = helper.spawnMob(EntityTypes.WARDEN, 1, 2, 3).spawn();
        dominated.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(dominated);
        Mob target = helper.spawnMob(EntityTypes.WARDEN, 1, 2, 6).spawn();
        require(target.hurtServer((ServerLevel) target.level(), owner.damageSources().playerAttack(owner), 1.0F),
                "owner hit did not land on the second warden");
        GameTestPlayers.runAfterDelay(helper, 6, () -> {
            require(dominated.getTarget() == target,
                    "dominated warden dropped the owner-commanded warden target across server ticks");
            GameTestPlayers.succeed(helper);
        });
    }

    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 2, z));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
