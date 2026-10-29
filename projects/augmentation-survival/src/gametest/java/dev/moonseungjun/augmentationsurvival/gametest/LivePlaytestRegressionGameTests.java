package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Regressions found only during the 2026-10-29 live playtest pass. */
public final class LivePlaytestRegressionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 20)
    public void portableRiptideConsumesOneWaterBucketPerDryUse(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("portable_riptide"), 1)
        );
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        AdvancedUtilityAugmentService.consumePortableRiptideWater(player);
        require(player.getOffhandItem().is(Items.BUCKET),
                "one successful dry Portable Riptide must turn the offhand water bucket into an empty bucket");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void wallSlamAddsDamageToRealMeleeAtNearbyWall(GameTestHelper helper) {
        ServerPlayer controlPlayer = GameTestPlayers.survivalPlayer(helper);
        ServerPlayer wallPlayer = GameTestPlayers.survivalPlayer(helper);

        BlockPos controlPos = helper.absolutePos(new BlockPos(4, 2, 2));
        controlPlayer.setPos(controlPos.getX() + 0.5D, controlPos.getY(), controlPos.getZ() + 0.5D);
        controlPlayer.setYRot(0.0F);
        LivingEntity controlTarget = helper.spawnMob(EntityTypes.ZOMBIE, 4, 2, 4).withNoFreeWill().spawn();
        float controlBefore = controlTarget.getHealth();
        controlPlayer.attack(controlTarget);
        float controlDamage = controlBefore - controlTarget.getHealth();
        require(controlDamage > 0.0F, "control real melee hit did not land");

        BlockPos wallPlayerPos = helper.absolutePos(new BlockPos(1, 2, 3));
        wallPlayer.setPos(wallPlayerPos.getX() + 0.5D, wallPlayerPos.getY(), wallPlayerPos.getZ() + 0.5D);
        wallPlayer.setYRot(0.0F);
        AugmentationPlayerStateService.replace(
                wallPlayer,
                AugmentationPlayerState.initial().withAugmentLevel(id("wall_slam"), 3)
        );
        helper.setBlock(new BlockPos(1, 2, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.STONE);
        LivingEntity wallTarget = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 5).withNoFreeWill().spawn();
        float wallBefore = wallTarget.getHealth();
        wallPlayer.attack(wallTarget);

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            float wallDamage = wallBefore - wallTarget.getHealth();
            require(wallDamage > controlDamage * 1.5F,
                    "Wall Slam did not add meaningful derived damage after a real melee hit reached the nearby wall; control="
                            + controlDamage + " wall=" + wallDamage);
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 20)
    public void delayedDamageStillTriggersAndThenRespectsLastStandInvulnerability(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("last_stand"), 1)
        );
        player.setHealth(4.0F);
        ServerLevel level = helper.getLevel();
        CombatProvenanceContext.callAsDelayed(12.0F,
                () -> player.hurtServer(level, player.damageSources().generic(), 12.0F));
        require(Math.abs(player.getHealth() - 1.0F) < 0.01F,
                "lethal delayed damage must trigger Last Stand and leave the player at 1 HP");
        CombatProvenanceContext.callAsDelayed(2.0F,
                () -> player.hurtServer(level, player.damageSources().generic(), 2.0F));
        require(Math.abs(player.getHealth() - 1.0F) < 0.01F,
                "delayed damage bypassed the active Last Stand invulnerability window");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void offensiveTargetPolicyIncludesNeutralCombatants(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        LivingEntity ironGolem = helper.spawnMob(EntityTypes.IRON_GOLEM, 3, 2, 3).withNoFreeWill().spawn();
        require(OffensiveTargetContext.isValid(player, ironGolem),
                "non-allied iron golems must be valid targets for player-owned offensive augments");
        GameTestPlayers.succeed(helper);
    }

    private static String id(String path) { return PREFIX + path; }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
