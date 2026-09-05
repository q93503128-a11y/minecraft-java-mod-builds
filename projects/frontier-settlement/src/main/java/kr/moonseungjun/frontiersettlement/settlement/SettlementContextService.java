package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.network.SettlementContextPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative, presentation-only context for compact HUD/Jade/Xaero consumers. */
public final class SettlementContextService {
    private SettlementContextService() {}

    public static SettlementContextPayload snapshot(MinecraftServer server, SettlementData data) {
        if (!data.founded()) return SettlementContextPayload.EMPTY;
        ServerLevel level = server.overworld();
        List<SettlementContextTarget> targets = new ArrayList<>();

        ConstructionState construction = data.construction();
        String projectLabel = "";
        int projectProgress = -1;
        if (construction.active()) {
            BuildingType type = BuildingType.fromId(construction.type());
            if (type != null) {
                int width = construction.buildingRotation().rotatedWidth(type);
                int depth = construction.buildingRotation().rotatedDepth(type);
                int buildTotal = SettlementConstructionService.totalSteps(type, construction.origin(), construction.rotation());
                int gradeTotal = SettlementConstructionService.gradingSteps(level, construction, type);
                int worked = construction.grading()
                        ? Math.min(gradeTotal, Math.max(0, construction.gradeStep()))
                        : gradeTotal + Math.max(0, construction.buildStep());
                projectProgress = percent(worked, gradeTotal + buildTotal);
                String constructionIssue = SettlementConstructionService.constructionIssue(server, data);
                projectLabel = type.displayName() + " 공사" + (constructionIssue.isBlank() ? "" : " · 막힘");
                String constructionDetail = construction.grading()
                        ? "부지 정리 중 · 건물 자재는 정리 완료 후 실물 운반"
                        : "자재 운반·시공 중";
                if (!constructionIssue.isBlank()) constructionDetail += " · " + constructionIssue;
                targets.add(new SettlementContextTarget(
                        "construction", "construction",
                        construction.originX(), construction.originY() - 2, construction.originZ(),
                        construction.originX() + width - 1, construction.originY() + type.clearHeight() + 2,
                        construction.originZ() + depth - 1,
                        construction.originX() + width / 2, construction.originY() + 1, construction.originZ() + depth / 2,
                        type.displayName(), constructionDetail, projectProgress));
            }
        } else if (data.roadConstruction().active()) {
            RoadConstructionState road = data.roadConstruction();
            projectLabel = "도로 공사";
            projectProgress = percent(road.step(), SettlementRoadService.totalSteps(road));
        } else if (data.outpostConstruction().active()) {
            OutpostConstructionState outpost = data.outpostConstruction();
            projectLabel = "전초기지 공사";
            int total = SettlementOutpostService.totalSteps(outpost);
            int worked = outpost.physicalBuilding() ? Math.max(0, outpost.buildStep())
                    : outpost.legacyPrepaidBuilding() ? Math.max(0, outpost.legacyStep()) : 0;
            projectProgress = percent(worked, total);
        } else {
            CivilWorkState civil = SettlementCivilWorkData.get(server).project();
            if (civil.active()) {
                projectLabel = SettlementCivilWorkService.phaseLabel(server);
                projectProgress = civil.progressPercent();
                int importedRemaining = SettlementCivilFillSupplyService.remainingImportedFill(level, civil);
                int availableFill = SettlementCivilFillSupplyService.availableFill(level, data);
                int availableRetaining = SettlementCivilRetainingService.availableRetaining(level, data);
                String imported = importedRemaining < 0
                        ? "외부 흙 확인 대기"
                        : "외부 흙 필요 " + importedRemaining;
                String storage = availableFill < 0 ? "창고 흙 미로드" : "창고 흙 " + availableFill;
                String retaining = civil.initialRetainingBlocks() <= 0
                        ? "옹벽 없음"
                        : "옹벽 잔여 " + civil.remainingRetainingBlocks() + " · 창고 조약돌 "
                                + (availableRetaining < 0 ? "미로드" : availableRetaining);
                targets.add(new SettlementContextTarget(
                        "civil_work", "civil_work",
                        civil.minX() - 1, civil.gradeY() - SettlementCivilRetainingService.MAX_RETAINING_HEIGHT, civil.minZ() - 1,
                        civil.maxX() + 1, civil.gradeY() + SettlementCivilWorkService.MAX_CUT_DEPTH, civil.maxZ() + 1,
                        civil.center().getX(), civil.gradeY(), civil.center().getZ(),
                        "선택영역 토목",
                        "현장 토사 " + civil.earthBank() + " · " + imported + " · " + storage + " · " + retaining + " · 가상 토사 0",
                        projectProgress));
            }
        }

        BlockPos stock = data.stockpilePos();
        SettlementResources resources = data.resources();
        targets.add(new SettlementContextTarget(
                "stockpile", "stockpile",
                stock.getX(), stock.getY(), stock.getZ(), stock.getX(), stock.getY(), stock.getZ(),
                stock.getX(), stock.getY(), stock.getZ(),
                "공동 창고",
                "실물 권위 · 목재 " + resources.wood() + " · 석재 " + resources.stone()
                        + " · 금속 " + resources.metal() + " · 식량 " + resources.food(), -1));

        for (BuildingRecord building : data.buildings()) {
            BuildingType type = building.buildingType();
            if (type == null) continue;
            int width = building.rotatedWidth();
            int depth = building.rotatedDepth();
            BlockPos marker = building.workCenter();
            targets.add(new SettlementContextTarget(
                    "building:" + type.id() + ":" + building.originX() + ":" + building.originY() + ":" + building.originZ(),
                    "building",
                    building.originX(), building.originY() - 1, building.originZ(),
                    building.originX() + width - 1, building.originY() + type.clearHeight() + 2,
                    building.originZ() + depth - 1,
                    marker.getX(), marker.getY(), marker.getZ(),
                    type.displayName(), buildingDetail(type, data), -1));
        }

        for (OutpostRecord outpost : data.outposts()) {
            String role = SettlementFishingOutpostService.specializationDisplayName(level, outpost);
            BlockPos center = outpost.center();
            targets.add(new SettlementContextTarget(
                    "outpost:" + outpost.id(), "outpost",
                    outpost.centerX() - 5, outpost.centerY() - 2, outpost.centerZ() - 5,
                    outpost.centerX() + 5, outpost.centerY() + 10, outpost.centerZ() + 5,
                    center.getX(), center.getY() + 1, center.getZ(),
                    "전초기지 #" + outpost.id(), "역할 · " + role + " · 도로 " + (outpost.roadIndex() + 1), -1));

            BlockPos waterfrontCrate = SettlementWaterfrontService.tradeCrate(server, outpost);
            if (waterfrontCrate != null) {
                targets.add(new SettlementContextTarget(
                        "waterfront:" + outpost.id(), "waterfront",
                        waterfrontCrate.getX(), waterfrontCrate.getY(), waterfrontCrate.getZ(),
                        waterfrontCrate.getX(), waterfrontCrate.getY(), waterfrontCrate.getZ(),
                        waterfrontCrate.getX(), waterfrontCrate.getY(), waterfrontCrate.getZ(),
                        "수변 교역통 #" + outpost.id(),
                        "대구/연어 " + SettlementWaterfrontService.TRADE_FISH_COST + " → 에메랄드 1 · 전용 투입", -1));
            }
        }

        return new SettlementContextPayload(data.buildings().size(), data.outposts().size(),
                projectLabel, projectProgress, targets);
    }

