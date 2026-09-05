package io.github.q93503128.turnbound.world;

/** Pure identity contract for world-shared authored field interaction actors. */
public final class FieldSharedActorCatalog {
    static final String COMMON_TAG = "turnbound_field_shared_actor";
    static final String ROLE_PREFIX = "turnbound_field_role:";

    public enum Role {
        SOUTHGATE_SCOUT, SOUTHGATE_RELAY_VILLAGE, SOUTHGATE_RELAY_MEADOW,
        GLOAM_RELAY, GLOAM_SPORE_1, GLOAM_SPORE_2, GLOAM_SPORE_3,
        AQUEDUCT_RELAY, AQUEDUCT_VALVE_1, AQUEDUCT_VALVE_2,
        QUARRY_RELAY, QUARRY_CORE_1, QUARRY_CORE_2,
        OLD_RELAY_FT, OLD_RELAY_RECORD_1, OLD_RELAY_RECORD_2, OLD_RELAY_RECORD_3, OLD_RELAY_RECORD_4,
        OLD_RELAY_FINAL_CONSOLE
    }

    private FieldSharedActorCatalog() {}

    static String roleTag(Role role) { return ROLE_PREFIX + role.name(); }

    static Role fromTag(String tag) {
        if (tag == null || !tag.startsWith(ROLE_PREFIX)) return null;
        try { return Role.valueOf(tag.substring(ROLE_PREFIX.length())); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static int gloamSporeIndex(Role role) {
        return switch (role) {
            case GLOAM_SPORE_1 -> 0;
            case GLOAM_SPORE_2 -> 1;
            case GLOAM_SPORE_3 -> 2;
            default -> -1;
        };
    }

    public static int aqueductValveIndex(Role role) {
        return switch (role) {
            case AQUEDUCT_VALVE_1 -> 0;
            case AQUEDUCT_VALVE_2 -> 1;
            default -> -1;
        };
    }

    public static int quarryCoreIndex(Role role) {
        return switch (role) {
            case QUARRY_CORE_1 -> 0;
            case QUARRY_CORE_2 -> 1;
            default -> -1;
        };
    }

    public static int oldRelayRecordIndex(Role role) {
        return switch (role) {
            case OLD_RELAY_RECORD_1 -> 0;
            case OLD_RELAY_RECORD_2 -> 1;
            case OLD_RELAY_RECORD_3 -> 2;
            case OLD_RELAY_RECORD_4 -> 3;
            default -> -1;
        };
    }
}
