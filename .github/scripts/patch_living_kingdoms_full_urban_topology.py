from pathlib import Path

ROOT = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def patch_placed() -> None:
    path = ROOT / "ErdenUrbanPlacedTopologyCatalog.java"
    text = path.read_text(encoding="utf-8")
    text = replace_once(text,
        "public static final int CATALOG_REVISION = 1;",
        "public static final int CATALOG_REVISION = 2;",
        "placed revision")
    text = replace_once(text,
        "    private static final int MAX_AUTHORED_UPPER_RISE = 12;\n",
        "",
        "remove placed height cap")
    text = replace_once(text,
'''            if (sourceBlock instanceof StairBlock
                    && interiorSide(snapshot, block.x(), block.z())
                    && block.y() >= Math.max(0, doorY == Integer.MAX_VALUE ? 0 : doorY - 1)
                    && block.y() <= (doorY == Integer.MAX_VALUE
                            ? snapshot.height() : doorY + MAX_AUTHORED_UPPER_RISE + 1)) {
                stairs++;
            }
''',
'''            // Stair counting is repeated after the retained entrance height is known.
            // Do not impose an arbitrary upper-floor cap here: tall licensed source buildings
            // keep their complete authored Y span in the cropped urban fragment.
''',
        "remove first-pass capped stair count")
    text = replace_once(text,
'''            if (block.state().getBlock() instanceof StairBlock
                    && interiorSide(snapshot, block.x(), block.z())
                    && block.y() >= Math.max(0, doorY - 1)
                    && block.y() <= doorY + MAX_AUTHORED_UPPER_RISE + 1) {
                stairs++;
            }
''',
'''            if (block.state().getBlock() instanceof StairBlock
                    && interiorSide(snapshot, block.x(), block.z())) {
                stairs++;
            }
''',
        "full-height stair count")
    text = replace_once(text,
'''                if (feetY > doorY + MAX_AUTHORED_UPPER_RISE) continue;
''',
'''                // The fragment retains the complete source height. Reachability itself is the
                // safety gate, so a valid authored staircase may continue to any retained floor.
''',
        "remove bfs height cap")
    text = replace_once(text,
'''        final int maximumAuthoredFeetY = doorY + MAX_AUTHORED_UPPER_RISE;
''',
'''        final int maximumAuthoredFeetY = snapshot.height() - 2;
''',
        "full-height upper band selection")
    path.write_text(text, encoding="utf-8")


def patch_opportunities() -> None:
    path = ROOT / "ErdenUrbanUpperRoomOpportunityCatalog.java"
    text = path.read_text(encoding="utf-8")
    text = replace_once(text,
        "public static final int CATALOG_REVISION = 3;",
        "public static final int CATALOG_REVISION = 4;",
        "opportunity revision")
    text = replace_once(text,
        "    private static final int MAX_UPPER_RISE = 16;",
        "    private static final int MAX_NEW_AUTHORED_UPPER_RISE = 16;",
        "separate synthetic upper cap")
    text = replace_once(text,
'''        int maximumY = Math.min(snapshot.height() - 2, groundY + MAX_UPPER_RISE);
''',
'''        // Existing source-authored floors may legitimately sit far above the entrance in tall
        // manor/castle fragments. Scan the complete retained source height. Only NEW authored
        // floors remain bounded to the conservative near-ground safety envelope below.
        int maximumY = snapshot.height() - 2;
''',
        "full-height existing floor scan")
    text = replace_once(text,
'''                    } else if (floor == null || floor.state().isAir()) {
                        newFloor.add(cellKey(x, z));
                        rejected.newFloorVoid++;
''',
'''                    } else if (feetY <= groundY + MAX_NEW_AUTHORED_UPPER_RISE
                            && (floor == null || floor.state().isAir())) {
                        newFloor.add(cellKey(x, z));
                        rejected.newFloorVoid++;
''',
        "keep synthetic floor cap")
    path.write_text(text, encoding="utf-8")


