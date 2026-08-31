package kr.moonseungjun.titanbreak.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TitanClientState {
    public record AugmentMeta(String id, int mk, int enhancement, boolean installed, String slot) {}

    private static volatile Map<String, String> values = Map.of();
    private static volatile long snapshotNanos = System.nanoTime();

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
        snapshotNanos = System.nanoTime();
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

    public static double liveCountdownSeconds(String tickKey) {
        int ticks = integer(tickKey, 0);
        if (ticks <= 0) return 0.0D;
        double elapsed = Math.max(0.0D, (System.nanoTime() - snapshotNanos) / 1_000_000_000.0D);
        return Math.max(0.0D, ticks / 20.0D - elapsed);
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

    public static AugmentMeta augmentMeta(String augmentId) {
        for (AugmentMeta meta : installedMetadata()) if (meta.id().equals(augmentId)) return meta;
        for (AugmentMeta meta : vaultMetadata()) if (meta.id().equals(augmentId)) return meta;
        return null;
    }

    public static List<AugmentMeta> installedMetadata() {
        List<AugmentMeta> result = new ArrayList<>();
        String raw = text("installedMeta", "");
        if (raw.isEmpty()) return List.of();
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":", 4);
            if (parts.length != 4) continue;
            try {
                result.add(new AugmentMeta(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), true, parts[0]));
            } catch (NumberFormatException ignored) {}
        }
        return List.copyOf(result);
    }

    public static List<AugmentMeta> vaultMetadata() {
        List<AugmentMeta> result = new ArrayList<>();
        String raw = text("vault", "");
        if (raw.isEmpty()) return List.of();
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":", 3);
            if (parts.length != 3) continue;
            try {
                result.add(new AugmentMeta(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), false, ""));
            } catch (NumberFormatException ignored) {}
        }
        return List.copyOf(result);
    }

    public static int masteryLevel(String augmentId) {
        String raw = text("mastery", "");
        if (raw.isEmpty()) return 0;
        for (String entry : raw.split(",")) {
            int split = entry.indexOf(':');
            if (split <= 0 || !entry.substring(0, split).equals(augmentId)) continue;
            try { return Integer.parseInt(entry.substring(split + 1)); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }
}
