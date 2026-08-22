package kr.moonseungjun.survivalascension.mining;

public enum MiningMode {
    AUTO("auto", "자동", 0),
    PLANE("plane", "굴착", 10),
    VEIN("vein", "광맥", 30),
    EXTRACT("extract", "추출", 90);

    private final String id;
    private final String koreanName;
    private final int requiredLevel;

    MiningMode(String id, String koreanName, int requiredLevel) {
        this.id = id;
        this.koreanName = koreanName;
        this.requiredLevel = requiredLevel;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public int requiredLevel() { return requiredLevel; }

    public static MiningMode fromId(String id) {
        for (MiningMode mode : values()) if (mode.id.equals(id)) return mode;
        return AUTO;
    }
}
