package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.Map;

/** One write-through owner for the phase-2 siege SavedData so independent systems cannot clobber each other. */
public final class VillageSiegePersistence {
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

    public static synchronized void resetForNewGame() {
        INTS.clear();
        STRINGS.clear();
        persist();
    }

    private static void persist() {
        if (savedData != null) savedData.replace(INTS, STRINGS);
    }
}
