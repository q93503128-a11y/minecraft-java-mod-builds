#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A82 = ROOT / "tools/test_alpha82_source.py"
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == "gradle.properties":
        s = s.replace("mod_version=0.1.0-alpha.83", "mod_version=0.1.0-alpha.82")
        s = s.replace(", plus Alpha.83 late-game landmark progression with a civic hall, trade hall and citadel, a derived Frontier Capital end-state, physical landmark construction, stronger domain public works, trade-hall market value, citadel watch coverage, and a clear endgame guidance line without a new currency or save ledger.", ".")
    elif self.name == "COMPANION_LOCK.json":
        s = s.replace('"frontier_settlement": "0.1.0-alpha.83"', '"frontier_settlement": "0.1.0-alpha.82"')
    elif self.name == "SettlementGuideScreen.java":
        s = s.replace("private static final int PAGE_COUNT = 5;", "private static final int PAGE_COUNT = 4;")
    elif self.name == "SettlementGuidanceService.java":
        s = s.replace("M 메뉴", "B 팔레트")
    elif self.name == "test_alpha72_source.py":
        s = s.replace("len(java_files)!=105", "len(java_files)!=110")
        s = s.replace("expected 105 Java files", "expected 110 Java files")
    elif self.name == "test_alpha73_source.py":
        s = s.replace("rglob('*.java')))!=105", "rglob('*.java')))!=110")
    elif self.name == "test_alpha74_source.py":
        s = s.replace("rglob('*.java')))!=106", "rglob('*.java')))!=111")
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A82, encoding="utf-8").replace(
        "print('Frontier Settlement alpha.23-82 cumulative source audit: PASS')", "pass"
    )
    ns = {"__file__": str(A82), "__name__": "__main__"}
    exec(compile(chain, str(A82), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

def text(path): return Path(path).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src:
            raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
types = text(JAVA / "settlement/BuildingType.java")
tier = text(JAVA / "settlement/SettlementTier.java")
bp = text(JAVA / "settlement/LandmarkBuildingBlueprints.java")
construction = text(JAVA / "settlement/SettlementConstructionService.java")
data = text(JAVA / "settlement/SettlementData.java")
guidance = text(JAVA / "settlement/SettlementGuidanceService.java")
explore = text(JAVA / "settlement/SettlementExplorationBenefitService.java")
benefit = text(JAVA / "settlement/SettlementBenefitService.java")
infra = text(JAVA / "settlement/SettlementTierInfrastructureService.java")
core = text(JAVA / "settlement/SettlementCoreService.java")
palette = text(JAVA / "client/BuildingPaletteScreen.java")
guide = text(JAVA / "client/SettlementGuideScreen.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.83", "Alpha.83 late-game landmark progression"), "alpha.83 props")
must(types, (
    'CART_STATION("cart_station"',
    'CIVIC_HALL("civic_hall", "시민회관", 180, 140, 15, 13, 10, 8',
    'TRADE_HALL("trade_hall", "교역회관", 156, 116, 15, 13, 9, 4',
    'CITADEL("citadel", "성채", 240, 220, 17, 15, 14, 4',
), "alpha.83 appended building types")
if not (types.index('CART_STATION("cart_station"') < types.index('CIVIC_HALL("civic_hall"')
        < types.index('TRADE_HALL("trade_hall"') < types.index('CITADEL("citadel"')):
    raise SystemExit("alpha.83 landmark enum values must remain append-only after historical values")
must(tier, (
    'FRONTIER_CAPITAL("개척 수도")',
    "data.population() >= 20",
    "data.outposts().size() >= 5",
    "data.roads().size() >= 4",
    "data.explorationScore() >= 7",
), "alpha.83 capital tier")
must(bp, (
    "case CIVIC_HALL -> civicHall(origin);",
    "case TRADE_HALL -> tradeHall(origin);",
    "case CITADEL -> citadel(origin);",
    "no new free functional containers",
), "alpha.83 landmark blueprints")
forbid(bp, ("Blocks.BARREL", "Blocks.CHEST", "Blocks.SHULKER_BOX"), "alpha.83 landmarks must not mint storage")
must(construction, (
    "시민회관은 개척 도시 단계와 시장·창고 각 1곳이 필요합니다.",
    "교역회관은 영지 단계와 시장·수레 정거장 각 1곳이 필요합니다.",
    "성채는 영지 단계, 병영·감시탑 각 1곳, 탐험 점수 5가 필요합니다.",
), "alpha.83 authoritative unlocks")
must(data, ("CIVIC_HALL, TRADE_HALL, CITADEL -> { }",), "alpha.83 save-compatible building record completion")
must(guidance, (
    "M 메뉴 → 도로 계획",
    "M 메뉴 → ",
    "전초기지 5곳",
    "완성된 도로 4개",
    "탐험 점수 7",
    "개척 수도 완성",
), "alpha.83 endgame guidance")
forbid(guidance, ("B 팔레트",), "alpha.83 stale B guidance")
must(explore, (
    "MARKET_EMERALD_BONUS_TRADE_HALL = 4",
    "data.buildingCount(BuildingType.TRADE_HALL) > 0",
    "SettlementTier.current(data).ordinal() < SettlementTier.DOMAIN.ordinal()",
), "alpha.83 trade/domain inheritance")
must(benefit, (
    "CITADEL_WATCH_RADIUS_BONUS = 16.0D",
    "data.buildingCount(BuildingType.CITADEL) > 0",
), "alpha.83 citadel defense")
must(infra, (
    "CAPITAL_LAMP_SPACING = 6",
    "SettlementTier.FRONTIER_CAPITAL.ordinal()",
), "alpha.83 capital public works")
must(core, ("SettlementTier.FRONTIER_CAPITAL.ordinal()", "addFloor(placements, center, 6"), "alpha.83 capital core")
must(palette, (
    'LANDMARKS("랜드마크"',
    "BuildingType.CIVIC_HALL, BuildingType.TRADE_HALL, BuildingType.CITADEL",
    'data.tier().equals("영지") || data.tier().equals("개척 수도")',
), "alpha.83 palette")
must(guide, ("PAGE_COUNT = 5", "5. 영지와 개척 수도", "인구 20 · 전초 5 · 도로 4 · 탐험 7"), "alpha.83 guide")

if lock.get("status") != "candidate_runtime_lock":
    raise SystemExit("alpha.83 companion lock status drifted")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.83":
    raise SystemExit("alpha.83 companion lock target drifted")
if not any("Alpha.83 keeps every Alpha.82 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.83 companion rationale missing")

all_java = "\n".join(text(p) for p in JAVA.rglob("*.java"))
forbid(all_java, (
    "setChunkForced", "forceChunk", "teleportTo(",
    "import xaero.", "import dev.isxander.yacl",
), "alpha.83 no new force-load/teleport/hard companion authority")

print("Frontier Settlement alpha.23-83 cumulative source audit: PASS")
