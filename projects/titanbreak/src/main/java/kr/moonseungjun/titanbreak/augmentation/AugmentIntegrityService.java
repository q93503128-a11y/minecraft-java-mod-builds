package kr.moonseungjun.titanbreak.augmentation;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Persistent integrity state for installed augmentation instances. */
public final class AugmentIntegrityService {
    public static final String STRAINED = "strained";
    public static final String DAMAGED = "damaged";
    public static final String CRITICAL = "critical";

    private AugmentIntegrityService() {}

    public static int rank(TitanPlayerData.AugmentInstance instance) {
        return instance == null ? 0 : rank(instance.damageState());
    }

    public static int rank(String damageState) {
        if (damageState == null || damageState.isBlank()) return 0;
        return switch (damageState) {
            case STRAINED -> 1;
            case DAMAGED -> 2;
            case CRITICAL -> 3;
            default -> 0;
        };
    }

    public static double loadMultiplier(TitanPlayerData.AugmentInstance instance) {
        return switch (rank(instance)) {
            case 1 -> 1.05D;
            case 2 -> 1.12D;
            case 3 -> 1.24D;
            default -> 1.0D;
        };
    }

    public static double worstLoadMultiplier(TitanPlayerData.State state, String augmentId) {
        double worst = 1.0D;
        Set<String> seen = new HashSet<>();
        for (TitanPlayerData.AugmentInstance instance : state.installedInstanceView().values()) {
            if (!instance.id().equals(augmentId) || !seen.add(instance.serial())) continue;
            worst = Math.max(worst, loadMultiplier(instance));
        }
        return worst;
    }

    public static int damagedCount(TitanPlayerData.State state) {
        int count = 0;
        Set<String> seen = new HashSet<>();
        for (TitanPlayerData.AugmentInstance instance : state.installedInstanceView().values()) {
            if (!seen.add(instance.serial())) continue;
            if (rank(instance) > 0) count++;
        }
        return count;
    }

    public static int worstRank(TitanPlayerData.State state) {
        int worst = 0;
        Set<String> seen = new HashSet<>();
        for (TitanPlayerData.AugmentInstance instance : state.installedInstanceView().values()) {
            if (!seen.add(instance.serial())) continue;
            worst = Math.max(worst, rank(instance));
        }
        return worst;
    }

    public static boolean stress(ServerPlayer player, TitanPlayerData.State state,
                                 TitanPlayerData.AugmentInstance instance, int steps) {
        if (instance == null || steps <= 0) return false;
        int current = rank(instance);
        int next = Math.min(3, current + steps);
        if (next == current) return false;
        return replaceInstalledState(player, state, instance.serial(), stateForRank(next));
    }

    public static boolean repairWorstInstalled(ServerPlayer player, TitanPlayerData.State state) {
        AugmentationCatalog.Slot bestSlot = null;
        TitanPlayerData.AugmentInstance best = null;
        int bestRank = 0;
        Set<String> seen = new HashSet<>();
        for (Map.Entry<AugmentationCatalog.Slot, TitanPlayerData.AugmentInstance> entry
                : state.installedInstanceView().entrySet()) {
            TitanPlayerData.AugmentInstance instance = entry.getValue();
            if (!seen.add(instance.serial())) continue;
            int candidateRank = rank(instance);
            if (candidateRank <= 0) continue;
            if (candidateRank > bestRank || (candidateRank == bestRank
                    && (bestSlot == null || entry.getKey().ordinal() < bestSlot.ordinal()))) {
                bestRank = candidateRank;
                bestSlot = entry.getKey();
                best = instance;
            }
        }
        if (best == null) return false;
        return replaceInstalledState(player, state, best.serial(), stateForRank(bestRank - 1));
    }

    private static boolean replaceInstalledState(ServerPlayer player, TitanPlayerData.State state,
                                                 String serial, String damageState) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        AugmentationCatalog.Slot anchor = null;
        for (Map.Entry<AugmentationCatalog.Slot, TitanPlayerData.AugmentInstance> entry
                : state.installedInstanceView().entrySet()) {
            if (entry.getValue().serial().equals(serial)) {
                anchor = entry.getKey();
                break;
            }
        }
        if (anchor == null) return false;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.AugmentInstance removed = data.removeInstance(player, anchor);
        if (removed == null) return false;
        TitanPlayerData.AugmentInstance updated = removed.withDamageState(damageState);
        if (data.installInstance(player, anchor, updated)) return true;

        data.installInstance(player, anchor, removed);
        return false;
    }

    private static String stateForRank(int rank) {
        return switch (Math.max(0, Math.min(3, rank))) {
            case 1 -> STRAINED;
            case 2 -> DAMAGED;
            case 3 -> CRITICAL;
            default -> "";
        };
    }
}
