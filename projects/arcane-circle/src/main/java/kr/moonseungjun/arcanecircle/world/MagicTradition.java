package kr.moonseungjun.arcanecircle.world;

/**
 * Save-compatible internal key for mage affiliations. The historical class name is retained so
 * existing worlds keep their stored value, but these are social factions rather than spell schools.
 */
public enum MagicTradition {
    UNBOUND("무소속", "어느 조직에도 속하지 않은 떠돌이·은둔·생활 마법사입니다."),
    ARCANE("왕국 마도연맹", "면허와 공공 질서를 중시하는 왕국 공인 마도 조직입니다."),
    DIVINE("백은 성약", "보호·치유·재난 대응을 우선하는 독립 성약입니다."),
    OCCULT("녹월 결사", "마녀와 민간 전승, 자연 의식을 포괄하는 느슨한 결사입니다."),
    PRIMAL("재의 밀약", "금지 마법과 지배를 추구해 연맹·성약과 적대하는 비밀 조직입니다.");

    private final String displayName;
    private final String description;

    MagicTradition(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }

    /** Kept at neutral values for binary/source compatibility; affiliation is not a spell school. */
    public double manaMultiplier() { return 1.0; }
    public double powerMultiplier() { return 1.0; }
    public double rangeMultiplier() { return 1.0; }
    public double cooldownMultiplier() { return 1.0; }

    public static MagicTradition parse(String value) {
        if (value == null || value.isBlank()) return UNBOUND;
        try { return valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return UNBOUND; }
    }
}