def patch_runtime_survey() -> None:
    path = ROOT / "ErdenUrbanAuthoredInteriorSurvey.java"
    text = path.read_text(encoding="utf-8")
    text = replace_once(text,
        "public static final int SURVEY_REVISION = 1;",
        "public static final int SURVEY_REVISION = 2;",
        "survey revision")
    text = replace_once(text,
'''    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 9;
    private static final int VERTICAL_SCAN = 16;
    private static final int PROCESS_BUDGET = 4;
''',
'''    // A full imported building can span several chunks and dozens of vertical blocks. Survey one
    // building per tick so exact-footprint verification remains loaded-only without a tick spike.
    private static final int PROCESS_BUDGET = 1;
''',
        "remove front-strip survey limits")
    text = replace_once(text,
'''        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return null;
        Vector inward = inward(entrance);
        Bounds bounds = bounds(entrance, inward);
        if (!chunksReady(level, bounds)) return null;
''',
'''        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return null;
        ErdenUrbanPlacedTopologyCatalog.PlacementProfile placement =
                ErdenUrbanPlacedTopologyCatalog.profile(entrance.x(), entrance.z());
        if (placement == null) return null;
        Vector inward = inward(entrance);
        Bounds bounds = new Bounds(
                placement.minX(), placement.maxX(), placement.minZ(), placement.maxZ());
        if (!chunksReady(level, bounds)) return null;
''',
        "exact runtime placement bounds")
    text = replace_once(text,
'''                if (!insideEnvelope(entrance, inward, x, z)) continue;
''',
'''                if (!insideBounds(bounds, x, z)) continue;
''',
        "bfs exact footprint")
    text = replace_once(text,
'''        int minimumY = Math.max(level.getMinY(), doorY - 1);
        int maximumY = Math.min(level.getMaxY() - 1, doorY + VERTICAL_SCAN);
''',
'''        int minimumY = Math.max(level.getMinY(), placement.baseY() - 1);
        int maximumY = Math.min(
                level.getMaxY() - 1, placement.baseY() + placement.height() - 1);
''',
        "runtime full source height")
    old_methods = '''    private static Bounds bounds(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance, Vector inward) {
        int lateralX = -inward.z;
        int lateralZ = inward.x;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int depth = 0; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                int x = entrance.x() + inward.x * depth + lateralX * lateral;
                int z = entrance.z() + inward.z * depth + lateralZ * lateral;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private static boolean insideEnvelope(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            Vector inward,
            int x,
            int z) {
        int dx = x - entrance.x();
        int dz = z - entrance.z();
        int depth = dx * inward.x + dz * inward.z;
        int lateral = dx * (-inward.z) + dz * inward.x;
        return depth >= 0 && depth <= DEPTH && Math.abs(lateral) <= HALF_WIDTH;
    }
'''
    new_methods = '''    private static boolean insideBounds(Bounds bounds, int x, int z) {
        return x >= bounds.minX && x <= bounds.maxX
                && z >= bounds.minZ && z <= bounds.maxZ;
    }
'''
    text = replace_once(text, old_methods, new_methods, "replace strip helpers")
    text = replace_once(text,
''' * fixtures are measured from the actual rotated schematic after streamed construction. The scan uses
 * exactly the same horizontal envelope as the current room converter, so it never requires chunks
 * that the converter itself would not already need and never synchronously loads terrain.</p>
''',
''' * fixtures are measured from the actual rotated schematic after streamed construction. The scan uses
 * the exact placed fragment footprint and retained source height. It still surveys loaded chunks only
 * and never synchronously loads terrain.</p>
''',
        "survey documentation")
    path.write_text(text, encoding="utf-8")


patch_placed()
patch_opportunities()
patch_runtime_survey()
print("Patched Living Kingdoms full-height/full-footprint urban topology")
