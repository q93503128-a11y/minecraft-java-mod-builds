#!/usr/bin/env python3
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
P = ROOT / "projects/frontier-settlement"
JAVA = P / "src/main/java/kr/moonseungjun/frontiersettlement"
SETT = JAVA / "settlement"
CLIENT = JAVA / "client"
T = ROOT / ".github/frontier-alpha83"

def read(path):
    return Path(path).read_text(encoding="utf-8")

def write(path, text):
    Path(path).write_text(text, encoding="utf-8")

def replace_once(path, old, new, label):
    path = Path(path)
    s = read(path)
    count = s.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 match, got {count}")
    write(path, s.replace(old, new, 1))

replace_once(P/"gradle.properties", "mod_version=0.1.0-alpha.82", "mod_version=0.1.0-alpha.83", "version")
replace_once(
    P/"gradle.properties",
    "plus Alpha.82 collision-safe pack key profile with M as the Frontier settlement menu and default-only Xaero quick-waypoint B normalization that preserves user-customized controls.",
    "plus Alpha.82 collision-safe pack key profile with M as the Frontier settlement menu and default-only Xaero quick-waypoint B normalization that preserves user-customized controls, plus Alpha.83 late-game landmark progression with a civic hall, trade hall and citadel, a derived Frontier Capital end-state, physical landmark construction, stronger domain public works, trade-hall market value, citadel watch coverage, and a clear endgame guidance line without a new currency or save ledger.",
    "description",
)
replace_once(
    SETT/"BuildingType.java",
    '    CART_STATION("cart_station", "수레 정거장", 104, 56, 13, 9, 8, 0, "도로 + 전초기지 필요");',
    '''    CART_STATION("cart_station", "수레 정거장", 104, 56, 13, 9, 8, 0, "도로 + 전초기지 필요"),
    CIVIC_HALL("civic_hall", "시민회관", 180, 140, 15, 13, 10, 8, "개척 도시 + 시장 + 창고 필요"),
    TRADE_HALL("trade_hall", "교역회관", 156, 116, 15, 13, 9, 4, "영지 + 시장 + 수레 정거장 필요"),
    CITADEL("citadel", "성채", 240, 220, 17, 15, 14, 4, "영지 + 병영 + 감시탑 + 탐험 필요");''',
    "building types",
)
replace_once(
    SETT/"BuildingBlueprints.java",
    '''            case MARKET -> MarketBuildingBlueprint.create(origin);
            case CART_STATION -> CartStationBuildingBlueprint.create(origin);''',
    '''            case MARKET -> MarketBuildingBlueprint.create(origin);
            case CART_STATION -> CartStationBuildingBlueprint.create(origin);
            case CIVIC_HALL, TRADE_HALL, CITADEL -> LandmarkBuildingBlueprints.create(type, origin);''',
    "blueprint routing",
)
replace_once(
    SETT/"SettlementData.java",
    '''            case FARM, QUARRY, MINE, WAREHOUSE, CONSTRUCTION_OFFICE, BLACKSMITH, WORKSHOP, ADVANCED_WORKSHOP,
                    CART_STATION, GUARD_POST, WATCHTOWER, BARRACKS, MARKET -> { }''',
    '''            case FARM, QUARRY, MINE, WAREHOUSE, CONSTRUCTION_OFFICE, BLACKSMITH, WORKSHOP, ADVANCED_WORKSHOP,
                    CART_STATION, GUARD_POST, WATCHTOWER, BARRACKS, MARKET, CIVIC_HALL, TRADE_HALL, CITADEL -> { }''',
    "completion switch",
)
replace_once(
    SETT/"SettlementTier.java",
    '''    FRONTIER_TOWN("개척 도시"),
    DOMAIN("영지");''',
    '''    FRONTIER_TOWN("개척 도시"),
    DOMAIN("영지"),
    FRONTIER_CAPITAL("개척 수도");''',
    "tier enum",
)
replace_once(
    SETT/"SettlementTier.java",
    "        boolean legacyDomain = data.population() >= 16",
    '''        boolean frontierCapital = data.population() >= 20
                && data.outposts().size() >= 5
                && data.roads().size() >= 4
                && data.buildingCount(BuildingType.CIVIC_HALL) >= 1
                && data.buildingCount(BuildingType.TRADE_HALL) >= 1
                && data.buildingCount(BuildingType.CITADEL) >= 1
                && data.explorationScore() >= 7;
        if (frontierCapital) return FRONTIER_CAPITAL;

        boolean legacyDomain = data.population() >= 16''',
    "capital condition",
)
replace_once(
    SETT/"SettlementConstructionService.java",
    '''        if (type == BuildingType.MARKET
                && SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return "시장은 마을 단계에 도달하면 열립니다.";
        }
        return null;''',
    '''        if (type == BuildingType.MARKET
                && SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return "시장은 마을 단계에 도달하면 열립니다.";
        }
        if (type == BuildingType.CIVIC_HALL
                && (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()
                || data.buildingCount(BuildingType.MARKET) < 1
                || data.buildingCount(BuildingType.WAREHOUSE) < 1)) {
            return "시민회관은 개척 도시 단계와 시장·창고 각 1곳이 필요합니다.";
        }
        if (type == BuildingType.TRADE_HALL
                && (SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()
                || data.buildingCount(BuildingType.MARKET) < 1
                || data.buildingCount(BuildingType.CART_STATION) < 1)) {
            return "교역회관은 영지 단계와 시장·수레 정거장 각 1곳이 필요합니다.";
        }
        if (type == BuildingType.CITADEL
                && (SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()
                || data.buildingCount(BuildingType.BARRACKS) < 1
                || data.buildingCount(BuildingType.WATCHTOWER) < 1
                || data.explorationScore() < 5)) {
            return "성채는 영지 단계, 병영·감시탑 각 1곳, 탐험 점수 5가 필요합니다.";
        }
        return null;''',
    "landmark locks",
)
replace_once(
    SETT/"SettlementExplorationBenefitService.java",
    "    public static final int MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL = 1;",
    '''    public static final int MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL = 1;
    public static final int MARKET_EMERALD_BONUS_TRADE_HALL = 4;''',
    "trade bonus const",
)
replace_once(
    SETT/"SettlementExplorationBenefitService.java",
    "        if (SettlementTier.current(data) != SettlementTier.DOMAIN) return 0;",
    "        if (SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()) return 0;",
    "domain inheritance",
)
replace_once(
    SETT/"SettlementExplorationBenefitService.java",
    "                + territoryNetworkLevel(data) * MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL;",
    '''                + territoryNetworkLevel(data) * MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL
                + (data.buildingCount(BuildingType.TRADE_HALL) > 0 ? MARKET_EMERALD_BONUS_TRADE_HALL : 0);''',
    "trade payout",
)
replace_once(
    SETT/"SettlementBenefitService.java",
    "    private static final double WATCHTOWER_ALERT_RADIUS = 40.0D;",
    '''    private static final double WATCHTOWER_ALERT_RADIUS = 40.0D;
    private static final double CITADEL_WATCH_RADIUS_BONUS = 16.0D;''',
    "citadel const",
)
replace_once(SETT/"SettlementBenefitService.java",
             "            Monster threat = nearestWatchThreat(level, home);",
             "            Monster threat = nearestWatchThreat(level, home, data);", "citadel call")
