package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ClientBattleState {
    public record Unit(
            String id, String defId, String side, String name,
            int hp, int maxHp, int barrier, long gauge, boolean downed,
            double x, double y, double z, List<String> statuses
    ) {
        public Unit {
            statuses = List.copyOf(statuses == null ? List.of() : statuses);
        }
        public Unit(String id, String defId, String side, String name,
                    int hp, int maxHp, int barrier, long gauge, boolean downed) {
            this(id, defId, side, name, hp, maxHp, barrier, gauge, downed, 0.0, 0.0, 0.0, List.of());
        }
        public boolean hasStatus(String id) { return statuses.contains(id); }
    }

    public record Skill(String id, String name, String targetRule, int baseCooldown, int remaining, String description) {
        public Skill(String id, String name, String targetRule, int baseCooldown, int remaining) {
            this(id, name, targetRule, baseCooldown, remaining, "");
        }
    }

    public record Snapshot(
            boolean active, boolean auto, int speed, String outcome, String actorId, boolean finished,
            boolean autoAllowed, boolean speedAllowed, boolean fleeAllowed,
            List<Unit> units, List<String> timeline, List<Skill> skills, String message,
            double arenaX, double arenaY, double arenaZ, float arenaYaw
    ) {
        public Snapshot(boolean active, boolean auto, int speed, String outcome, String actorId, boolean finished,
                        List<Unit> units, List<String> timeline, List<Skill> skills, String message) {
            this(active, auto, speed, outcome, actorId, finished, true, true, true,
                    units, timeline, skills, message, 0.0, 0.0, 0.0, 0.0F);
        }
    }

    private static volatile Snapshot snapshot = new Snapshot(
            false, false, 1, "RUNNING", "", true, true, true, true,
            List.of(), List.of(), List.of(), "", 0.0, 0.0, 0.0, 0.0F);
    private static volatile long revision;

    private ClientBattleState() {}
    public static Snapshot snapshot() { return snapshot; }
    public static long revision() { return revision; }

    public static void update(String raw) {
        boolean active = false, auto = false, finished = false;
        boolean autoAllowed = true, speedAllowed = true, fleeAllowed = true;
        int speed = 1;
        String outcome = "RUNNING", actor = "", message = "";
        double arenaX = 0.0, arenaY = 0.0, arenaZ = 0.0;
        float arenaYaw = 0.0F;
        List<Unit> units = new ArrayList<>();
        List<String> timeline = new ArrayList<>();
        List<Skill> skills = new ArrayList<>();

        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] p = line.split("\\|", -1);
            try {
                switch (p[0]) {
                    case "H" -> {
                        active = "1".equals(p[1]);
                        auto = "1".equals(p[2]);
                        speed = Integer.parseInt(p[3]);
                        outcome = p[4]; actor = p[5]; finished = "1".equals(p[6]);
                        if (p.length > 9) {
                            autoAllowed = "1".equals(p[7]);
                            speedAllowed = "1".equals(p[8]);
                            fleeAllowed = "1".equals(p[9]);
                        }
                    }
                    case "A" -> {
                        arenaX = Double.parseDouble(p[1]); arenaY = Double.parseDouble(p[2]);
                        arenaZ = Double.parseDouble(p[3]); arenaYaw = Float.parseFloat(p[4]);
                    }
                    case "U" -> {
                        List<String> statuses = p.length > 13 && !p[13].isBlank() ? Arrays.asList(p[13].split(",")) : List.of();
                        units.add(new Unit(p[1], p[2], p[3], p[4], Integer.parseInt(p[5]), Integer.parseInt(p[6]),
                                Integer.parseInt(p[7]), Long.parseLong(p[8]), "1".equals(p[9]),
                                p.length > 12 ? Double.parseDouble(p[10]) : 0.0,
                                p.length > 12 ? Double.parseDouble(p[11]) : 0.0,
                                p.length > 12 ? Double.parseDouble(p[12]) : 0.0, statuses));
                    }
                    case "T" -> { if (p.length > 1 && !p[1].isBlank()) timeline.addAll(Arrays.asList(p[1].split(","))); }
                    case "S" -> skills.add(new Skill(p[1], p[2], p[3], Integer.parseInt(p[4]), Integer.parseInt(p[5]), p.length > 6 ? p[6] : ""));
                    default -> { }
                }
            } catch (RuntimeException ignored) { }
        }

        snapshot = new Snapshot(active, auto, speed, outcome, actor, finished, autoAllowed, speedAllowed, fleeAllowed,
                List.copyOf(units), List.copyOf(timeline), List.copyOf(skills), message,
                arenaX, arenaY, arenaZ, arenaYaw);
        revision++;
    }
}
