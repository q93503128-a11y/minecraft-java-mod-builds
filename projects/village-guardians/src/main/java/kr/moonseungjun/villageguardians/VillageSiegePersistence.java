package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.Map;

/** One write-through owner for the phase-2 siege SavedData so independent systems cannot clobber each other. */
public final class VillageSiegePersistence {
    private static final String NIGHT = "$night_";
    private static final Map<String, Integer> INTS = new LinkedHashMap<>();
    private static final Map<String, String> STRINGS = new LinkedHashMap<>();
    private static VillageSiegeData savedData;

    private VillageSiegePersistence() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageSiegeData.TYPE);
        INTS.clear();
        STRINGS.clear();
        INTS.putAll(savedData.integers());
        STRINGS.putAll(savedData.strings());
        persist();
    }

    public static synchronized int getInt(String key, int fallback) {
        return INTS.getOrDefault(key, fallback);
    }

    public static synchronized void putInt(String key, int value) {
        INTS.put(key, value);
        persist();
    }

    public static synchronized String getString(String key, String fallback) {
        return STRINGS.getOrDefault(key, fallback);
    }

    public static synchronized void putString(String key, String value) {
        STRINGS.put(key, value == null ? "" : value);
        persist();
    }

    public static synchronized void removeString(String key) {
        STRINGS.remove(key);
        persist();
    }

    public static synchronized Map<String, String> stringsWithPrefix(String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        STRINGS.forEach((key, value) -> {
            if (key.startsWith(prefix)) result.put(key, value);
        });
        return result;
    }

    /** Save exactly the phase-2 combat state that is allowed to change during the coming night. */
    public static synchronized void captureNightSnapshot() {
        INTS.keySet().removeIf(key -> key.startsWith(NIGHT + "segment_hp_")
                || key.startsWith(NIGHT + "segment_breach_"));
        STRINGS.keySet().removeIf(key -> key.startsWith(NIGHT + "turret_"));

        Map<String, Integer> intsCopy = new LinkedHashMap<>(INTS);
        intsCopy.forEach((key, value) -> {
            if (key.startsWith("segment_hp_") || key.startsWith("segment_breach_")) {
                INTS.put(NIGHT + key, value);
            }
        });
        Map<String, String> stringsCopy = new LinkedHashMap<>(STRINGS);
        stringsCopy.forEach((key, value) -> {
            if (key.startsWith("turret_")) STRINGS.put(NIGHT + key, value);
        });
        INTS.put(NIGHT + "snapshot_ready", 1);
        persist();
    }

    /** Restore the failed night to the exact segment/turret state captured before combat began. */
    public static synchronized void restoreNightSnapshot() {
        if (INTS.getOrDefault(NIGHT + "snapshot_ready", 0) <= 0) return;
        INTS.keySet().removeIf(key -> key.startsWith("segment_hp_") || key.startsWith("segment_breach_"));
        STRINGS.keySet().removeIf(key -> key.startsWith("turret_"));

        Map<String, Integer> intsCopy = new LinkedHashMap<>(INTS);
        intsCopy.forEach((key, value) -> {
            if (key.startsWith(NIGHT + "segment_hp_") || key.startsWith(NIGHT + "segment_breach_")) {
                INTS.put(key.substring(NIGHT.length()), value);
            }
        });
        Map<String, String> stringsCopy = new LinkedHashMap<>(STRINGS);
        stringsCopy.forEach((key, value) -> {
            if (key.startsWith(NIGHT + "turret_")) STRINGS.put(key.substring(NIGHT.length()), value);
        });
        persist();
    }

    public static synchronized void resetForNewGame() {
        INTS.clear();
        STRINGS.clear();
        persist();
    }

    private static void persist() {
        if (savedData != null) savedData.replace(INTS, STRINGS);
    }
}
