package io.github.q93503128.turnbound.world;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Minecraft-free codec and lookup rules for per-player Drehmal onboarding milestones. */
final class DrehmalOnboardingFlags {
    private DrehmalOnboardingFlags() {}

    static Set<String> decode(List<String> encoded) {
        if (encoded == null || encoded.isEmpty()) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        for (String value : encoded) {
            int split = value == null ? -1 : value.indexOf('|');
            if (split <= 0 || split >= value.length() - 1) continue;
            try {
                UUID playerId = UUID.fromString(value.substring(0, split));
                String flag = clean(value.substring(split + 1));
                if (!flag.isBlank()) out.add(entry(playerId, flag));
            } catch (IllegalArgumentException ignored) {
                // Corrupt foreign entries are ignored without invalidating the authored-world save.
            }
        }
        return Set.copyOf(out);
    }

    static boolean contains(Set<String> entries, UUID playerId, String flag) {
        String clean = clean(flag);
        return entries != null && playerId != null && !clean.isBlank()
                && entries.contains(entry(playerId, clean));
    }

    static Set<String> forPlayer(Set<String> entries, UUID playerId) {
        if (entries == null || entries.isEmpty() || playerId == null) return Set.of();
        String prefix = playerId + "|";
        Set<String> out = new LinkedHashSet<>();
        for (String value : entries) {
            if (value != null && value.startsWith(prefix) && value.length() > prefix.length()) {
                out.add(value.substring(prefix.length()));
            }
        }
        return Set.copyOf(out);
    }

    static boolean add(Set<String> entries, UUID playerId, String flag) {
        String clean = clean(flag);
        return entries != null && playerId != null && !clean.isBlank()
                && entries.add(entry(playerId, clean));
    }

    private static String entry(UUID playerId, String flag) {
        return playerId + "|" + flag;
    }

    private static String clean(String flag) {
        String clean = flag == null ? "" : flag.trim();
        if (clean.isBlank() || clean.length() > 64 || clean.indexOf('|') >= 0) return "";
        return clean;
    }
}
