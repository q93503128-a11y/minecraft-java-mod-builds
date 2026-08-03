from pathlib import Path


def replace_once_or_verify(text: str, old: str, new: str, label: str) -> str:
    old_count = text.count(old)
    new_count = text.count(new)
    if old_count == 1:
        return text.replace(old, new)
    if old_count == 0 and new_count == 1:
        return text
    raise SystemExit(
        f"Patch {label!r} expected one old match or one applied match; "
        f"old={old_count} new={new_count}"
    )


# Keep the exterior audit bounded while preserving normal player-driven streaming.
exterior_path = Path(
    "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java"
)
exterior = exterior_path.read_text(encoding="utf-8")
exterior_replacements = [
    (
        "    private static final int TICK_BUDGET = 2_000;\n    private static final int CI_FORCE_BUDGET = 1;",
        "    private static final int TICK_BUDGET = 2_000;\n    private static final int CI_TICK_BUDGET = 16_000;\n    private static final int CI_FORCE_BUDGET = 1;\n    private static final int CI_MAX_IN_FLIGHT = 3;",
        "exterior budgets",
    ),
    (
        "    private static final Set<Long> CI_LOADING = new HashSet<>();\n    private static final Set<Long> QUEUED = new HashSet<>();",
        "    private static final Set<Long> CI_LOADING = new HashSet<>();\n    private static final Set<Long> CI_REQUIRED = new HashSet<>();\n    private static final Set<Long> QUEUED = new HashSet<>();",
        "required chunks",
    ),
    (
        "        ChunkPos chunk = event.getChunk().getPos();\n        if (!intersectsExterior(chunk)) return;\n        enqueue(level, pack(chunk.x(), chunk.z()), false);",
        "        ChunkPos chunk = event.getChunk().getPos();\n        if (!intersectsExterior(chunk)) return;\n        long packed = pack(chunk.x(), chunk.z());\n        if (isCi() && !CI_REQUIRED.contains(packed)) return;\n        enqueue(level, packed, false);",
        "ignore incidental CI loads",
    ),
    (
        "        active.plan.apply(level, TICK_BUDGET);",
        "        active.plan.apply(level, isCi() ? CI_TICK_BUDGET : TICK_BUDGET);",
        "CI write budget",
    ),
    (
        "        CI_LOADING.clear();\n        QUEUED.clear();",
        "        CI_LOADING.clear();\n        CI_REQUIRED.clear();\n        QUEUED.clear();",
        "reset required chunks",
    ),
    (
        "        CI_REQUESTS.addAll(unique);",
        "        CI_REQUIRED.addAll(unique);\n        CI_REQUESTS.addAll(unique);",
        "register required chunks",
    ),
    (
        "        for (int forced = 0; forced < CI_FORCE_BUDGET && !CI_REQUESTS.isEmpty(); forced++) {",
        "        for (int forced = 0; forced < CI_FORCE_BUDGET\n                && !CI_REQUESTS.isEmpty()\n                && RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {",
        "bound forced chunks",
    ),
    (
        "                \"Requested Erden exterior CI anchors nodes={} request_queue={} metre_scale=true streamed=true staggered=true synchronous_get_chunk=false\",",
        "                \"Requested Erden exterior CI anchors nodes={} request_queue={} metre_scale=true streamed=true staggered=true synchronous_get_chunk=false max_in_flight={}\",",
        "CI request marker",
    ),
    (
        "                ErdenKingdomSupplyCatalog.nodes().size(), CI_REQUESTS.size());",
        "                ErdenKingdomSupplyCatalog.nodes().size(), CI_REQUESTS.size(), CI_MAX_IN_FLIGHT);",
        "CI request marker values",
    ),
]
for old, new, label in exterior_replacements:
    exterior = replace_once_or_verify(exterior, old, new, label)
exterior_path.write_text(exterior, encoding="utf-8")


