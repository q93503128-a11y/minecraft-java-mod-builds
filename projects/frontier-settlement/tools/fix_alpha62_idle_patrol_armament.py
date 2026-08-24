#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = root / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementMilitaryOutpostService.java'
s = service.read_text(encoding='utf-8')
old = '''        Monster threat = nearestCombatThreat(level, outpost.center());
        if (threat != null) { sentry.setTarget(threat); return; }
        standDown(outpost, sentry);
'''
new = '''        Monster threat = nearestCombatThreat(level, outpost.center());
        if (threat != null) { sentry.setTarget(threat); return; }
        // The dangerous overlay may remain active because of darkness/creeper pressure even when
        // there is no immediate combat target. In that idle window, equip only from the local
        // stockpile; an actual combat target always wins before this branch.
        if (SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) return;
        standDown(outpost, sentry);
'''
if old not in s:
    if new not in s:
        raise SystemExit('alpha.62 patrol armament patch target missing')
else:
    s = s.replace(old, new, 1)
    service.write_text(s, encoding='utf-8')

audit = root / 'tools/test_alpha62_source.py'
a = audit.read_text(encoding='utf-8')
anchor = "print('Frontier Settlement alpha.23-62 cumulative source audit: PASS')"
check = """patrol=military.find('private static void patrol('); target=military.find('Monster threat = nearestCombatThreat',patrol); arm_idle=military.find('if (SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) return;',target); stand=military.find('standDown(outpost, sentry);',arm_idle)\nif min(patrol,target,arm_idle,stand)<0 or not (patrol < target < arm_idle < stand): raise SystemExit('alpha.62 immediate combat must precede local sentry armament and stand-down')\n"""
if check not in a:
    if anchor not in a:
        raise SystemExit('alpha.62 source audit print anchor missing')
    a = a.replace(anchor, check + anchor, 1)
    audit.write_text(a, encoding='utf-8')
print('Applied Alpha.62 idle-without-combat local sentry armament finalizer.')
