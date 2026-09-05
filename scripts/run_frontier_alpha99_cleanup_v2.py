from pathlib import Path

script = Path('scripts/apply_frontier_alpha99_authority_cleanup.py')
source = script.read_text(encoding='utf-8')
old = "    '    private static boolean placeClaimedTower(',"
new = "    '    private static void placeClaimedTower(',"
if source.count(old) != 1:
    raise SystemExit(f'placeClaimedTower driver patch expected one match, found {source.count(old)}')
source = source.replace(old, new, 1)
namespace = {'__name__': '__main__', '__file__': str(script)}
exec(compile(source, str(script), 'exec'), namespace)

# The Alpha98 integrity authority is percent-based, not a 0.45 double literal.
verifier = Path('projects/frontier-settlement/tools/test_current_source.py')
check = verifier.read_text(encoding='utf-8')
bad = 'require("0.45D" in integrity and "removeCompletedBuilding" in integrity, "Alpha98 house integrity authority regressed")'
good = 'require("RUIN_INTACT_PERCENT = 45" in integrity and "removeCompletedBuilding" in integrity and "clearKnownHouseRemnants" in integrity, "Alpha98 house integrity authority regressed")'
if check.count(bad) != 1:
    raise SystemExit(f'integrity verifier patch expected one match, found {check.count(bad)}')
verifier.write_text(check.replace(bad, good, 1), encoding='utf-8')
