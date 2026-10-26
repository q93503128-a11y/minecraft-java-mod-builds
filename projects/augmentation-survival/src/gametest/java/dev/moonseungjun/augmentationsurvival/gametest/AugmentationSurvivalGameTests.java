package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    public void lastStandPrecedesInventoryTotem(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState state = AugmentationPlayerState.initial()
                .withAugmentLevel(id("last_stand"), 1)
                .withAugmentLevel(id("emergency_totem"), 1);
        AugmentationPlayerStateService.replace(player, state);
        player.getInventory().add(new ItemStack(Items.TOTEM_OF_UNDYING));
        int beforeTotems = player.getInventory().countItem(Items.TOTEM_OF_UNDYING);
        long now = player.level().getGameTime();

        helper.hurt(player, player.damageSources().generic(), player.getMaxHealth() * 20.0F);

        require(!player.isDeadOrDying(), "last stand allowed a real death");
        require(player.getHealth() == 1.0F, "last stand did not stop lethal damage at 1 HP");
        require(player.getInventory().countItem(Items.TOTEM_OF_UNDYING) == beforeTotems,
                "emergency totem was consumed before last stand");
        require(AugmentationPlayerStateService.state(player).cooldownReadyTick(id("last_stand")) > now,
                "last stand did not consume its cooldown");
        helper.succeed();
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

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
