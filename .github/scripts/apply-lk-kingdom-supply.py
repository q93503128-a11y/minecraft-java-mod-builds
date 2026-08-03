from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoritativeEconomyManager.java")
text = path.read_text(encoding="utf-8")

old = """        if (level.getGameTime() % SYNC_INTERVAL == 0L) {\n            captureLoadedContainers(level, economy);\n        }\n        processDailyEconomy(level, population, economy);\n"""
new = """        if (level.getGameTime() % SYNC_INTERVAL == 0L) {\n            captureLoadedContainers(level, economy);\n        }\n        ErdenKingdomSupplyManager.prepareBeforeCityEconomy(level, economy);\n        processDailyEconomy(level, population, economy);\n"""
assert text.count(old) == 1, "server tick integration anchor changed"
text = text.replace(old, new)

old = """                            + \" | 현금 \" + site.metric(\"coins\")\n                            + \" | 수령 \" + site.metric(\"received\")\n"""
new = """                            + \" | 현금 \" + site.metric(\"coins\")\n                            + \" | 외곽 입고 \" + site.metric(\"kingdom_supply_received\")\n                            + \" | 수령 \" + site.metric(\"received\")\n"""
assert text.count(old) == 1, "warehouse interaction anchor changed"
text = text.replace(old, new)

old = """        ErdenLivingEconomyManager.prepareDay(day, sites);\n        importWarehouseStock(sites);\n        deliverRawMaterials(sites, workers, counters);\n"""
new = """        ErdenLivingEconomyManager.prepareDay(day, sites);\n        deliverRawMaterials(sites, workers, counters);\n"""
assert text.count(old) == 1, "daily import call anchor changed"
text = text.replace(old, new)

old = """    private static void importWarehouseStock(\n            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites) {\n        for (ErdenPhysicalEconomySavedData.SiteState site : List.copyOf(sites.values())) {\n            if (!site.role().equals(\"warehouse\")) continue;\n            ErdenPhysicalEconomySavedData.SiteState updated = site\n                    .addStock(\"wheat\", 96L)\n                    .addStock(\"coal\", 32L)\n                    .addStock(\"leather\", 24L)\n                    .addStock(\"paper\", 32L)\n                    .addStock(\"iron\", 20L)\n                    .addStock(\"hay\", 40L)\n                    .addMetric(\"imports\", 244L);\n            sites.put(updated.id(), updated);\n        }\n    }\n\n"""
assert text.count(old) == 1, "fixed warehouse import method anchor changed"
text = text.replace(old, "")

old = """                || !\"1\".equals(System.getenv(\"LIVING_KINGDOMS_CI_REALM_TEST\"))\n                || economy.lastProcessedDay() < 0L\n"""
new = """                || !\"1\".equals(System.getenv(\"LIVING_KINGDOMS_CI_REALM_TEST\"))\n                || !ErdenKingdomSupplyManager.isReady(level, economy)\n                || economy.lastProcessedDay() < 0L\n"""
assert text.count(old) == 1, "physical economy CI anchor changed"
text = text.replace(old, new)

old = """                \"LK_ERDEN_PHYSICAL_ECONOMY_PASS sites={} warehouses={} wallets={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} purchase_outcomes={} purchase_failures={} wallet_coins={} containers={} authoritative_transport=true\",\n"""
new = """                \"LK_ERDEN_PHYSICAL_ECONOMY_PASS sites={} warehouses={} wallets={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} purchase_outcomes={} purchase_failures={} wallet_coins={} containers={} authoritative_transport=true kingdom_supply=true\",\n"""
assert text.count(old) == 1, "physical economy pass marker anchor changed"
text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
