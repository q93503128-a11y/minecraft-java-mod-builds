package io.github.q93503128.turnbound.world;

/**
 * Pure key policy for shared-world boss clears.
 *
 * <p>Legacy Aster March boss encounters keep their historical boss-id keys. New authored-world bosses are keyed by
 * encounter id so reusing a combatant never grants or suppresses unrelated legacy world progression.</p>
 */
final class WorldEncounterClearPolicy {
    private static final String ENCOUNTER_CLEAR_CLAIM_PREFIX = "ENCOUNTER_CLEAR:";

    private WorldEncounterClearPolicy() {}

    static String legacyBossId(String encounterId) {
        return switch (encounterId == null ? "" : encounterId) {
            case "BATTLE_B01" -> "B01";
            case "BATTLE_B02" -> "B02";
            case "BATTLE_B03" -> "B03";
            case "BATTLE_B04" -> "B04";
            case "BATTLE_B05" -> "B05";
            default -> null;
        };
    }

    static String claimKey(String encounterId) {
        return ENCOUNTER_CLEAR_CLAIM_PREFIX + (encounterId == null ? "" : encounterId);
    }
}
