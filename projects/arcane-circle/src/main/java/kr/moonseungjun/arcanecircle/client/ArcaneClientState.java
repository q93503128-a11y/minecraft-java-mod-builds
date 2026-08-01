package kr.moonseungjun.arcanecircle.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ArcaneClientState {
    private record Cooldown(int remainingTicks, int totalTicks) {}

    private static Map<String, String> values = Map.of();
    private static Map<String, Cooldown> cooldowns = Map.of();
    private static long updatedAtNanos;

    private ArcaneClientState() {}

    public static void update(String snapshot) {
        Map<String, String> parsed = new HashMap<>();
        for (String part : snapshot.split(";")) {
            int index = part.indexOf('=');
            if (index > 0) parsed.put(part.substring(0, index), part.substring(index + 1));
        }
        values = Map.copyOf(parsed);
        cooldowns = parseCooldowns(parsed.getOrDefault("cooldowns", ""));
        updatedAtNanos = System.nanoTime();
    }

    public static void reset() {
        values = Map.of();
        cooldowns = Map.of();
        updatedAtNanos = 0L;
    }

    public static boolean ready() {
        return !values.isEmpty();
    }

    public static int integer(String key, int fallback) {
        try { return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static double decimal(String key, double fallback) {
        try { return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static String text(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public static List<String> slots() {
        List<String> parsed = split(text("slots", "arcane_dart|ember|frost_needle|gale_step|lesser_ward"));
        List<String> result = new ArrayList<>(List.of("arcane_dart", "ember", "frost_needle", "gale_step", "lesser_ward"));
        for (int i = 0; i < Math.min(5, parsed.size()); i++) result.set(i, parsed.get(i));
        return List.copyOf(result);
    }

    public static String slot(int index) {
        List<String> slots = slots();
        return index >= 0 && index < slots.size() ? slots.get(index) : "";
    }

    public static List<String> queue() {
        return split(text("queue", ""));
    }

    public static String queueResult() {
        return text("queue_result", "");
    }

    public static List<String> queueCandidates() {
        return split(text("queue_candidates", ""));
    }

    public static boolean queueCanExtend() {
        return integer("queue_extend", 0) != 0;
    }

    public static Set<String> known() {
        return split(text("known", "")).stream().collect(Collectors.toUnmodifiableSet());
    }

    public static int mastery(String spellId) {
        String raw = text("mastery", "");
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

    public static int cooldownRemainingTicks(int slot) {
        Cooldown cooldown = cooldowns.get(slot(slot));
        if (cooldown == null) return 0;
        long elapsed = Math.max(0L, (System.nanoTime() - updatedAtNanos) / 50_000_000L);
        return Math.max(0, cooldown.remainingTicks() - (int) Math.min(Integer.MAX_VALUE, elapsed));
    }

    public static double cooldownFraction(int slot) {
        Cooldown cooldown = cooldowns.get(slot(slot));
        if (cooldown == null || cooldown.totalTicks() <= 0) return 0.0;
        return Math.min(1.0, cooldownRemainingTicks(slot) / (double) cooldown.totalTicks());
    }

    public static double regenPerSecond() {
        return integer("regen_milli", 2000) / 1000.0;
    }

    public static double staffMultiplier(String key) {
        return integer(key, 1000) / 1000.0;
    }

    private static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return List.of(raw.split("\\|"));
    }

    private static Map<String, Cooldown> parseCooldowns(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        Map<String, Cooldown> result = new HashMap<>();
        for (String entry : raw.split("\\|")) {
            String[] parts = entry.split(":");
            if (parts.length != 3) continue;
            try {
                int remaining = Math.max(0, Integer.parseInt(parts[1]));
                int total = Math.max(1, Integer.parseInt(parts[2]));
                if (remaining > 0) result.put(parts[0], new Cooldown(remaining, total));
            } catch (NumberFormatException ignored) {}
        }
        return Map.copyOf(result);
    }
}
