package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.ReviveAugmentService;
import dev.moonseungjun.augmentationsurvival.mixin.ActiveAbilityServiceInvoker;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative 10-slot active augment router and loadout authority. */
public final class ActiveAbilityRouter {
    private static final String THREAT_SUPPRESSION_ID = id("threat_suppression");
    private static final String DEATH_RETURN_ID = id("death_return");
    private static final String RETURN_HOME_ID = id("return_home");
    private static final String REWIND_ID = id("rewind");
    private static final String TIME_ACCELERATION_ID = id("time_acceleration");
    private static final String CLONE_ID = CloneActiveService.CLONE_ID;
    private static final String SHIELD_THROW_ID = id("shield_throw");
    private static final String TEMPORARY_PLATFORM_ID = TemporaryPlatformService.TEMPORARY_PLATFORM_ID;
    private static final String PHASE_SHIFT_ID = id("phase_shift");
    private static final String DOMINATION_ID = id("domination");
    private static final String GIGANTIFICATION_ID = id("gigantification");
    private static final String AREA_ACCELERATION_ID = id("area_acceleration");
    private static final String REVIVE_ID = id("revive");
    private static final String TIME_STOP_ID = id("time_stop");
    private static final String DIVINE_PUNISHMENT_ID = DivinePunishmentService.ID;
    private static final String THREAT_TOGGLE_KEY = "active_toggle/threat_suppression";
    private static final String MANUAL_LAYOUT_KEY = "active_loadout/manual_layout";

    private static final List<String> LEGACY_ORDER = List.of(
            id("cleanse"), id("pull"), id("swap"), id("shield"), id("berserk"), id("healing_wave")
    );
    private static final List<String> ALL_ORDER = List.of(
            id("cleanse"), id("pull"), id("swap"), id("shield"), TEMPORARY_PLATFORM_ID,
            id("berserk"), id("healing_wave"), SHIELD_THROW_ID, REWIND_ID, TIME_ACCELERATION_ID,
            CLONE_ID, PHASE_SHIFT_ID, THREAT_SUPPRESSION_ID, DEATH_RETURN_ID,
            RETURN_HOME_ID, DOMINATION_ID, GIGANTIFICATION_ID, AREA_ACCELERATION_ID, REVIVE_ID,
            TIME_STOP_ID, DIVINE_PUNISHMENT_ID
    );
    private static final Set<String> ALL_SUPPORTED = Set.copyOf(ALL_ORDER);

    private ActiveAbilityRouter() { }

    public static void initialize() {
        CloneActiveService.initialize();
        TimeStopIntegrityService.initialize();
        TimeStopBoundaryService.initialize();
        DivinePunishmentService.initialize();
        for (String augmentId : ALL_ORDER) {
            AugmentCatalog.require(augmentId);
            if (!ActivePresentationService.hasProfile(augmentId)) {
                throw new IllegalStateException("Missing active presentation profile: " + augmentId);
            }
        }
    }

