from pathlib import Path

# Producer barrels only: river wharves do not own production stock.
inv_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorInventoryManager.java")
inv = inv_path.read_text(encoding="utf-8")
old = """        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n            if (!containers.isMaterialized(node.id)\n"""
new = """        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n            if (!node.producer()) continue;\n            if (!containers.isMaterialized(node.id)\n"""
assert inv.count(old) == 1, "capture loop anchor changed"
inv = inv.replace(old, new)
old = """        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n            if (!ErdenKingdomExteriorBuilder.anchorBuilt(level, node)) continue;\n"""
new = """        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n            if (!node.producer()) continue;\n            if (!ErdenKingdomExteriorBuilder.anchorBuilt(level, node)) continue;\n"""
assert inv.count(old) == 1, "materialize loop anchor changed"
inv = inv.replace(old, new)
inv_path.write_text(inv, encoding="utf-8")

# Preserve working stock at the producer instead of dispatching every last unit.
supply_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomSupplyManager.java")
supply = supply_path.read_text(encoding="utf-8")
old = """            for (ErdenKingdomSupplySavedData.ResourceStock stock : List.copyOf(node.stocks())) {\n                if (stock.amount() <= 0L) continue;\n                int routeMetres = routeMetres(node, warehouse);\n                long departureTick = day * 24_000L + 2_000L;\n                long arrivalTick = departureTick + Math.max(\n                        MIN_TRAVEL_TICKS,\n                        (long) routeMetres * TICKS_PER_METRE);\n                ErdenKingdomSupplySavedData.ShipmentState shipment =\n                        new ErdenKingdomSupplySavedData.ShipmentState(\n                                supply.nextShipmentId(day),\n                                node.id(), warehouse.id(), stock.resource(), stock.amount(),\n                                departureTick, arrivalTick,\n                                \"in_transit\", transportMode(node.role()), routeMetres, false);\n                node = node.addStock(stock.resource(), -stock.amount());\n                supply.addShipment(shipment);\n                dispatched += stock.amount();\n            }\n"""
new = """            for (ErdenKingdomSupplySavedData.ResourceStock stock : List.copyOf(node.stocks())) {\n                long reserve = localReserve(node.role(), stock.resource());\n                long shippable = Math.max(0L, stock.amount() - reserve);\n                if (shippable <= 0L) continue;\n                int routeMetres = routeMetres(node, warehouse);\n                long departureTick = day * 24_000L + 2_000L;\n                long arrivalTick = departureTick + Math.max(\n                        MIN_TRAVEL_TICKS,\n                        (long) routeMetres * TICKS_PER_METRE);\n                ErdenKingdomSupplySavedData.ShipmentState shipment =\n                        new ErdenKingdomSupplySavedData.ShipmentState(\n                                supply.nextShipmentId(day),\n                                node.id(), warehouse.id(), stock.resource(), shippable,\n                                departureTick, arrivalTick,\n                                \"in_transit\", transportMode(node.role()), routeMetres, false);\n                node = node.addStock(stock.resource(), -shippable);\n                supply.addShipment(shipment);\n                dispatched += shippable;\n            }\n"""
assert supply.count(old) == 1, "daily dispatch anchor changed"
supply = supply.replace(old, new)
old = """    private static String transportMode(String role) {\n        return role.equals(\"paper_mill\") ? \"barge\" : \"wagon\";\n    }\n\n"""
new = """    private static long localReserve(String role, String resource) {\n        return switch (role + \"/\" + resource) {\n            case \"grain_estate/wheat\" -> 12L;\n            case \"ranch/leather\" -> 8L;\n            case \"ranch/hay\" -> 6L;\n            case \"colliery/coal\" -> 8L;\n            case \"iron_mine/iron\" -> 4L;\n            case \"paper_mill/paper\" -> 10L;\n            default -> 0L;\n        };\n    }\n\n    private static String transportMode(String role) {\n        return role.equals(\"paper_mill\") ? \"barge\" : \"wagon\";\n    }\n\n"""
assert supply.count(old) == 1, "local reserve insertion anchor changed"
supply = supply.replace(old, new)
old = "fixed_daily_imports=false shipment_escrow=true route_modes=wagon,barge"
new = "fixed_daily_imports=false shipment_escrow=true local_reserves=true route_modes=wagon,barge"
assert supply.count(old) == 1, "supply marker anchor changed"
supply = supply.replace(old, new)
supply_path.write_text(supply, encoding="utf-8")

# Order is authoritative: capture barrel edits, run supply/city economy, then write final stocks.
main_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java")
main = main_path.read_text(encoding="utf-8")
old = "import kr.moonseungjun.livingkingdoms.world.ErdenKingdomExteriorBuilder;\n"
new = old + "import kr.moonseungjun.livingkingdoms.world.ErdenKingdomExteriorInventoryManager;\n"
assert main.count(old) == 1, "inventory manager import anchor changed"
main = main.replace(old, new)
old = """        ErdenPopulationCiChunkRetainer.onServerTick(event);\n        ErdenPopulationManager.onServerTick(event);\n        ErdenAuthoritativeEconomyManager.onServerTick(event);\n        ErdenLivingEconomyManager.onServerTick(event);\n"""
new = """        ErdenPopulationCiChunkRetainer.onServerTick(event);\n        ErdenPopulationManager.onServerTick(event);\n        ErdenKingdomExteriorInventoryManager.captureBeforeSupply(event);\n        ErdenAuthoritativeEconomyManager.onServerTick(event);\n        ErdenKingdomExteriorInventoryManager.materializeAfterSupply(event);\n        ErdenLivingEconomyManager.onServerTick(event);\n"""
assert main.count(old) == 1, "server tick order anchor changed"
main = main.replace(old, new)
old = """    private void onWorkstationInteraction(PlayerInteractEvent.RightClickBlock event) {\n        ErdenAuthoritativeEconomyManager.handleInteraction(event);\n        FantasyWorldRules.handleWorkstation(event);\n    }\n"""
new = """    private void onWorkstationInteraction(PlayerInteractEvent.RightClickBlock event) {\n        ErdenKingdomExteriorInventoryManager.onInteraction(event);\n        ErdenAuthoritativeEconomyManager.handleInteraction(event);\n        FantasyWorldRules.handleWorkstation(event);\n    }\n"""
assert main.count(old) == 1, "workstation interaction anchor changed"
main = main.replace(old, new)
main_path.write_text(main, encoding="utf-8")
