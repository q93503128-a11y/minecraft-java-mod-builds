from pathlib import Path

ROOT = Path("projects/frontier-settlement")
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
SETTLEMENT = JAVA / "settlement"

def replace_once(path, old, new, label):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

context = SETTLEMENT / "SettlementContextService.java"
replace_once(context,
    '            case WAREHOUSE -> "완공 · 실물 저장";',
'''            case WAREHOUSE -> "완공 · 물류 " + SettlementLogisticsUpgradeService.gradeLabel(SettlementLogisticsUpgradeService.grade(building))
                    + " · 중앙 저장 " + SettlementLogisticsUpgradeService.warehouseStorageCount(building) + "통 · "
                    + SettlementLogisticsUpgradeService.storageSummary(level, building) + " · "
                    + SettlementLogisticsUpgradeService.upgradeHint(data, building);''',
    "warehouse context")
replace_once(context,
    '            case CART_STATION -> "완공 · 도로 화물 허브";',
'''            case CART_STATION -> "완공 · 물류 " + SettlementLogisticsUpgradeService.gradeLabel(SettlementLogisticsUpgradeService.grade(building))
                    + " · 화물 저장 " + SettlementLogisticsUpgradeService.cartFreightStorageCount(building) + "통 · 생산 운송 "
                    + SettlementOutpostLogisticsService.productiveTransportBatchSize(data) + "개/회 · "
                    + SettlementLogisticsUpgradeService.storageSummary(level, building) + " · "
                    + SettlementLogisticsUpgradeService.upgradeHint(data, building);''',
    "cart context")

guide = JAVA / "client/SettlementGuideScreen.java"
replace_once(guide,
'''            case 3 -> draw(g, x, y, "4. 영토와 물류",
                    "M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.",
                    "도로 끝에 전초기지를 세워 영토·생산 거점을 넓힙니다.",
                    "체크포인트를 바꿔도 저장된 거점 좌표는 사라지지 않습니다.",
                    "언로드 지역은 강제로 로드하지 않으며 운송도 멈춥니다.");''',
'''            case 3 -> draw(g, x, y, "4. 영토와 물류",
                    "M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.",
                    "창고·수레 정거장은 빈손 웅크리기+저장통 우클릭으로 물류 II~III를 확장합니다.",
                    "확장된 창고는 실물 저장통을 늘리고, 수레 정거장은 전초 생산 화물 처리량을 높입니다.",
                    "언로드 지역은 강제로 로드하지 않으며 운송도 멈춥니다.");''',
    "teach logistics investment")

props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.1.0-alpha.122", "mod_version=0.1.0-alpha.123", "version bump")
with props.open("a", encoding="utf-8") as f:
    f.write("\n# Alpha.123 central logistics investment: warehouse/cart-station II-III add physical storage and productive freight throughput.\n")
    f.write("# Alpha.123 canonical validation trigger (2026-09-07).\n")

readme = ROOT / "README.md"
replace_once(readme, "## Current version: 0.1.0-alpha.122", "## Current version: 0.1.0-alpha.123", "README version")
with readme.open("a", encoding="utf-8") as f:
    f.write('''

## Alpha.123 RTS central logistics investment

- Warehouse and cart-station logistics grades are persistent per building. Grade II unlocks at Frontier Town and III at Domain; settlement tier is an unlock ceiling, never a free logistics upgrade.
- Warehouse II costs 256 wood + 192 stone + 24 physical copper/iron items; III costs 512 wood + 384 stone + 64 copper/iron items.
- Cart-station II costs 320 wood + 224 stone + 32 physical copper/iron items; III costs 640 wood + 448 stone + 96 copper/iron items.
- Existing warehouse/cart-station base storage remains valid. Warehouse expansion adds bounded real barrels from 6 -> 10 -> 14; cart-station freight storage grows 4 -> 6 -> 8.
- Inactive future expansion cells never join the resource ledger. Upgrade placement refuses fluids, containers and unrelated/player blocks instead of replacing them.
- Productive outpost-to-town freight grows with cart-station grade (32/40/48 items before bonuses), then receives up to +8 from the best warehouse and the existing territory-network bonus, with a hard 64-item cap.
- Military and waterfront reverse-supply trips deliberately keep the existing ordinary cart-station cap; Alpha.123 does not create a second transporter, teleport cargo or force-load routes.
- M/Jade building context shows logistics grade, physical storage usage, saturation and the next upgrade requirement/cost.
''')

