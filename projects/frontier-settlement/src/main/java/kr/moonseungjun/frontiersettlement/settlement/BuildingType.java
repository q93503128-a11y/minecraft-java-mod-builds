package kr.moonseungjun.frontiersettlement.settlement;

import java.util.Locale;

public enum BuildingType {
    HOUSE("house", "주택", 48, 20, 9, 9, 10, 4),
    LUMBER_CAMP("lumber_camp", "벌목소", 56, 12, 11, 9, 10, 0);

    private final String id;
    private final String displayName;
    private final long woodCost;
    private final long stoneCost;
    private final int width;
    private final int depth;
    private final int clearHeight;
    private final int housingGain;

    BuildingType(String id, String displayName, long woodCost, long stoneCost,
                 int width, int depth, int clearHeight, int housingGain) {
        this.id = id;
        this.displayName = displayName;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.width = width;
        this.depth = depth;
        this.clearHeight = clearHeight;
        this.housingGain = housingGain;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public long woodCost() { return woodCost; }
    public long stoneCost() { return stoneCost; }
    public int width() { return width; }
    public int depth() { return depth; }
    public int clearHeight() { return clearHeight; }
    public int housingGain() { return housingGain; }

    public static BuildingType fromId(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (BuildingType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }
}
