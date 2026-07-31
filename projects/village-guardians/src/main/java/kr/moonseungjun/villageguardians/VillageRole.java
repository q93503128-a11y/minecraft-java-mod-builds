package kr.moonseungjun.villageguardians;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum VillageRole {
    GUARD_CAPTAIN("guard_captain", "경비대장"),
    BUILDER("builder", "건축가"),
    QUARTERMASTER("quartermaster", "보급관"),
    SCOUT("scout", "정찰병"),
    STEWARD("steward", "농업관"),
    MEDIC("medic", "의무관");

    private final String id;
    private final String koreanName;

    VillageRole(String id, String koreanName) {
        this.id = id;
        this.koreanName = koreanName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return koreanName + " (" + id + ")";
    }

    public static Optional<VillageRole> parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(role -> role.id.equals(normalized))
                .findFirst();
    }

    public static String ids() {
        return String.join(", ", Arrays.stream(values()).map(VillageRole::id).toList());
    }
}
