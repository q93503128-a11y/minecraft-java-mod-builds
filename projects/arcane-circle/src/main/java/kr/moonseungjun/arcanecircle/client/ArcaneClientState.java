package kr.moonseungjun.arcanecircle.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ArcaneClientState {
    private static Map<String, String> values = Map.of();

    private ArcaneClientState() {}

    public static void update(String snapshot) {
        Map<String, String> parsed = new HashMap<>();
        for (String part : snapshot.split(";")) {
            int index = part.indexOf('=');
            if (index > 0) parsed.put(part.substring(0, index), part.substring(index + 1));
        }
        values = Map.copyOf(parsed);
    }

    public static int integer(String key, int fallback) {
        try { return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static int selected() { return Math.max(0, Math.min(4, integer("selected", 0))); }

    public static List<String> slots() {
        String raw = values.getOrDefault("slots", "arcane_dart|ember|frost_needle|gale_step|lesser_ward");
        List<String> result = new ArrayList<>(List.of(raw.split("\\|", -1)));
        while (result.size() < 5) result.add("arcane_dart");
        return result.subList(0, 5);
    }

    public static Set<String> known() {
        String raw = values.getOrDefault("known", "");
        if (raw.isBlank()) return Set.of();
        return List.of(raw.split("\\|")).stream().collect(Collectors.toUnmodifiableSet());
    }
}
