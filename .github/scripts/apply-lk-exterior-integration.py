from pathlib import Path

# Share the authoritative supply-node catalog with the economy runtime.
supply_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomSupplyManager.java")
supply = supply_path.read_text(encoding="utf-8")
start = supply.index("    private static final List<NodeTemplate> NODES = List.of(")
end = supply.index("    private static final List<OutputRate> OUTPUTS", start)
supply = supply[:start] + "    private static final List<ErdenKingdomSupplyCatalog.SupplyNode> NODES =\n            ErdenKingdomSupplyCatalog.nodes();\n\n" + supply[end:]
supply = supply.replace("\n    private record NodeTemplate(String id, int x, int z, String role) {\n    }\n", "\n")
supply_path.write_text(supply, encoding="utf-8")

# Expose the existing licensed external building templates for exterior production anchors.
building_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ExternalDistrictBuildingBuilder.java")
building = building_path.read_text(encoding="utf-8")
anchor = """    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {\n        for (Placement placement : ALL_PLACEMENTS) {\n            BuildingTemplate template = template(placement.resource);\n            if (!placement.intersects(chunk, template)) continue;\n            pasteClipped(plan, level, chunk, template, placement);\n        }\n    }\n\n"""
addition = anchor + """    /** Places one attributed external building as the architectural anchor of an exterior supply site. */\n    public static void addSupplyBuildingChunk(\n            IncrementalWorldEditPlan plan,\n            ServerLevel level,\n            ChunkPos chunk,\n            int centerX,\n            int centerZ,\n            String nodeId,\n            String role,\n            String buildingStyle,\n            int facingQuarterTurns) {\n        String resource = switch (buildingStyle) {\n            case \"manor\" -> MANOR;\n            case \"castle_house\" -> CASTLE_HOUSE;\n            case \"church\" -> CHURCH;\n            default -> HOUSE;\n        };\n        Rotation rotation = switch (Math.floorMod(facingQuarterTurns, 4)) {\n            case 1 -> Rotation.CLOCKWISE_90;\n            case 2 -> Rotation.CLOCKWISE_180;\n            case 3 -> Rotation.COUNTERCLOCKWISE_90;\n            default -> Rotation.NONE;\n        };\n        Placement placement = new Placement(\n                resource, centerX, centerZ, rotation, \"supply_\" + role + \"_\" + nodeId);\n        BuildingTemplate template = template(resource);\n        if (placement.intersects(chunk, template)) {\n            pasteClipped(plan, level, chunk, template, placement);\n        }\n    }\n\n"""
assert building.count(anchor) == 1, "external building insertion anchor changed"
building = building.replace(anchor, addition)
building_path.write_text(building, encoding="utf-8")

# Register streamed exterior construction on normal server and chunk events.
main_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java")
main = main_path.read_text(encoding="utf-8")
old = "import kr.moonseungjun.livingkingdoms.world.ErdenCapitalStreamingBuilder;\n"
new = old + "import kr.moonseungjun.livingkingdoms.world.ErdenKingdomExteriorBuilder;\n"
assert main.count(old) == 1, "main import anchor changed"
main = main.replace(old, new)
old = """        ErdenCapitalStreamingBuilder.onServerTick(event);\n        ErdenUrbanInteriorBuilder.onServerTick(event);\n"""
new = """        ErdenCapitalStreamingBuilder.onServerTick(event);\n        ErdenKingdomExteriorBuilder.onServerTick(event);\n        ErdenUrbanInteriorBuilder.onServerTick(event);\n"""
assert main.count(old) == 1, "server tick anchor changed"
main = main.replace(old, new)
old = """    private void onChunkLoad(ChunkEvent.Load event) {\n        ErdenCapitalStreamingBuilder.onChunkLoad(event);\n    }\n"""
new = """    private void onChunkLoad(ChunkEvent.Load event) {\n        ErdenCapitalStreamingBuilder.onChunkLoad(event);\n        ErdenKingdomExteriorBuilder.onChunkLoad(event);\n    }\n"""
assert main.count(old) == 1, "chunk load anchor changed"
main = main.replace(old, new)
main_path.write_text(main, encoding="utf-8")
