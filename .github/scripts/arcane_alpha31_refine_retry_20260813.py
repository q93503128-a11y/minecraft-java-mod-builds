from pathlib import Path
import runpy

repo=Path(__file__).resolve().parents[2]
patch=repo/'.github/scripts/arcane_alpha31_refine_20260813.py'
text=patch.read_text(encoding='utf-8')
old="'meteor_swarm','runeRing','fusionFormula']:\n"
new="'meteor_swarm','fusionFormula']:\n"
if text.count(old)!=1:
    raise SystemExit(f'expected one stale runeRing audit token, found {text.count(old)}')
patch.write_text(text.replace(old,new,1),encoding='utf-8')
runpy.run_path(str(patch),run_name='__main__')
(repo/'.github/scripts/arcane_alpha31_refine_retry_20260813.py').unlink(missing_ok=True)
(repo/'.github/workflows/maintenance-arcane31-refine-retry-20260813.yml').unlink(missing_ok=True)
print('Arcane Circle alpha.31 retry wrapper complete')
