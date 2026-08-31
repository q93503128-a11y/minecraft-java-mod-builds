from pathlib import Path

ROOT = Path('projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement')
SERVICE = ROOT / 'settlement/SettlementService.java'
STORAGE = ROOT / 'settlement/SettlementStorageService.java'
CORE = ROOT / 'settlement/SettlementCoreService.java'


def must_replace(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one anchor, found {count}')
    return text.replace(old, new, 1)


def patch_service() -> None:
    s = SERVICE.read_text(encoding='utf-8')
    if 'import kr.moonseungjun.frontiersettlement.content.FrontierContent;' not in s:
        s = must_replace(
            s,
            'package kr.moonseungjun.frontiersettlement.settlement;\n\n',
            'package kr.moonseungjun.frontiersettlement.settlement;\n\n'
            'import kr.moonseungjun.frontiersettlement.content.FrontierContent;\n',
            'SettlementService import')
    s = must_replace(
        s,
        'if (stockpile == null) return new FoundResult(false, "표식 주변에 공동 창고를 둘 안전한 자리가 없습니다.");',
        'if (stockpile == null) return new FoundResult(false, "표식 주변에 공용 보급고를 둘 안전한 자리가 없습니다.");',
        'SettlementService safe position message')
    s = must_replace(
        s,
        'if (!level.setBlock(stockpile, Blocks.BARREL.defaultBlockState(), 3)\n'
        '                || !(level.getBlockEntity(stockpile) instanceof net.minecraft.world.Container)) {',
        'if (!level.setBlock(stockpile, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3)\n'
        '                || !(level.getBlockEntity(stockpile) instanceof net.minecraft.world.Container)) {',
        'SettlementService starter block')
    s = must_replace(
        s,
        'return new FoundResult(false, "공동 창고를 월드에 설치하지 못했습니다. 마을은 생성되지 않았습니다.");\n'
        '        }\n'
        '        data.found(center, stockpile);',
        'return new FoundResult(false, "공용 보급고를 월드에 설치하지 못했습니다. 마을은 생성되지 않았습니다.");\n'
        '        }\n'
        '        SupplyDepotRegistryService.tryRegister(level, stockpile);\n'
        '        data.found(center, stockpile);',
        'SettlementService register starter depot')
    s = must_replace(
        s,
        'return new FoundResult(true, "공동 개척지가 시작되었습니다. 자원을 창고에 넣고 건설 위치를 정해 마을을 키우세요.");',
        'return new FoundResult(true, "공동 개척지가 시작되었습니다. 자원을 공용 보급고에 넣고 건설 위치를 정해 마을을 키우세요.");',
        'SettlementService success message')
    SERVICE.write_text(s, encoding='utf-8')


def patch_storage() -> None:
    s = STORAGE.read_text(encoding='utf-8')
    if 'import kr.moonseungjun.frontiersettlement.content.FrontierContent;' not in s:
        s = must_replace(
            s,
            'import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;\n',
            'import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;\n'
            'import kr.moonseungjun.frontiersettlement.content.FrontierContent;\n',
            'SettlementStorageService import')

    old_constants = '''    private static final int PUBLIC_STOCKPILE_TARGET_BARRELS = 4;
    private static final int[][] PUBLIC_STOCKPILE_OFFSETS = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };
'''
    new_constants = '''    // Alpha.91 used these cells as a vanilla-barrel public-storage cluster. Keep the layout
    // only as a one-way save migration map. New settlements receive one 54-slot shared depot;
    // extra shared capacity is crafted and placed by the player.
    private static final int[][] LEGACY_PUBLIC_STOCKPILE_OFFSETS = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };
'''
    s = must_replace(s, old_constants, new_constants, 'SettlementStorageService constants')

    old_public = '''    /**
     * Public storage remains ordinary vanilla barrels. The first barrel is the persisted founding
     * stockpile; up to three safe neighboring cells are maintained as physical capacity annexes.
     * One vanilla barrel still has 27 slots, so the cluster provides up to 108 slots without a
     * custom block entity/menu/network protocol or save migration.
     */
    public static List<BlockPos> publicStockpilePositions(SettlementData data) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos origin = data.stockpilePos();
        for (int[] offset : PUBLIC_STOCKPILE_OFFSETS) {
            positions.add(origin.offset(offset[0], 0, offset[1]));
        }
        return positions;
    }
'''
    new_public = '''    /** The persisted founding stockpile is the dedicated shared supply depot. */
    public static List<BlockPos> publicStockpilePositions(SettlementData data) {
        return List.of(data.stockpilePos());
    }
'''
    s = must_replace(s, old_public, new_public, 'SettlementStorageService public stockpile')

    start = s.index('    /**\n     * Save-compatible backfill.')
    end = s.index('    private static boolean canSafelyCreateManagedBarrel', start)
    replacement = '''    /**
     * Save-compatible storage maintenance. The starter public stockpile is a 54-slot dedicated
     * shared supply depot. Alpha.91 public barrels are upgraded in-place with every ItemStack
     * preserved; profession worksite barrels remain local physical buffers.
     */
    public static void ensureManagedStorage(ServerLevel level, SettlementData data) {
        // One-way compatibility gate: only a world whose authoritative saved stockpile is still a
        // vanilla barrel can be an Alpha.91 public-barrel save. New settlements start with the
        // dedicated depot, so placing a cheap barrel near it can never mint a free shared depot.
        BlockPos stockpile = data.stockpilePos();
        boolean legacyPublicStorage = level.hasChunkAt(stockpile) && level.getBlockState(stockpile).is(Blocks.BARREL);
        if (legacyPublicStorage) upgradeLegacyPublicBarrels(level, data);
        ensureStarterSupplyDepot(level, stockpile);
        for (BlockPos pos : worksiteStoragePositions(data)) {
            if (!canSafelyCreateManagedBarrel(level, pos)) continue;
            level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
        }
    }

    private static void upgradeLegacyPublicBarrels(ServerLevel level, SettlementData data) {
        BlockPos origin = data.stockpilePos();
        for (int[] offset : LEGACY_PUBLIC_STOCKPILE_OFFSETS) {
            BlockPos pos = origin.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.BARREL)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container oldContainer)) continue;
            replaceBarrelWithSupplyDepot(level, pos, oldContainer);
        }
    }

    private static void ensureStarterSupplyDepot(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return;
        BlockState current = level.getBlockState(pos);
        if (current.is(FrontierContent.SUPPLY_DEPOT.get()) && level.getBlockEntity(pos) instanceof Container) {
            SupplyDepotRegistryService.tryRegister(level, pos);
            return;
        }
        if (current.is(Blocks.BARREL) && level.getBlockEntity(pos) instanceof Container oldContainer) {
            replaceBarrelWithSupplyDepot(level, pos, oldContainer);
            return;
        }
        BlockState below = level.getBlockState(pos.below());
        if (level.getBlockEntity(pos) != null) return;
        if (!current.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return;
        if (!current.isAir() && !current.canBeReplaced()) return;
        if (below.isAir() || below.canBeReplaced()) return;
        if (level.setBlock(pos, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3)
                && level.getBlockEntity(pos) instanceof Container) {
            SupplyDepotRegistryService.tryRegister(level, pos);
        }
    }

    private static void replaceBarrelWithSupplyDepot(ServerLevel level, BlockPos pos, Container oldContainer) {
        List<ItemStack> preserved = new ArrayList<>(oldContainer.getContainerSize());
        for (int slot = 0; slot < oldContainer.getContainerSize(); slot++) {
            preserved.add(oldContainer.getItem(slot).copy());
            oldContainer.setItem(slot, ItemStack.EMPTY);
        }
        oldContainer.setChanged();
        BlockState oldState = level.getBlockState(pos);
        boolean placed = level.setBlock(pos, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3);
        if (!placed || !(level.getBlockEntity(pos) instanceof Container replacement)) {
            level.setBlock(pos, oldState, 3);
            if (level.getBlockEntity(pos) instanceof Container rollback) restoreItems(rollback, preserved);
            return;
        }
        restoreItems(replacement, preserved);
        SupplyDepotRegistryService.tryRegister(level, pos);
    }

    private static void restoreItems(Container target, List<ItemStack> preserved) {
        int limit = Math.min(target.getContainerSize(), preserved.size());
        for (int slot = 0; slot < limit; slot++) target.setItem(slot, preserved.get(slot).copy());
        target.setChanged();
    }

'''
    s = s[:start] + replacement + s[end:]
    STORAGE.write_text(s, encoding='utf-8')


def patch_core() -> None:
    s = CORE.read_text(encoding='utf-8')
    if 'import kr.moonseungjun.frontiersettlement.content.FrontierContent;' not in s:
        s = must_replace(
            s,
            'package kr.moonseungjun.frontiersettlement.settlement;\n\n',
            'package kr.moonseungjun.frontiersettlement.settlement;\n\n'
            'import kr.moonseungjun.frontiersettlement.content.FrontierContent;\n',
            'SettlementCoreService import')
    s = must_replace(
        s,
        'if (SettlementStorageService.isManagedStoragePosition(data, pos) && current.is(Blocks.BARREL)) {',
        'if (SettlementStorageService.isManagedStoragePosition(data, pos)\n'
        '                && (current.is(Blocks.BARREL) || current.is(FrontierContent.SUPPLY_DEPOT.get()))) {',
        'SettlementCoreService managed storage protection')
    CORE.write_text(s, encoding='utf-8')


def validate() -> None:
    service = SERVICE.read_text(encoding='utf-8')
    storage = STORAGE.read_text(encoding='utf-8')
    core = CORE.read_text(encoding='utf-8')
    content = (ROOT / 'content/FrontierContent.java').read_text(encoding='utf-8')
    checks = {
        'founding uses shared depot': 'level.setBlock(stockpile, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3)' in service,
        'founding registers depot': 'SupplyDepotRegistryService.tryRegister(level, stockpile);' in service,
        'no automatic public barrel target': 'PUBLIC_STOCKPILE_TARGET_BARRELS' not in storage,
        'starter public storage single position': 'return List.of(data.stockpilePos());' in storage,
        'legacy barrels migrate': 'replaceBarrelWithSupplyDepot' in storage and 'LEGACY_PUBLIC_STOCKPILE_OFFSETS' in storage,
        'starter depot protected': 'current.is(FrontierContent.SUPPLY_DEPOT.get())' in core,
        'creative tab exposure retained': 'BuildCreativeModeTabContentsEvent' in content and 'SUPPLY_DEPOT_ITEM.get()' in content,
    }
    bad = [key for key, ok in checks.items() if not ok]
    if bad:
        raise SystemExit('invariant failure: ' + ', '.join(bad))
    print('STARTER SHARED SUPPLY DEPOT PATCH + INVARIANTS PASS')


if __name__ == '__main__':
    patch_service()
    patch_storage()
    patch_core()
    validate()
