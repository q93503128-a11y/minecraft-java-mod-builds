from pathlib import Path


def replace_once(text: str, old: str, new: str, name: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{name} anchor count={count}")
    return text.replace(old, new)


manager = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoritativeEconomyManager.java")
text = manager.read_text()
text = replace_once(text,
'''        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        long previousDay = economy.lastProcessedDay();
''',
'''        ErdenLivingEconomySavedData livingEconomy = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        long previousDay = economy.lastProcessedDay();
''', "daily living data")
text = replace_once(text,
'                result = processDay(population, economy.sites(), economy.wallets());\n',
'                result = processDay(day, population, economy.sites(), economy.wallets(), livingEconomy);\n',
"processDay invocation")
text = replace_once(text,
'''            economy.applyDay(
                    day, result.sites, result.wallets,
                    result.deliveries, result.crafted,
                    result.sales, result.wages);
            lastFulfilledHouseholds = result.fulfilledHouseholds;
''',
'''            economy.applyDay(
                    day, result.sites, result.wallets,
                    result.deliveries, result.crafted,
                    result.sales, result.wages);
            livingEconomy.applyDay(
                    ErdenLivingEconomyManager.LIVING_ECONOMY_REVISION,
                    day, result.market.states(),
                    result.market.fulfilledHouseholds(), result.market.failedHouseholds(),
                    result.market.closedFailures(), result.market.stockoutFailures(),
                    result.market.unaffordableFailures(), result.market.salesCoins());
            lastFulfilledHouseholds = result.fulfilledHouseholds;
''', "apply living day")
text = replace_once(text,
'''    private static DayResult processDay(
            ErdenPopulationSavedData population,
            List<ErdenPhysicalEconomySavedData.SiteState> existingSites,
            List<ErdenPhysicalEconomySavedData.WalletState> existingWallets) {
''',
'''    private static DayResult processDay(
            long day,
            ErdenPopulationSavedData population,
            List<ErdenPhysicalEconomySavedData.SiteState> existingSites,
            List<ErdenPhysicalEconomySavedData.WalletState> existingWallets,
            ErdenLivingEconomySavedData livingEconomy) {
''', "processDay signature")
text = replace_once(text,
'''        Map<Long, WorkerRef> workers = livingWorkers(population);
        DayCounters counters = new DayCounters();

        importWarehouseStock(sites);
''',
'''        Map<Long, WorkerRef> workers = livingWorkers(population);
        DayCounters counters = new DayCounters();

        ErdenLivingEconomyManager.prepareDay(day, sites);
        importWarehouseStock(sites);
''', "prepare market day")
text = replace_once(text,
'        sellToHouseholds(population, sites, wallets, counters);\n',
'''        ErdenLivingEconomyManager.MarketResult market =
                ErdenLivingEconomyManager.runDailyMarket(
                        day, population, sites, wallets, livingEconomy);
        counters.sales += market.salesCoins();
        counters.fulfilledHouseholds += market.fulfilledHouseholds();
''', "daily market")
text = replace_once(text,
'''                counters.sales, counters.wages,
                counters.fulfilledHouseholds,
                counters.reserveTransfers, counters.reserveMoved,
''',
'''                counters.sales, counters.wages,
                counters.fulfilledHouseholds, market,
                counters.reserveTransfers, counters.reserveMoved,
''', "DayResult constructor")
text = replace_once(text,
'''            long wages,
            int fulfilledHouseholds,
            int reserveTransfers,
''',
'''            long wages,
            int fulfilledHouseholds,
            ErdenLivingEconomyManager.MarketResult market,
            int reserveTransfers,
''', "DayResult record")
old = '''            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            switch (site.role()) {
'''
new = '''            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            if (site.metric("operating_today") <= 0L) continue;
            switch (site.role()) {
'''
if text.count(old) != 2:
    raise SystemExit(f"operating day anchors count={text.count(old)}")
text = text.replace(old, new)
text = replace_once(text,
'''                            + " | 출고 중 " + compactInTransit(site)
                            + " | 현금 " + site.metric("coins")
''',
'''                            + " | 출고 중 " + compactInTransit(site)
                            + " | 영업 " + ErdenLivingEconomyManager.siteStatus(site, level.getGameTime())
                            + " | 가격 " + ErdenLivingEconomyManager.priceText(site)
                            + " | 현금 " + site.metric("coins")
''', "worksite market status")
manager.write_text(text)

mod = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java")
text = mod.read_text()
text = replace_once(text,
'import kr.moonseungjun.livingkingdoms.world.ErdenDiagnosticDebrisSettler;\n',
'import kr.moonseungjun.livingkingdoms.world.ErdenDiagnosticDebrisSettler;\nimport kr.moonseungjun.livingkingdoms.world.ErdenLivingEconomyManager;\n',
"mod import")
text = replace_once(text,
'''        ErdenAuthoritativeEconomyManager.onServerTick(event);
        ErdenTransportManager.onServerTick(event);
''',
'''        ErdenAuthoritativeEconomyManager.onServerTick(event);
        ErdenLivingEconomyManager.onServerTick(event);
        ErdenTransportManager.onServerTick(event);
''', "mod tick")
text = replace_once(text,
'''        StarterNpcManager.handleInteraction(event);
        ErdenPopulationManager.handleInteraction(event);
''',
'''        StarterNpcManager.handleInteraction(event);
        ErdenPopulationManager.handleInteraction(event);
        ErdenLivingEconomyManager.handleInteraction(event);
''', "mod interaction")
mod.write_text(text)

workflow = Path(".github/workflows/build-living-kingdoms.yml")
text = workflow.read_text()
text = replace_once(text,
'''              && grep -Fq 'LK_ERDEN_PHYSICAL_ECONOMY_PASS sites=156 warehouses=15 wallets=77' ../../logs/server-smoke.log \\
              && grep -Fq 'LK_ERDEN_TRANSPORT_PASS revision=2 manifests=' ../../logs/server-smoke.log; then
''',
'''              && grep -Fq 'LK_ERDEN_PHYSICAL_ECONOMY_PASS sites=156 warehouses=15 wallets=77' ../../logs/server-smoke.log \\
              && grep -Fq 'LK_ERDEN_LIVING_ECONOMY_PASS revision=1 households=77 shops=50' ../../logs/server-smoke.log \\
              && grep -Fq 'LK_ERDEN_TRANSPORT_PASS revision=2 manifests=' ../../logs/server-smoke.log; then
''', "workflow readiness")
anchor = "          grep -F 'LK_ERDEN_PHYSICAL_ECONOMY_PASS sites=156 warehouses=15 wallets=77' ../../logs/server-smoke.log\n"
text = replace_once(text, anchor, anchor +
"          grep -F 'LK_ERDEN_LIVING_ECONOMY_PASS revision=1 households=77 shops=50' ../../logs/server-smoke.log\n"
"          grep -F 'schedules=true' ../../logs/server-smoke.log\n"
"          grep -F 'dynamic_prices=true' ../../logs/server-smoke.log\n"
"          grep -F 'stockout_path=true' ../../logs/server-smoke.log\n"
"          grep -F 'unaffordable_path=true' ../../logs/server-smoke.log\n", "workflow market markers")
anchor = "          grep -Fx 'kr/moonseungjun/livingkingdoms/world/ErdenPhysicalEconomySavedData.class' logs/jar-entries.txt\n"
text = replace_once(text, anchor, anchor +
"          grep -Fx 'kr/moonseungjun/livingkingdoms/world/ErdenLivingEconomyManager.class' logs/jar-entries.txt\n"
"          grep -Fx 'kr/moonseungjun/livingkingdoms/world/ErdenLivingEconomySavedData.class' logs/jar-entries.txt\n", "workflow jar classes")
anchor = "          - Physical worksite inventories, warehouse deliveries, household purchases and wages: PASS\n"
text = replace_once(text, anchor, anchor +
"          - Distributed shop hours, weekly holidays, stock-based prices and household purchase outcomes: PASS\n"
"          - Loaded dependent residents visibly follow recorded shopping errands: PASS\n", "workflow report")
workflow.write_text(text)

docs = Path("projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md")
text = docs.read_text()
anchor = "- 새 월드에서 창고·제빵소·상점 컨테이너 3곳의 실제 아이템 재고와 77가구 구매·154명 임금·제빵소 보존 재고를 동시에 확인하는 전용 회귀 검사\n"
addition = anchor + """- 출발 시 원재고를 차감해 출고 중으로 묶고, 실제 하역 완료 때만 도착 재고에 반영하며 실패 화물은 출발지로 반환하는 권위적 운송 정산
- 플레이어 주변 배송만 도로 경로·짐꾼·화물 수레·적재·이동·하역 상태로 실체화하고 언로드 구역은 같은 운송 시간을 집계 정산하는 성능 보호
- 새 월드에서 빵 1개를 상점에서 실제 출고한 뒤 제빵소에 하역·입고하고 총수량 보존을 확인하는 화물 에스크로 회귀 검사
- 50개 상점에 실제 영업시간과 서로 분산된 7일 주간 휴무를 배정하고, 휴무일에는 생산·서비스·구매가 중단되는 생활 경제
- 상점의 남은 빵·생활품 재고와 연속 품절 기간에 따라 빵·생활품·가구 묶음 가격이 제한 범위 안에서 변하는 동적 가격
- 77가구가 정해진 장보기 시간에 가까운 상점을 순서대로 탐색하고 휴무·품절·구매력 부족 시 다른 상점을 찾거나 실패 사유를 저장하는 구매 행동
- 로드된 가구의 아이·노인이 저장된 장보기 시간에 실제 상점 앞까지 이동하며, 주민 상호작용에서 오늘의 구매 성공·대체 상점·휴무·품절·가격 부족 결과를 설명하는 생활 동선
- 새 월드에서 77가구 시장 기록, 50개 상점 일정, 7일 휴무 분산, 동적 가격과 성공·휴무·품절·구매력 부족 네 경로를 확인하는 생활 경제 회귀 검사
"""
text = replace_once(text, anchor, addition, "documentation completed items")
for obsolete in (
    "- 작업장과 창고 사이를 실제로 이동하는 짐꾼·수레, 마구간 출발·도착, 하역, 경로 막힘과 배송 지연을 반영하는 실물 운송\n",
    "- 상점 개점·폐점, 주간 휴일, 가격 변동, 품절과 구매 실패를 주민 행동에 연결하는 생활 경제\n",
):
    text = replace_once(text, obsolete, "", "documentation remaining item")
docs.write_text(text)
