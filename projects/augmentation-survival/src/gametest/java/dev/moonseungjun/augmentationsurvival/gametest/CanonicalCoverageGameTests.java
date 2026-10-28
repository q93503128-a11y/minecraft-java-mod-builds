package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentIconCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.stat.PlayerStatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.stat.StatAugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Canonical whole-roster contracts that exercise every augment identity through real runtime state. */
public final class CanonicalCoverageGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";
    private static final Set<String> DEDICATED_KEY_ACTIVES = Set.of(
            AuthorityTheftService.AUTHORITY_THEFT_ID,
            FlightAugmentService.FLIGHT_ID,
            UtilityAugmentService.INSTANT_MINING_ID
    );

    @GameTest public void everyCanonicalAugmentRoundTripsThroughServerPlayerState(GameTestHelper helper) {
        ServerPlayer player = player(helper); List<AugmentDefinition> definitions = AugmentCatalog.allDefinitions();
        require(definitions.size() == 149, "canonical roster size drifted; expected=149, actual=" + definitions.size());
        for (AugmentDefinition definition : definitions) {
            AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial().withAugmentLevel(definition.id(), definition.maxLevel()));
            AugmentationPlayerState persisted = AugmentationPlayerStateService.state(player);
            require(persisted.augmentLevels().size() == 1, "single-augment state gained unrelated identities for " + definition.id());
            require(persisted.level(definition.id()) == definition.maxLevel(), "server player state failed max-level round trip for " + definition.id());
            AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
            require(AugmentationPlayerStateService.state(player).augmentLevels().isEmpty(), "server player state failed to clear after " + definition.id());
        }
        GameTestPlayers.succeed(helper);
    }

    @GameTest public void everyCanonicalAugmentIndexAndPowerBoundaryIsStable(GameTestHelper helper) {
        List<AugmentDefinition> definitions = AugmentCatalog.allDefinitions();
        require(definitions.size() == 149, "canonical roster size drifted; expected=149, actual=" + definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            AugmentDefinition definition = definitions.get(index);
            require(AugmentCatalog.indexOf(definition.id()) == index, "catalog index lookup drifted for " + definition.id());
            require(AugmentCatalog.definitionAt(index).equals(definition), "catalog reverse index drifted for " + definition.id());
            requireClose(definition.powerScore(-1), 0.0D, 1.0E-9D, "negative level produced power for " + definition.id());
            requireClose(definition.powerScore(0), 0.0D, 1.0E-9D, "zero level produced power for " + definition.id());
            double max = definition.powerScore(definition.maxLevel()); require(Double.isFinite(max) && max > 0.0D, "max-level power score is not finite and positive for " + definition.id());
            requireClose(definition.powerScore(definition.maxLevel() + 100), max, 1.0E-9D, "power score exceeded canonical max-level cap for " + definition.id());
        }
        helper.succeed();
    }

    @GameTest public void everyCanonicalAugmentHasDistinctIcon(GameTestHelper helper) {
        var icons = AugmentIconCatalog.canonicalIcons();
        require(icons.size() == 149, "icon catalog does not cover all 149 augments; actual=" + icons.size());
        require(new LinkedHashSet<>(icons.values()).size() == 149,
                "two or more canonical augments share the same icon item");
        for (AugmentDefinition definition : AugmentCatalog.allDefinitions()) {
            require(icons.containsKey(definition.id()), "missing icon for canonical augment: " + definition.id());
        }
        helper.succeed();
    }

    @GameTest public void canonicalActiveRosterMatchesServerRoutingContracts(GameTestHelper helper) {
        Set<String> canonicalActives = new LinkedHashSet<>();
        for (AugmentDefinition definition : AugmentCatalog.allDefinitions()) if (definition.canonicalNameKo().contains("액티브")) canonicalActives.add(definition.id());
        // Instant Mining is canonically a toggle controlled by its own rebindable key.
        canonicalActives.add(UtilityAugmentService.INSTANT_MINING_ID);
        Set<String> expectedStandardActives = new LinkedHashSet<>(canonicalActives); expectedStandardActives.removeAll(DEDICATED_KEY_ACTIVES);
        List<String> routedList = ActiveAbilityRouter.supportedIds(); Set<String> routed = new LinkedHashSet<>(routedList);
        require(routed.size() == routedList.size(), "server active router contains duplicate IDs");
        require(routed.equals(expectedStandardActives), "canonical standard-active roster and server router diverged; expected=" + expectedStandardActives + ", actual=" + routed);
        require(canonicalActives.containsAll(DEDICATED_KEY_ACTIVES), "dedicated-key active is no longer marked active in the canonical roster");
        for (String dedicated : DEDICATED_KEY_ACTIVES) { require(!routed.contains(dedicated), "dedicated-key active leaked into the normal 10-slot router: " + dedicated); AugmentCatalog.require(dedicated); }
        helper.succeed();
    }

    @GameTest public void everyStandardActiveRejectsUnownedThenSupportsTenthSlot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (String activeId : ActiveAbilityRouter.supportedIds()) {
            AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
            require(!ActiveAbilityRouter.assignSlot(player, 9, activeId), "unowned active was assignable to slot 10: " + activeId);
            AugmentDefinition definition = AugmentCatalog.require(activeId);
            AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial().withAugmentLevel(activeId, definition.maxLevel()));
            require(ActiveAbilityRouter.assignSlot(player, 9, activeId), "owned active could not be assigned to slot 10: " + activeId);
            require(activeId.equals(ActiveAbilityRouter.resolvedSlots(AugmentationPlayerStateService.state(player)).get(9)), "slot 10 resolved to the wrong active: " + activeId);
            require(ActiveAbilityRouter.clearSlot(player, 9), "slot 10 could not be cleared for active: " + activeId);
            require(ActiveAbilityRouter.resolvedSlots(AugmentationPlayerStateService.state(player)).get(9).isBlank(), "slot 10 retained an active after clear: " + activeId);
        }
        require(!ActiveAbilityRouter.assignSlot(player, -1, ActiveAbilityRouter.supportedIds().getFirst()), "negative active slot index was accepted");
        require(!ActiveAbilityRouter.assignSlot(player, 10, ActiveAbilityRouter.supportedIds().getFirst()), "slot index 10 exceeded the canonical 0..9 range");
        GameTestPlayers.succeed(helper);
    }

    @GameTest public void everyDataDrivenStatAugmentAppliesAtOneAndMaxThenClears(GameTestHelper helper) {
        ServerPlayer player = player(helper); AugmentationPlayerState empty = AugmentationPlayerState.initial();
        for (StatAugmentCatalog.StatModifierDefinition stat : StatAugmentCatalog.current().definitions()) {
            AugmentDefinition definition = AugmentCatalog.require(stat.augmentId()); PlayerStatAugmentService.reconcile(player, empty); double baseline = player.getAttributeValue(stat.attribute().attribute());
            AugmentationPlayerState levelOne = empty.withAugmentLevel(stat.augmentId(), 1); PlayerStatAugmentService.reconcile(player, levelOne); double one = player.getAttributeValue(stat.attribute().attribute()); require(one > baseline, "level-one stat augment did not increase its attribute: " + stat.augmentId());
            AugmentationPlayerState maxLevel = empty.withAugmentLevel(stat.augmentId(), definition.maxLevel()); PlayerStatAugmentService.reconcile(player, maxLevel); double max = player.getAttributeValue(stat.attribute().attribute()); require(max + 1.0E-9D >= one, "max-level stat augment regressed below level one: " + stat.augmentId());
            PlayerStatAugmentService.reconcile(player, empty); requireClose(player.getAttributeValue(stat.attribute().attribute()), baseline, 1.0E-9D, "stat augment modifier did not clear: " + stat.augmentId());
        }
        GameTestPlayers.succeed(helper);
    }

    @GameTest public void mythicGigantificationIgnoresCommonActiveCooldownModifiers(GameTestHelper helper) {
        ServerPlayer player = player(helper); String giant = id("gigantification"); long now = player.level().getGameTime();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial().withAugmentLevel(giant, 1).withAugmentLevel(id("ability_haste"), 1).withAugmentLevel(id("double_charge"), 1).withAugmentLevel(id("combat_cycle"), 1).withActiveSlots(List.of(giant)));
        require(ActiveAbilityRouter.tryUseSlot(player, 0), "mythic gigantification failed its initial cast");
        AugmentationPlayerState afterCast = AugmentationPlayerStateService.state(player); long expectedReady = now + AdvancedWorldTuning.current().gigantificationCooldownTicks();
        require(afterCast.cooldownReadyTick(giant) == expectedReady, "ability haste incorrectly changed mythic gigantification cooldown");
        require(afterCast.cooldownReadyTick(giant + "#charge0") == 0L && afterCast.cooldownReadyTick(giant + "#charge1") == 0L, "double charge incorrectly created mythic charge cooldowns");
        require(!ActiveAbilityRouter.tryUseSlot(player, 0), "mythic gigantification accepted an immediate second cast");
        ActiveAbilityService.reduceSupportedCooldowns(player, 200); require(AugmentationPlayerStateService.state(player).cooldownReadyTick(giant) == expectedReady, "combat cycle incorrectly reduced a mythic active cooldown");
        GameTestPlayers.succeed(helper);
    }

    @GameTest public void glassCannonHealthPenaltyMatchesCanonicalTuningAndClamps(GameTestHelper helper) {
        ServerPlayer player = player(helper); String glassCannon = id("glass_cannon"); AugmentDefinition definition = AugmentCatalog.require(glassCannon); AugmentationPlayerState empty = AugmentationPlayerState.initial();
        PlayerStatAugmentService.reconcile(player, empty); double baseline = player.getAttributeValue(Attributes.MAX_HEALTH);
        PlayerStatAugmentService.reconcile(player, empty.withAugmentLevel(glassCannon, 1)); double one = player.getAttributeValue(Attributes.MAX_HEALTH); requireClose(one, baseline * ExpansionTuning.current().glassCannonHealthMultiplier(1), 1.0E-9D, "glass cannon level-one health multiplier diverged from canonical tuning");
        PlayerStatAugmentService.reconcile(player, empty.withAugmentLevel(glassCannon, definition.maxLevel())); double max = player.getAttributeValue(Attributes.MAX_HEALTH); requireClose(max, baseline * ExpansionTuning.current().glassCannonHealthMultiplier(definition.maxLevel()), 1.0E-9D, "glass cannon max-level health multiplier diverged from canonical tuning");
        PlayerStatAugmentService.reconcile(player, empty.withAugmentLevel(glassCannon, definition.maxLevel() + 50)); requireClose(player.getAttributeValue(Attributes.MAX_HEALTH), max, 1.0E-9D, "glass cannon exceeded canonical max-level clamping");
        PlayerStatAugmentService.reconcile(player, empty); requireClose(player.getAttributeValue(Attributes.MAX_HEALTH), baseline, 1.0E-9D, "glass cannon max-health modifier did not clear");
        GameTestPlayers.succeed(helper);
    }

    private static ServerPlayer player(GameTestHelper helper) { ServerPlayer player = GameTestPlayers.survivalPlayer(helper); BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1)); player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D); player.setHealth(player.getMaxHealth()); return player; }
    private static String id(String path) { return PREFIX + path; }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void requireClose(double actual, double expected, double tolerance, String message) { if (Math.abs(actual - expected) > tolerance) throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual); }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { GameTestPlayers.invoke(this, helper, method); }
}
