from pathlib import Path

script = Path('scripts/apply_frontier_alpha99_authority_cleanup.py')
source = script.read_text(encoding='utf-8')

patches = [
    (
        "    '    private static boolean placeClaimedTower(',",
        "    '    private static void placeClaimedTower(',",
        'placeClaimedTower return type',
    ),
    (
        "s = replace_once(s, 'SettlementGuidanceService.nextGoal(data)', 'SettlementGuidanceService.nextGoal(server, data)', 'guidance server authority')",
        "s = replace_once(s, 'SettlementGuidanceService.nextGoal(data)', 'SettlementGuidanceService.nextGoal(player.level().getServer(), data)', 'guidance server authority')",
        'snapshot server context',
    ),
]
for old, new, label in patches:
    if source.count(old) != 1:
        raise SystemExit(f'{label}: expected one match, found {source.count(old)}')
    source = source.replace(old, new, 1)

namespace = {'__name__': '__main__', '__file__': str(script)}
exec(compile(source, str(script), 'exec'), namespace)

# Correct generated verifier details that deliberately track current source invariants.
verifier = Path('projects/frontier-settlement/tools/test_current_source.py')
check = verifier.read_text(encoding='utf-8')
replacements = [
    (
        'require("0.45D" in integrity and "removeCompletedBuilding" in integrity, "Alpha98 house integrity authority regressed")',
        'require("RUIN_INTACT_PERCENT = 45" in integrity and "removeCompletedBuilding" in integrity and "clearKnownHouseRemnants" in integrity, "Alpha98 house integrity authority regressed")',
        'integrity percent invariant',
    ),
    (
        'require("SettlementGuidanceService.nextGoal(server, data)" in service, "guidance is missing server authority")',
        'require("SettlementGuidanceService.nextGoal(player.level().getServer(), data)" in service, "guidance is missing server authority")',
        'guidance server invariant',
    ),
]
for old, new, label in replacements:
    if check.count(old) != 1:
        raise SystemExit(f'{label}: expected one match, found {check.count(old)}')
    check = check.replace(old, new, 1)
verifier.write_text(check, encoding='utf-8')
