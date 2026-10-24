package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Unified active-slot router. Cooldown actives stay in their shared engines; special modes route here. */
public final class ActiveAbilityRouter {
    private static final String THREAT_SUPPRESSION_ID = id("threat_suppression");
    private static final String INSTANT_MINING_ID = id("instant_mining");
    private static final String DEATH_RETURN_ID = id("death_return");
    private static final String RETURN_HOME_ID = id("return_home");
    private static final String REWIND_ID = id("rewind");
    private static final String TIME_ACCELERATION_ID = id("time_acceleration");
    private static final String SHIELD_THROW_ID = id("shield_throw");
    private static final String PHASE_SHIFT_ID = id("phase_shift");
    private static final String DOMINATION_ID = id("domination");
    private static final String GIGANTIFICATION_ID = id("gigantification");
    private static final String TIME_STOP_ID = id("time_stop");
    private static final String DIVINE_PUNISHMENT_ID = id("divine_punishment");
    private static final String THREAT_TOGGLE_KEY = "active_toggle/threat_suppression";
    private static final String INSTANT_MINING_TOGGLE_KEY = "active_toggle/instant_mining";

    private static final List<String> LEGACY_ORDER = List.of(
            id("cleanse"), id("pull"), id("swap"), id("shield"), id("berserk"), id("healing_wave")
    );
    private static final List<String> ALL_ORDER = List.of(
            id("cleanse"), id("pull"), id("swap"), id("shield"), id("berserk"), id("healing_wave"),
            SHIELD_THROW_ID, REWIND_ID, TIME_ACCELERATION_ID, PHASE_SHIFT_ID,
            THREAT_SUPPRESSION_ID, INSTANT_MINING_ID, DEATH_RETURN_ID, RETURN_HOME_ID,
            DOMINATION_ID, GIGANTIFICATION_ID, TIME_STOP_ID, DIVINE_PUNISHMENT_ID
    );

    private ActiveAbilityRouter() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                THREAT_SUPPRESSION_ID, INSTANT_MINING_ID, DEATH_RETURN_ID, RETURN_HOME_ID,
                REWIND_ID, TIME_ACCELERATION_ID, SHIELD_THROW_ID, PHASE_SHIFT_ID,
                DOMINATION_ID, GIGANTIFICATION_ID, TIME_STOP_ID, DIVINE_PUNISHMENT_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
    }

    public static boolean tryUseSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 10 || player.isDeadOrDying()
                || CombatAugmentService.isActionLocked(player)) {
            return false;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> resolved = resolveSlots(state, ALL_ORDER);
        if (slotIndex >= resolved.size()) {
            return false;
        }
        String abilityId = resolved.get(slotIndex);
        if (abilityId.equals(THREAT_SUPPRESSION_ID)) {
            return toggle(player, state, THREAT_SUPPRESSION_ID, THREAT_TOGGLE_KEY);
        }
        if (abilityId.equals(INSTANT_MINING_ID)) {
            return toggle(player, state, INSTANT_MINING_ID, INSTANT_MINING_TOGGLE_KEY);
        }
        if (abilityId.equals(DEATH_RETURN_ID)) {
            return DeathReturnAugmentService.tryUse(player);
        }
        if (abilityId.equals(REWIND_ID)
                || abilityId.equals(TIME_ACCELERATION_ID)
                || abilityId.equals(SHIELD_THROW_ID)) {
            return AdvancedActiveAugmentService.tryUse(player, abilityId);
        }
        if (abilityId.equals(PHASE_SHIFT_ID)
                || abilityId.equals(DOMINATION_ID)
                || abilityId.equals(GIGANTIFICATION_ID)) {
            return AdvancedWorldAugmentService.tryUse(player, abilityId);
        }
        if (abilityId.equals(RETURN_HOME_ID)
                || abilityId.equals(TIME_STOP_ID)
                || abilityId.equals(DIVINE_PUNISHMENT_ID)) {
            return SpecialActiveAugmentService.tryUse(player, abilityId);
        }

        List<String> legacy = resolveSlots(state, LEGACY_ORDER);
        int legacySlot = legacy.indexOf(abilityId);
        return legacySlot >= 0 && ActiveAbilityService.tryUseSlot(player, legacySlot);
    }

    public static boolean isThreatSuppressionEnabled(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        return boundedLevel(state, THREAT_SUPPRESSION_ID) > 0
                && state.persistentInt(THREAT_TOGGLE_KEY) > 0;
    }

    public static boolean isInstantMiningEnabled(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        return boundedLevel(state, INSTANT_MINING_ID) > 0
                && state.persistentInt(INSTANT_MINING_TOGGLE_KEY) > 0;
    }

    private static boolean toggle(ServerPlayer player, AugmentationPlayerState state, String augmentId, String key) {
        if (boundedLevel(state, augmentId) <= 0) return false;
        int next = state.persistentInt(key) > 0 ? 0 : 1;
        AugmentationPlayerStateService.replace(player, state.withPersistentInt(key, next));
        return true;
    }

    private static List<String> resolveSlots(AugmentationPlayerState state, List<String> supportedOrder) {
        Set<String> supported = Set.copyOf(supportedOrder);
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String configured : state.activeSlots()) {
            if (supported.contains(configured) && boundedLevel(state, configured) > 0) resolved.add(configured);
        }
        for (String fallback : supportedOrder) {
            if (boundedLevel(state, fallback) > 0) resolved.add(fallback);
        }
        return new ArrayList<>(resolved);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
