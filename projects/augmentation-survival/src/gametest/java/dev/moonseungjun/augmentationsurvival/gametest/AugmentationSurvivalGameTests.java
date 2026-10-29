package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestTuning;
import dev.moonseungjun.augmentationsurvival.augment.resource.ReplantFallbackService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import dev.moonseungjun.augmentationsurvival.augment.utility.InstantMiningToggleService;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;

/** High-risk runtime regression tests executed inside a real Minecraft GameTest server. */
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
        GameTestPlayers.succeed(helper);
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
        GameTestPlayers.succeed(helper);
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
        GameTestPlayers.succeed(helper);
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
        GameTestPlayers.succeed(helper);
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

        GameTestPlayers.runAfterDelay(helper, 70, () -> {
            long now = player.level().getGameTime();
            lethalDamage(player);

            require(!player.isDeadOrDying(), "last stand allowed a real death");
            require(player.getHealth() == 1.0F,
                    "last stand did not stop lethal damage at 1 HP; health=" + player.getHealth());
            require(player.getInventory().countItem(Items.TOTEM_OF_UNDYING) == beforeTotems,
                    "emergency totem was consumed before last stand");
            require(AugmentationPlayerStateService.state(player).cooldownReadyTick(id("last_stand")) > now,
                    "last stand did not consume its cooldown");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 100)
    public void lastStandPrecedesReincarnation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("last_stand"), 1)
                .withAugmentLevel(id("reincarnation"), 1));

        GameTestPlayers.runAfterDelay(helper, 70, () -> {
            long now = player.level().getGameTime();
            lethalDamage(player);
            AugmentationPlayerState after = AugmentationPlayerStateService.state(player);
            require(!player.isDeadOrDying(), "last stand + reincarnation allowed death");
            require(player.getHealth() == 1.0F, "last stand did not own the lethal interception");
            require(after.cooldownReadyTick(id("last_stand")) > now,
                    "last stand cooldown was not consumed");
            require(after.cooldownReadyTick(id("reincarnation")) <= now,
                    "reincarnation was consumed before last stand");
            require(!CombatAugmentService.isActionLocked(player),
                    "reincarnation stasis started even though last stand won priority");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 100)
    public void emergencyTotemPrecedesReincarnation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("emergency_totem"), 1)
                .withAugmentLevel(id("reincarnation"), 1));
        player.getInventory().add(new ItemStack(Items.TOTEM_OF_UNDYING));

        GameTestPlayers.runAfterDelay(helper, 70, () -> {
            long now = player.level().getGameTime();
            lethalDamage(player);
            AugmentationPlayerState after = AugmentationPlayerStateService.state(player);
            require(!player.isDeadOrDying(), "emergency totem + reincarnation allowed death");
            require(player.getInventory().countItem(Items.TOTEM_OF_UNDYING) == 0,
                    "emergency totem was not consumed on the post-vanilla protection layer");
            require(after.cooldownReadyTick(id("reincarnation")) <= now,
                    "reincarnation was consumed before emergency totem");
            require(!CombatAugmentService.isActionLocked(player),
                    "reincarnation stasis started despite emergency totem priority");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 100)
    public void reincarnationHandlesLethalDamageAfterEarlierLayersAreAbsent(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("reincarnation"), 1));

        GameTestPlayers.runAfterDelay(helper, 70, () -> {
            long now = player.level().getGameTime();
            lethalDamage(player);
            AugmentationPlayerState after = AugmentationPlayerStateService.state(player);
            require(!player.isDeadOrDying(), "reincarnation failed to intercept lethal damage");
            require(player.getHealth() == 1.0F,
                    "reincarnation did not enter stasis at 1 HP; health=" + player.getHealth());
            require(after.cooldownReadyTick(id("reincarnation")) > now,
                    "reincarnation did not consume its cooldown");
            require(CombatAugmentService.isActionLocked(player),
                    "reincarnation did not enter action-locked stasis");
            GameTestPlayers.succeed(helper);
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
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void vanillaBossCompatibilityProfileIsBound(GameTestHelper helper) {
        var wither = helper.spawnMob(EntityTypes.WITHER, 1, 2, 1).withNoFreeWill().spawn();
        require(EntityCompatibilityProfileService.isBoss(wither), "wither is missing the shared boss compatibility profile");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void instantMiningRequiresAValidHarvestTool(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String instantMining = id("instant_mining");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(instantMining, 1));
        require(!UtilityAugmentService.isInstantMiningEnabled(player),
                "instant mining started enabled from ownership alone");
        require(!ActiveAbilityRouter.assignSlot(player, 0, instantMining),
                "instant mining leaked into the K active loadout");
        require(InstantMiningToggleService.toggle(player),
                "instant mining could not be toggled on through the dedicated-key server path");

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition().offset(2, 0, 0);
        var stone = Blocks.STONE.defaultBlockState();
        level.setBlock(pos, stone, Block.UPDATE_ALL);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(Items.DIAMOND_PICKAXE));

        require(player.hasCorrectToolForDrops(stone),
                "instant-mining precondition: diamond pickaxe was not recognized as the correct stone tool");
        float validToolProgress = stone.getDestroyProgress(player, level, pos);
        require(validToolProgress >= 1.0F,
                "enabled instant mining did not reach one-game-tick root progress; progress=" + validToolProgress);

        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        float emptyHandProgress = stone.getDestroyProgress(player, level, pos);
        require(emptyHandProgress < 1.0F,
                "instant mining ignored the correct-tool harvest contract; empty-hand progress=" + emptyHandProgress);
        GameTestPlayers.succeed(helper);
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
        GameTestPlayers.succeed(helper);
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
        GameTestPlayers.succeed(helper);
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
        GameTestPlayers.runAfterDelay(helper, delay, () -> {
            require(level.getBlockState(cropPos).equals(wheat.getStateForAge(0)),
                    "replant failed after the harvested seed was already picked up");
            require(player.getInventory().countItem(Items.WHEAT_SEEDS) == 0,
                    "replant did not consume exactly one owner-inventory planting item");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 240)
    public void expiredReplantOwnerContextDoesNotConsumeLaterSeeds(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ServerLevel level = (ServerLevel) player.level();
        BlockPos cropPos = player.blockPosition().offset(2, 0, 0);
        player.getInventory().setItem(9, new ItemStack(Items.WHEAT_SEEDS));
        ReplantFallbackService.record(level, cropPos, player);

        GameTestPlayers.runAfterDelay(helper, 205, () -> {
            require(!ReplantFallbackService.consumeOwnerInventory(level, cropPos, Items.WHEAT_SEEDS),
                    "expired replant context consumed a planting item long after its pending operation");
            require(player.getInventory().countItem(Items.WHEAT_SEEDS) == 1,
                    "expired replant context changed the owner's inventory");
            GameTestPlayers.succeed(helper);
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

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            ItemStack selected = player.getInventory().getItem(selectedSlot);
            require(selected.is(Items.COBBLESTONE) && selected.getCount() == 32,
                    "auto refill did not move the matching main-inventory stack into the selected slot");
            require(player.getInventory().getItem(9).isEmpty(),
                    "auto refill duplicated the source stack instead of transferring it");
            GameTestPlayers.succeed(helper);
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

        LivingEntity derivedVictim = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 3).withNoFreeWill().spawn();
        fireKillEvent(level, player, derivedVictim, CombatProvenance.DERIVED_AOE);
        AugmentationPlayerState afterDerived = AugmentationPlayerStateService.state(player);
        require(afterDerived.cooldownReadyTick(firstCharge) == now + 5,
                "derived kill incorrectly reduced the first active charge cooldown");
        require(afterDerived.cooldownReadyTick(secondCharge) == now + 50,
                "derived kill incorrectly reduced the second active charge cooldown");

        LivingEntity rootVictim = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).withNoFreeWill().spawn();
        fireKillEvent(level, player, rootVictim, CombatProvenance.ROOT_MELEE);
        AugmentationPlayerState afterRoot = AugmentationPlayerStateService.state(player);
        long actualFirst = afterRoot.cooldownReadyTick(firstCharge);
        long actualSecond = afterRoot.cooldownReadyTick(secondCharge);
        require(actualFirst == now,
                "combat cycle did not finish the nearly-ready first charge; expected=" + now
                        + ", actual=" + actualFirst);
        require(actualSecond == now + 35,
                "combat-cycle overflow did not carry the remaining 15 ticks into the next charge; expected="
                        + (now + 35) + ", actual=" + actualSecond);
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void derivedKillEventsDoNotTriggerRootKillRewardsOrChains(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(id("lifesteal"), 1)
                .withAugmentLevel(id("kill_streak"), 1)
                .withAugmentLevel(id("chain_explosion"), 1));
        player.setHealth(Math.max(1.0F, player.getMaxHealth() - 6.0F));
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity derivedVictim = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).withNoFreeWill().spawn();
        LivingEntity secondary = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 3).withNoFreeWill().spawn();
        float playerHealthBefore = player.getHealth();
        float secondaryHealthBefore = secondary.getHealth();
        double attackSpeedBefore = player.getAttributeValue(Attributes.ATTACK_SPEED);

        fireKillEvent(level, player, derivedVictim, CombatProvenance.DERIVED_AOE);
        requireClose(player.getHealth(), playerHealthBefore, 1.0E-5F,
                "derived kill incorrectly triggered lifesteal");
        requireClose(secondary.getHealth(), secondaryHealthBefore, 1.0E-5F,
                "derived kill incorrectly triggered chain explosion");
        requireClose((float) player.getAttributeValue(Attributes.ATTACK_SPEED), (float) attackSpeedBefore, 1.0E-5F,
                "derived kill incorrectly advanced kill streak");

        LivingEntity rootVictim = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 4).withNoFreeWill().spawn();
        fireKillEvent(level, player, rootVictim, CombatProvenance.ROOT_MELEE);
        require(player.getHealth() > playerHealthBefore,
                "root direct kill did not trigger lifesteal");
        require(secondary.getHealth() < secondaryHealthBefore || secondary.isDeadOrDying(),
                "root direct kill did not trigger chain explosion");
        require(player.getAttributeValue(Attributes.ATTACK_SPEED) > attackSpeedBefore,
                "root direct kill did not advance kill streak");
        GameTestPlayers.succeed(helper);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void lethalDamage(ServerPlayer player) {
        player.hurtServer(
                (ServerLevel) player.level(),
                player.damageSources().generic(),
                player.getMaxHealth() * 20.0F
        );
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