    private static int percent(int worked, int total) {
        if (total <= 0) return 0;
        return Math.max(0, Math.min(100, worked * 100 / total));
    }

    private static String buildingDetail(BuildingType type, SettlementData data) {
        return switch (type) {
            case HOUSE -> "완공 · 주거 +" + type.housingGain();
            case LUMBER_CAMP, FARM, QUARRY, MINE -> "완공 · " + SettlementProductionEfficiencyService.detail(type, data);
            case WAREHOUSE -> "완공 · 실물 저장";
            case CONSTRUCTION_OFFICE -> "완공 · 건설 자재 집결";
            case BLACKSMITH -> "완공 · 장비 지원";
            case WORKSHOP -> "완공 · 금속 1 → 외부무기 내구 +" + SettlementExplorationBenefitService.repairPerMetal(data);
            case ADVANCED_WORKSHOP -> "완공 · 고급 제작 위력 " + SettlementExplorationBenefitService.forgePower(data)
                    + " · 영지 재련 " + SettlementExplorationBenefitService.reforgePower(data);
            case GUARD_POST -> "완공 · 근거리 경비";
            case WATCHTOWER -> "완공 · 로드 위협 대응";
            case BARRACKS -> "완공 · 정식 주둔 3슬롯";
            case MARKET -> "완공 · 유물 → 실물 교역 · 개척 보너스 +" + SettlementExplorationBenefitService.marketPayoutBonus(data);
            case CART_STATION -> "완공 · 도로 화물 허브";
            case CIVIC_HALL -> "완공 · 시민 중심 · 주거 +" + type.housingGain();
            case TRADE_HALL -> "완공 · 유물 교역 보너스 +4 · 주거 +" + type.housingGain();
            case CITADEL -> "완공 · 감시망 반경 56 · 주거 +" + type.housingGain();
        };
    }
}
