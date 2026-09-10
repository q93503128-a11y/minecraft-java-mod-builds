from pathlib import Path

path = Path('.github/scripts/ets_alpha14_apply.py')
body = path.read_text(encoding='utf-8')
old = r'''    if "ShipSystemsManager.loadSupply(\n                ship, type, player.level().getServer())" not in runtime:
        fail("hull service is not bound to the exact authoritative ship entry")'''
new = '''    if "ShipSystemsManager.loadSupply(" not in runtime or "ship, type, player.level().getServer())" not in runtime:
        fail("hull service is not bound to the exact authoritative ship entry")'''
count = body.count(old)
if count != 1:
    raise SystemExit(f'expected one alpha14 validator generator fix target, found {count}')
path.write_text(body.replace(old, new, 1), encoding='utf-8')
print('EARTH_TO_STARS_ALPHA14_RUNNER_FIX_READY')
