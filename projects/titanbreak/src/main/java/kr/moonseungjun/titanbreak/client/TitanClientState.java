package kr.moonseungjun.titanbreak.client;

import java.util.HashMap;
import java.util.Map;

public final class TitanClientState {
    private static volatile Map<String, String> values = Map.of();

    private TitanClientState() {}

    public static void update(String snapshot) {
        Map<String, String> next = new HashMap<>();
        if (snapshot != null) {
            for (String token : snapshot.split(";")) {
                int split = token.indexOf('=');
                if (split <= 0) continue;
                next.put(token.substring(0, split), token.substring(split + 1));
            }
        }
        values = Map.copyOf(next);
    }

    public static String text(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public static double decimal(String key, double fallback) {
        try { return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static int integer(String key, int fallback) {
        try { return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static boolean flag(String key) {
        return integer(key, 0) != 0;
    }

    public static boolean hasInstalled(String augmentId) {
        String installed = text("installed", "");
        if (installed.isEmpty()) return false;
        for (String entry : installed.split(",")) {
            int split = entry.indexOf(':');
            if (split > 0 && entry.substring(split + 1).equals(augmentId)) return true;
        }
        return false;
    }

    public static String installedIn(String slot) {
        String installed = text("installed", "");
        if (installed.isEmpty()) return "";
        for (String entry : installed.split(",")) {
            int split = entry.indexOf(':');
            if (split > 0 && entry.substring(0, split).equals(slot)) return entry.substring(split + 1);
        }
        return "";
    }
}