replace_once(
    SETT/"SettlementBenefitService.java",
    '''    private static Monster nearestWatchThreat(ServerLevel level, BlockPos home) {
        AABB area = new AABB(home).inflate(WATCHTOWER_ALERT_RADIUS, 16.0D, WATCHTOWER_ALERT_RADIUS);''',
    '''    private static Monster nearestWatchThreat(ServerLevel level, BlockPos home, SettlementData data) {
        double radius = WATCHTOWER_ALERT_RADIUS
                + (data.buildingCount(BuildingType.CITADEL) > 0 ? CITADEL_WATCH_RADIUS_BONUS : 0.0D);
        AABB area = new AABB(home).inflate(radius, 16.0D, radius);''',
    "citadel method",
)
replace_once(
    SETT/"SettlementTierInfrastructureService.java",
    "    private static final int DOMAIN_LAMP_SPACING = 8;",
    '''    private static final int DOMAIN_LAMP_SPACING = 8;
    private static final int CAPITAL_LAMP_SPACING = 6;''',
    "capital lamp const",
)
replace_once(
    SETT/"SettlementTierInfrastructureService.java",
    "        int spacing = tier == SettlementTier.DOMAIN ? DOMAIN_LAMP_SPACING : FRONTIER_TOWN_LAMP_SPACING;",
    '''        int spacing = tier.ordinal() >= SettlementTier.FRONTIER_CAPITAL.ordinal()
                ? CAPITAL_LAMP_SPACING
                : tier.ordinal() >= SettlementTier.DOMAIN.ordinal() ? DOMAIN_LAMP_SPACING : FRONTIER_TOWN_LAMP_SPACING;''',
    "capital lamp choice",
)
replace_once(
    SETT/"SettlementTierInfrastructureService.java",
    '''        if(block!=Blocks.OAK_FENCE&&block!=Blocks.LANTERN)return;
        if(matchesLampPlan(level,data,pos,block,DOMAIN_LAMP_SPACING)||matchesLampPlan(level,data,pos,block,FRONTIER_TOWN_LAMP_SPACING)){event.setCanceled(true);event.setNotifyClient(true);}''',
    '''        if(block!=Blocks.OAK_FENCE&&block!=Blocks.LANTERN)return;
        if(matchesLampPlan(level,data,pos,block,CAPITAL_LAMP_SPACING)
                ||matchesLampPlan(level,data,pos,block,DOMAIN_LAMP_SPACING)
                ||matchesLampPlan(level,data,pos,block,FRONTIER_TOWN_LAMP_SPACING)){event.setCanceled(true);event.setNotifyClient(true);}''',
    "capital lamp protection",
)
replace_once(
    SETT/"SettlementCoreService.java",
    '''        if (tier.ordinal() >= SettlementTier.DOMAIN.ordinal()) {
            addFloor(placements, center, 5, Blocks.STONE_BRICKS.defaultBlockState());
            addCross(placements, center, 5, Blocks.POLISHED_ANDESITE.defaultBlockState());
            addCornerAccents(placements, center, 5, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            addLampRing(placements, center, 5, 4, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        }
        return new ArrayList<>(placements.values());''',
    '''        if (tier.ordinal() >= SettlementTier.DOMAIN.ordinal()) {
            addFloor(placements, center, 5, Blocks.STONE_BRICKS.defaultBlockState());
            addCross(placements, center, 5, Blocks.POLISHED_ANDESITE.defaultBlockState());
            addCornerAccents(placements, center, 5, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            addLampRing(placements, center, 5, 4, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        }
        if (tier.ordinal() >= SettlementTier.FRONTIER_CAPITAL.ordinal()) {
            addFloor(placements, center, 6, Blocks.STONE_BRICKS.defaultBlockState());
            addCross(placements, center, 6, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            addCornerAccents(placements, center, 6, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            addLampRing(placements, center, 6, 5, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        }
        return new ArrayList<>(placements.values());''',
    "capital core",
)
replace_once(
    CLIENT/"BuildingPaletteScreen.java",
    '''        DEFENSE("방어", "경계 감시와 주둔 병력", List.of(BuildingType.GUARD_POST, BuildingType.WATCHTOWER, BuildingType.BARRACKS)),
        INFRA("인프라", "도로, 전초기지, 영지 토목", List.of());''',
    '''        DEFENSE("방어", "경계 감시와 주둔 병력", List.of(BuildingType.GUARD_POST, BuildingType.WATCHTOWER, BuildingType.BARRACKS)),
        LANDMARKS("랜드마크", "중후반 도시 기능과 최종 목표", List.of(BuildingType.CIVIC_HALL, BuildingType.TRADE_HALL, BuildingType.CITADEL)),
        INFRA("인프라", "도로, 전초기지, 영지 토목", List.of());''',
    "landmark category",
)
replace_once(
    CLIENT/"BuildingPaletteScreen.java",
    '''        Button civil = Button.builder(Component.literal(
                        data.tier().equals("영지") ? "토목 평탄화 · 절토/성토" : "토목 평탄화 [영지 잠김]"),''',
    '''        boolean civilUnlocked = data.tier().equals("영지") || data.tier().equals("개척 수도");
        Button civil = Button.builder(Component.literal(
                        civilUnlocked ? "토목 평탄화 · 절토/성토" : "토목 평탄화 [영지 잠김]"),''',
    "civil label",
)
replace_once(CLIENT/"BuildingPaletteScreen.java",
             '        civil.active = data.tier().equals("영지");',
             "        civil.active = civilUnlocked;", "civil active")