# Make daily production and dispatch depend on the saved, living exterior workforce.
supply_path = Path(
    "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomSupplyManager.java"
)
supply = supply_path.read_text(encoding="utf-8")
supply_replacements = [
    (
        "        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);\n        if (!supply.hasSupply(SUPPLY_REVISION, EXPECTED_NODES)) {",
        "        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);\n        ErdenExteriorWorkforceManager.prepareBeforeSupply(level, currentDay);\n        if (!supply.hasSupply(SUPPLY_REVISION, EXPECTED_NODES)) {",
        "prepare workforce before supply",
    ),
    (
        "        ErdenKingdomSupplySavedData supply = level.getDataStorage()\n                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);\n        return supply.hasSupply(SUPPLY_REVISION, EXPECTED_NODES)",
        "        ErdenKingdomSupplySavedData supply = level.getDataStorage()\n                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);\n        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);\n        return supply.hasSupply(SUPPLY_REVISION, EXPECTED_NODES)",
        "supply readiness day",
    ),
    (
        "                && supply.totalReceived() > 0L\n                && receivedResourceCount(economy) == EXPECTED_RESOURCES;",
        "                && supply.totalReceived() > 0L\n                && receivedResourceCount(economy) == EXPECTED_RESOURCES\n                && ErdenExteriorWorkforceManager.isReady(level, currentDay);",
        "supply readiness workforce",
    ),
    (
        "                \"Prepared Erden kingdom supply nodes={} producers={} wharves={} opening_convoys={} fixed_daily_imports=false shipment_escrow=true\",",
        "                \"Prepared Erden kingdom supply nodes={} producers={} wharves={} opening_convoys={} fixed_daily_imports=false shipment_escrow=true workforce_linked=true\",",
        "prepared supply marker",
    ),
    (
        "            long produced = produceDay(supply, day);",
        "            long produced = produceDay(level, supply, day);",
        "produce with level",
    ),
    (
        "    private static long produceDay(\n            ErdenKingdomSupplySavedData supply,\n            long day) {\n        long produced = 0L;\n        int percentage = productionPercentage(day);",
        "    private static long produceDay(\n            ServerLevel level,\n            ErdenKingdomSupplySavedData supply,\n            long day) {\n        long produced = 0L;\n        int seasonalPercentage = productionPercentage(day);",
        "staffed production signature",
    ),
    (
        "            ErdenKingdomSupplySavedData.NodeState node = snapshot;\n            for (OutputRate output : outputsFor(node.role())) {\n                long amount = Math.max(1L, output.dailyAmount * percentage / 100L);\n                node = node.produce(output.resource, amount, day);\n                produced += amount;\n            }",
        "            ErdenKingdomSupplySavedData.NodeState node = snapshot;\n            int laborPercentage = ErdenExteriorWorkforceManager.productionPercent(\n                    level, node.id(), day);\n            for (OutputRate output : outputsFor(node.role())) {\n                long amount = output.dailyAmount * seasonalPercentage * laborPercentage / 10_000L;\n                if (amount <= 0L) continue;\n                node = node.produce(output.resource, amount, day);\n                produced += amount;\n            }",
        "staffed production formula",
    ),
    (
        "                        \"Processed Erden kingdom supply day={} produced={} dispatched={} in_transit={} blocked={} fixed_daily_imports=false\",",
        "                        \"Processed Erden kingdom supply day={} produced={} dispatched={} in_transit={} blocked={} fixed_daily_imports=false staffed_production=true\",",
        "processed supply marker",
    ),
    (
        "            if (routeBlocked(node, day)) {",
        "            if (routeBlocked(level, node, day)) {",
        "staffed route call",
    ),
    (
        "    private static boolean routeBlocked(\n            ErdenKingdomSupplySavedData.NodeState node,\n            long day) {\n        long seed = (long) node.id().hashCode() * 31L + day * 17L;\n        return Math.floorMod(seed, 19L) == 0L;\n    }",
        "    private static boolean routeBlocked(\n            ServerLevel level,\n            ErdenKingdomSupplySavedData.NodeState node,\n            long day) {\n        if (!ErdenExteriorWorkforceManager.nodeOperational(level, node.id(), day)) return true;\n        if (node.role().equals(\"paper_mill\")) {\n            ErdenKingdomSupplyCatalog.SupplyNode wharf = nearestWharfNode(node.x(), node.z());\n            if (wharf == null\n                    || !ErdenExteriorWorkforceManager.nodeOperational(level, wharf.id, day)) return true;\n        }\n        long seed = (long) node.id().hashCode() * 31L + day * 17L;\n        return Math.floorMod(seed, 19L) == 0L;\n    }",
        "staffed route rule",
    ),
    (
        "    private static Point nearestWharf(int x, int z) {\n        return NODES.stream()\n                .filter(node -> node.role.equals(\"river_wharf\"))\n                .map(node -> new Point(node.x, node.z))\n                .min(Comparator.comparingLong(point -> manhattan(x, z, point.x, point.z)))\n                .orElseGet(() -> nearestGate(x, z));\n    }",
        "    private static Point nearestWharf(int x, int z) {\n        ErdenKingdomSupplyCatalog.SupplyNode wharf = nearestWharfNode(x, z);\n        return wharf == null ? nearestGate(x, z) : new Point(wharf.x, wharf.z);\n    }\n\n    private static ErdenKingdomSupplyCatalog.SupplyNode nearestWharfNode(int x, int z) {\n        return NODES.stream()\n                .filter(node -> node.role.equals(\"river_wharf\"))\n                .min(Comparator.comparingLong(node -> manhattan(x, z, node.x, node.z)))\n                .orElse(null);\n    }",
        "nearest staffed wharf",
    ),
    (
        "                \"LK_ERDEN_KINGDOM_SUPPLY_PASS revision={} nodes={} producers={} wharves={} resources={} opening_convoys={} produced={} dispatched={} received={} blocked={} warehouses_supplied={} active_shipments={} fixed_daily_imports=false shipment_escrow=true local_reserves=true route_modes=wagon,barge\",",
        "                \"LK_ERDEN_KINGDOM_SUPPLY_PASS revision={} nodes={} producers={} wharves={} resources={} opening_convoys={} produced={} dispatched={} received={} blocked={} warehouses_supplied={} active_shipments={} fixed_daily_imports=false shipment_escrow=true local_reserves=true route_modes=wagon,barge workforce_linked=true staffed_production=true wharf_labor=true\",",
        "supply pass workforce marker",
    ),
]
for old, new, label in supply_replacements:
    supply = replace_once_or_verify(supply, old, new, label)
