package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.DivinePunishmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityAttachments;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityState;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real-world-path regressions derived from the first manual Augmentation Survival playtest. */
public final class PlaytestRegressionGameTests implements CustomTestMethodInvoker {
    private static final String ABUNDANCE_ID = AugmentationSurvival.MOD_ID + ":abundance";
    private static final String TIME_STOP_ID = AugmentationSurvival.MOD_ID + ":time_stop";
    private static final String DOMINATION_ID = AugmentationSurvival.MOD_ID + ":domination";

    @GameTest
    public void instantMiningIsAnExplicitActiveToggle(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(UtilityAugmentService.INSTANT_MINING_ID, 1)
        );
        require(!UtilityAugmentService.isInstantMiningEnabled(player),
                "instant mining started enabled from ownership alone");
        require(ActiveAbilityRouter.assignSlot(player, 0, UtilityAugmentService.INSTANT_MINING_ID),
                "instant mining could not be assigned to an active slot");
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "instant mining active slot did not toggle on");
        require(UtilityAugmentService.isInstantMiningEnabled(player), "instant mining toggle state stayed off");
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "instant mining active slot did not toggle off");
        require(!UtilityAugmentService.isInstantMiningEnabled(player), "instant mining toggle state stayed on");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void abundanceDoublesActualDirectBlockDrop(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(ABUNDANCE_ID, 1)
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        require(player.gameMode.destroyBlock(pos), "direct stone break failed before abundance could be evaluated");
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            int cobblestone = level.getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(pos).inflate(2.0D),
                    ItemEntity::isAlive
            ).stream().filter(item -> item.getItem().is(Items.COBBLESTONE))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            require(cobblestone == 2,
                    "abundance did not double the actual direct block drop; cobblestone=" + cobblestone);
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 50)
    public void timeStopKeepsHostilePositionLocked(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(TIME_STOP_ID, 1)
        );
        require(ActiveAbilityRouter.assignSlot(player, 0, TIME_STOP_ID), "time stop could not be assigned");
        Mob zombie = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 4).spawn();
        Vec3 frozenAt = zombie.position();
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "time stop active use failed");
        zombie.setDeltaMovement(0.8D, 0.0D, 0.4D);
        zombie.getNavigation().moveTo(player, 1.2D);
        GameTestPlayers.runAfterDelay(helper, 8, () -> {
            require(zombie.position().distanceToSqr(frozenAt) < 1.0E-5D,
                    "time-stopped hostile changed world position: " + zombie.position());
            require(zombie.getDeltaMovement().lengthSqr() < 1.0E-6D,
                    "time-stopped hostile retained movement velocity");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 50)
    public void divinePunishmentDamagesActualNearbyHostile(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(DivinePunishmentService.ID, 1)
        );
        require(ActiveAbilityRouter.assignSlot(player, 0, DivinePunishmentService.ID),
                "divine punishment could not be assigned");
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 4).withNoFreeWill().spawn();
        zombie.setHealth(zombie.getMaxHealth());
        float before = zombie.getHealth();
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "divine punishment active use failed");
        float fraction = (float) SpecialActiveTuning.current().divinePunishmentFraction(1, false);
        float expected = before - zombie.getMaxHealth() * fraction;
        requireClose(zombie.getHealth(), expected, 0.06F,
                "divine punishment did not apply its configured percent-health strike");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 50)
    public void dominatedMobRejectsNeutralTargetsButObeysOwnerAttack(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(
                owner,
                AugmentationPlayerState.initial().withAugmentLevel(DOMINATION_ID, 1)
        );
        Mob dominated = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 2).withNoFreeWill().spawn();
        dominated.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(dominated);
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 1, 2, 4).withNoFreeWill().spawn();
        require(!CompanionAugmentService.mayDominatedMobDamage(dominated, cow),
                "dominated mob was allowed to attack an arbitrary neutral mob");
        require(cow.hurtServer((ServerLevel) cow.level(), owner.damageSources().playerAttack(owner), 1.0F),
                "owner attack used to issue domination command did not land");
        require(CompanionAugmentService.mayDominatedMobDamage(dominated, cow),
                "owner-hit neutral target was not promoted to domination command target");
        require(dominated.getTarget() == cow,
                "dominated mob did not immediately target the entity its owner attacked");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 60)
    public void poweredFlightActualTickDamagesBodyCollisionTarget(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(FlightAugmentService.FLIGHT_ID, 1)
        );
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 2).withNoFreeWill().spawn();
        zombie.setHealth(zombie.getMaxHealth());
        float before = zombie.getHealth();
        require(FlightAugmentService.toggle(player), "powered flight did not activate");
        GameTestPlayers.runAfterDelay(helper, 4, () -> {
            require(zombie.getHealth() < before,
                    "actual powered-flight server ticks crossed the target without collision damage");
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