canonical = ROOT / "CANONICAL_PLAN.md"
with canonical.open("a", encoding="utf-8") as f:
    f.write('''

## Alpha.123 logistics authority lock

Central logistics now follows the same RTS investment rule as production: settlement tier unlocks capacity, while each warehouse/cart station must be paid individually with real physical resources. Warehouse grades expand only bounded physical barrel capacity; cart-station grades increase only the existing road transporter's productive outpost-to-town pickup ceiling. Local production barrels remain buffers, shared/warehouse storage remains the durable resource authority, and optional shared depots remain compatible. No logistics grade may introduce virtual cargo, background teleport delivery, forced chunk loading, a second transport worker authority, or destructive replacement of player containers/fluids/blocks.
''')

test = ROOT / "tools/test_current_source.py"
replace_once(test,
    'require("mod_version=0.1.0-alpha.122" in gradle, "current verifier/version drift")',
    'require("mod_version=0.1.0-alpha.123" in gradle, "current verifier/version drift")',
    "source audit version")
with test.open("a", encoding="utf-8") as f:
    f.write(r'''

logistics_upgrade = text(SETTLEMENT / "SettlementLogisticsUpgradeService.java")
warehouse_layout = text(SETTLEMENT / "WarehouseLayout.java")
cart_layout = text(SETTLEMENT / "CartStationLayout.java")
require("case CAMP, HAMLET, VILLAGE -> 1" in logistics_upgrade
        and "case FRONTIER_TOWN -> 2" in logistics_upgrade
        and "case DOMAIN, FRONTIER_CAPITAL -> 3" in logistics_upgrade,
        "logistics tier ceiling drifted")
require("new UpgradeCost(256L, 192L, 24L)" in logistics_upgrade
        and "new UpgradeCost(512L, 384L, 64L)" in logistics_upgrade
        and "new UpgradeCost(320L, 224L, 32L)" in logistics_upgrade
        and "new UpgradeCost(640L, 448L, 96L)" in logistics_upgrade,
        "warehouse/cart logistics investment costs drifted")
require("player.isShiftKeyDown()" in logistics_upgrade and "event.getItemStack().isEmpty()" in logistics_upgrade
        and "logisticsBuildingAt" in logistics_upgrade,
        "player-directed logistics upgrade interaction missing")
require("countCommonUpgradeMetal" in logistics_upgrade and "consumeLogisticsUpgrade" in logistics_upgrade,
        "logistics investment bypasses common-metal atomic payment")
require("Grade I/II/III = 6/10/14 real barrels" in warehouse_layout
        and "activeStoragePositions" in warehouse_layout,
        "warehouse physical capacity ladder missing")
require("Grade I/II/III = 4/6/8 physical freight barrels" in cart_layout
        and "activeFreightPositions" in cart_layout,
        "cart-station physical freight capacity ladder missing")
require("WarehouseLayout.activeStoragePositions(building)" in storage
        and "CartStationLayout.activeFreightPositions(building)" in storage,
        "inactive future logistics barrels can join the settlement ledger")
require("SettlementLogisticsUpgradeService.ensureManagedStorage(level, data)" in storage
        and "public static boolean canSafelyCreateManagedBarrel" in storage,
        "safe logistics storage provisioning is not wired")
require("CART_STATION_GRADE_II_TRANSPORT_STACK = 40" in logistics
        and "CART_STATION_GRADE_III_TRANSPORT_STACK = 48" in logistics
        and "MAX_PRODUCTIVE_TRANSPORT_STACK = 64" in logistics
        and "SettlementLogisticsUpgradeService.warehouseFreightBonus(data)" in logistics,
        "productive freight grade/warehouse throughput ladder missing")
require("public static int transportBatchSize(SettlementData data)" in logistics
        and "CART_STATION_TRANSPORT_STACK" in logistics,
        "ordinary reverse-supply transport authority was removed")
require("SettlementLogisticsUpgradeService.tick(server, data)" in service,
        "logistics legacy migration is not wired into settlement runtime")
require("SettlementLogisticsUpgradeService::onRightClickBlock" in entry,
        "logistics upgrade interaction is not registered")
require("SettlementLogisticsUpgradeService.storageSummary(level, building)" in context,
        "warehouse/cart saturation context missing")
require("창고·수레 정거장은 빈손 웅크리기+저장통 우클릭" in guide_screen,
        "in-game guide does not teach logistics investment")
''')

print("UI/docs/test patch applied")
