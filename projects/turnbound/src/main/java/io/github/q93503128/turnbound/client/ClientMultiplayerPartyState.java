package io.github.q93503128.turnbound.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Client read-only projection of the server-owned player party. */
public final class ClientMultiplayerPartyState {
    public record Member(
            UUID id, String name, boolean leader, boolean online, boolean sameLevel, boolean inBattle,
            double x, double y, double z
    ) {}

    public record Snapshot(UUID leader, int size, List<Member> members) {
        public Snapshot { members = List.copyOf(members); }
        public static Snapshot empty() { return new Snapshot(null, 1, List.of()); }
    }

    private static volatile Snapshot snapshot = Snapshot.empty();
    private static volatile long revision;

    private ClientMultiplayerPartyState() {}

    public static Snapshot snapshot() { return snapshot; }
    public static long revision() { return revision; }

    public static void update(String raw) {
        UUID leader = null;
        int size = 1;
        List<Member> members = new ArrayList<>();
        if (raw != null) {
            for (String line : raw.split("\n")) {
                if (line.isBlank()) continue;
                String[] p = line.split("\\|", -1);
                try {
                    if ("H".equals(p[0]) && p.length >= 3) {
                        leader = p[1].isBlank() ? null : UUID.fromString(p[1]);
                        size = Math.max(1, Integer.parseInt(p[2]));
                    } else if ("M".equals(p[0]) && p.length >= 10) {
                        members.add(new Member(
                                UUID.fromString(p[1]), read(p[2]), bit(p[3]), bit(p[4]), bit(p[5]), bit(p[6]),
                                Double.parseDouble(p[7]), Double.parseDouble(p[8]), Double.parseDouble(p[9])));
                    }
                } catch (RuntimeException ignored) {
                    // One stale/offline row must not discard the rest of the party snapshot.
                }
            }
        }
        snapshot = new Snapshot(leader, size, members);
        revision++;
    }

    public static void clear() {
        snapshot = Snapshot.empty();
        revision++;
    }

    private static boolean bit(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static String read(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