supply_path.write_text(supply, encoding="utf-8")


# Register workforce ticks, interactions and permanent deaths with the mod event flow.
main_path = Path(
    "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java"
)
main = main_path.read_text(encoding="utf-8")
main_replacements = [
    (
        "import kr.moonseungjun.livingkingdoms.world.ErdenDiagnosticDebrisSettler;\nimport kr.moonseungjun.livingkingdoms.world.ErdenLivingEconomyManager;",
        "import kr.moonseungjun.livingkingdoms.world.ErdenDiagnosticDebrisSettler;\nimport kr.moonseungjun.livingkingdoms.world.ErdenExteriorWorkforceManager;\nimport kr.moonseungjun.livingkingdoms.world.ErdenLivingEconomyManager;",
        "workforce import",
    ),
    (
        "        ErdenKingdomExteriorBuilder.onServerTick(event);\n        ErdenUrbanInteriorBuilder.onServerTick(event);",
        "        ErdenKingdomExteriorBuilder.onServerTick(event);\n        ErdenExteriorWorkforceManager.onServerTick(event);\n        ErdenUrbanInteriorBuilder.onServerTick(event);",
        "workforce tick",
    ),
    (
        "            ErdenPopulationManager.markDeadIfResident(level, villager);",
        "            ErdenPopulationManager.markDeadIfResident(level, villager);\n            ErdenExteriorWorkforceManager.markDeadIfWorker(level, villager);",
        "workforce death",
    ),
    (
        "        ErdenPopulationManager.handleInteraction(event);\n        ErdenLivingEconomyManager.handleInteraction(event);",
        "        ErdenPopulationManager.handleInteraction(event);\n        ErdenExteriorWorkforceManager.handleInteraction(event);\n        ErdenLivingEconomyManager.handleInteraction(event);",
        "workforce interaction",
    ),
]
for old, new, label in main_replacements:
    main = replace_once_or_verify(main, old, new, label)
main_path.write_text(main, encoding="utf-8")
