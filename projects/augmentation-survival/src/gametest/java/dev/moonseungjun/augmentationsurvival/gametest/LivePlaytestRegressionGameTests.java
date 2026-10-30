package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeStateAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

/** Regressions found only during the 2026-10-29/30 live playtest passes. */
public final class LivePlaytestRegressionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 20)
    public void portableRiptideRunsActualDryVanillaUseKeepsBucketAndSpin(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("portable_riptide"), 1)
        );

        ItemStack trident = new ItemStack(Items.TRIDENT);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        trident.enchant(enchantments.getOrThrow(Enchantments.RIPTIDE), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, trident);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        require(!player.isInWaterOrRain(), "Portable Riptide regression must run in a genuinely dry state");

        TridentItem tridentItem = (TridentItem) trident.getItem();
        tridentItem.use(level, player, InteractionHand.MAIN_HAND);
        require(player.isUsingItem(), "dry Portable Riptide did not enter the vanilla trident use state");

        int remainingUseTicks = tridentItem.getUseDuration(trident, player) - 20;
        boolean released = tridentItem.releaseUsing(trident, level, player, remainingUseTicks);
        require(released, "dry Portable Riptide did not complete vanilla TridentItem.releaseUsing");
        require(player.getDeltaMovement().lengthSqr() > 0.05D,
                "successful dry Portable Riptide produced no vanilla launch velocity");
        require(player.isAutoSpinAttack(),
                "successful dry Portable Riptide did not enter the vanilla spin-attack state");
        require(player.getOffhandItem().is(Items.WATER_BUCKET),
                "Portable Riptide must keep the water bucket intact for unlimited dry uses");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void wallSlamAddsDamageToRealMeleeAtNearbyWall(GameTestHelper helper) {
        ServerPlayer controlPlayer = GameTestPlayers.survivalPlayer(helper);
        ServerPlayer wallPlayer = GameTestPlayers.survivalPlayer(helper);

        BlockPos controlPos = helper.absolutePos(new BlockPos(4, 2, 2));
        controlPlayer.setPos(controlPos.getX() + 0.5D, controlPos.getY(), controlPos.getZ() + 0.5D);
        controlPlayer.setYRot(0.0F);
        LivingEntity controlTarget = helper.spawnMob(EntityTypes.ZOMBIE, 4, 2, 4).withNoFreeWill().spawn();

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

        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            controlTarget.setHealth(controlTarget.getMaxHealth());
            controlTarget.invulnerableTime = 0;
            float controlBefore = controlTarget.getHealth();
            controlPlayer.attack(controlTarget);
            float controlDamage = controlBefore - controlTarget.getHealth();
            require(controlDamage > 0.0F, "control real melee hit did not land");

            wallTarget.setHealth(wallTarget.getMaxHealth());
            wallTarget.invulnerableTime = 0;
            float wallBefore = wallTarget.getHealth();
            wallPlayer.attack(wallTarget);

            GameTestPlayers.runAfterDelay(helper, 2, () -> {
                float wallDamage = wallBefore - wallTarget.getHealth();
                require(wallDamage > controlDamage * 1.5F,
                        "Wall Slam did not add meaningful derived damage after a real melee hit reached the nearby wall; control="
                                + controlDamage + " wall=" + wallDamage);
                GameTestPlayers.succeed(helper);
            });
        });
    }

    @GameTest(maxTicks = 20)
    public void retracePlayerTextKeepsItsActualMechanic(GameTestHelper helper) {
        AugmentPresentationCatalog.Entry entry = AugmentPresentationCatalog.get(
                AugmentCatalog.require(id("retrace"))
        );
        require(entry.summary().contains("지난 2초") && entry.summary().contains("잔상"),
                "Retrace summary lost the path-rewind mechanic: " + entry.summary());
        require(entry.detail().contains("50% / 75% / 100%") && entry.detail().contains("5초"),
                "Retrace detail lost level damage or cooldown information: " + entry.detail());
        require(!entry.summary().contains("ROOT") && !entry.detail().contains("ROOT"),
                "Retrace player-facing text leaked the internal ROOT term");
        GameTestPlayers.succeed(helper);
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

    @GameTest(maxTicks = 400)
    public void realDelayedReservationTriggersLastStandClearsDebtAndSuppressesNewReservations(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        int delayedLevel = 3;
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial()
                        .withAugmentLevel(id("delayed_damage"), delayedLevel)
                        .withAugmentLevel(id("last_stand"), 1)
        );
        player.getFoodData().setFoodLevel(6);
        player.getFoodData().setSaturation(0.0F);

        CombatTuning.Snapshot tuning = CombatTuning.current();
        int installments = tuning.delayedDamageInstallmentCount();
        int interval = tuning.delayedDamageIntervalTicks();
        require(installments == 5, "live-playtest regression expects the canonical five delayed installments");

        float startHealth = 10.0F;
        player.setHealth(startHealth);
        double fraction = tuning.delayedDamageFraction(delayedLevel);
        float hit = (float) (startHealth
                / ((1.0D - fraction) + 3.5D * fraction / installments));
        boolean accepted = player.hurtServer(level, player.damageSources().generic(), hit);
        require(accepted, "real delayed-damage root hit was rejected");
        require(player.getHealth() > 0.0F && player.getHealth() < startHealth,
                "real delayed-damage root hit did not leave an immediate HP portion");
        require(AugmentationPlayerStateService.state(player).cooldownReadyTick(id("last_stand"))
                        <= player.level().getGameTime(),
                "Last Stand triggered on the root hit instead of a later reserved installment");

        GameTestPlayers.runAfterDelay(helper, interval * 4 + 2, () -> {
            require(Math.abs(player.getHealth() - 1.0F) < 0.01F,
                    "the fourth reserved installment should trigger Last Stand and leave 1 HP; health="
                            + player.getHealth());
            require(AugmentationPlayerStateService.state(player).cooldownReadyTick(id("last_stand"))
                            > player.level().getGameTime(),
                    "Last Stand cooldown was not armed by the lethal reserved installment");

            ProtectionRuntimeStateAccessor runtime = protectionRuntime(player);
            require(runtime.augmentationSurvival$getDelayed().isEmpty(),
                    "Last Stand left old delayed-damage debt queued after activation");

            float duringWindow = ProtectionAugmentService.modifyAfterReductions(
                    player,
                    player.damageSources().generic(),
                    10.0F
            );
            require(Math.abs(duringWindow - 10.0F) < 0.01F,
                    "Last Stand protection window still split new damage into delayed reservations");
            require(runtime.augmentationSurvival$getDelayed().isEmpty(),
                    "new delayed-damage debt was created during Last Stand protection");
        });

        long finalDelay = interval * 4L + tuning.lastStandInvulnerableTicks() + interval + 5L;
        GameTestPlayers.runAfterDelay(helper, finalDelay, () -> {
            require(Math.abs(player.getHealth() - 1.0F) < 0.01F,
                    "old delayed damage killed or damaged the player after Last Stand protection ended; health="
                            + player.getHealth());
            ProtectionRuntimeStateAccessor runtime = protectionRuntime(player);
            require(runtime.augmentationSurvival$getDelayed().isEmpty(),
                    "delayed-damage debt reappeared after Last Stand protection expired");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 20)
    public void offensiveTargetPolicyIncludesNeutralCombatants(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        LivingEntity ironGolem = helper.spawnMob(EntityTypes.IRON_GOLEM, 3, 2, 3).withNoFreeWill().spawn();
        require(OffensiveTargetContext.isValid(player, ironGolem),
                "non-allied iron golems must be valid targets for player-owned offensive augments");
        GameTestPlayers.succeed(helper);
    }

    private static ProtectionRuntimeStateAccessor protectionRuntime(ServerPlayer player) {
        Object runtime = ProtectionRuntimeAccessor.augmentationSurvival$getRuntime().get(player.getUUID());
        if (!(runtime instanceof ProtectionRuntimeStateAccessor accessor)) {
            throw new AssertionError("missing protection runtime for delayed-damage regression");
        }
        return accessor;
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
