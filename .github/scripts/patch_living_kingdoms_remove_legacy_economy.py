from pathlib import Path

manager_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoritativeEconomyManager.java")
manager = manager_path.read_text(encoding="utf-8")

old_constants = (
    "    public static final int ECONOMY_REVISION = ErdenPhysicalEconomyManager.ECONOMY_REVISION;\n"
    "    public static final int EXPECTED_SITES = ErdenPhysicalEconomyManager.EXPECTED_SITES;\n"
    "    public static final int EXPECTED_WAREHOUSES = ErdenPhysicalEconomyManager.EXPECTED_WAREHOUSES;\n"
    "    public static final int EXPECTED_WALLETS = ErdenPhysicalEconomyManager.EXPECTED_WALLETS;"
)
new_constants = (
    "    public static final int ECONOMY_REVISION = 1;\n"
    "    public static final int EXPECTED_SITES = 156;\n"
    "    public static final int EXPECTED_WAREHOUSES = 15;\n"
    "    public static final int EXPECTED_WALLETS = 77;"
)
if manager.count(old_constants) != 1:
    raise SystemExit("authoritative constants pattern missing")
manager = manager.replace(old_constants, new_constants)

anchor = "    private static void reset(MinecraftServer server) {"
ci_method = """    public static List<ExternalUrbanFabricBuilder.UrbanEntrance> ciEntrances() {
        List<ExternalUrbanFabricBuilder.UrbanEntrance> result = new ArrayList<>();
        for (String role : List.of("warehouse", "bakery", "shop")) {
            ExternalUrbanFabricBuilder.entrances().stream()
                    .filter(entrance -> entrance.role().equals(role))
                    .sorted(Comparator.comparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                            .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                    .findFirst()
                    .ifPresent(result::add);
        }
        return List.copyOf(result);
    }

"""
if manager.count(anchor) != 1:
    raise SystemExit("reset anchor missing")
manager = manager.replace(anchor, ci_method + anchor)
manager_path.write_text(manager, encoding="utf-8")

retainer_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenPopulationCiChunkRetainer.java")
retainer = retainer_path.read_text(encoding="utf-8")
old_ref = "                : ErdenPhysicalEconomyManager.ciEntrances()) {"
new_ref = "                : ErdenAuthoritativeEconomyManager.ciEntrances()) {"
if retainer.count(old_ref) != 1:
    raise SystemExit("legacy ci entrance reference missing")
retainer = retainer.replace(old_ref, new_ref)
old_load = """                level.setChunkForced(chunkX, chunkZ, true);
                long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
                if (RETAINED_CHUNKS.add(key) || !level.hasChunk(chunkX, chunkZ)) {
                    level.getChunk(chunkX, chunkZ);
                }
"""
new_load = """                long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
                if (RETAINED_CHUNKS.add(key)) {
                    level.setChunkForced(chunkX, chunkZ, true);
                }
"""
if retainer.count(old_load) != 1:
    raise SystemExit("synchronous ci chunk load pattern missing")
retainer = retainer.replace(old_load, new_load)
retainer_path.write_text(retainer, encoding="utf-8")
