from pathlib import Path

p = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanFullInteriorRouteCatalog.java')
s = p.read_text(encoding='utf-8')


def once(old: str, new: str, label: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    s = s.replace(old, new, 1)

once('public static final int CATALOG_REVISION = 1;',
     'public static final int CATALOG_REVISION = 2;', 'revision')
once(
'''            List<RegionRoute> regionRoutes = new ArrayList<>();
            Set<Long> floorCells = new HashSet<>();
            for (ErdenUrbanFullInteriorPlanCatalog.PlannedRegion region : target.regions()) {
''',
'''            List<RegionRoute> regionRoutes = new ArrayList<>();
            Set<Long> floorCells = new HashSet<>();
            // Room branches are planned sequentially. Once one branch has claimed an ascending
            // stair cell, later branches may share it only in the same direction. This turns the
            // independent shortest paths into one coherent staircase network instead of allowing
            // two stair blocks at the same position to demand opposite facings.
            Map<Long, RiseDirection> committedAscents = new HashMap<>();
            for (ErdenUrbanFullInteriorPlanCatalog.PlannedRegion region : target.regions()) {
''', 'committed ascents declaration')
once(
'''                RouteSearch search = search(
                        snapshot, blocks, lower.feetY(), lowerCells,
                        target.feetY(), regionCells);
''',
'''                RouteSearch search = search(
                        snapshot, blocks, lower.feetY(), lowerCells,
                        target.feetY(), regionCells, committedAscents);
''', 'search call')
once(
'''                floorCells.addAll(regionCells);
                regionRoutes.add(new RegionRoute(
''',
'''                commitAscents(search.path(), committedAscents, snapshot.fragmentKey());
                floorCells.addAll(regionCells);
                regionRoutes.add(new RegionRoute(
''', 'commit path ascents')
once(
'''            int targetY,
            Set<Long> targetCells) {
''',
'''            int targetY,
            Set<Long> targetCells,
            Map<Long, RiseDirection> committedAscents) {
''', 'search signature')
once(
'''                    if (y < lowerY || y > targetY) continue;
                    if (!routeBodyClear(snapshot, blocks, x, y, z)) continue;
''',
'''                    if (y < lowerY || y > targetY) continue;
                    if (dy == 1) {
                        long riseKey = nodeKey(current.x(), current.y(), current.z());
                        RiseDirection committed = committedAscents.get(riseKey);
                        if (committed != null
                                && (committed.dx() != direction[0] || committed.dz() != direction[1])) {
                            continue;
                        }
                    }
                    if (!routeBodyClear(snapshot, blocks, x, y, z)) continue;
''', 'rise compatibility')
anchor = '''    private static boolean routeBodyClear(
'''
helper = '''    private static void commitAscents(
            List<Node> path,
            Map<Long, RiseDirection> committedAscents,
            String fragmentKey) {
        for (int index = 1; index < path.size(); index++) {
            Node previous = path.get(index - 1);
            Node current = path.get(index);
            if (current.y() - previous.y() != 1) continue;
            int dx = current.x() - previous.x();
            int dz = current.z() - previous.z();
            RiseDirection direction = new RiseDirection(dx, dz);
            long key = nodeKey(previous.x(), previous.y(), previous.z());
            RiseDirection old = committedAscents.putIfAbsent(key, direction);
            if (old != null && !old.equals(direction)) {
                throw new IllegalStateException(
                        "Conflicting source-route ascent survived planning fragment="
                                + fragmentKey + " at=" + previous + " old=" + old
                                + " new=" + direction);
            }
        }
    }

'''
if anchor not in s:
    raise SystemExit('routeBodyClear anchor missing')
s = s.replace(anchor, helper + anchor, 1)
record_anchor = '''    private record RouteSearch(List<Node> path, int exploredNodes) {
    }
'''
record_new = '''    private record RiseDirection(int dx, int dz) {
    }

    private record RouteSearch(List<Node> path, int exploredNodes) {
    }
'''
once(record_anchor, record_new, 'rise direction record')
p.write_text(s, encoding='utf-8')
print('Patched full-interior route planner with coherent shared stair ascents')
