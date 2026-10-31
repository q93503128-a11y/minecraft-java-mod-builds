package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveChargePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityAttachments;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityState;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Live regressions from the first mythic-focused 2026-10-31 playtest pass. */
public final class MythicLiveFixBatchHGameTests implements CustomTestMethodInvoker {
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String GIGANTIFICATION_ID = id("gigantification");
    private static final String DOMINATION_ID = id("domination");
    private static final String AUTO_REFILL_ID = id("auto_refill");

    @GameTest
    public void mythicGigantificationShowsOneRealCooldownInsteadOfTwoFakeCharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(GIGANTIFICATION_ID, 1)
                .withAugmentLevel(DOUBLE_CHARGE_ID, 1)
                .withActiveSlots(List.of(GIGANTIFICATION_ID)));
        long now = player.level().getGameTime();
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "gigantification did not activate");
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        ActiveChargePresentationService.Snapshot snapshot =
                ActiveChargePresentationService.snapshot(state, GIGANTIFICATION_ID, now);
        require(snapshot.maxCharges() == 1, "mythic gigantification still exposed two HUD charges");
        require(snapshot.readyCharges() == 0, "gigantification cooldown HUD remained ready after use");
        require(snapshot.nextRechargeTicks() > 0, "gigantification did not expose its real single cooldown");
        require(state.cooldownReadyTick(GIGANTIFICATION_ID) > now,
                "gigantification did not consume its canonical cooldown");
        require(state.cooldownReadyTick(GIGANTIFICATION_ID + "#charge0") == 0L
                        && state.cooldownReadyTick(GIGANTIFICATION_ID + "#charge1") == 0L,
                "gigantification created forbidden double-charge cooldown keys");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void cooldownlessDominationShowsNoChargePips(GameTestHelper helper) {
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(DOMINATION_ID, 1)
                .withAugmentLevel(DOUBLE_CHARGE_ID, 1);
        ActiveChargePresentationService.Snapshot snapshot =
                ActiveChargePresentationService.snapshot(state, DOMINATION_ID, 0L);
        require(snapshot.maxCharges() == 0, "cooldown-less domination still exposed charge pips");
        require(snapshot.readyCharges() == 0, "cooldown-less domination reported a ready charge");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void selectingAnEmptyHotbarSlotDoesNotTriggerAutoRefill(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        int originalSlot = player.getInventory().getSelectedSlot();
        int emptySlot = originalSlot == 1 ? 2 : 1;
        player.getInventory().setItem(originalSlot, new ItemStack(Items.COBBLESTONE));
        player.getInventory().setItem(emptySlot, ItemStack.EMPTY);
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 32));
        AugmentationPlayerState state = AugmentationPlayerState.initial().withAugmentLevel(AUTO_REFILL_ID, 1);
        AugmentationPlayerStateService.replace(player, state);
        SurvivalAugmentService.reconcile(player, state);

        player.getInventory().setSelectedSlot(emptySlot);
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(player.getInventory().getItem(emptySlot).isEmpty(),
                    "auto refill populated an intentionally selected empty hotbar slot");
            require(player.getInventory().getItem(9).is(Items.COBBLESTONE)
                            && player.getInventory().getItem(9).getCount() == 32,
                    "auto refill moved reserve items after a mere hotbar selection change");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 30)
    public void actualLastStackItemDepletionStillTriggersAutoRefill(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        int selectedSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setItem(selectedSlot, new ItemStack(Items.COBBLESTONE, 1));
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 32));
        AugmentationPlayerState state = AugmentationPlayerState.initial().withAugmentLevel(AUTO_REFILL_ID, 1);
        AugmentationPlayerStateService.replace(player, state);
        SurvivalAugmentService.reconcile(player, state);

        player.getInventory().setItem(selectedSlot, ItemStack.EMPTY);
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            ItemStack refill = player.getInventory().getItem(selectedSlot);
            require(refill.is(Items.COBBLESTONE) && refill.getCount() == 32,
                    "auto refill no longer responds to a genuine one-item-to-empty depletion");
            require(player.getInventory().getItem(9).isEmpty(),
                    "auto refill duplicated the reserve stack during genuine depletion");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest
    public void gigantificationScalesVanillaSweepGeometryWithBodySize(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AABB base = player.getBoundingBox().inflate(1.0D, 0.25D, 1.0D);
        double normalWidth = SweepingEnhancementService.expandVanillaSweepBox(player, base).getXsize();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(GIGANTIFICATION_ID, 1));
        require(AdvancedWorldAugmentService.tryUse(player, GIGANTIFICATION_ID),
                "gigantification did not activate for sweep-geometry test");
        double giantWidth = SweepingEnhancementService.expandVanillaSweepBox(player, base).getXsize();
        require(giantWidth >= normalWidth * 1.70D,
                "gigantification did not expand vanilla sweep geometry with body scale; normal="
                        + normalWidth + " giant=" + giantWidth);
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void dominationCanReleaseTheAimedOwnedCompanion(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 1, 1);
        owner.setYRot(0.0F);
        owner.setXRot(0.0F);
        AugmentationPlayerStateService.replace(owner, AugmentationPlayerState.initial()
                .withAugmentLevel(DOMINATION_ID, 1));
        Mob dominated = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 3).withNoFreeWill().spawn();
        dominated.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(dominated);
        require(CompanionAugmentService.isDominated(dominated), "release precondition failed");
        require(CompanionAugmentService.tryDominate(owner), "aimed dominated companion could not be released");
        require(!CompanionAugmentService.isDominated(dominated), "released companion retained domination ownership");
        require(!dominated.isCurrentlyGlowing(), "released companion retained domination glow");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void farDominatedCompanionReturnsToOwnerInsteadOfChasingAway(GameTestHelper helper) {
        ServerPlayer owner = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(owner, AugmentationPlayerState.initial()
                .withAugmentLevel(DOMINATION_ID, 1));
        Mob dominated = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 3).withNoFreeWill().spawn();
        dominated.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(dominated);
        dominated.setPos(owner.getX() + 45.0D, owner.getY(), owner.getZ());
        require(dominated.distanceToSqr(owner) > 40.0D * 40.0D, "recall precondition was not far enough");
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(dominated.distanceToSqr(owner) < 40.0D * 40.0D,
                    "dominated companion remained beyond hard owner recall distance");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 60)
    public void poweredFlightBodyCollisionAlsoDamagesNeutralMobs(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(FlightAugmentService.FLIGHT_ID, 1));
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 1, 2, 2).withNoFreeWill().spawn();
        cow.setHealth(cow.getMaxHealth());
        float before = cow.getHealth();
        require(FlightAugmentService.toggle(player), "powered flight did not activate");
        GameTestPlayers.runAfterDelay(helper, 4, () -> {
            require(cow.getHealth() < before,
                    "powered-flight body impact crossed a neutral mob without collision damage");
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
