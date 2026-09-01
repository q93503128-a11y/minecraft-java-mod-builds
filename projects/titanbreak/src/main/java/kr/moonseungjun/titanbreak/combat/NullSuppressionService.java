package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NullSuppressionService {
    private static final Set<String> COUNTERABLE = Set.of(
            "reflex_drive_i",
            "phase_step_spine",
            "combat_autopilot",
            "overdrive_circulation",
            "jump_booster_legs",
            "propulsion_legs",
            "blade_arm",
            "high_frequency_blade_arm",
            "power_arm",
            "wire_hook_arm",
            "rail_projector_arm",
            "photon_emitter_arm",
            "shock_palm",
            "shield_projector_arm"
    );

    private static final Map<UUID, SuppressionState> STATES = new ConcurrentHashMap<>();

    private NullSuppressionService() {}

    public static Set<String> apply(ServerPlayer player, int ticks, int maximumSystems) {
        if (!(player.level() instanceof ServerLevel level)) return Set.of();
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        List<String> candidates = rankedCounterableSystems(state);
        if (candidates.isEmpty()) {
            STATES.remove(player.getUUID());
            return Set.of();
        }

        int count = Math.max(1, Math.min(Math.max(1, maximumSystems), candidates.size()));
        LinkedHashSet<String> blocked = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) blocked.add(candidates.get(i));

        long until = level.getGameTime() + Math.max(1, ticks);
        STATES.put(player.getUUID(), new SuppressionState(until, Set.copyOf(blocked)));

        if (blocked.contains("reflex_drive_i")) {
            ReflexDriveService.setRequested(player, false);
            ReflexDriveService.setActive(player, false);
        }
        if (blocked.contains("combat_autopilot")) {
            CombatAutopilotService.clear(player.getUUID());
        }
        if (blocked.contains("overdrive_circulation")) {
            OverdriveCirculationService.clear(player);
        }
        return Set.copyOf(blocked);
    }

    public static boolean isSuppressed(ServerPlayer player, String augmentId) {
        SuppressionState state = current(player);
        return state != null && state.blocked().contains(augmentId);
    }

    public static Set<String> blockedSystems(ServerPlayer player) {
        SuppressionState state = current(player);
        return state == null ? Set.of() : state.blocked();
    }

    public static int remainingTicks(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        SuppressionState state = current(player);
        if (state == null) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, state.untilTick() - level.getGameTime()));
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void clearAll() {
        STATES.clear();
    }

    private static SuppressionState current(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        SuppressionState state = STATES.get(player.getUUID());
        if (state == null) return null;
        if (state.untilTick() <= level.getGameTime()) {
            STATES.remove(player.getUUID(), state);
            return null;
        }
        return state;
    }

    private static List<String> rankedCounterableSystems(TitanPlayerData.State state) {
        Set<String> installed = new LinkedHashSet<>(state.installedView().values());
        List<String> result = new ArrayList<>();
        for (String id : installed) if (COUNTERABLE.contains(id)) result.add(id);
        result.sort(Comparator
                .comparingInt((String id) -> suppressionPriority(state, id))
                .reversed()
                .thenComparing(id -> id));
        return result;
    }

    private static int suppressionPriority(TitanPlayerData.State state, String augmentId) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance(augmentId);
        int score = definition == null ? 0
                : definition.tier() * 100
                + Math.max(0, definition.powerLoad()) * 3
                + Math.max(0, definition.heatLoad()) * 2
                + Math.max(0, definition.neuralLoad()) * 4;
        if (instance != null) score += instance.mk() * 12 + instance.enhancement() * 3;
        return score;
    }

    private record SuppressionState(long untilTick, Set<String> blocked) {}
}
