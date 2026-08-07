from pathlib import Path

saved_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceSavedData.java")
saved = saved_path.read_text(encoding="utf-8")
needle = """    public boolean householdBuilt(String householdId, int revision) {
        return residenceRevision == revision && builtHouseholds.contains(householdId);
    }

"""
addition = needle + """    public boolean chunkBuilt(int chunkX, int chunkZ, int revision) {
        return residenceRevision == revision && builtChunks.contains(pack(chunkX, chunkZ));
    }

"""
if "public boolean chunkBuilt(int chunkX, int chunkZ, int revision)" not in saved:
    if saved.count(needle) != 1:
        raise SystemExit("residence chunkBuilt insertion point missing")
    saved = saved.replace(needle, addition, 1)
saved_path.write_text(saved, encoding="utf-8")

reaper_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorTicketReaper.java")
reaper = reaper_path.read_text(encoding="utf-8")
old = """        ErdenKingdomExteriorSavedData exterior = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        observePhysicalStorage(level);
"""
new = """        ErdenKingdomExteriorSavedData exterior = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        observePhysicalStorage(level);
"""
if old in reaper:
    reaper = reaper.replace(old, new, 1)
elif new not in reaper:
    raise SystemExit("ticket reaper residence data insertion point missing")
old = """            if (RELEASED.contains(packed)
                    || !exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)
                    || (sampleAnchors.contains(packed) && !releaseSample)) continue;
"""
new = """            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            boolean residenceReady = !ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)
                    || residences.chunkBuilt(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            if (RELEASED.contains(packed)
                    || !exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)
                    || !residenceReady
                    || (sampleAnchors.contains(packed) && !releaseSample)) continue;
"""
if old in reaper:
    reaper = reaper.replace(old, new, 1)
elif new not in reaper:
    raise SystemExit("ticket release residence gate insertion point missing")
reaper_path.write_text(reaper, encoding="utf-8")
