package io.github.q93503128.turnbound.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Client read-only projection of the server-owned social player party. */
public final class ClientMultiplayerPartyState {
    public record Member(
            UUID id, String name, boolean leader, boolean online, boolean sameLevel, boolean inBattle, boolean self,
            double x, double y, double z
    ) {}

    public record Invite(UUID id, String name) {}
    public record Candidate(UUID id, String name, boolean inBattle, double x, double y, double z) {}

    public record Snapshot(
            UUID leader, int size, int maxPlayers, List<Member> members,
            Invite pendingInvite, List<Candidate> candidates, String feedback
    ) {
        public Snapshot {
            members = List.copyOf(members == null ? List.of() : members);
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
            feedback = feedback == null ? "" : feedback;
        }
        public static Snapshot empty() { return new Snapshot(null, 1, 4, List.of(), null, List.of(), ""); }
        public boolean localLeader() { return members.stream().anyMatch(member -> member.self() && member.leader()); }
        public Member self() { return members.stream().filter(Member::self).findFirst().orElse(null); }
    }

    private static volatile Snapshot snapshot = Snapshot.empty();
    private static volatile long revision;

    private ClientMultiplayerPartyState() {}

    public static Snapshot snapshot() { return snapshot; }
    public static long revision() { return revision; }

    public static void update(String raw) {
        UUID leader = null;
        int size = 1, maxPlayers = 4;
        List<Member> members = new ArrayList<>();
        Invite invite = null;
        List<Candidate> candidates = new ArrayList<>();
        String feedback = snapshot.feedback();

        if (raw != null) {
            for (String line : raw.split("\n")) {
                if (line.isBlank()) continue;
                String[] p = line.split("\\|", -1);
                try {
                    switch (p[0]) {
                        case "H" -> {
                            if (p.length >= 3) {
                                leader = p[1].isBlank() ? null : UUID.fromString(p[1]);
                                size = Math.max(1, Integer.parseInt(p[2]));
                            }
                            if (p.length >= 4) maxPlayers = Math.max(1, Integer.parseInt(p[3]));
                        }
                        case "M" -> {
                            if (p.length >= 10) {
                                boolean modern = p.length >= 11;
                                int coordinate = modern ? 8 : 7;
                                members.add(new Member(
                                        UUID.fromString(p[1]), read(p[2]), bit(p[3]), bit(p[4]), bit(p[5]), bit(p[6]),
                                        modern && bit(p[7]),
                                        Double.parseDouble(p[coordinate]), Double.parseDouble(p[coordinate + 1]),
                                        Double.parseDouble(p[coordinate + 2])));
                            }
                        }
                        case "I" -> {
                            if (p.length >= 3) invite = new Invite(UUID.fromString(p[1]), read(p[2]));
                        }
                        case "C" -> {
                            if (p.length >= 7) candidates.add(new Candidate(
                                    UUID.fromString(p[1]), read(p[2]), bit(p[3]),
                                    Double.parseDouble(p[4]), Double.parseDouble(p[5]), Double.parseDouble(p[6])));
                        }
                        case "F" -> {
                            if (p.length >= 2) feedback = read(p[1]);
                        }
                        default -> { }
                    }
                } catch (RuntimeException ignored) {
                    // One stale row must not discard the rest of the party snapshot.
                }
            }
        }

        snapshot = new Snapshot(leader, size, maxPlayers, members, invite, candidates, feedback);
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
