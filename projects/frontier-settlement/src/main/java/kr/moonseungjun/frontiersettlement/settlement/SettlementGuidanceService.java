package kr.moonseungjun.frontiersettlement.settlement;

/** A single server-authored next step. This stays lightweight guidance rather than a separate task system. */
public final class SettlementGuidanceService {
    private SettlementGuidanceService() {}

    public static String nextGoal(SettlementData data) {
        if (!data.founded()) return "";
        if (data.construction().active()) {
            BuildingType type = BuildingType.fromId(data.construction().type());
            String name = type == null ? "건물" : type.displayName();
            return data.construction().step() == 0
                    ? "진행 중 · " + name + " 자재 운반"
                    : "진행 중 · " + name + " 건설";
        }
        if (data.roadConstruction().active()) return "진행 중 · 도로 공사";
        if (data.outpostConstruction().active()) return "진행 중 · 전초기지 건설";

        if (data.houseCount() < 1) return buildingGoal(data, BuildingType.HOUSE);
        if (data.lumberCampCount() < 1) return buildingGoal(data, BuildingType.LUMBER_CAMP);
        if (data.buildingCount(BuildingType.FARM) < 1) return buildingGoal(data, BuildingType.FARM);
        if (data.buildingCount(BuildingType.QUARRY) < 1) return buildingGoal(data, BuildingType.QUARRY);
        if (data.roads().isEmpty()) return "다음 목표 · B 팔레트 → 도로 계획";
        if (data.outposts().isEmpty()) return "다음 목표 · B 팔레트 → 도로 끝에 전초기지";
        if (data.population() < 4) return populationGoal(data, 4);
        if (data.buildingCount(BuildingType.MINE) < 1) return buildingGoal(data, BuildingType.MINE);
        if (data.outposts().size() < 2) return "다음 목표 · 두 번째 도로·전초기지 확보";
        if (data.population() < 8) return populationGoal(data, 8);
        if (data.buildingCount(BuildingType.BLACKSMITH) < 1) return buildingGoal(data, BuildingType.BLACKSMITH);
        if (data.buildingCount(BuildingType.FARM) < 2) return "다음 목표 · 농장 2곳으로 식량 기반 확대";
        if (data.outposts().size() < 4) return "다음 목표 · 전초기지 4곳까지 영토 확장";
        if (data.population() < 16) return populationGoal(data, 16);
        if (data.buildingCount(BuildingType.GUARD_POST) < 1) return buildingGoal(data, BuildingType.GUARD_POST);
        return "영지 운영 · 도로망과 전문 전초기지를 계속 확장";
    }

    private static String buildingGoal(SettlementData data, BuildingType type) {
        SettlementResources r = data.resources();
        if (r.wood() < type.woodCost() || r.stone() < type.stoneCost()) {
            return "다음 목표 · " + type.displayName() + " 자원 목" + type.woodCost() + " 석" + type.stoneCost();
        }
        return "다음 목표 · B 팔레트 → " + type.displayName() + " 건설";
    }

    private static String populationGoal(SettlementData data, int target) {
        if (data.housingCapacity() <= data.population()) return "다음 목표 · 주택을 늘려 인구 " + target + " 준비";
        if (data.resources().food() < 8L) return "다음 목표 · 공동 창고 식량 확보 → 인구 " + target;
        return "다음 목표 · 생산 거점을 늘려 인구 " + target + " 유치";
    }
}
