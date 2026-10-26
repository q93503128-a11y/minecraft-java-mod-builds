package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestTuning;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;

/** High-risk runtime regression tests executed inside a real Minecraft GameTest server. */
@SuppressWarnings({"removal", "deprecation"})
public final class AugmentationSurvivalGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void reorderedLegacyActiveExecutesAssignedSlot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("cleanse"), 1)
                .withAugmentLevel(id("pull"), 1)
                .withActiveSlots(List.of(id("pull"), id("cleanse")));
        AugmentationPlayerStateService.replace(player, state);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));

        require(player.hasEffect(MobEffects.POISON), "precondition: poison was not applied");
        require(ActiveAbilityRouter.tryUseSlot(player, 1), "reordered cleanse slot did not execute");
        require(!player.hasEffect(MobEffects.POISON), "slot 2 did not route to cleanse after reordering");
        helper.succeed();
    }

    @GameTest
    public void abilityHasteAndDoubleChargeRemainSequential(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("shield"), 1)
                .withAugmentLevel(id("ability_haste"), 1)
                .withAugmentLevel(id("double_charge"), 1)
                .withActiveSlots(List.of(id("shield")));
        AugmentationPlayerStateService.replace(player, state);
        long now = player.level().getGameTime();

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "first shield charge failed");
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "second shield charge failed");
        require(!ActiveAbilityRouter.tryUseSlot(player, 0), "third shield cast bypassed two-charge limit");

        AugmentationPlayerState after = AugmentationPlayerStateService.state(player);
        long first = after.cooldownReadyTick(id("shield") + "#charge0");
        long second = after.cooldownReadyTick(id("shield") + "#charge1");
        require(first > now, "first charge did not enter cooldown");
        require(second > first, "double-charge cooldowns are not sequential");
        helper.succeed();
    }

    @GameTest
    public void reflectedHarmfulEffectDoesNotRecurse(GameTestHelper helper) {
        ServerPlayer attacker = player(helper);
        ServerPlayer victim = player(helper);
        AugmentationPlayerState reflect = AugmentationPlayerState.initial()
                .withAugmentLevel(id("status_reflect"), 1);
        AugmentationPlayerStateService.replace(attacker, reflect);
        AugmentationPlayerStateService.replace(victim, reflect);

        require(victim.addEffect(new MobEffectInstance(MobEffects.POISON, 200), attacker),
                "incoming harmful effect was unexpectedly rejected");
        require(victim.hasEffect(MobEffects.POISON), "victim lost the original harmful effect");
        require(attacker.hasEffect(MobEffects.POISON), "status reflect did not create the reflected copy");
        helper.succeed();
    }

    @GameTest
    public void statusReflectionAndStrongMindComposeWithoutRecursion(GameTestHelper helper) {
        ServerPlayer attacker = player(helper);
        ServerPlayer victim = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("status_reflect"), 1)
                .withAugmentLevel(id("strong_mind"), 1);
        AugmentationPlayerStateService.replace(victim, state);

        require(victim.addEffect(new MobEffectInstance(MobEffects.POISON, 200), attacker),
                "strong-mind victim rejected the transformed poison unexpectedly");
        MobEffectInstance victimPoison = victim.getEffect(MobEffects.POISON);
        MobEffectInstance attackerPoison = attacker.getEffect(MobEffects.POISON);
        require(victimPoison != null, "victim lost the transformed harmful effect");
        require(attackerPoison != null, "status reflection did not reach the attacker");
        int expectedVictimDuration = Math.max(1, (int) Math.ceil(
                200 * (1.0D - ReactiveCombatTuning.current().strongMindDurationReduction(1))));
        int expectedReflectedDuration = Math.max(1, (int) Math.floor(
                200 * ReactiveCombatTuning.current().statusReflectDurationFraction()));
        require(victimPoison.getDuration() == expectedVictimDuration,
                "strong mind duration mismatch; expected=" + expectedVictimDuration
                        + ", actual=" + victimPoison.getDuration());
        require(attackerPoison.getDuration() == expectedReflectedDuration,
                "status reflect duration mismatch; expected=" + expectedReflectedDuration
                        + ", actual=" + attackerPoison.getDuration());
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void lastStandPrecedesInventoryTotem(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("last_stand"), 1)
                .withAugmentLevel(id("emergency_totem"), 1);
        AugmentationPlayerStateService.replace(player, state);
        player.getInventory().add(new ItemStack(Items.TOTEM_OF_UNDYING));
        int beforeTotems = player.getInventory().countItem(Items.TOTEM_OF_UNDYING);

        helper.runAfterDelay(70, () -> {
            long now = player.level().getGameTime();
            player.hurtServer(
                    (ServerLevel) player.level(),
                    player.damageSources().generic(),
                    player.getMaxHealth() * 20.0F
            );

            require(!player.isDeadOrDying(), "last stand allowed a real death");
            require(player.getHealth() == 1.0F,
                    "last stand did not stop lethal damage at 1 HP; health=" + player.getHealth());
            require(player.getInventory().countItem(Items.TOTEM_OF_UNDYING) == beforeTotems,
                    "emergency totem was consumed before last stand");
            require(AugmentationPlayerStateService.state(player).cooldownReadyTick(id("last_stand")) > now,
                    "last stand did not consume its cooldown");
            helper.succeed();
        });
    }

    @GameTest
    public void threatSuppressionTogglePersistsAcrossUses(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("threat_suppression"), 1)
                .withActiveSlots(List.of(id("threat_suppression")));
        AugmentationPlayerStateService.replace(player, state);

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "threat suppression did not toggle on");
        require(ActiveAbilityRouter.isThreatSuppressionEnabled(player), "threat suppression state did not persist on");
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "threat suppression did not toggle off");
        require(!ActiveAbilityRouter.isThreatSuppressionEnabled(player), "threat suppression state did not persist off");
        helper.succeed();
    }

    @GameTest
    public void vanillaBossCompatibilityProfileIsBound(GameTestHelper helper) {
        var wither = helper.spawnMob(EntityTypes.WITHER, 1, 2, 1).withNoFreeWill().spawn();
        require(EntityCompatibilityProfileService.isBoss(wither), "wither is missing the shared boss compatibility profile");
        helper.succeed();
    }

    @GameTest
    public void instantMiningRequiresAValidHarvestTool(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("instant_mining"), 1));
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition().offset(2, 0, 0);
        var stone = Blocks.STONE.defaultBlockState();
        level.setBlock(pos, stone, Block.UPDATE_ALL);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(Items.DIAMOND_PICKAXE));

        require(player.hasCorrectToolForDrops(stone),
                "instant-mining precondition: diamond pickaxe was not recognized as the correct stone tool");
        float validToolProgress = stone.getDestroyProgress(player, level, pos);
        require(validToolProgress >= 1.0F,
                "instant mining did not reach one-game-tick root progress; progress=" + validToolProgress);

        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        float emptyHandProgress = stone.getDestroyProgress(player, level, pos);
        require(emptyHandProgress < 1.0F,
                "instant mining ignored the correct-tool harvest contract; empty-hand progress=" + emptyHandProgress);
        helper.succeed();
    }

    @GameTest
    public void tunnelingDerivedBreaksDoNotInflateContinuousMining(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("continuous_mining"), 1)
                .withAugmentLevel(id("tunneling"), 1)
                .withAugmentLevel(id("instant_mining"), 1);
        AugmentationPlayerStateService.replace(player, state);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(Items.DIAMOND_PICKAXE));

        ServerLevel level = (ServerLevel) player.level();
        BlockPos root = player.blockPosition().offset(2, 0, 0);
        BlockPos derived = root.east();
        level.setBlock(root, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(derived, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        require(player.gameMode.destroyBlock(root), "root tunneling break failed");
        require(level.getBlockState(root).isAir(), "root tunneling block remained");
        require(level.getBlockState(derived).isAir(), "tunneling did not destroy the derived block");

        float actual = SurvivalAugmentService.modifyBlockBreakSpeed(
                player, Blocks.STONE.defaultBlockState(), 1.0F);
        float expected = (float) (1.0D + SurvivalTuning.current().continuousMiningSpeedPerStackPerLevel());
        requireClose(actual, expected, 1.0E-5F,
                "derived tunneling block incorrectly counted as a continuous-mining ROOT stack");
        helper.succeed();
    }

    @GameTest
    public void treeFellingDerivedBreaksDoNotInflateContinuousMining(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("continuous_mining"), 1)
                .withAugmentLevel(id("tree_felling"), 1);
        AugmentationPlayerStateService.replace(player, state);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(Items.DIAMOND_AXE));

        ServerLevel level = (ServerLevel) player.level();
        BlockPos root = player.blockPosition().offset(2, 0, 0);
        BlockPos derived = root.above();
        level.setBlock(root, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(derived, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);

        require(player.gameMode.destroyBlock(root), "root tree-felling break failed");
        require(level.getBlockState(root).isAir(), "root log remained after tree felling");
        require(level.getBlockState(derived).isAir(), "tree felling did not destroy the derived log");

        float actual = SurvivalAugmentService.modifyBlockBreakSpeed(
                player, Blocks.STONE.defaultBlockState(), 1.0F);
        float expected = (float) (1.0D + SurvivalTuning.current().continuousMiningSpeedPerStackPerLevel());
        requireClose(actual, expected, 1.0E-5F,
                "derived tree-felling block incorrectly counted as a continuous-mining ROOT stack");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void replantConsumesSeedAfterImmediatePickupWindow(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("replant"), 1));
        ServerLevel level = (ServerLevel) player.level();
        BlockPos cropPos = player.blockPosition().offset(2, 0, 0);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        level.setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cropPos, wheat.getStateForAge(wheat.getMaxAge()), Block.UPDATE_ALL);

        require(player.gameMode.destroyBlock(cropPos), "mature crop root break failed");
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class, new AABB(cropPos).inflate(3.0D), entity -> entity.isAlive())) {
            item.discard();
        }
        player.getInventory().setItem(9, new ItemStack(Items.WHEAT_SEEDS));

        int delay = ResourceHarvestTuning.current().replantDelayTicks() + 2;
        helper.runAfterDelay(delay, () -> {
            require(level.getBlockState(cropPos).equals(wheat.getStateForAge(0)),
                    "replant failed after the harvested seed was already picked up");
            require(player.getInventory().countItem(Items.WHEAT_SEEDS) == 0,
                    "replant did not consume exactly one owner-inventory planting item");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void autoRefillMovesMatchingMainInventoryStackIntoSelectedHotbar(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        int selectedSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setItem(selectedSlot, new ItemStack(Items.COBBLESTONE));
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 32));
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("auto_refill"), 1);
        AugmentationPlayerStateService.replace(player, state);
        SurvivalAugmentService.reconcile(player, state);
        player.getInventory().setItem(selectedSlot, ItemStack.EMPTY);

        helper.runAfterDelay(2, () -> {
            ItemStack selected = player.getInventory().getItem(selectedSlot);
            require(selected.is(Items.COBBLESTONE) && selected.getCount() == 32,
                    "auto refill did not move the matching main-inventory stack into the selected slot");
            require(player.getInventory().getItem(9).isEmpty(),
                    "auto refill duplicated the source stack instead of transferring it");
            helper.succeed();
        });
    }

    @GameTest
    public void combatCycleOnlyConsumesRootKillAndCarriesReductionToNextCharge(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        long now = player.level().getGameTime();
        String firstCharge = id("shield_throw") + "#charge0";
        String secondCharge = id("shield_throw") + "#charge1";
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("shield_throw"), 1)
                .withAugmentLevel(id("ability_haste"), 1)
                .withAugmentLevel(id("double_charge"), 1)
                .withAugmentLevel(id("combat_cycle"), 1)
                .withCooldownReadyTick(firstCharge, now + 5)
                .withCooldownReadyTick(secondCharge, now + 50);
        AugmentationPlayerStateService.replace(player, state);
        ServerLevel level = (ServerLevel) player.level();

        var derivedVictim = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 3).withNoFreeWill().spawn();
        derivedVictim.setHealth(1.0F);
        CombatProvenanceContext.runAs(CombatProvenance.DERIVED_AOE,
                () -> derivedVictim.hurtServer(level, player.damageSources().playerAttack(player), 10.0F));
        require(derivedVictim.isDeadOrDying(), "derived provenance precondition did not produce a kill");
        AugmentationPlayerState afterDerived = AugmentationPlayerStateService.state(player);
        require(afterDerived.cooldownReadyTick(firstCharge) == now + 5,
                "derived kill incorrectly reduced the first active charge cooldown");
        require(afterDerived.cooldownReadyTick(secondCharge) == now + 50,
                "derived kill incorrectly reduced the second active charge cooldown");

        var rootVictim = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).withNoFreeWill().spawn();
        rootVictim.setHealth(1.0F);
        CombatProvenanceContext.runAs(CombatProvenance.ROOT_MELEE,
                () -> rootVictim.hurtServer(level, player.damageSources().playerAttack(player), 10.0F));
        require(rootVictim.isDeadOrDying(), "root provenance precondition did not produce a kill");
        AugmentationPlayerState afterRoot = AugmentationPlayerStateService.state(player);
        long actualFirst = afterRoot.cooldownReadyTick(firstCharge);
        long actualSecond = afterRoot.cooldownReadyTick(secondCharge);
        require(actualFirst == now,
                "combat cycle did not finish the nearly-ready first charge; expected=" + now
                        + ", actual=" + actualFirst);
        require(actualSecond == now + 35,
                "combat-cycle overflow did not carry the remaining 15 ticks into the next charge; expected="
                        + (now + 35) + ", actual=" + actualSecond);
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static String id(String path) {
        return PREFIX + path;
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
        try {
            method.invoke(this, helper);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw exception;
        }
    }
}
