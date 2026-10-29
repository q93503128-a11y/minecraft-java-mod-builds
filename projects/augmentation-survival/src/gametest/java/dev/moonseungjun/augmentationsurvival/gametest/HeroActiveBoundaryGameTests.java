package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.mixin.EnchantmentMenuAugmentAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Bundle-five regressions for broad Pull policy and the real enchantment-menu refresh boundary. */
public final class HeroActiveBoundaryGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void pullMovesNeutralEntityButLeavesPlayerAllyUntouchedThroughActiveSlot(GameTestHelper helper) {
        ServerPlayer owner = GameTestPlayers.survivalPlayer(helper);
        ServerPlayer ally = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 3, 3));
        owner.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        ally.setPos(anchor.getX() + 2.5D, anchor.getY(), anchor.getZ() + 0.5D);
        makeAllies(owner, ally);
        owner.setDeltaMovement(Vec3.ZERO);
        ally.setDeltaMovement(Vec3.ZERO);
        AugmentationPlayerStateService.replace(
                owner,
                AugmentationPlayerState.initial().withAugmentLevel(id("pull"), 1)
        );

        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 3, 3, 5).withNoFreeWill().spawn();
        cow.setDeltaMovement(Vec3.ZERO);
        Vec3 cowTowardOwner = owner.position().subtract(cow.position()).normalize();

        require(ActiveAbilityRouter.tryUseSlot(owner, 0),
                "active HUD slot did not route owned Pull into its server runtime");
        require(cow.getDeltaMovement().dot(cowTowardOwner) > 0.01D,
                "broad Pull did not move an ordinary neutral living entity toward the player");
        require(ally.getDeltaMovement().lengthSqr() < 1.0E-8D,
                "Pull moved a scoreboard-allied player despite the ally exclusion contract");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void enchantRefreshUsesRealMenuButtonAndConsumesOnlyOnSuccess(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("enchant_refresh"), 1)
        );
        player.giveExperiencePoints(30);

        BlockPos relativeTable = new BlockPos(2, 2, 2);
        helper.setBlock(relativeTable, Blocks.ENCHANTING_TABLE);
        BlockPos table = helper.absolutePos(relativeTable);
        EnchantmentMenu menu = new EnchantmentMenu(
                7,
                player.getInventory(),
                ContainerLevelAccess.create((ServerLevel) player.level(), table)
        );
        Container slots = ((EnchantmentMenuAugmentAccessor) menu).augmentationSurvival$getEnchantSlots();
        slots.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        slots.setItem(1, new ItemStack(Items.LAPIS_LAZULI, 2));
        menu.slotsChanged(slots);
        menu.costs[0] = Math.max(1, menu.costs[0]);

        int xpBefore = player.totalExperience;
        int lapisBefore = slots.getItem(1).getCount();
        int seedBefore = ((EnchantmentMenuAugmentAccessor) menu)
                .augmentationSurvival$getEnchantmentSeedData().get();

        require(menu.clickMenuButton(player, 3),
                "real EnchantmentMenu refresh button rejected a valid refresh");
        int seedAfter = ((EnchantmentMenuAugmentAccessor) menu)
                .augmentationSurvival$getEnchantmentSeedData().get();
        require(player.totalExperience == xpBefore - 5,
                "successful enchant refresh did not consume exactly five XP points");
        require(slots.getItem(1).getCount() == lapisBefore - 1,
                "successful enchant refresh did not consume exactly one lapis");
        require(seedAfter != seedBefore,
                "successful enchant refresh did not replace the enchantment seed/candidates");

        EnchantmentMenu failed = new EnchantmentMenu(
                8,
                player.getInventory(),
                ContainerLevelAccess.create((ServerLevel) player.level(), table)
        );
        Container failedSlots = ((EnchantmentMenuAugmentAccessor) failed).augmentationSurvival$getEnchantSlots();
        failedSlots.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        failed.slotsChanged(failedSlots);
        failed.costs[0] = Math.max(1, failed.costs[0]);
        int failedXpBefore = player.totalExperience;
        int failedSeedBefore = ((EnchantmentMenuAugmentAccessor) failed)
                .augmentationSurvival$getEnchantmentSeedData().get();

        require(!failed.clickMenuButton(player, 3),
                "enchant refresh succeeded without required lapis");
        require(player.totalExperience == failedXpBefore,
                "failed enchant refresh consumed XP");
        require(((EnchantmentMenuAugmentAccessor) failed)
                        .augmentationSurvival$getEnchantmentSeedData().get() == failedSeedBefore,
                "failed enchant refresh changed the enchantment seed/candidates");

        GameTestPlayers.succeed(helper);
    }

    private static void makeAllies(ServerPlayer first, ServerPlayer second) {
        Scoreboard scoreboard = first.level().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("pull" + first.getId());
        scoreboard.addPlayerToTeam(first.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(second.getScoreboardName(), team);
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
