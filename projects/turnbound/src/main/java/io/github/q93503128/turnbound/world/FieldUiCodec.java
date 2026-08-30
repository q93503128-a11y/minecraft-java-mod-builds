package io.github.q93503128.turnbound.world;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Line codec kept deliberately small so server-authoritative field state is easy to audit. */
public final class FieldUiCodec {
    private FieldUiCodec() {}

    public static String encode(FieldUiSnapshot snapshot) {
        StringBuilder out = new StringBuilder();
        out.append("H|").append(bit(snapshot.active())).append('|').append(snapshot.mode().name())
                .append('|').append(snapshot.patrolsCleared())
                .append('|').append(snapshot.patrolGoal())
                .append('|').append(bit(snapshot.bossUnlocked()))
                .append('|').append(bit(snapshot.chapterCleared()))
                .append('|').append(snapshot.earnedXp())
                .append('|').append(snapshot.earnedGold()).append('\n');
        out.append("O|").append(text(snapshot.objective())).append('\n');
        out.append("D|").append(text(snapshot.dialogue())).append('\n');
        FieldUiSnapshot.Reward reward = snapshot.reward();
        out.append("R|").append(text(reward.encounterLabel()))
                .append('|').append(reward.xp())
                .append('|').append(reward.gold())
                .append('|').append(bit(reward.firstClear()))
                .append('|').append(bit(reward.chapterCleared())).append('\n');
        for (FieldUiSnapshot.Encounter encounter : snapshot.encounters()) {
            out.append("E|").append(text(encounter.id()))
                    .append('|').append(text(encounter.label()))
                    .append('|').append(bit(encounter.cleared()))
                    .append('|').append(bit(encounter.unlocked()))
                    .append('|').append(bit(encounter.boss())).append('\n');
        }
        for (FieldUiSnapshot.Travel travel : snapshot.travels()) {
            out.append("T|").append(text(travel.id()))
                    .append('|').append(text(travel.label()))
                    .append('|').append(bit(travel.unlocked()))
                    .append('|').append(bit(travel.current())).append('\n');
        }
        return out.toString();
    }

    public static FieldUiSnapshot decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return FieldUiSnapshot.inactive();
        boolean active = false;
        FieldUiSnapshot.Mode mode = FieldUiSnapshot.Mode.NONE;
        int patrols = 0;
        int goal = 0;
        boolean bossUnlocked = false;
        boolean chapterCleared = false;
        int xp = 0;
        int gold = 0;
        String objective = "";
        String dialogue = "";
        FieldUiSnapshot.Reward reward = FieldUiSnapshot.Reward.none();
        List<FieldUiSnapshot.Encounter> encounters = new ArrayList<>();
        List<FieldUiSnapshot.Travel> travels = new ArrayList<>();

        for (String line : encoded.split("\\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", -1);
            try {
                switch (parts[0]) {
                    case "H" -> {
                        if (parts.length < 9) continue;
                        active = bool(parts[1]);
                        mode = FieldUiSnapshot.Mode.valueOf(parts[2]);
                        patrols = Integer.parseInt(parts[3]);
                        goal = Integer.parseInt(parts[4]);
                        bossUnlocked = bool(parts[5]);
                        chapterCleared = bool(parts[6]);
                        xp = Integer.parseInt(parts[7]);
                        gold = Integer.parseInt(parts[8]);
                    }
                    case "O" -> { if (parts.length >= 2) objective = read(parts[1]); }
                    case "D" -> { if (parts.length >= 2) dialogue = read(parts[1]); }
                    case "R" -> {
                        if (parts.length >= 6) reward = new FieldUiSnapshot.Reward(
                                read(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                                bool(parts[4]), bool(parts[5]));
                    }
                    case "E" -> {
                        if (parts.length >= 6) encounters.add(new FieldUiSnapshot.Encounter(
                                read(parts[1]), read(parts[2]), bool(parts[3]), bool(parts[4]), bool(parts[5])));
                    }
                    case "T" -> {
                        if (parts.length >= 5) travels.add(new FieldUiSnapshot.Travel(
                                read(parts[1]), read(parts[2]), bool(parts[3]), bool(parts[4])));
                    }
                    default -> { }
                }
            } catch (RuntimeException ignored) {
                // A malformed optional line must not make the client lose the entire field state.
            }
        }
        return new FieldUiSnapshot(active, mode, patrols, goal, bossUnlocked, chapterCleared, xp, gold,
                objective, dialogue, reward, encounters, travels);
    }

    private static int bit(boolean value) { return value ? 1 : 0; }
    private static boolean bool(String value) { return "1".equals(value) || "true".equalsIgnoreCase(value); }

    private static String text(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String read(String encoded) {
        if (encoded == null || encoded.isEmpty()) return "";
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
