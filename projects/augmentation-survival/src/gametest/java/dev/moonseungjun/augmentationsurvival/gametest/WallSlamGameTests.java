package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/** Real knockback/movement/collision regression for Wall Slam. */
public final class WallSlamGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 80)
    public void knockedTargetCollidingWithWallTakesBonusDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("wall_slam"), 3)
        );

        helper.setBlock(new BlockPos(1, 2, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.STONE);
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 5).withNoFreeWill().spawn();
        ServerLevel level = helper.getLevel();
        float before = target.getHealth();
        float rootDamage = 4.0F;
        DamageSource source = player.damageSources().playerAttack(player);

        CombatProvenanceContext.runAs(
                CombatProvenance.ROOT_MELEE,
                () -> target.hurtServer(level, source, rootDamage)
        );
        float afterRoot = target.getHealth();
        require(afterRoot < before, "qualifying root melee damage did not land");

        // Exercise Minecraft's actual LivingEntity.knockback boundary. The registered mixin must
        // observe this call and arm the pending collision window; the test no longer calls the
        // Wall Slam service hook directly.
        target.knockback(0.75D, 0.0D, -1.0D, source, rootDamage, false);
        require(target.getDeltaMovement().horizontalDistanceSqr() > 0.0025D,
                "real knockback did not produce enough horizontal motion for Wall Slam");

        GameTestPlayers.runAfterDelay(helper, 8, () -> {
            require(target.getHealth() < afterRoot,
                    "target reached the wall without receiving Wall Slam bonus damage");
            GameTestPlayers.succeed(helper);
        });
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
