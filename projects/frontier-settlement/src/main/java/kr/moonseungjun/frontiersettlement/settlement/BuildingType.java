package kr.moonseungjun.frontiersettlement.settlement;

import java.util.Locale;

public enum BuildingType {
    HOUSE("house", "주택", 48, 20, 9, 9, 10, 4, ""),
    LUMBER_CAMP("lumber_camp", "벌목소", 56, 12, 11, 9, 10, 0, ""),
    FARM("farm", "농장", 52, 8, 13, 11, 7, 0, "주택 1채 필요"),
    QUARRY("quarry", "채석장", 44, 28, 11, 11, 8, 0, "벌목소 1곳 필요"),
    MINE("mine", "광산", 68, 44, 11, 11, 9, 0, "채석장 + 전초기지 필요"),
    WAREHOUSE("warehouse", "창고", 72, 36, 11, 9, 10, 0, "농장 1곳 필요"),
    BLACKSMITH("blacksmith", "대장간", 80, 52, 9, 9, 9, 0, "광산 1곳 필요"),
    GUARD_POST("guard_post", "경비초소", 64, 48, 9, 9, 8, 0, "마을 단계 필요");

    private final String id;
    private final String displayName;
    private final long woodCost;
    private final long stoneCost;
    private final int width;
    private final int depth;
    private final int clearHeight;
    private final int housingGain;
    private final String unlockHint;

    BuildingType(String id, String displayName, long woodCost, long stoneCost,
                 int width, int depth, int clearHeight, int housingGain, String unlockHint) {
        this.id = id;
        this.displayName = displayName;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.width = width;
        this.depth = depth;
        this.clearHeight = clearHeight;
        this.housingGain = housingGain;
        this.unlockHint = unlockHint;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public long woodCost() { return woodCost; }
    public long stoneCost() { return stoneCost; }
    public int width() { return width; }
    public int depth() { return depth; }
    public int clearHeight() { return clearHeight; }
    public int housingGain() { return housingGain; }
    public String unlockHint() { return unlockHint; }

    public static BuildingType fromId(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (BuildingType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }
}
