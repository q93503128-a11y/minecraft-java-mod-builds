package io.github.q93503128.turnbound.world;

import java.util.Set;

/** Pure progression rules used by the shared field-enemy presentation registry. */
final class FieldSharedEncounterRules {
    enum Region { GLOAMWOOD, AQUEDUCT, QUARRY, RELAY, OTHER }

    private FieldSharedEncounterRules() {}

    static Region regionOf(String encounterId) {
        if (encounterId == null) return Region.OTHER;
        if (encounterId.startsWith("ENC_G") || "BATTLE_B02".equals(encounterId)) return Region.GLOAMWOOD;
        if (encounterId.startsWith("ENC_A") || "BATTLE_B03".equals(encounterId)) return Region.AQUEDUCT;
        if (encounterId.startsWith("ENC_Q") || "BATTLE_B04".equals(encounterId)) return Region.QUARRY;
        if (encounterId.startsWith("ENC_R") || "BATTLE_B05".equals(encounterId)) return Region.RELAY;
        return Region.OTHER;
    }

    static boolean unlocked(String encounterId, Set<String> completedQuests, Set<String> unlockFlags) {
        if (encounterId == null) return false;
        Set<String> completed = completedQuests == null ? Set.of() : completedQuests;
        Set<String> flags = unlockFlags == null ? Set.of() : unlockFlags;
        return switch (regionOf(encounterId)) {
            case GLOAMWOOD -> gloamUnlocked(encounterId, completed, flags);
            case AQUEDUCT -> aqueductUnlocked(encounterId, completed, flags);
            case QUARRY -> quarryUnlocked(encounterId, completed, flags);
            case RELAY -> relayUnlocked(encounterId, completed, flags);
            case OTHER -> false;
        };
    }

    private static boolean gloamUnlocked(String id, Set<String> completed, Set<String> flags) {
        if ("ENC_G01".equals(id) || "ENC_G02".equals(id)) return true;
        if ("BATTLE_B02".equals(id)) {
            return flags.contains("B02_GATE") || completed.contains("MQ_C02_02_root_wall");
        }
        if (id.startsWith("ENC_G")) {
            return flags.contains("GLOAM_DEEP_PATH") || completed.contains("MQ_C02_01_spores");
        }
        return false;
    }

    private static boolean aqueductUnlocked(String id, Set<String> completed, Set<String> flags) {
        if ("ENC_A01".equals(id) || "ENC_A02".equals(id)) return true;
        if ("BATTLE_B03".equals(id)) {
            return flags.contains("ORO_ROOM") || completed.contains("MQ_C03_02_old_orders");
        }
        if (id.startsWith("ENC_A")) {
            return flags.contains("AQUEDUCT_LOWER") || completed.contains("MQ_C03_01_dry_channel");
        }
        return false;
    }

    private static boolean quarryUnlocked(String id, Set<String> completed, Set<String> flags) {
        if ("ENC_Q01".equals(id) || "ENC_Q02".equals(id)) return true;
        if ("BATTLE_B04".equals(id)) {
            return flags.contains("B04_GATE") || completed.contains("MQ_C04_02_core_fragment");
        }
        if (id.startsWith("ENC_Q")) {
            return flags.contains("FT_QUARRY") || completed.contains("MQ_C04_01_ash_route");
        }
        return false;
    }

    private static boolean relayUnlocked(String id, Set<String> completed, Set<String> flags) {
        if ("BATTLE_B05".equals(id)) {
            return flags.contains("B05_GATE") || completed.contains("MQ_C05_02_serak_record");
        }
        return id.startsWith("ENC_R");
    }
}
