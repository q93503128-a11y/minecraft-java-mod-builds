from pathlib import Path

p = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredInteriorSurvey.java')
s = p.read_text(encoding='utf-8')


def once(old: str, new: str, label: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    s = s.replace(old, new, 1)


once('public static final int SURVEY_REVISION = 2;',
     'public static final int SURVEY_REVISION = 3;', 'revision')
once(
'''    private static MinecraftServer activeServer;
    private static final Map<Long, Profile> PROFILES = new HashMap<>();
    private static boolean completionLogged;
''',
'''    private static MinecraftServer activeServer;
    private static final Map<Long, Profile> PROFILES = new HashMap<>();
    private static final Map<String, Integer> SOURCE_AUTHORED_BLOCK_COUNTS = new HashMap<>();
    private static boolean completionLogged;
''', 'source-count cache')
once(
'''        int stairs = 0;
        int doors = 0;
        int fixtures = 0;
        int authoredBlocks = 0;
        int minimumY = Math.max(level.getMinY(), placement.baseY() - 1);
        int maximumY = Math.min(
                level.getMaxY() - 1, placement.baseY() + placement.height() - 1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                for (int y = minimumY; y <= maximumY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    Block block = state.getBlock();
                    if (block instanceof StairBlock) stairs++;
                    if (block instanceof DoorBlock) doors++;
                    if (functionalFixture(block)) fixtures++;
                    if (authoredStructuralBlock(state)) authoredBlocks++;
                }
            }
        }
''',
'''        // Rotation does not change the inventory of retained source blocks. Counting the whole
        // 34x38x50+ world volume here used to issue tens of thousands of block-state reads in one
        // tick. Reachability above is still measured from the actual loaded world; immutable source
        // inventory comes from the exact fragment profile instead.
        ErdenUrbanPlacedTopologyCatalog.FragmentProfile source = placement.fragment();
        int stairs = source.stairBlocks();
        int doors = source.doors();
        int fixtures = source.functionalFixtures();
        int authoredBlocks = sourceAuthoredBlockCount(placement.fragmentKey());
''', 'volume scan')
once(
'''    private static boolean functionalFixture(Block block) {
''',
'''    private static int sourceAuthoredBlockCount(String fragmentKey) {
        return SOURCE_AUTHORED_BLOCK_COUNTS.computeIfAbsent(fragmentKey, key -> {
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics().get(key);
            if (snapshot == null) {
                throw new IllegalStateException("Missing source fragment for authored survey " + key);
            }
            int count = 0;
            for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
                if (authoredStructuralBlock(block.state())) count++;
            }
            return count;
        });
    }

    private static boolean functionalFixture(Block block) {
''', 'source count helper')
once(
''' * fixtures are measured from the actual rotated schematic after streamed construction. The scan uses
 * the exact placed fragment footprint and retained source height. It still surveys loaded chunks only
 * and never synchronously loads terrain.</p>
''',
''' * reachability is measured from the actual rotated schematic after streamed construction. Immutable
 * source inventory (stairs, doors, fixtures and authored structure blocks) comes from the exact retained
 * fragment profile instead of rereading the complete 3-D world volume on one tick. It surveys loaded
 * chunks only and never synchronously loads terrain.</p>
''', 'documentation')

p.write_text(s, encoding='utf-8')
print('Patched authored interior survey to bounded world reads')
