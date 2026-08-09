from pathlib import Path

ROOT = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world")

# Workforce: materialise residents only from a loaded physical home and bound navigation work.
path = ROOT / "ErdenExteriorWorkforceManager.java"
text = path.read_text(encoding="utf-8")
text = text.replace(
    "    private static final int ROUTINE_INTERVAL = 60;",
    "    private static final int ROUTINE_INTERVAL = 60;\n    private static final int NAVIGATION_BUDGET = 6;\n    private static final int ROUTE_LOAD_SAMPLE = 8;",
    1,
)
old = '''            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(household.nodeId());
            if (node == null
                    || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)
                    || !ErdenKingdomExteriorBuilder.residenceBuilt(level, household.id())
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {'''
new = '''            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(household.nodeId());
            BlockPos physicalHome = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                    household.id(), 0);
            if (node == null
                    || physicalHome.equals(BlockPos.ZERO)
                    || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)
                    || !ErdenKingdomExteriorBuilder.residenceBuilt(level, household.id())
                    || !level.hasChunk(physicalHome.getX() >> 4, physicalHome.getZ() >> 4)) continue;
            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {'''
if old not in text:
    raise SystemExit("workforce physical home spawn gate anchor missing")
text = text.replace(old, new, 1)
old = '''        int x = spawn.getX();
        int z = spawn.getZ();
        int standingY = safeStandingY(level, x, spawn.getY(), z);'''
new = '''        int x = spawn.getX();
        int z = spawn.getZ();
        if (spawn.equals(BlockPos.ZERO) || !level.hasChunk(x >> 4, z >> 4)) return false;
        int standingY = safeStandingY(level, x, spawn.getY(), z);'''
if old not in text:
    raise SystemExit("workforce spawn defensive loaded check anchor missing")
text = text.replace(old, new, 1)
old = '''        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> references.containsKey(candidate.getName().getString()))) {'''
new = '''        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        int navigationBudget = NAVIGATION_BUDGET;
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, exteriorBounds(level),
                candidate -> references.containsKey(candidate.getName().getString()))) {'''
if old not in text:
    raise SystemExit("workforce navigation budget anchor missing")
text = text.replace(old, new, 1)
old = '''            villager.setPersistenceRequired();
            if (villager.distanceToSqr(target.x() + 0.5D, target.y(), target.z() + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        target.x() + 0.5D, target.y(), target.z() + 0.5D, 0.58D);
            }'''
new = '''            villager.setPersistenceRequired();
            BlockPos targetPos = new BlockPos(target.x(), target.y(), target.z());
            if (navigationBudget > 0
                    && villager.distanceToSqr(target.x() + 0.5D, target.y(), target.z() + 0.5D) > 4.0D
                    && routeLoaded(level, villager.blockPosition(), targetPos)) {
                navigationBudget--;
                villager.getNavigation().moveTo(
                        target.x() + 0.5D, target.y(), target.z() + 0.5D, 0.58D);
            }'''
if old not in text:
    raise SystemExit("workforce navigation call anchor missing")
text = text.replace(old, new, 1)
anchor = '''    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
'''
helper = '''    private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int distance = Math.max(Math.abs(dx), Math.abs(dz));
        int steps = Math.max(1, (distance + ROUTE_LOAD_SAMPLE - 1) / ROUTE_LOAD_SAMPLE);
        for (int step = 0; step <= steps; step++) {
            int x = from.getX() + Math.floorDiv(dx * step, steps);
            int z = from.getZ() + Math.floorDiv(dz * step, steps);
            if (!level.hasChunk(x >> 4, z >> 4)) return false;
        }
        return true;
    }

''' + anchor
if "private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to)" not in text:
    if anchor not in text:
        raise SystemExit("workforce routeLoaded insertion anchor missing")
    text = text.replace(anchor, helper, 1)
path.write_text(text, encoding="utf-8")

# Lifecycle: same physical-home spawn gate, bounded pathfinding, and no duplicate routine control for founding dependents.
path = ROOT / "ErdenExteriorLifecycleManager.java"
text = path.read_text(encoding="utf-8")
text = text.replace(
    "    private static final int ROUTINE_INTERVAL = 80;",
    "    private static final int ROUTINE_INTERVAL = 80;\n    private static final int NAVIGATION_BUDGET = 4;\n    private static final int ROUTE_LOAD_SAMPLE = 8;",
    1,
)
old = '''            ErdenExteriorWorkforceSavedData.Household household = households.get(person.householdId());
            if (household == null
                    || !ErdenKingdomExteriorBuilder.residenceBuilt(level, household.id())
                    || !level.hasChunk(household.homeX() >> 4, household.homeZ() >> 4)) continue;
            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(person.nodeId());'''
