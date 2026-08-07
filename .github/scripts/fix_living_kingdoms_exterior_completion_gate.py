from pathlib import Path

root = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world')
reaper_path = root / 'ErdenExteriorTicketReaper.java'
builder_path = root / 'ErdenKingdomExteriorBuilder.java'

reaper = reaper_path.read_text(encoding='utf-8')
old = '''            boolean residenceReady = !ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)
                    || residences.chunkBuilt(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (RELEASED.contains(packed)
                    || !exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)
                    || !residenceReady
                    || (sampleAnchors.contains(packed) && !releaseSample)) continue;
'''
new = '''            boolean residenceReady = !ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)
                    || residences.chunkBuilt(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            boolean storageReady = storageReadyForChunk(packed);
            if (RELEASED.contains(packed)
                    || !exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)
                    || !residenceReady
                    || !storageReady
                    || (sampleAnchors.contains(packed) && !releaseSample)) continue;
'''
if old not in reaper:
    raise SystemExit('reaper release gate pattern missing')
reaper = reaper.replace(old, new, 1)

anchor = '''    private static boolean sampleResidentsReady(
'''
insert = '''    public static boolean storageValidationComplete() {
        return VALIDATED_STORAGE_NODES.size() == ErdenKingdomSupplyCatalog.nodes().size();
    }

    private static boolean storageReadyForChunk(long packed) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (ErdenKingdomExteriorBuilder.storageAnchorChunk(node) == packed
                    && !VALIDATED_STORAGE_NODES.contains(node.id)) return false;
        }
        return true;
    }

''' + anchor
if anchor not in reaper:
    raise SystemExit('reaper helper insertion point missing')
reaper = reaper.replace(anchor, insert, 1)
reaper_path.write_text(reaper, encoding='utf-8')

builder = builder_path.read_text(encoding='utf-8')
old = '''        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            BlockPos storage = storagePosition(level, node);
            if (!level.hasChunkAt(storage)
                    || !level.getBlockState(storage).is(Blocks.BARREL)) return;
        }
        ciPassed = true;
'''
new = '''        if (!ErdenExteriorTicketReaper.storageValidationComplete()) return;
        ciPassed = true;
'''
if old not in builder:
    raise SystemExit('builder simultaneous storage check pattern missing')
builder = builder.replace(old, new, 1)
builder_path.write_text(builder, encoding='utf-8')
