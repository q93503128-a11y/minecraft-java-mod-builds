package dev.moonseungjun.augmentationsurvival.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client cache of acquired augment levels. Server snapshots are authoritative. */
public final class ClientAugmentProgressState {
    private static Map<String, Integer> levels = Map.of();

    private ClientAugmentProgressState() { }

    public static void apply(List<String> ids, List<Integer> values) {
        LinkedHashMap<String, Integer> next = new LinkedHashMap<>();
        int count = Math.min(ids.size(), values.size());
        for (int i = 0; i < count; i++) {
            String id = ids.get(i);
            int level = values.get(i);
            if (id != null && !id.isBlank() && level > 0) next.put(id, level);
        }
        levels = Map.copyOf(next);
    }

    public static void clear() { levels = Map.of(); }
    public static Map<String, Integer> levels() { return levels; }
    public static int level(String id) { return levels.getOrDefault(id, 0); }
}