copies = {
    T/"SettlementGuidanceService.java": SETT/"SettlementGuidanceService.java",
    T/"SettlementGuideScreen.java": CLIENT/"SettlementGuideScreen.java",
    T/"LandmarkBuildingBlueprints.java": SETT/"LandmarkBuildingBlueprints.java",
    T/"CONTENT_EXPANSION_ALPHA83.md": P/"CONTENT_EXPANSION_ALPHA83.md",
    T/"test_alpha83_source.py": P/"tools/test_alpha83_source.py",
    T/"test_alpha83_docs.py": P/"tools/test_alpha83_docs.py",
}
for src, dst in copies.items():
    if not src.exists():
        raise SystemExit(f"missing template {src}")
    write(dst, read(src))

lock_path = P/"COMPANION_LOCK.json"
lock = json.loads(read(lock_path))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.82":
    raise SystemExit("lock was not alpha.82")
lock["generated_at"] = "2026-08-28"
lock["target"]["frontier_settlement"] = "0.1.0-alpha.83"
note = ("Alpha.83 keeps every Alpha.82 companion binary pin unchanged while adding Frontier-owned late-game "
        "landmarks (civic hall, trade hall, citadel) and the derived Frontier Capital end-state. Landmark "
        "construction still consumes real shared wood/stone through the existing builder transaction; the "
        "trade hall only boosts existing physical relic emerald payout, the citadel only extends existing "
        "loaded watchtower coverage, and no companion code/assets, new currency, force-load, teleport or "
        "second logistics authority is added.")
if not any(str(n).startswith("Alpha.83 ") for n in lock.get("notes", [])):
    lock.setdefault("notes", []).append(note)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

for p in [ROOT/".github/workflows/apply-frontier-alpha83-content.yml",
          ROOT/".github/scripts/frontier-alpha83-content-apply.py"]:
    if p.exists():
        p.unlink()
if T.exists():
    shutil.rmtree(T)
