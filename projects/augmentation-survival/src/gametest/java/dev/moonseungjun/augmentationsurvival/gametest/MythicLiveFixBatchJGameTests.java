package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.TimeStopBoundaryService;
import dev.moonseungjun.augmentationsurvival.augment.combat.LightningDirectHitService;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Live regressions reported immediately after Bundle I. */
public final class MythicLiveFixBatchJGameTests implements CustomTestMethodInvoker {
    private static final String TIME_STOP_ID = id("time_stop");
    private static final String LIGHTNING_ID = id("lightning");
    private static final String ECHO_STRIKE_ID = id("echo_strike");
    private static final String RETRACE_ID = id("retrace");

    @GameTest(maxTicks = 45)
    public void timeStopFreezesOtherPlayersAndStaysAtCastOrigin(GameTestHelper helper) {
        ServerPlayer caster = player(helper, 1, 1);
        ServerPlayer otherPlayer = player(helper, 3, 1);
        AugmentationPlayerStateService.replace(caster, AugmentationPlayerState.initial()
                .withAugmentLevel(TIME_STOP_ID, 1)
                .withActiveSlots(List.of(TIME_STOP_ID)));

        require(ActiveAbilityRouter.tryUseSlot(caster, 0), "time stop did not activate");
        require(SpecialActiveAugmentService.isTimeStopped(otherPlayer),
                "a second player inside the time-stop sphere was not frozen");

        TimeStopBoundaryService.ActiveField field = TimeStopBoundaryService.activeField(caster);
        require(field != null, "time stop did not create a world-space field");
        Vec3 castCenter = field.center();
        double radius = SpecialActiveTuning.current().timeStopRadius();
        caster.setPos(castCenter.x + radius + 10.0D, caster.getY(), castCenter.z);

        ServerLevel level = caster.level();
        ItemEntity atCastOrigin = new ItemEntity(level, castCenter.x + 1.0D, castCenter.y, castCenter.z,
                new ItemStack(Items.DIAMOND));
        ItemEntity besideMovedCaster = new ItemEntity(level, caster.getX() + 1.0D, caster.getY() + 1.0D, caster.getZ(),
                new ItemStack(Items.EMERALD));
        level.addFreshEntity(atCastOrigin);
        level.addFreshEntity(besideMovedCaster);

        require(TimeStopBoundaryService.freezesWorldEntity(atCastOrigin),
                "fixed time-stop field no longer covered its cast origin after the caster moved");
        require(!TimeStopBoundaryService.freezesWorldEntity(besideMovedCaster),
                "time-stop field followed the caster instead of remaining at the cast origin");

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            TimeStopBoundaryService.ActiveField still = TimeStopBoundaryService.activeField(caster);
            require(still != null && still.center().distanceToSqr(castCenter) < 1.0E-8D,
                    "time-stop field center changed after caster movement");
            LivingEntity lateCow = helper.spawnMob(EntityTypes.COW, 4, 2, 1).withNoFreeWill().spawn();
            GameTestPlayers.runAfterDelay(helper, 2, () -> {
                require(SpecialActiveAugmentService.isTimeStopped(lateCow),
                        "late living entrant at the original field center was not frozen");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    @GameTest(maxTicks = 55)
    public void lightningCountsBareHandAndWoodenSwordRootMelee(GameTestHelper helper) {
        ServerPlayer attacker = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(attacker,
                AugmentationPlayerState.initial().withAugmentLevel(LIGHTNING_ID, 3));
        LivingEntity primary = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 1).withNoFreeWill().spawn();
        LivingEntity secondary = helper.spawnMob(EntityTypes.ZOMBIE, 4, 2, 1).withNoFreeWill().spawn();

        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            attacker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            primary.invulnerableTime = 0;
            attacker.attack(primary);
            require(LightningDirectHitService.hitCount(attacker) == 1,
                    "bare-hand Player.attack did not count as a Lightning direct hit");

            // Give vanilla attack strength the same recovery window a real player would have.
            GameTestPlayers.runAfterDelay(helper, 20, () -> {
                float secondaryBefore = secondary.getHealth();
                attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
                primary.invulnerableTime = 0;
                attacker.attack(primary);

                require(LightningDirectHitService.hitCount(attacker) == 0,
                        "wooden-sword Player.attack did not complete the Lv.3 Lightning threshold");
                require(secondary.getHealth() < secondaryBefore,
                        "real melee Lightning threshold did not chain damage to the nearby hostile");
                require(!attacker.level().getEntitiesOfClass(
                        LightningBolt.class,
                        primary.getBoundingBox().inflate(2.0D),
                        bolt -> true
                ).isEmpty(), "Lightning threshold did not create the primary visual lightning bolt");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    @GameTest(maxTicks = 80)
    public void echoStrikeLandsAsDelayedSecondMeleeHit(GameTestHelper helper) {
        ServerPlayer attacker = player(helper, 1, 1);
        AugmentationPlayerStateService.replace(attacker,
                AugmentationPlayerState.initial().withAugmentLevel(ECHO_STRIKE_ID, 3));
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 1).withNoFreeWill().spawn();

        GameTestPlayers.runAfterDelay(helper, 20, () -> {
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            attacker.attack(target);
            GameTestPlayers.runAfterDelay(helper, 20, () -> {
                target.invulnerableTime = 0;
                attacker.attack(target);
                float afterRootSecondHit = target.getHealth();
                require(afterRootSecondHit > 0.0F, "echo regression target died before delayed hit could be measured");

                GameTestPlayers.runAfterDelay(helper, 11, () -> {
                    require(target.getHealth() < afterRootSecondHit,
                            "Echo Strike did not land its distinct delayed afterimage hit");
                    GameTestPlayers.succeed(helper);
                });
            });
        });
    }

    @GameTest(maxTicks = 20)
    public void retraceIsExcludedFromFutureEpicOffers(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        AugmentationPlayerState state = AugmentationPlayerState.initial();
        for (AugmentDefinition definition : AugmentCatalog.definitions(AugmentTier.EPIC)) {
            if (RETRACE_ID.equals(definition.id())) continue;
            state = state.withAugmentLevel(definition.id(), definition.maxLevel());
        }
        AugmentationPlayerStateService.replace(player, state);
        player.getInventory().add(new ItemStack(AugmentCoreItems.item(AugmentTier.EPIC)));

        AugmentOfferService.BeginResult result = AugmentOfferService.beginOffer(player, AugmentTier.EPIC);
        require(result.status() == AugmentOfferService.BeginStatus.FULLY_MAXED,
                "retired retrace remained obtainable as the final Epic offer: " + result.candidateIds());
        GameTestPlayers.succeed(helper);
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
