package kr.moonseungjun.frontiersettlement.settlement;

public enum SettlementTier {
    CAMP("개척 캠프"),
    HAMLET("촌락"),
    VILLAGE("마을"),
    FRONTIER_TOWN("개척 도시"),
    DOMAIN("영지");

    private final String displayName;

    SettlementTier(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static SettlementTier current(SettlementData data) {
        if (!data.founded()) return CAMP;

        if (data.population() >= 16
                && data.outposts().size() >= 4
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.FARM) >= 2) {
            return DOMAIN;
        }

        if (data.population() >= 8
                && data.outposts().size() >= 2
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.QUARRY) >= 1) {
            return FRONTIER_TOWN;
        }

        if (data.population() >= 4
                && data.outposts().size() >= 1
                && data.roads().size() >= 1
                && data.buildingCount(BuildingType.QUARRY) >= 1) {
            return VILLAGE;
        }

        if (data.houseCount() >= 1
                && data.lumberCampCount() >= 1
                && data.buildingCount(BuildingType.FARM) >= 1) {
            return HAMLET;
        }

        return CAMP;
    }
}
