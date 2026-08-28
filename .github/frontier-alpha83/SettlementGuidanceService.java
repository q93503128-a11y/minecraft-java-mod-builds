package kr.moonseungjun.frontiersettlement.settlement;

/** A single server-authored next step. This stays lightweight guidance rather than a separate task system. */
public final class SettlementGuidanceService {
    private SettlementGuidanceService() {}

    public static String nextGoal(SettlementData data) {
        if (!data.founded()) return "";
        if (data.construction().active()) {
            BuildingType type = BuildingType.fromId(data.construction().type());
            String name = type == null ? "건물" : type.displayName();
            return "진행 중 · " + name + " " + SettlementConstructionService.phaseLabel(data.construction());
        }
        if (data.roadConstruction().active()) return "진행 중 · " + SettlementRoadService.phaseLabel(data.roadConstruction());
        if (data.outpostConstruction().active()) return "진행 중 · " + SettlementOutpostService.phaseLabel(data.outpostConstruction());

        if (data.houseCount() < 1) return buildingGoal(data, BuildingType.HOUSE);
        if (data.lumberCampCount() < 1) return buildingGoal(data, BuildingType.LUMBER_CAMP);
        if (data.buildingCount(BuildingType.FARM) < 1) return buildingGoal(data, BuildingType.FARM);
        if (data.buildingCount(BuildingType.QUARRY) < 1) return buildingGoal(data, BuildingType.QUARRY);
        if (data.buildingCount(BuildingType.WAREHOUSE) < 1) return buildingGoal(data, BuildingType.WAREHOUSE);
        if (data.roads().isEmpty()) return "다음 목표 · M 메뉴 → 도로 계획";
        if (data.outposts().isEmpty()) return "다음 목표 · M 메뉴 → 도로 끝에 전초기지";
        if (data.population() < 4) return populationGoal(data, 4);
        if (data.buildingCount(BuildingType.MARKET) < 1) return buildingGoal(data, BuildingType.MARKET);
        if (data.buildingCount(BuildingType.CART_STATION) < 1) return buildingGoal(data, BuildingType.CART_STATION);
        if (data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1) return buildingGoal(data, BuildingType.CONSTRUCTION_OFFICE);
        if (data.buildingCount(BuildingType.MINE) < 1) return buildingGoal(data, BuildingType.MINE);
        if (data.outposts().size() < 2) return "다음 목표 · 두 번째 도로·전초기지 확보";
        if (data.population() < 8) return populationGoal(data, 8);
        if (data.buildingCount(BuildingType.BLACKSMITH) < 1) return buildingGoal(data, BuildingType.BLACKSMITH);
        if (data.buildingCount(BuildingType.WORKSHOP) < 1) return buildingGoal(data, BuildingType.WORKSHOP);
        if (data.buildingCount(BuildingType.FARM) < 2) return "다음 목표 · 농장 2곳으로 식량 기반 확대";
        if (data.buildingCount(BuildingType.CIVIC_HALL) < 1) {
            String lock = SettlementConstructionService.lockedReason(data, BuildingType.CIVIC_HALL);
            return lock == null ? buildingGoal(data, BuildingType.CIVIC_HALL) : "중후반 목표 · " + lock;
        }
        if (data.outposts().size() < 4) return "다음 목표 · 전초기지 4곳까지 영토 확장";
        if (data.population() < 16) return populationGoal(data, 16);
        if (data.buildingCount(BuildingType.GUARD_POST) < 1) return buildingGoal(data, BuildingType.GUARD_POST);
        if (data.buildingCount(BuildingType.WATCHTOWER) < 1) return buildingGoal(data, BuildingType.WATCHTOWER);
        if (data.buildingCount(BuildingType.BARRACKS) < 1) return buildingGoal(data, BuildingType.BARRACKS);
        if (data.buildingCount(BuildingType.ADVANCED_WORKSHOP) < 1
                && SettlementAdvancedWorkshopService.lockedReason(data) == null) {
            return buildingGoal(data, BuildingType.ADVANCED_WORKSHOP);
        }
        if (data.buildingCount(BuildingType.TRADE_HALL) < 1) {
            String lock = SettlementConstructionService.lockedReason(data, BuildingType.TRADE_HALL);
            return lock == null ? buildingGoal(data, BuildingType.TRADE_HALL) : "후반 목표 · " + lock;
        }
        if (data.buildingCount(BuildingType.CITADEL) < 1) {
            String lock = SettlementConstructionService.lockedReason(data, BuildingType.CITADEL);
            return lock == null ? buildingGoal(data, BuildingType.CITADEL) : "후반 목표 · " + lock;
        }
        if (data.outposts().size() < 5) return "최종 목표 · 전초기지 5곳으로 영지망 완성";
        if (data.roads().size() < 4) return "최종 목표 · 완성된 도로 4개 이상 확보";
        if (data.population() < 20) return populationGoal(data, 20);
        if (data.explorationScore() < 7) {
            return "최종 목표 · 탐험 점수 7 달성 (새 구조물 조사·강적 정복)";
        }
        if (SettlementTier.current(data) == SettlementTier.FRONTIER_CAPITAL) {
            return "개척 수도 완성 · 도로망·전초기지·교역·성채를 자유롭게 확장하세요.";
        }
        return "최종 목표 · 개척 수도 승격 조건을 확인하세요.";
    }

    private static String buildingGoal(SettlementData data, BuildingType type) {
        SettlementResources r = data.resources();
        if (r.wood() < type.woodCost() || r.stone() < type.stoneCost()) {
            return "다음 목표 · " + type.displayName() + " 자원 목" + type.woodCost() + " 석" + type.stoneCost();
        }
        return "다음 목표 · M 메뉴 → " + type.displayName() + " 건설";
    }

    private static String populationGoal(SettlementData data, int target) {
        if (data.housingCapacity() <= data.population()) return "다음 목표 · 주택/시민시설을 늘려 인구 " + target + " 준비";
        if (data.resources().food() < 8L) return "다음 목표 · 공동 창고 식량 확보 → 인구 " + target;
        return "다음 목표 · 생산 거점을 늘려 인구 " + target + " 유치";
    }
}
