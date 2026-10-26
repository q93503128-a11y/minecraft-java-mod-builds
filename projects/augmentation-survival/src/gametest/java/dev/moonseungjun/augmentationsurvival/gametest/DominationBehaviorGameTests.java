package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/** Behavioral proof for permanent domination ownership, alliance and combat targeting. */
public final class DominationBehaviorGameTests implements CustomTestMethodInvoker {
    private static final String DOMINATION = AugmentationSurvival.MOD_ID + ":domination";

    @GameTest(maxTicks = 80)
    public void dominationConvertsEnemyIntoPersistentCombatAlly(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setHealth(player.getMaxHealth());

        Mob dominated = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 4).spawn();
        dominated.setTarget(player);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(DOMINATION, 1)
                .withActiveSlots(List.of(DOMINATION)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "domination active did not route successfully");
        require(CompanionAugmentService.isDominated(dominated), "domination did not persist companion ownership");
        require(CompanionAugmentService.isOwnedBy(dominated, player), "dominated mob owner was not the caster");
        require(dominated.isPersistenceRequired(), "dominated mob was not protected from normal despawn");
        require(dominated.getTarget() == null, "domination did not clear the previous hostile target immediately");
        require(player.isAlliedTo(dominated) && dominated.isAlliedTo(player),
                "vanilla ally checks did not expose domination ownership");

        ServerLevel level = (ServerLevel) player.level();
        float ownerHealth = player.getHealth();
        DamageSource friendlyAttack = player.damageSources().mobAttack(dominated);
        boolean friendlyApplied = player.hurtServer(level, friendlyAttack, 4.0F);
        require(!friendlyApplied, "dominated companion damage reached its owner");
        require(player.getHealth() == ownerHealth, "dominated companion changed owner health");

        Mob hostile = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 7).withNoFreeWill().spawn();
        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            require(dominated.getTarget() == hostile,
                    "dominated companion did not acquire a nearby hostile target");
            require(!CompanionAugmentService.areAllied(dominated, hostile),
                    "unowned hostile was incorrectly treated as a domination ally");

            float hostileHealth = hostile.getHealth();
            DamageSource companionAttack = hostile.damageSources().mobAttack(dominated);
            boolean hostileApplied = hostile.hurtServer(level, companionAttack, 4.0F);
            require(hostileApplied, "dominated companion could not damage a hostile target");
            require(hostile.getHealth() < hostileHealth,
                    "dominated companion attack did not reduce hostile health");
            GameTestPlayers.succeed(helper);
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
