from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorTicketReaper.java")
text = path.read_text(encoding="utf-8")

old = '''        AABB bounds = new AABB(
                sampleNode.x - 96, level.getMinY(), sampleNode.z - 96,
                sampleNode.x + 96, level.getMaxY(), sampleNode.z + 96);
        int loaded = level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();'''
new = '''        BlockPos physicalHome = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                sample.id(), 0);
        if (physicalHome.equals(BlockPos.ZERO)
                || !level.hasChunk(physicalHome.getX() >> 4, physicalHome.getZ() >> 4)) return false;
        AABB bounds = new AABB(
                physicalHome.getX() - 16, level.getMinY(), physicalHome.getZ() - 16,
                physicalHome.getX() + 16, level.getMaxY(), physicalHome.getZ() + 16);
        int loaded = level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();'''
if old not in text:
    raise SystemExit("sampleResidentsReady v1 bounds anchor missing")
text = text.replace(old, new, 1)

old = '''        AABB bounds = new AABB(
                sampleNode.x - 96, level.getMinY(), sampleNode.z - 96,
                sampleNode.x + 96, level.getMaxY(), sampleNode.z + 96);
        return level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();'''
new = '''        BlockPos physicalHome = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                sample.id(), 0);
        if (physicalHome.equals(BlockPos.ZERO)
                || !level.hasChunk(physicalHome.getX() >> 4, physicalHome.getZ() >> 4)) return 0;
        AABB bounds = new AABB(
                physicalHome.getX() - 16, level.getMinY(), physicalHome.getZ() - 16,
                physicalHome.getX() + 16, level.getMaxY(), physicalHome.getZ() + 16);
        return level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();'''
if old not in text:
    raise SystemExit("sampleResidentCount v1 bounds anchor missing")
text = text.replace(old, new, 1)

if "sampleNode.x - 96" in text or "sampleNode.z - 96" in text:
    raise SystemExit("legacy ±96 sample bounds remain")

path.write_text(text, encoding="utf-8")
