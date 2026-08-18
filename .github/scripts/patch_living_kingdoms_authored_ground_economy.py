from pathlib import Path

# Economy containers must use source-authored plan coordinates, never the removed 7x9 synthetic room.
p = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoritativeEconomyManager.java')
s = p.read_text(encoding='utf-8')
s = s.replace('import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;\n', '')
s = s.replace('import net.minecraft.world.level.block.DoorBlock;\n', '')
old = '''    private static BlockPos primaryContainerPos(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData.SiteState site) {\n        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(site.x(), site.z());\n        if (entrance == null) return null;\n        int doorY = findLowestDoorY(level, site.x(), site.z());\n        if (doorY == Integer.MIN_VALUE) return null;\n        Room room = room(entrance, doorY - 1);\n        Point point = switch (site.role()) {\n            case "shop" -> room.point(-3, 7);\n            case "bakery" -> room.point(-3, 5);\n            case "inn" -> room.point(-3, 9);\n            case "stable" -> room.point(3, 5);\n            case "guard_post" -> room.point(-3, 3);\n            case "warehouse" -> room.point(-3, 3);\n            default -> null;\n        };\n        return point == null ? null : new BlockPos(point.x, room.floorY + 1, point.z);\n    }\n'''
new = '''    private static BlockPos primaryContainerPos(\n            ServerLevel level,\n            ErdenPhysicalEconomySavedData.SiteState site) {\n        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(site.x(), site.z());\n        if (entrance == null) return null;\n        BlockPos planned = ErdenUrbanAuthoredGroundPlanCatalog.primaryContainer(entrance);\n        if (planned == null) {\n            throw new IllegalStateException(\n                    "Economy site has no authored primary container role=" + site.role()\n                            + " entrance=" + site.x() + "," + site.z());\n        }\n        return level.hasChunk(planned.getX() >> 4, planned.getZ() >> 4) ? planned : null;\n    }\n'''
if old not in s:
    if new not in s:
        raise SystemExit('primaryContainerPos synthetic block not found')
else:
    s = s.replace(old, new, 1)

# Remove synthetic room helpers that existed only for the old primary container calculation.
start = s.find('    private static int findLowestDoorY(ServerLevel level, int x, int z) {')
end = s.find('    private static String compactStocks(', start)
if start >= 0 and end > start:
    s = s[:start] + s[end:]

old_records = '''    private record Point(int x, int z) {\n    }\n\n    private record Room(\n            int floorY,\n            int originX,\n            int originZ,\n            int inwardX,\n            int inwardZ,\n            int rightX,\n            int rightZ) {\n        Point point(int lateral, int depth) {\n            return new Point(\n                    originX + inwardX * depth + rightX * lateral,\n                    originZ + inwardZ * depth + rightZ * lateral);\n        }\n    }\n\n'''
s = s.replace(old_records, '')
p.write_text(s, encoding='utf-8')

# Bootstrap the source-only functional plan during server startup so topology/planning drift fails early.
p = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java')
s = p.read_text(encoding='utf-8')
imp = 'import kr.moonseungjun.livingkingdoms.world.ErdenUrbanAuthoredInteriorSurvey;\n'
new_imp = 'import kr.moonseungjun.livingkingdoms.world.ErdenUrbanAuthoredGroundPlanCatalog;\n'
if new_imp not in s:
    if imp not in s:
        raise SystemExit('LivingKingdoms import anchor missing')
    s = s.replace(imp, new_imp + imp, 1)
anchor = '        ErdenUrbanGroundVoidOpportunityCatalog.bootstrap();\n'
line = '        ErdenUrbanAuthoredGroundPlanCatalog.bootstrap();\n'
if line not in s:
    if anchor not in s:
        raise SystemExit('LivingKingdoms bootstrap anchor missing')
    s = s.replace(anchor, anchor + line, 1)
p.write_text(s, encoding='utf-8')
