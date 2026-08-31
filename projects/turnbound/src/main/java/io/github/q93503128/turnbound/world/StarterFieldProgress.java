package io.github.q93503128.turnbound.world;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure projection from persistent campaign clears into the playable Southgate Chapter 1 field. */
final class StarterFieldProgress {
    static final List<String> SOUTHGATE_IDS = List.of(
            "ENC_M01", "ENC_M02", "ENC_M03", "ENC_M04", "ENC_M05", "BATTLE_B01");

    private StarterFieldProgress() {}

    static Set<String> project(Set<String> persistedClears) {
        Set<String> result = new LinkedHashSet<>();
        if (persistedClears == null) return Set.of();
        for (String id : persistedClears) {
            String canonical = canonical(id);
            if (SOUTHGATE_IDS.contains(canonical)) result.add(canonical);
        }
        return Set.copyOf(result);
    }

    static boolean starterPatrolComplete(Set<String> clears) {
        return clears.contains("ENC_M01") && clears.contains("ENC_M02");
    }

    static boolean bossUnlocked(Set<String> clears) {
        return clears.contains("ENC_M04") || clears.contains("BATTLE_B01");
    }

    static boolean chapterComplete(Set<String> clears) {
        return clears.contains("BATTLE_B01");
    }

    static int normalClearCount(Set<String> clears) {
        int count = 0;
        for (String id : List.of("ENC_M01", "ENC_M02", "ENC_M03", "ENC_M04", "ENC_M05")) if (clears.contains(id)) count++;
        return count;
    }

    private static String canonical(String id) {
        return switch (id) {
            case "southgate_enc_m01" -> "ENC_M01";
            case "southgate_enc_m02" -> "ENC_M02";
            case "southgate_enc_m03" -> "ENC_M03";
            case "southgate_enc_m04" -> "ENC_M04";
            case "southgate_enc_m05" -> "ENC_M05";
            case "southgate_b01_graul" -> "BATTLE_B01";
            default -> id;
        };
    }
}
