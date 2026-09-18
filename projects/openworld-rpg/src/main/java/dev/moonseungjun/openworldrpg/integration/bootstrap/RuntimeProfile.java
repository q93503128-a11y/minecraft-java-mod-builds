package dev.moonseungjun.openworldrpg.integration.bootstrap;

import java.util.Locale;

public enum RuntimeProfile {
    CORE("core"),
    GAMEPLAY("gameplay"),
    ESSENTIAL("essential");

    private final String id;

    RuntimeProfile(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static RuntimeProfile current() {
        String value = System.getProperty("openworld_rpg.profile");
        if (value == null || value.isBlank()) {
            value = System.getenv("OPENWORLD_RPG_PROFILE");
        }
        if (value == null || value.isBlank()) {
            return CORE;
        }
        return fromId(value);
    }

    public static RuntimeProfile fromId(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (RuntimeProfile profile : values()) {
            if (profile.id.equals(normalized)) {
                return profile;
            }
        }
        throw new IllegalArgumentException("Unknown Openworld RPG runtime profile: " + value);
    }
}
