package kr.moonseungjun.frontiersettlement.settlement;

public enum SettlementTier {
    CAMP("개척 캠프"),
    HAMLET("촌락"),
    VILLAGE("마을"),
    FRONTIER_TOWN("개척 도시"),
    DOMAIN("영지"),
    FRONTIER_CAPITAL("개척 수도");

    private final String displayName;

    SettlementTier(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static SettlementTier current(SettlementData data) {
        if (!data.founded()) return CAMP;

        boolean frontierCapital = data.population() >= 20
                && data.outposts().size() >= 5
                && data.roads().size() >= 4
                && data.buildingCount(BuildingType.CIVIC_HALL) >= 1
                && data.buildingCount(BuildingType.TRADE_HALL) >= 1
                && data.buildingCount(BuildingType.CITADEL) >= 1
                && data.explorationScore() >= 7;
        if (frontierCapital) return FRONTIER_CAPITAL;

        boolean legacyDomain = data.population() >= 16
                && data.outposts().size() >= 4
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.FARM) >= 2;
        boolean explorationDomain = data.population() >= 14
                && data.outposts().size() >= 3
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.FARM) >= 2
                && data.explorationScore() >= 5;
        if (legacyDomain || explorationDomain) return DOMAIN;

        boolean legacyFrontierTown = data.population() >= 8
                && data.outposts().size() >= 2
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.QUARRY) >= 1;
        boolean explorationFrontierTown = data.population() >= 7
                && data.outposts().size() >= 2
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.QUARRY) >= 1
                && data.explorationScore() >= 2;
        if (legacyFrontierTown || explorationFrontierTown) return FRONTIER_TOWN;

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
