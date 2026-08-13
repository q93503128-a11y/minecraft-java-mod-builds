from pathlib import Path
import runpy

repo=Path(__file__).resolve().parents[2]
patch=repo/'.github/scripts/arcane_alpha31_refine_20260813.py'
text=patch.read_text(encoding='utf-8')
old="'meteor_swarm','runeRing','fusionFormula']:"
new="'meteor_swarm','fusionFormula']:"
count=text.count(old)
if count!=1:
    raise SystemExit(f'expected exactly one stale runeRing audit token, found {count}')
patch.write_text(text.replace(old,new,1),encoding='utf-8')
runpy.run_path(str(patch),run_name='__main__')
for rel in [
    '.github/scripts/arcane_alpha31_refine_retry_20260813.py',
    '.github/workflows/maintenance-arcane31-refine-retry-20260813.yml',
    '.github/scripts/arcane_alpha31_refine_retry2_20260813.py',
    '.github/workflows/maintenance-arcane31-refine-retry2-20260813.yml',
]:
    (repo/rel).unlink(missing_ok=True)
print('Arcane Circle alpha.31 robust retry complete')