    public static boolean tryUseSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 10 || player.isDeadOrDying() || CombatAugmentService.isActionLocked(player)) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> slots = resolvedSlots(state);
        String abilityId = slots.get(slotIndex);
        if (abilityId.isBlank()) return false;
        ActivePresentationService.Capture capture = ActivePresentationService.capture(player, abilityId);
        boolean success = executeResolved(player, state, abilityId);
        if (success) ActivePresentationService.presentSuccessfulUse(player, abilityId, capture);
        return success;
    }

    private static boolean executeResolved(ServerPlayer player, AugmentationPlayerState state, String abilityId) {
        if (abilityId.equals(THREAT_SUPPRESSION_ID)) return toggle(player, state, THREAT_SUPPRESSION_ID, THREAT_TOGGLE_KEY);
        if (abilityId.equals(DEATH_RETURN_ID)) return DeathReturnAugmentService.tryUse(player);
        if (abilityId.equals(CLONE_ID)) return CloneActiveService.tryUse(player);
        if (abilityId.equals(TEMPORARY_PLATFORM_ID)) return TemporaryPlatformService.tryUse(player);
        if (abilityId.equals(REWIND_ID) || abilityId.equals(TIME_ACCELERATION_ID) || abilityId.equals(SHIELD_THROW_ID)) {
            return AdvancedActiveAugmentService.tryUse(player, abilityId);
        }
        if (abilityId.equals(PHASE_SHIFT_ID) || abilityId.equals(DOMINATION_ID) || abilityId.equals(GIGANTIFICATION_ID)) {
            return AdvancedWorldAugmentService.tryUse(player, abilityId);
        }
        if (abilityId.equals(AREA_ACCELERATION_ID)) return AreaAccelerationService.tryUse(player);
        if (abilityId.equals(REVIVE_ID)) return ReviveAugmentService.tryUse(player);
        if (abilityId.equals(TIME_STOP_ID)) {
            boolean used = SpecialActiveAugmentService.tryUse(player, abilityId);
            if (used) TimeStopBoundaryService.begin(player);
            return used;
        }
        if (abilityId.equals(DIVINE_PUNISHMENT_ID)) return DivinePunishmentService.tryUse(player);
        if (abilityId.equals(RETURN_HOME_ID)) return SpecialActiveAugmentService.tryUse(player, abilityId);
        if (LEGACY_ORDER.contains(abilityId)) return ActiveAbilityServiceInvoker.augmentationSurvival$tryUse(player, state, abilityId);
        return false;
    }

    public static List<String> supportedIds() { return ALL_ORDER; }

    public static List<String> ownedActiveIds(AugmentationPlayerState state) {
        List<String> owned = new ArrayList<>();
        for (String id : ALL_ORDER) if (boundedLevel(state, id) > 0) owned.add(id);
        return List.copyOf(owned);
    }

    public static List<String> resolvedSlots(AugmentationPlayerState state) {
        List<String> result = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) result.add("");

        Set<String> used = new HashSet<>();
        if (!state.activeSlots().isEmpty()) {
            for (int i = 0; i < Math.min(10, state.activeSlots().size()); i++) {
                String id = state.activeSlots().get(i);
                if (id != null && ALL_SUPPORTED.contains(id) && boundedLevel(state, id) > 0 && used.add(id)) {
                    result.set(i, id);
                }
            }
        }

        if (state.persistentInt(MANUAL_LAYOUT_KEY) <= 0) {
            for (String id : ALL_ORDER) {
                if (boundedLevel(state, id) <= 0 || used.contains(id)) continue;
                int empty = result.indexOf("");
                if (empty < 0) break;
                result.set(empty, id);
                used.add(id);
            }
        }
        return result;
    }

    public static boolean assignSlot(ServerPlayer player, int slot, String augmentId) {
        if (slot < 0 || slot >= 10 || augmentId == null || !ALL_SUPPORTED.contains(augmentId)) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, augmentId) <= 0) return false;
        List<String> slots = new ArrayList<>(resolvedSlots(state));
        for (int i = 0; i < slots.size(); i++) if (augmentId.equals(slots.get(i))) slots.set(i, "");
        slots.set(slot, augmentId);
        AugmentationPlayerState next = state.withActiveSlots(slots).withPersistentInt(MANUAL_LAYOUT_KEY, 1);
        AugmentationPlayerStateService.replace(player, next);
        return true;
    }

    public static boolean clearSlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= 10) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> slots = new ArrayList<>(resolvedSlots(state));
        slots.set(slot, "");
        AugmentationPlayerState next = state.withActiveSlots(slots).withPersistentInt(MANUAL_LAYOUT_KEY, 1);
        AugmentationPlayerStateService.replace(player, next);
        return true;
    }

    public static boolean swapSlots(ServerPlayer player, int first, int second) {
        if (first < 0 || first >= 10 || second < 0 || second >= 10 || first == second) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> slots = new ArrayList<>(resolvedSlots(state));
        String value = slots.get(first);
        slots.set(first, slots.get(second));
        slots.set(second, value);
        AugmentationPlayerState next = state.withActiveSlots(slots).withPersistentInt(MANUAL_LAYOUT_KEY, 1);
        AugmentationPlayerStateService.replace(player, next);
        return true;
    }

    public static boolean isThreatSuppressionEnabled(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        return boundedLevel(state, THREAT_SUPPRESSION_ID) > 0 && state.persistentInt(THREAT_TOGGLE_KEY) > 0;
    }

    private static boolean toggle(ServerPlayer player, AugmentationPlayerState state, String augmentId, String key) {
        if (boundedLevel(state, augmentId) <= 0) return false;
        int next = state.persistentInt(key) > 0 ? 0 : 1;
        AugmentationPlayerStateService.replace(player, state.withPersistentInt(key, next));
        return true;
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }
}
