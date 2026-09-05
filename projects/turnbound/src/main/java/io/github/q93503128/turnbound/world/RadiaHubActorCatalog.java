package io.github.q93503128.turnbound.world;

import java.util.List;

/** Pure identity catalog for the world-shared Radia interaction actors. */
public final class RadiaHubActorCatalog {
    public static final String COMMON_TAG = "turnbound_radia_actor";
    public static final String ROLE_TAG_PREFIX = COMMON_TAG + ":";

    public enum Role {
        DIRECTOR(null, -1),
        RELAY(null, -1),
        SOUTH_GATE(null, -1),
        TUTORIAL_1(null, 0),
        TUTORIAL_2(null, 1),
        TUTORIAL_3(null, 2),
        ECHO_ARCHIVE("ECHO_ARCHIVE", -1),
        FORGE_ANNEX("FORGE_ANNEX", -1),
        MARKET_ROW("MARKET_ROW", -1),
        TRAINING_YARD("TRAINING_YARD", -1),
        RIFT_GATE("RIFT_GATE", -1),
        MEMORIAL_STEPS("MEMORIAL_STEPS", -1),
        CLOCK_TOWER("CLOCK_TOWER", -1),
        BARRACKS("BARRACKS", -1);

        private final String facilityId;
        private final int tutorialIndex;

        Role(String facilityId, int tutorialIndex) {
            this.facilityId = facilityId;
            this.tutorialIndex = tutorialIndex;
        }

        public String facilityId() { return facilityId; }
        public int tutorialIndex() { return tutorialIndex; }
        public boolean facility() { return facilityId != null; }
        public boolean tutorial() { return tutorialIndex >= 0; }
    }

    private RadiaHubActorCatalog() {}

    public static List<Role> roles() {
        return List.of(Role.values());
    }

    public static String roleTag(Role role) {
        if (role == null) throw new IllegalArgumentException("Missing Radia actor role");
        return ROLE_TAG_PREFIX + role.name();
    }

    public static Role fromTag(String tag) {
        if (tag == null || !tag.startsWith(ROLE_TAG_PREFIX)) return null;
        String raw = tag.substring(ROLE_TAG_PREFIX.length());
        try {
            return Role.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Role facilityRole(String facilityId) {
        if (facilityId == null) return null;
        for (Role role : Role.values()) {
            if (facilityId.equals(role.facilityId())) return role;
        }
        return null;
    }
}