new = '''            ErdenExteriorWorkforceSavedData.Household household = households.get(person.householdId());
            BlockPos physicalHome = household == null
                    ? BlockPos.ZERO
                    : ErdenExteriorResidenceBuilder.residentSpawnPosition(household.id(), 0);
            if (household == null
                    || physicalHome.equals(BlockPos.ZERO)
                    || !ErdenKingdomExteriorBuilder.residenceBuilt(level, household.id())
                    || !level.hasChunk(physicalHome.getX() >> 4, physicalHome.getZ() >> 4)) continue;
            ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(person.nodeId());'''
if old not in text:
    raise SystemExit("lifecycle physical home spawn gate anchor missing")
text = text.replace(old, new, 1)
old = '''        int x = spawn.getX();
        int z = spawn.getZ();
        villager.setPos(
                x + 0.5D,
                safeStandingY(level, x, spawn.getY(), z),'''
new = '''        int x = spawn.getX();
        int z = spawn.getZ();
        if (spawn.equals(BlockPos.ZERO) || !level.hasChunk(x >> 4, z >> 4)) return false;
        villager.setPos(
                x + 0.5D,
                safeStandingY(level, x, spawn.getY(), z),'''
if old not in text:
    raise SystemExit("lifecycle spawn defensive loaded check anchor missing")
text = text.replace(old, new, 1)
old = '''        Map<String, ErdenExteriorLifecycleSavedData.Person> people = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!person.foundingWorker() || person.retiredOn(day)) {
                people.put(person.name(), person);
            }
        }'''
new = '''        Map<String, ErdenExteriorLifecycleSavedData.Person> people = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!person.founder() || person.retiredOn(day)) {
                people.put(person.name(), person);
            }
        }'''
if old not in text:
    raise SystemExit("lifecycle duplicate founder routine filter anchor missing")
text = text.replace(old, new, 1)
old = '''        Map<String, ErdenExteriorWorkforceSavedData.Household> households = householdMap(workforce);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (Villager villager : level.getEntitiesOfClass('''
new = '''        Map<String, ErdenExteriorWorkforceSavedData.Household> households = householdMap(workforce);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        int navigationBudget = NAVIGATION_BUDGET;
        for (Villager villager : level.getEntitiesOfClass('''
if old not in text:
    raise SystemExit("lifecycle navigation budget anchor missing")
text = text.replace(old, new, 1)
old = '''            int y = safeStandingY(level, x, destination.getY(), z);
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(x + 0.5D, y, z + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(x + 0.5D, y, z + 0.5D, 0.56D);
            }'''
new = '''            int y = safeStandingY(level, x, destination.getY(), z);
            villager.setPersistenceRequired();
            BlockPos targetPos = new BlockPos(x, y, z);
            if (navigationBudget > 0
                    && villager.distanceToSqr(x + 0.5D, y, z + 0.5D) > 4.0D
                    && routeLoaded(level, villager.blockPosition(), targetPos)) {
                navigationBudget--;
                villager.getNavigation().moveTo(x + 0.5D, y, z + 0.5D, 0.56D);
            }'''
if old not in text:
    raise SystemExit("lifecycle navigation call anchor missing")
text = text.replace(old, new, 1)
anchor = '''    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
'''
helper = '''    private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int distance = Math.max(Math.abs(dx), Math.abs(dz));
        int steps = Math.max(1, (distance + ROUTE_LOAD_SAMPLE - 1) / ROUTE_LOAD_SAMPLE);
        for (int step = 0; step <= steps; step++) {
            int x = from.getX() + Math.floorDiv(dx * step, steps);
            int z = from.getZ() + Math.floorDiv(dz * step, steps);
            if (!level.hasChunk(x >> 4, z >> 4)) return false;
        }
        return true;
    }

''' + anchor
if "private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to)" not in text:
    if anchor not in text:
        raise SystemExit("lifecycle routeLoaded insertion anchor missing")
    text = text.replace(anchor, helper, 1)
path.write_text(text, encoding="utf-8")
