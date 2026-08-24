#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
road = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementRoadService.java'
audit = ROOT / 'tools/test_alpha53_source.py'

def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    if text.count(old) != 1:
        raise SystemExit(f'{path}: expected one patch anchor, found {text.count(old)}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

replace_once(road,
'''            Placement placement = plan.get(i);
            BlockPos surface = placement.pos();
            if (surface.equals(pos) && current.is(placement.state().getBlock())) {''',
'''            Placement placement = plan.get(i);
            BlockPos surface = placement.pos();
            // The natural tunnel floor is future paving/support and must not be removed mid-project.
            if (placement.tunnel() && surface.equals(pos)) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
            if (surface.equals(pos) && current.is(placement.state().getBlock())) {''')

replace_once(audit,
'''            'for (TunnelCell cell : tunnelExcavationPlan(road))', 'cell.target().equals(pos)',
            'event.setCanceled(true)'), 'alpha.53 physical tunnel authority')''',
'''            'for (TunnelCell cell : tunnelExcavationPlan(road))', 'cell.target().equals(pos)',
            'placement.tunnel() && surface.equals(pos)', 'event.setCanceled(true)'), 'alpha.53 physical tunnel authority')''')

print('Applied Alpha.53 active tunnel-floor protection fix.')
