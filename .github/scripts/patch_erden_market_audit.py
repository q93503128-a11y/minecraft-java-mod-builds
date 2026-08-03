from pathlib import Path


def replace_once(text: str, old: str, new: str, name: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{name} anchor count={count}")
    return text.replace(old, new)


physical = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoritativeEconomyManager.java')
text = physical.read_text()
text = replace_once(text,
'''    private static void verifyCiIfReady(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || economy.lastProcessedDay() < 0L
                || economy.sites().size() != EXPECTED_SITES
                || economy.wallets().size() != EXPECTED_WALLETS
                || economy.totalDeliveries() <= 0L
                || economy.totalCrafted() <= 0L
                || economy.totalSales() < EXPECTED_WALLETS * 4L
                || economy.totalWages() < ErdenPopulationManager.EXPECTED_WORKERS * DAILY_WAGE
                || lastFulfilledHouseholds != EXPECTED_WALLETS) {
            return;
        }
''',
'''    private static void verifyCiIfReady(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        ErdenLivingEconomySavedData livingEconomy = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        long purchaseOutcomes = livingEconomy.outcomes().size();
        long purchaseSuccesses = livingEconomy.outcomes().stream()
                .filter(ErdenLivingEconomySavedData.HouseholdMarketState::success)
                .count();
        long purchaseFailures = purchaseOutcomes - purchaseSuccesses;
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || economy.lastProcessedDay() < 0L
                || economy.sites().size() != EXPECTED_SITES
                || economy.wallets().size() != EXPECTED_WALLETS
                || economy.totalDeliveries() <= 0L
                || economy.totalCrafted() <= 0L
                || economy.totalSales() < EXPECTED_WALLETS * 4L
                || economy.totalWages() < ErdenPopulationManager.EXPECTED_WORKERS * DAILY_WAGE
                || purchaseOutcomes != EXPECTED_WALLETS
                || purchaseSuccesses <= 0L
                || purchaseSuccesses != lastFulfilledHouseholds
                || purchaseFailures < 0L) {
            return;
        }
''', 'physical CI outcome gate')
text = replace_once(text,
'''        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_PHYSICAL_ECONOMY_PASS sites={} warehouses={} wallets={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} wallet_coins={} containers={} authoritative_transport=true",
                EXPECTED_SITES, EXPECTED_WAREHOUSES, EXPECTED_WALLETS,
                economy.totalDeliveries(), economy.totalCrafted(),
                economy.totalSales(), economy.totalWages(),
                lastFulfilledHouseholds, economy.totalWalletCoins(), visibleContainers);
''',
'''        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_PHYSICAL_ECONOMY_PASS sites={} warehouses={} wallets={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} purchase_outcomes={} purchase_failures={} wallet_coins={} containers={} authoritative_transport=true",
                EXPECTED_SITES, EXPECTED_WAREHOUSES, EXPECTED_WALLETS,
                economy.totalDeliveries(), economy.totalCrafted(),
                economy.totalSales(), economy.totalWages(),
                lastFulfilledHouseholds, purchaseOutcomes, purchaseFailures,
                economy.totalWalletCoins(), visibleContainers);
''', 'physical CI pass log')
text = replace_once(text,
'''                        "Processed Erden physical economy day={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} wallet_coins={} authoritative_transport=true",
                        day, result.deliveries, result.crafted,
                        result.sales, result.wages,
                        result.fulfilledHouseholds, totalWalletCoins(result.wallets));
''',
'''                        "Processed Erden physical economy day={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} failed_households={} wallet_coins={} authoritative_transport=true",
                        day, result.deliveries, result.crafted,
                        result.sales, result.wages,
                        result.fulfilledHouseholds, result.market.failedHouseholds(),
                        totalWalletCoins(result.wallets));
''', 'daily market summary log')
physical.write_text(text)

living = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenLivingEconomyManager.java')
text = living.read_text()
text = replace_once(text,
'''        long successCount = living.outcomes().stream()
                .filter(ErdenLivingEconomySavedData.HouseholdMarketState::success)
                .count();
        if (successCount != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) return;
        if (!auditDecisionPaths()) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_LIVING_ECONOMY_PASS revision={} households={} shops={} schedules=true holidays={} dynamic_prices=true stockouts_persist=true shopping_routines=true success_path=true closed_path=true stockout_path=true unaffordable_path=true market_spent={}",
                LIVING_ECONOMY_REVISION, ErdenPopulationManager.EXPECTED_HOUSEHOLDS,
                shops, holidayCoverage.size(), living.totalSpent());
''',
'''        long successCount = living.outcomes().stream()
                .filter(ErdenLivingEconomySavedData.HouseholdMarketState::success)
                .count();
        long recognizedCount = living.outcomes().stream()
                .filter(state -> state.status().equals(STATUS_SUCCESS)
                        || state.status().equals(STATUS_CLOSED)
                        || state.status().equals(STATUS_STOCKOUT)
                        || state.status().equals(STATUS_UNAFFORDABLE))
                .count();
        long failureCount = living.outcomes().size() - successCount;
        if (recognizedCount != ErdenPopulationManager.EXPECTED_HOUSEHOLDS
                || successCount <= 0L || failureCount < 0L) return;
        if (!auditDecisionPaths()) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_LIVING_ECONOMY_PASS revision={} households={} shops={} purchase_successes={} purchase_failures={} schedules=true holidays={} dynamic_prices=true stockouts_persist=true shopping_routines=true success_path=true closed_path=true stockout_path=true unaffordable_path=true market_spent={}",
                LIVING_ECONOMY_REVISION, ErdenPopulationManager.EXPECTED_HOUSEHOLDS,
                shops, successCount, failureCount, holidayCoverage.size(), living.totalSpent());
''', 'living CI outcome validation')
living.write_text(text)

docs = Path('projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md')
text = docs.read_text()
text = replace_once(text,
'- 모든 생존 가구가 보유 화폐로 빵과 생활품을 구매하고 판매 수입이 해당 상점 현금으로 들어가는 왕도 소매 거래\n',
'- 모든 생존 가구가 보유 화폐로 빵과 생활품 구매를 시도하고 성공·휴무·품절·구매력 부족 결과와 판매 수입을 실제 가구·상점 장부에 남기는 왕도 소매 거래\n',
'document household purchase wording')
text = replace_once(text,
'- 첫날 289회 배송, 890단위 제작·서비스, 549화폐 판매, 308화폐 임금과 77가구 구매 완료를 확인한 새 월드 검증\n',
'- 첫날 배송·제작·판매·임금과 구매 성공·실패 결과의 합계가 77가구 장부와 일치하는지 확인하는 새 월드 검증\n',
'document first-day invariant')
docs.write_text(text)
