from pathlib import Path

RUNTIME = Path('projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementStorageService.java')
GENERATOR = Path('tools/apply_starter_shared_supply_depot.py')

OLD = '''    public static void ensureManagedStorage(ServerLevel level, SettlementData data) {
        upgradeLegacyPublicBarrels(level, data);
        ensureStarterSupplyDepot(level, data.stockpilePos());
'''
NEW = '''    public static void ensureManagedStorage(ServerLevel level, SettlementData data) {
        // One-way compatibility gate: only a world whose authoritative saved stockpile is still a
        // vanilla barrel can be an Alpha.91 public-barrel save. New settlements start with the
        // dedicated depot, so placing a cheap barrel near it can never mint a free shared depot.
        BlockPos stockpile = data.stockpilePos();
        boolean legacyPublicStorage = level.hasChunkAt(stockpile) && level.getBlockState(stockpile).is(Blocks.BARREL);
        if (legacyPublicStorage) upgradeLegacyPublicBarrels(level, data);
        ensureStarterSupplyDepot(level, stockpile);
'''


def replace_once(path: Path) -> None:
    text = path.read_text(encoding='utf-8')
    if NEW in text:
        return
    count = text.count(OLD)
    if count != 1:
        raise SystemExit(f'{path}: migration anchor count={count}')
    path.write_text(text.replace(OLD, NEW, 1), encoding='utf-8')


replace_once(RUNTIME)
replace_once(GENERATOR)

runtime = RUNTIME.read_text(encoding='utf-8')
for token in (
    'boolean legacyPublicStorage = level.hasChunkAt(stockpile)',
    'if (legacyPublicStorage) upgradeLegacyPublicBarrels(level, data);',
    'ensureStarterSupplyDepot(level, stockpile);',
):
    if token not in runtime:
        raise SystemExit(f'migration gate missing: {token}')

print('ONE-WAY STARTER DEPOT MIGRATION GATE PASS')
