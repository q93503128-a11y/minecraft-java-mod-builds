package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Presentation-only RTS summary derived from the existing authoritative settlement snapshot/context.
 *
 * This class never becomes simulation or save authority. It intentionally reuses the target list that
 * is already shipped for HUD/Jade/location presentation so opening the operations screen cannot launch
 * a second server entity/world scan.
 */
public record SettlementOperationsSummary(
        int housingCapacity,
        int productionSites,
        int productionWorking,
        int productionMissingWorker,
        int productionBlocked,
        int productionUnknown,
        int productionUpgradeBacklog,
        int warehouses,
        int cartStations,
        int logisticsSaturated,
        int logisticsUpgradeBacklog,
        int guardPosts,
        int watchtowers,
        int barracks,
        int citadels,
        int militaryUpgradeBacklog,
        int cityInvestmentBacklog,
        int productiveOutpostDiversity,
        int territoryNetworkLevel,
        int outposts,
        String priority,
        List<String> alerts) {

    public SettlementOperationsSummary {
        alerts = List.copyOf(alerts);
    }

    public static SettlementOperationsSummary from(SettlementSnapshotPayload snapshot) {
        int housingCapacity = 0;
        int productionSites = 0;
        int productionWorking = 0;
        int productionMissingWorker = 0;
        int productionBlocked = 0;
        int productionUnknown = 0;
        int productionUpgradeBacklog = 0;
        int warehouses = 0;
        int cartStations = 0;
        int logisticsSaturated = 0;
        int logisticsUpgradeBacklog = 0;
        int guardPosts = 0;
        int watchtowers = 0;
        int barracks = 0;
        int citadels = 0;
        int militaryUpgradeBacklog = 0;
        int cityInvestmentBacklog = 0;
        Set<String> productiveOutpostRoles = new HashSet<>();

        int productionCeiling = productionCeiling(snapshot.tier());
        int logisticsCeiling = logisticsCeiling(snapshot.tier());
        int militaryCeiling = militaryCeiling(snapshot.tier());
        int cityCeiling = militaryCeiling;

        for (SettlementContextTarget target : snapshot.context().targets()) {
            if ("outpost".equals(target.kind())) {
                collectProductiveOutpostRole(productiveOutpostRoles, target.detail());
                continue;
            }
            if (!"building".equals(target.kind())) continue;
            BuildingType type = BuildingType.fromId(buildingId(target.key()));
            if (type == null) continue;
            housingCapacity += Math.max(0, type.housingGain());

            if (isProduction(type)) {
                productionSites++;
                String status = productionStatus(target.detail());
                if ("정상 작업 중".equals(status)) productionWorking++;
                else if (status.contains("주민 없음")) productionMissingWorker++;
                else if (status.contains("상태 확인 중") || status.contains("청크 미로드") || status.isBlank()) productionUnknown++;
                else productionBlocked++;

                int grade = gradeAfter(target.detail(), "개량 ");
                if (grade > 0 && grade < productionCeiling) productionUpgradeBacklog++;
                continue;
            }

            if (type == BuildingType.WAREHOUSE || type == BuildingType.CART_STATION) {
                if (type == BuildingType.WAREHOUSE) warehouses++; else cartStations++;
                if (target.detail().contains("포화")) logisticsSaturated++;
                int grade = gradeAfter(target.detail(), "물류 ");
                if (grade > 0 && grade < logisticsCeiling) logisticsUpgradeBacklog++;
                continue;
            }

            if (type == BuildingType.CIVIC_HALL || type == BuildingType.TRADE_HALL) {
                int grade = gradeAfter(target.detail(), "도시 ");
                if (grade > 0 && grade < cityCeiling) cityInvestmentBacklog++;
                continue;
            }

            if (type == BuildingType.GUARD_POST || type == BuildingType.WATCHTOWER
                    || type == BuildingType.BARRACKS || type == BuildingType.CITADEL) {
                if (type == BuildingType.GUARD_POST) guardPosts++;
                else if (type == BuildingType.WATCHTOWER) watchtowers++;
                else if (type == BuildingType.BARRACKS) barracks++;
                else citadels++;
                int grade = gradeAfter(target.detail(), "군사 ");
                if (grade > 0 && grade < militaryCeiling) militaryUpgradeBacklog++;
            }
        }

        int productiveOutpostDiversity = productiveOutpostRoles.size();
        int territoryNetworkLevel = territoryNetworkLevel(snapshot.tier(), productiveOutpostDiversity);

        List<String> alerts = new ArrayList<>();
        String project = snapshot.context().projectLabel();
        if (project != null && project.contains("막힘")) alerts.add("진행 중 공사가 막혀 있습니다.");
        if (housingCapacity > 0 && snapshot.population() >= housingCapacity) {
            alerts.add("주거 여유가 없습니다. 주택 또는 시민회관을 증설하세요.");
        }
        if (productionMissingWorker > 0) alerts.add("생산시설 " + productionMissingWorker + "곳에 주민이 없습니다.");
        if (productionBlocked > 0) alerts.add("생산시설 " + productionBlocked + "곳이 자원·저장·경로 문제로 멈췄습니다.");
        if (logisticsSaturated > 0) alerts.add("물류 저장시설 " + logisticsSaturated + "곳이 포화 상태입니다.");
        if (productionUpgradeBacklog > 0) alerts.add("현재 등급에서 생산시설 " + productionUpgradeBacklog + "곳을 더 개량할 수 있습니다.");
        if (logisticsUpgradeBacklog > 0) alerts.add("현재 등급에서 물류시설 " + logisticsUpgradeBacklog + "곳을 더 확장할 수 있습니다.");
        if (cityInvestmentBacklog > 0) alerts.add("현재 등급에서 도시 핵심시설 " + cityInvestmentBacklog + "곳에 추가 투자할 수 있습니다.");
        if (militaryUpgradeBacklog > 0) alerts.add("현재 등급에서 방어시설 " + militaryUpgradeBacklog + "곳을 더 개량할 수 있습니다.");

        String priority;
        if (project != null && project.contains("막힘")) {
            priority = "진행 중 공사의 병목부터 해소";
        } else if (housingCapacity > 0 && snapshot.population() >= housingCapacity) {
            priority = "주거 수용력 확장";
        } else if (productionMissingWorker > 0) {
            priority = "비어 있는 생산 일자리 충원";
        } else if (logisticsSaturated > 0) {
            priority = "중앙 저장·화물 공간 확장";
        } else if (productionBlocked > 0) {
            priority = "멈춘 생산시설 원인 확인";
        } else if (productionUpgradeBacklog > 0) {
            priority = "생산시설 개량 투자";
        } else if (logisticsUpgradeBacklog > 0) {
            priority = "창고·수레 정거장 확장";
        } else if (cityInvestmentBacklog > 0) {
            priority = "시민회관·교역회관 도시 투자";
        } else if (militaryUpgradeBacklog > 0) {
            priority = "방어망 개량 투자";
        } else if (matureTerritoryTier(snapshot.tier()) && territoryNetworkLevel < 3) {
            priority = "생산 특화 전초 다양화 · 영지망 " + territoryNetworkLevel + "/3";
        } else if (snapshot.nextGoal() != null && !snapshot.nextGoal().isBlank()) {
            priority = snapshot.nextGoal();
        } else {
            priority = "영토 확장과 방어망 보강";
        }

        return new SettlementOperationsSummary(
                housingCapacity, productionSites, productionWorking, productionMissingWorker,
                productionBlocked, productionUnknown, productionUpgradeBacklog,
                warehouses, cartStations, logisticsSaturated, logisticsUpgradeBacklog,
                guardPosts, watchtowers, barracks, citadels, militaryUpgradeBacklog, cityInvestmentBacklog,
                productiveOutpostDiversity, territoryNetworkLevel,
                snapshot.context().outpostCount(), priority, alerts);
    }

    private static void collectProductiveOutpostRole(Set<String> roles, String detail) {
        if (detail == null) return;
        if (detail.contains("역할 · 벌목")) roles.add("lumber");
        else if (detail.contains("역할 · 농업")) roles.add("agriculture");
        else if (detail.contains("역할 · 채석")) roles.add("quarry");
        else if (detail.contains("역할 · 광업")) roles.add("mining");
    }

    private static boolean matureTerritoryTier(String tier) {
        return "영지".equals(tier) || "개척 수도".equals(tier);
    }

    private static int territoryNetworkLevel(String tier, int diversity) {
        if (!matureTerritoryTier(tier)) return 0;
        return Math.min(3, Math.max(0, diversity - 1));
    }

    private static String buildingId(String key) {
        if (key == null || !key.startsWith("building:")) return "";
        int start = "building:".length();
        int end = key.indexOf(':', start);
        return end < 0 ? key.substring(start) : key.substring(start, end);
    }

    private static String productionStatus(String detail) {
        if (detail == null) return "";
        String marker = "상태: ";
        int index = detail.lastIndexOf(marker);
        return index < 0 ? "" : detail.substring(index + marker.length()).trim();
    }

    private static int gradeAfter(String detail, String marker) {
        if (detail == null) return 0;
        int index = detail.indexOf(marker);
        if (index < 0) return 0;
        String rest = detail.substring(index + marker.length()).trim();
        if (rest.startsWith("IV")) return 4;
        if (rest.startsWith("III")) return 3;
        if (rest.startsWith("II")) return 2;
        if (rest.startsWith("I")) return 1;
        return 0;
    }

    private static boolean isProduction(BuildingType type) {
        return type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM
                || type == BuildingType.QUARRY || type == BuildingType.MINE;
    }

    private static int productionCeiling(String tier) {
        return switch (tier) {
            case "마을" -> 2;
            case "개척 도시" -> 3;
            case "영지", "개척 수도" -> 4;
            default -> 1;
        };
    }

    private static int logisticsCeiling(String tier) {
        return switch (tier) {
            case "개척 도시" -> 2;
            case "영지", "개척 수도" -> 3;
            default -> 1;
        };
    }

    private static int militaryCeiling(String tier) {
        return switch (tier) {
            case "개척 도시" -> 2;
            case "영지" -> 3;
            case "개척 수도" -> 4;
            default -> 1;
        };
    }
}
