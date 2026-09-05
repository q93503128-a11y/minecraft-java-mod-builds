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
