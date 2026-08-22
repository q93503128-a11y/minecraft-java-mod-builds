package kr.moonseungjun.survivalascension.construction;

public enum ConstructionMode {
    SINGLE("single", "단일", 0),
    LINE("line", "선", 10),
    WALL("wall", "벽", 30),
    FLOOR("floor", "바닥", 30);

    private final String id;
    private final String koreanName;
    private final int requiredLevel;

    ConstructionMode(String id, String koreanName, int requiredLevel) {
        this.id = id;
        this.koreanName = koreanName;
        this.requiredLevel = requiredLevel;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public int requiredLevel() { return requiredLevel; }

    public static ConstructionMode fromId(String id) {
        if (id != null) {
            for (ConstructionMode mode : values()) {
                if (mode.id.equals(id)) return mode;
            }
        }
        return SINGLE;
    }
}
