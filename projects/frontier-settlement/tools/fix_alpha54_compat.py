#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ROAD = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementRoadService.java'


def replace_once(old: str, new: str) -> None:
    text = ROAD.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'alpha.54 compatibility anchor expected once, found {count}: {old[:100]!r}')
    ROAD.write_text(text.replace(old, new, 1), encoding='utf-8')


replace_once(
'''        if (!placement.bridge() && !placement.portal()) {
            BlockState support = level.getBlockState(target.below());
            if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                builder.getNavigation().stop();
                return false;
            }
        }''',
'''        if (!placement.bridge()) {
            if (!placement.portal()) {
                BlockState support = level.getBlockState(target.below());
                if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                    builder.getNavigation().stop();
                    return false;
                }
            }
        }''')

replace_once(
'''            if (!placement.bridge() && !placement.portal()) {
                BlockState support = level.getBlockState(placement.pos().below());
                if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                    builder.getNavigation().stop();
                    return false;
                }
            }''',
'''            if (!placement.bridge()) {
                if (!placement.portal()) {
                    BlockState support = level.getBlockState(placement.pos().below());
                    if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                        builder.getNavigation().stop();
                        return false;
                    }
                }
            }''')

replace_once(
'''        if (level.getBlockEntity(cell.target()) != null || !current.getFluidState().isEmpty()
                || !isNaturalTunnelExcavation(current)) {''',
'''        if (level.getBlockEntity(cell.target()) != null || !current.getFluidState().isEmpty()
                || (!isNaturalTunnelExcavation(current) && !isNaturalTunnelPortalBlock(current))) {''')

print('Applied Alpha.54 legacy road-audit and portal-natural-block compatibility fix.')
