package kr.moonseungjun.arcanecircle.client;

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

    public static String text(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public static String focus() { return text("focus", "arcane_dart"); }
    public static String weave() { return text("weave", "ember"); }
    public static String fusion() { return text("fusion", ""); }

    public static Set<String> known() {
        String raw = values.getOrDefault("known", "");
        if (raw.isBlank()) return Set.of();
        return List.of(raw.split("\\|")).stream().collect(Collectors.toUnmodifiableSet());
    }

    public static int mastery(String spellId) {
        String raw = values.getOrDefault("mastery", "");
        if (raw.isBlank()) return 0;
        for (String entry : raw.split("\\|")) {
            int split = entry.lastIndexOf(':');
            if (split > 0 && entry.substring(0, split).equals(spellId)) {
                try { return Integer.parseInt(entry.substring(split + 1)); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return 0;
    }
}
